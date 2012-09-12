/*
 * Copyright (C) 2012 THM webMedia
 * 
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.services;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Document;
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Session;

@Service
public class SessionService implements ISessionService {

	@Autowired
	IUserService userService;
	
	private String databaseHost;
	private int databasePort;
	private String databaseName;
	
	private Database database;
	
	private Map<String, String> user2session = new ConcurrentHashMap<String, String>();
	
	public static final Logger logger = LoggerFactory.getLogger(SessionService.class);

	@Value("#{props['couchdb.host']}")
	public final void setDatabaseHost(String databaseHost) {
		this.databaseHost = databaseHost;
	}
	
	@Value("#{props['couchdb.port']}")
	public final void setDatabasePort(String databasePort) {
		this.databasePort = Integer.parseInt(databasePort);
	}
	
	@Value("#{props['couchdb.name']}")
	public final void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	@Override
	public Session getSession(String keyword) {
		View view = new View("session/by_keyword");
		view.setKey(URLEncoder.encode("\"" + keyword + "\""));
		ViewResults results = this.getDatabase().view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null)
			return null;

		Session result = (Session) JSONObject.toBean(
				results.getJSONArray("rows").optJSONObject(0)
						.optJSONObject("value"), Session.class);

		if (result.isActive() || result.getCreator().equals(this.actualUserName())) {
			this.addUserToSessionMap(this.actualUserName(), keyword);
			return result;
		}
		
		return null;
	}
	
	@Override
	public Session saveSession(Session session) {
		
		Document sessionDocument = new Document();
		sessionDocument.put("type","session");
		sessionDocument.put("name", session.getName());
		sessionDocument.put("shortName", session.getShortName());
		sessionDocument.put("keyword", this.generateKeyword());
		sessionDocument.put("creator", this.actualUserName());
		sessionDocument.put("active", true);
		try {
			database.saveDocument(sessionDocument);
		} catch (IOException e) {
			return null;
		}
		
		return this.getSession(sessionDocument.getString("keyword"));
	}

	@Override
	public Feedback getFeedback(String keyword) {
		String sessionId = this.getSessionId(keyword);
		if (sessionId == null)
			return null;

		logger.info("Time: {}", this.currentTimestamp());

		View view = new View("understanding/by_session");
		view.setGroup(true);
		view.setStartKey(URLEncoder.encode("[\"" + sessionId + "\"]"));
		view.setEndKey(URLEncoder.encode("[\"" + sessionId + "\",{}]"));
		ViewResults results = this.getDatabase().view(view);

		logger.info("Feedback: {}", results.getJSONArray("rows"));

		int values[] = { 0, 0, 0, 0 };
		List<Integer> result = new ArrayList<Integer>();
		
		try {
			for (int i = 0; i <= 3; i++) {
				String key = results.getJSONArray("rows").optJSONObject(i)
						.optJSONArray("key").getString(1);
				if (key.equals("Bitte schneller"))
					values[0] = results.getJSONArray("rows").optJSONObject(i)
							.getInt("value");
				if (key.equals("Kann folgen"))
					values[1] = results.getJSONArray("rows").optJSONObject(i)
							.getInt("value");
				if (key.equals("Zu schnell"))
					values[2] = results.getJSONArray("rows").optJSONObject(i)
							.getInt("value");
				if (key.equals("Nicht mehr dabei"))
					values[3] = results.getJSONArray("rows").optJSONObject(i)
							.getInt("value");
			}
		} catch (Exception e) {
			return new Feedback(
					values[0],
					values[1],
					values[2],
					values[3]
			);
		}

		return new Feedback(
				values[0],
				values[1],
				values[2],
				values[3]
		);
	}

	@Override
	public boolean postFeedback(String keyword, int value, de.thm.arsnova.entities.User user) {
		String sessionId = this.getSessionId(keyword);
		if (sessionId == null) return false;
		
		Document feedback = new Document();
		feedback.put("type", "understanding");
		feedback.put("user", user.getUsername());
		feedback.put("sessionId", sessionId);
		feedback.put("timestamp", System.currentTimeMillis());
		
		switch (value) {
			case 0:
				feedback.put("value", "Bitte schneller");
				break;
			case 1:
				feedback.put("value", "Kann folgen");
				break;
			case 2:
				feedback.put("value", "Zu schnell");
				break;
			case 3:
				feedback.put("value", "Nicht mehr dabei");
				break;
			default:
				return false;
		}
		
		try {
			this.getDatabase().saveDocument(feedback);
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}

	@Override
	@Transactional(isolation=Isolation.READ_COMMITTED)
	public boolean sessionKeyAvailable(String keyword) {
		View view = new View("session/by_keyword");
		ViewResults results = this.getDatabase().view(view);
		
		return ! results.containsKey(keyword);
	}
	
	@Override
	public boolean isUserInSession(de.thm.arsnova.entities.User user, String keyword) {
		if (keyword == null) return false;
		return (this.user2session.get(user.getUsername()).equals(keyword));
	}
	
	@Override
	public void addUserToSessionMap(String username, String keyword) {
		this.user2session.put(username, keyword);	
	}
	
	private String getSessionId(String keyword) {
		View view = new View("session/by_keyword");
		view.setKey(URLEncoder.encode("\"" + keyword + "\""));
		ViewResults results = this.getDatabase().view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null)
			return null;

		return results.getJSONArray("rows").optJSONObject(0)
				.optJSONObject("value").getString("_id");
	}

	private String currentTimestamp() {
		return Long.toString(System.currentTimeMillis());
	}
	
	private String actualUserName() {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			User user = (User) authentication.getPrincipal();
			return user.getUsername();
		} catch (ClassCastException e) {}
		return null;
	}

	@Override
	public String generateKeyword() {
		final int low = 10000000;
		final int high = 100000000;
		String keyword = String.valueOf((int)(Math.random() * (high - low) + low));
		
		if (this.sessionKeyAvailable(keyword)) return keyword;
		return generateKeyword();
	}
	
	private Database getDatabase() {
		if (database == null) {
			try {
				com.fourspaces.couchdb.Session session = new com.fourspaces.couchdb.Session(
						databaseHost,
						databasePort
					);
					
				database = session.getDatabase(databaseName);
			} catch (Exception e) {
				logger.error(
					"Cannot connect to CouchDB database '"
					+ databaseName
					+"' on host '"
					+ databaseHost
					+ "' using port "
					+ databasePort
				);
			}
		}
		
		return database;
	}
}
