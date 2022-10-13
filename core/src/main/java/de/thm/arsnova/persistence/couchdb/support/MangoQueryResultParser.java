/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.persistence.couchdb.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public MangoQueryResultParser(final Class<T> type, final ObjectMapper objectMapper) {
    this.type = type;
    this.objectMapper = objectMapper;
  }

  public MangoQueryResultParser(final String propertyName, final Class<T> type, final ObjectMapper objectMapper) {
    this.propertyName = propertyName;
    this.type = type;
    this.objectMapper = objectMapper;
  }

  public void parseResult(final InputStream json) throws IOException {
    final JsonParser jp = objectMapper.getFactory().createParser(json);

    try {
      parseResult(jp);
    } finally {
      jp.close();
    }
  }

  private void parseResult(final JsonParser jp) throws IOException {
    if (jp.nextToken() != JsonToken.START_OBJECT) {
      throw new DbAccessException("Expected data to start with an Object");
    }

    String error = null;
    String reason = null;

    // Issue #98: Can't assume order of JSON fields.
    while (jp.nextValue() != JsonToken.END_OBJECT) {
      final String currentName = jp.getCurrentName();
      if (DOCS_FIELD_NAME.equals(currentName)) {
        docs = new ArrayList<>();
        try {
          parseDocs(jp);
        } catch (final JsonMappingException e) {
          logger.error("Failed to map document at index {}.", docs.size());
          throw e;
        }
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
      final String errorDesc = reason != null ? reason : error;
      throw new DbAccessException("CouchDB Mango query failed: " + errorDesc);
    }
  }

  private void parseDocs(final JsonParser jp) throws IOException {
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
          final String fieldName = jp.getText();
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
