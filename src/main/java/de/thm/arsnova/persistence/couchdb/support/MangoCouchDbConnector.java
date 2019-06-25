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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpResponse;
import org.ektorp.http.StdResponseHandler;
import org.ektorp.impl.ObjectMapperFactory;
import org.ektorp.impl.StdCouchDbConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thm.arsnova.model.serialization.View;

/**
 * This Connector adds a query method which uses CouchDB's Mango API to retrieve data.
 */
public class MangoCouchDbConnector extends StdCouchDbConnector {
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	/**
	 * Represents a <code>_find</code> query for CouchDB's Mango API.
	 * See http://docs.couchdb.org/en/stable/api/database/find.html#db-find.
	 */
	public static class MangoQuery {
		@JsonSerialize(converter = Sort.ToMapConverter.class)
		public static class Sort {
			public static class ToMapConverter implements Converter<Sort, Map<String, String>> {
				@Override
				public Map<String, String> convert(Sort value) {
					Map<String, String> map = new HashMap<>();
					map.put(value.field, value.descending ? "desc" : "asc");

					return map;
				}

				@Override
				public JavaType getInputType(TypeFactory typeFactory) {
					return typeFactory.constructType(Sort.class);
				}

				@Override
				public JavaType getOutputType(TypeFactory typeFactory) {
					return typeFactory.constructMapType(Map.class, String.class, String.class);
				}
			}

			private String field;
			private boolean descending = false;

			public Sort(String field, boolean descending) {
				this.field = field;
				this.descending = descending;
			}

			public String getField() {
				return field;
			}

			public void setField(String field) {
				this.field = field;
			}

			public boolean isDescending() {
				return descending;
			}

			public void setDescending(boolean descending) {
				this.descending = descending;
			}
		}

		private Map<String, Object> selector;
		private List<String> fields = new ArrayList<>();
		private List<Sort> sort = new ArrayList<>();
		private int limit = 0;
		private int skip = 0;
		private String indexDocument;
		private String indexName;
		private boolean update = true;
		private boolean stable = false;
		private String bookmark;

		public MangoQuery() {
			this.selector = new HashMap<>();
		}

		/**
		 * @param selector See http://docs.couchdb.org/en/stable/api/database/find.html#selector-syntax.
		 */
		public MangoQuery(Map<String, Object> selector) {
			this.selector = selector;
		}

		@JsonInclude(JsonInclude.Include.ALWAYS)
		@JsonView(View.Persistence.class)
		public Map<String, ?> getSelector() {
			return selector;
		}

		/**
		 * @param selector See http://docs.couchdb.org/en/stable/api/database/find.html#selector-syntax.
		 */
		public void setSelector(Map<String, Object> selector) {
			this.selector = selector;
		}

		@JsonView(View.Persistence.class)
		public List<String> getFields() {
			return fields;
		}

		public void setFields(List<String> fields) {
			this.fields = fields;
		}

		@JsonView(View.Persistence.class)
		public List<Sort> getSort() {
			return sort;
		}

		public void setSort(List<Sort> sort) {
			this.sort = sort;
		}

		@JsonView(View.Persistence.class)
		public int getLimit() {
			return limit;
		}

		public void setLimit(int limit) {
			this.limit = limit;
		}

		@JsonView(View.Persistence.class)
		public int getSkip() {
			return skip;
		}

		public void setSkip(int skip) {
			this.skip = skip;
		}

		public String getIndexDocument() {
			return indexDocument;
		}

		public void setIndexDocument(String indexDocument) {
			this.indexDocument = indexDocument;
		}

		public String getIndexName() {
			return indexName;
		}

		public void setIndexName(String indexName) {
			this.indexName = indexName;
		}

		@JsonView(View.Persistence.class)
		@JsonProperty("use_index")
		public Object getIndex() {
			return indexName != null ? new String[] {indexDocument, indexName} : indexDocument;
		}

		@JsonView(View.Persistence.class)
		public boolean isUpdate() {
			return update;
		}

		public void setUpdate(boolean update) {
			this.update = update;
		}

		@JsonView(View.Persistence.class)
		public boolean isStable() {
			return stable;
		}

		public void setStable(boolean stable) {
			this.stable = stable;
		}

		@JsonView(View.Persistence.class)
		public String getBookmark() {
			return bookmark;
		}

		public void setBookmark(final String bookmark) {
			this.bookmark = bookmark;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(MangoCouchDbConnector.class);

	public MangoCouchDbConnector(String databaseName, CouchDbInstance dbInstance) {
		super(databaseName, dbInstance);
	}

	public MangoCouchDbConnector(String databaseName, CouchDbInstance dbi, ObjectMapperFactory om) {
		super(databaseName, dbi, om);
	}

	/**
	 *
	 * @param query The query sent to CouchDB's Mango API
	 * @param rh Handler for the response to the query
	 * @return List of retrieved entities
	 */
	public <T> List<T> query(final MangoQuery query, final MangoResponseHandler<T> rh) {
		String queryString;
		try {
			queryString = objectMapper.writeValueAsString(query);
			logger.debug("Querying CouchDB using Mango API: {}", queryString);
		} catch (JsonProcessingException e) {
			throw new DbAccessException("Could not serialize Mango query.");
		}
		List<T> result = restTemplate.postUncached(dbURI.append("_find").toString(), queryString, rh);
		//List<T> result = restTemplate.post(dbURI.append("_find").toString(),
		//		new JacksonableEntity(query, objectMapper), rh);

		logger.debug("Answer from CouchDB Mango query: {}", result);

		return result;
	}

	/**
	 *
	 * @param query The query sent to CouchDB's Mango API
	 * @param type Type for deserialization of retrieved entities
	 * @return List of retrieved entities
	 */
	public <T> List<T> query(final MangoQuery query, final Class<T> type) {
		MangoResponseHandler<T> rh = new MangoResponseHandler<>(type, objectMapper, true);
		return query(query, rh);
	}

	/**
	 *
	 * @param query The query sent to CouchDB's Mango API
	 * @param propertyName Name of the entity's property to be parsed
	 * @param type Type for deserialization of retrieved entities
	 * @return List of retrieved entities
	 */
	public <T> List<T> query(final MangoQuery query, final String propertyName, final Class<T> type) {
		query.setFields(Arrays.asList(new String[] {propertyName}));
		MangoResponseHandler<T> rh = new MangoResponseHandler<>(propertyName, type, objectMapper);

		return query(query, rh);
	}

	/**
	 *
	 * @param query The query sent to CouchDB's Mango API
	 * @param type Type for deserialization of retrieved entities
	 * @return List of retrieved entities
	 */
	public <T> PagedMangoResponse<T> queryForPage(final MangoQuery query, final Class<T> type) {
		MangoResponseHandler<T> rh = new MangoResponseHandler<>(type, objectMapper, true);
		return new PagedMangoResponse<T>(query(query, rh), rh.getBookmark());
	}

	public void createPartialJsonIndex(
			final String name, final List<MangoQuery.Sort> fields, final Map<String, Object> filterSelector) {
		Map<String, Object> query = new HashMap<>();
		Map<String, Object> index = new HashMap<>();
		query.put("ddoc", name);
		query.put("type", "json");
		query.put("index", index);
		index.put("fields", fields);
		if (filterSelector != null) {
			index.put("partial_filter_selector", filterSelector);
		}
		String queryString;
		try {
			queryString = objectMapper.writeValueAsString(query);
			logger.debug("Creating CouchDB index using Mango API: {}", queryString);
		} catch (JsonProcessingException e) {
			throw new DbAccessException(e);
		}
		restTemplate.postUncached(dbURI.append("_index").toString(), queryString);
	}

	public void createJsonIndex(final String name, final List<MangoQuery.Sort> fields) {
		createPartialJsonIndex(name, fields, null);
	}

	public boolean initializeIndex(final String name) {
		MangoQuery query = new MangoQuery(Collections.EMPTY_MAP);
		query.setIndexDocument(name);
		query.setLimit(0);
		try {
			String queryString = objectMapper.writeValueAsString(query);
			logger.debug("Using Mango API query to initialize CouchDB index: {}", queryString);
			HttpResponse response = restTemplate.postUncached(dbURI.append("_find").toString(), queryString);
			response.releaseConnection();
		} catch (JsonProcessingException e) {
			throw new DbAccessException("Could not serialize Mango query.");
		} catch (DbAccessException e) {
			logger.debug("CouchDB index is not ready yet: {}", name, e);
			return false;
		}

		return true;
	}

	/* DbInfo type and getDbInfo() of Ektorp do not handle purge_seq of CouchDB 2.x correctly, so this workaround is
	 * needed. */
	@Override
	public DbInfo getDbInfo() {
		return restTemplate.get(dbURI.toString(),
				new StdResponseHandler<DbInfo>() {

					@Override
					public DbInfo success(HttpResponse hr) throws Exception {
						return objectMapper.readValue(hr.getContent(),
								DbInfo.class);
					}
				}
		);
	}

	@JsonIgnoreProperties("purge_seq")
	public static class DbInfo extends org.ektorp.DbInfo {
		private String purgeSeq;

		@JsonCreator
		public DbInfo(@JsonProperty("db_name") String dbName) {
			super(dbName);
		}
	}
}
