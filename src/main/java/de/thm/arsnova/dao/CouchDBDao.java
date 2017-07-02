/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.dao;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.Document;
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.entities.*;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.MotdRepository;
import de.thm.arsnova.persistance.SessionRepository;
import de.thm.arsnova.services.ISessionService;
import net.sf.json.JSONObject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Database implementation based on CouchDB.
 *
 * Note to developers:
 *
 * This class makes use of Spring Framework's caching annotations. When you are about to add new functionality,
 * you should also think about the possibility of caching. Ideally, your methods should be dependent on domain
 * objects like Session or Content, which can be used as cache keys. Relying on plain String objects as a key, e.g.
 * by passing only a Session's keyword, will make your cache annotations less readable. You will also need to think
 * about cases where your cache needs to be updated and evicted.
 *
 * In order to use cached methods from within this object, you have to use the getDatabaseDao() method instead of
 * using the "this" pointer. This is because caching is only available if the method is called through a Spring Proxy,
 * which is not the case when using "this".
 *
 * @see <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/cache.html">Spring Framework's Cache Abstraction</a>
 * @see <a href="https://github.com/thm-projects/arsnova-backend/wiki/Caching">Caching in ARSnova explained</a>
 */
@Profile("!test")
@Service("databaseDao")
public class CouchDBDao implements IDatabaseDao {

	@Autowired
	private ISessionService sessionService;

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private SessionRepository sessionRepository;

	@Autowired
	private MotdRepository motdRepository;

	@Autowired
	private ContentRepository contentRepository;

	private String databaseHost;
	private int databasePort;
	private String databaseName;
	private Database database;

	private static final Logger logger = LoggerFactory.getLogger(CouchDBDao.class);

	@Value("${couchdb.host}")
	public void setDatabaseHost(final String newDatabaseHost) {
		databaseHost = newDatabaseHost;
	}

	@Value("${couchdb.port}")
	public void setDatabasePort(final String newDatabasePort) {
		databasePort = Integer.parseInt(newDatabasePort);
	}

	@Value("${couchdb.name}")
	public void setDatabaseName(final String newDatabaseName) {
		databaseName = newDatabaseName;
	}

	public void setSessionService(final ISessionService service) {
		sessionService = service;
	}

	/**
	 * <strike>
	 * Allows access to the proxy object. It has to be used instead of <code>this</code> for local calls to public
	 * methods for caching purposes. This is an ugly but necessary temporary workaround until a better solution is
	 * implemented (e.g. use of AspectJ's weaving).
	 * @return the proxy for CouchDBDao
	 * </strike>
	 */
	private @NonNull IDatabaseDao getDatabaseDao() {
		return this;
	}

	private Database getDatabase() {
		if (database == null) {
			try {
				final com.fourspaces.couchdb.Session session = new com.fourspaces.couchdb.Session(
						databaseHost,
						databasePort
						);
				database = session.getDatabase(databaseName);
			} catch (final Exception e) {
				logger.error("Cannot connect to CouchDB database '{}' on host '{}' using port {}.",
						databaseName, databaseHost, databasePort, e);
			}
		}

		return database;
	}

	private void deleteDocument(final String documentId) throws IOException {
		final Document d = getDatabase().getDocument(documentId);
		getDatabase().deleteDocument(d);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getObjectFromId(final String documentId, final Class<T> klass) {
		try {
			final Document doc = getDatabase().getDocument(documentId);
			if (doc == null) {
				return null;
			}
			// TODO: This needs some more error checking...
			return (T) JSONObject.toBean(doc.getJSONObject(), klass);
		} catch (ClassCastException | IOException | net.sf.json.JSONException e) {
			return null;
		}
	}

	private boolean isEmptyResults(final ViewResults results) {
		return results == null || results.getResults().isEmpty() || results.getJSONArray("rows").isEmpty();
	}

	/**
	 * Adds convenience methods to CouchDB4J's view class.
	 */
	private static class ExtendedView extends View {

		ExtendedView(final String fullname) {
			super(fullname);
		}

		void setCourseIdKeys(final List<Course> courses) {
			List<String> courseIds = new ArrayList<>();
			for (Course c : courses) {
				courseIds.add(c.getId());
			}
			setKeys(courseIds);
		}

		void setSessionIdKeys(final List<Session> sessions) {
			List<String> sessionIds = new ArrayList<>();
			for (Session s : sessions) {
				sessionIds.add(s.getId());
			}
			setKeys(sessionIds);
		}
	}
}
