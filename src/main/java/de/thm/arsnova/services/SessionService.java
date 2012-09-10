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

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private final com.fourspaces.couchdb.Session session = new com.fourspaces.couchdb.Session(
			"localhost", 5984);
	private final Database database = session.getDatabase("arsnova");

	public static final Logger logger = LoggerFactory
			.getLogger(SessionService.class);

	@Override
	public Session getSession(String keyword) {
		View view = new View("session/by_keyword");
		view.setKey(URLEncoder.encode("\"" + keyword + "\""));
		ViewResults results = database.view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null)
			return null;

		Session result = (Session) JSONObject.toBean(
				results.getJSONArray("rows").optJSONObject(0)
						.optJSONObject("value"), Session.class);

		if (result.isActive() || result.getCreator().equals(this.actualUserName())) {
			return result;
		}
		
		return null;
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
		ViewResults results = database.view(view);

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
	public boolean postFeedback(String keyword, int value) {
		String sessionId = this.getSessionId(keyword);
		if (sessionId == null) return false;
		
		Document feedback = new Document();
		feedback.put("type", "understanding");
		feedback.put("user", this.actualUserName());
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
			database.saveDocument(feedback);
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}

	@Override
	@Transactional(isolation=Isolation.READ_COMMITTED)
	public boolean sessionKeyAvailable(String keyword) {
		View view = new View("session/by_keyword");
		ViewResults results = database.view(view);
		
		return ! results.containsKey(keyword);
	}
	
	private String getSessionId(String keyword) {
		View view = new View("session/by_keyword");
		view.setKey(URLEncoder.encode("\"" + keyword + "\""));
		ViewResults results = database.view(view);

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

	public String generateKeyword() {
		// Generates a number between >=low and <high, so our keyword has exactly 8 digits.
		final int low = 10000000;
		final int high = 100000000;
		return String.valueOf((int)(Math.random() * (high - low) + low));
	}
}
