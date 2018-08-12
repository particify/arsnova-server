package de.thm.arsnova.persistence.couchdb.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MangoQueryResultParser<T> {
	private static final String DOCS_FIELD_NAME = "docs";
	private static final String BOOKMARK_FIELD_NAME = "bookmark";
	private static final String WARNING_FIELD_NAME = "warning";
	private static final String ERROR_FIELD_NAME = "error";
	private static final String REASON_FIELD_NAME = "reason";

	private static final Logger logger = LoggerFactory.getLogger(MangoQueryResultParser.class);

	private Class<T> type;
	private ObjectMapper objectMapper;
	private List<T> docs;
	private String bookmark;
	private String propertyName = null;

	public MangoQueryResultParser(Class<T> type, ObjectMapper objectMapper) {
		this.type = type;
		this.objectMapper = objectMapper;
	}

	public MangoQueryResultParser(String propertyName, Class<T> type, ObjectMapper objectMapper) {
		this.propertyName = propertyName;
		this.type = type;
		this.objectMapper = objectMapper;
	}

	public void parseResult(InputStream json) throws IOException {
		JsonParser jp = objectMapper.getFactory().createParser(json);

		try {
			parseResult(jp);
		} finally {
			jp.close();
		}
	}

	private void parseResult(JsonParser jp) throws IOException {
		if (jp.nextToken() != JsonToken.START_OBJECT) {
			throw new DbAccessException("Expected data to start with an Object");
		}

		String error = null;
		String reason = null;

		// Issue #98: Can't assume order of JSON fields.
		while (jp.nextValue() != JsonToken.END_OBJECT) {
			String currentName = jp.getCurrentName();
			if (DOCS_FIELD_NAME.equals(currentName)) {
				docs = new ArrayList<T>();
				parseDocs(jp);
			} else if (BOOKMARK_FIELD_NAME.equals(currentName)) {
				bookmark = jp.getText();
			} else if (WARNING_FIELD_NAME.equals(currentName)) {
				logger.warn("Warning for CouchDB Mango query: {}", jp.getText());
			} else if (ERROR_FIELD_NAME.equals(currentName)) {
				error = jp.getText();
			} else if (REASON_FIELD_NAME.equals(currentName)) {
				reason = jp.getText();
			}
		}

		if (error != null) {
			String errorDesc = reason != null ? reason : error;
			throw new DbAccessException("CouchDB Mango query failed: " + errorDesc);
		}
	}

	private void parseDocs(JsonParser jp) throws IOException {
		if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
			throw new DbAccessException("Expected rows to start with an Array");
		}

		while (jp.nextToken() == JsonToken.START_OBJECT) {
			T doc = null;
			if (propertyName == null) {
				doc = jp.readValueAs(type);
				docs.add(doc);
			} else {
				while (jp.nextToken() == JsonToken.FIELD_NAME) {
					String fieldName = jp.getText();
					jp.nextToken();
					if (fieldName.equals(propertyName)) {
						doc = jp.readValueAs(type);
						docs.add(doc);
					}
				}
				if (doc == null) {
					throw new DbAccessException("Cannot parse response from CouchDB. Property is missing.");
				}
				if (jp.currentToken() != JsonToken.END_OBJECT) {
					throw new DbAccessException("Cannot parse response from CouchDB. Unexpected data.");
				}
			}
		}

		if (jp.currentToken() != JsonToken.END_ARRAY) {
			throw new DbAccessException("Cannot parse response from CouchDB. Unexpected data.");
		}
	}

	public List<T> getDocs() {
		return docs;
	}

	public String getBookmark() {
		return bookmark;
	}
}
