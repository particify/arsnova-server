package de.thm.arsnova.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.couchbase.core.CouchbaseOperations;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;

import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

/**
 * View-based implementation of repository for interposed questions.
 */
public class InterposedQuestionRepositoryImpl implements InterposedQuestionRepositoryCustom {

	private static final String DESIGN_DOCUMENT = "interposedquestion";

	@Autowired
	private CouchbaseOperations ops;

	@Override
	public InterposedReadingCount countReadingBySessionAndCreator(Session session, User creator) {
		ViewQuery query = ViewQuery.from(DESIGN_DOCUMENT, "count_by_session_reading_for_creator")
		.startKey(JsonArray.from(session.get_id(), creator.getUsername()))
		.endKey(JsonArray.from(session.get_id(), creator.getUsername(), JsonObject.empty()))
		.reduce()
		.group();

		return executeReadingView(query);
	}

	@Override
	public InterposedReadingCount countReadingBySession(Session session) {
		ViewQuery query = ViewQuery.from(DESIGN_DOCUMENT, "count_by_session_reading_for_creator")
		.startKey(JsonArray.from(session.get_id()))
		.endKey(JsonArray.from(session.get_id(), JsonObject.empty()))
		.reduce()
		.group();

		return executeReadingView(query);
	}

	private InterposedReadingCount executeReadingView(ViewQuery query) {
		ViewResult response = ops.queryView(query);
		if (!response.success()) {
			return new InterposedReadingCount();
		}

		long readCount = 0;
		long unreadCount = 0;
		for (ViewRow row : response) {
			JsonArray key = (JsonArray) row.key();
			long value = Long.parseLong(row.value().toString());
			if (key.get(2).equals("unread")) {
				unreadCount += value;
			} else {
				readCount += value;
			}
		}
		return new InterposedReadingCount(readCount, unreadCount);
	}
}
