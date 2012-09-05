package de.thm.arsnova.services;

import java.net.URLEncoder;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fourspaces.couchdb.Database;
import com.fourspaces.couchdb.View;
import com.fourspaces.couchdb.ViewResults;

import de.thm.arsnova.entities.Session;

@Service
public class SessionService implements ISessionService {

	private final com.fourspaces.couchdb.Session session = new com.fourspaces.couchdb.Session("localhost", 5984);
	public static final Logger logger = LoggerFactory
			.getLogger(SessionService.class);

	@Override
	public Session getSession(String keyword) {
		Database db = session.getDatabase("arsnova");
		View view = new View("session/by_keyword");
		view.setKey(URLEncoder.encode("\""+keyword+"\""));
		ViewResults results = db.view(view);

		if (results.getJSONArray("rows").optJSONObject(0) == null) return null;
		
		Session result = (Session)JSONObject.toBean(results.getJSONArray("rows").optJSONObject(0).optJSONObject("value"), Session.class);
		return result;
	}

}
