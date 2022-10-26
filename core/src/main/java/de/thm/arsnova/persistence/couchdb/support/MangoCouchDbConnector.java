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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentOperationResult;
import org.ektorp.Options;
import org.ektorp.ViewQuery;
import org.ektorp.http.HttpResponse;
import org.ektorp.http.ResponseCallback;
import org.ektorp.http.StdResponseHandler;
import org.ektorp.impl.ObjectMapperFactory;
import org.ektorp.impl.StdCouchDbConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import de.thm.arsnova.event.ReadRepositoryEvent;
import de.thm.arsnova.model.Entity;
import de.thm.arsnova.model.serialization.View;

/**
 * This Connector adds a query method which uses CouchDB's Mango API to retrieve data.
 */
public class MangoCouchDbConnector extends StdCouchDbConnector implements ApplicationEventPublisherAware {
	/**
	 * Represents a <code>_find</code> query for CouchDB's Mango API.
	 * See http://docs.couchdb.org/en/stable/api/database/find.html#db-find.
	 */
	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	public static class MangoQuery {
		@JsonSerialize(converter = Sort.ToMapConverter.class)
		public static class Sort {
			public static class ToMapConverter implements Converter<Sort, Map<String, String>> {
				@Override
				public Map<String, String> convert(final Sort value) {
					final Map<String, String> map = new HashMap<>();
					map.put(value.field, value.descending ? "desc" : "asc");

					return map;
				}

				@Override
				public JavaType getInputType(final TypeFactory typeFactory) {
					return typeFactory.constructType(Sort.class);
				}

				@Override
				public JavaType getOutputType(final TypeFactory typeFactory) {
					return typeFactory.constructMapType(Map.class, String.class, String.class);
				}
			}

			private String field;
			private boolean descending = false;

			public Sort(final String field, final boolean descending) {
				this.field = field;
				this.descending = descending;
			}

			public String getField() {
				return field;
			}

			public void setField(final String field) {
				this.field = field;
			}

			public boolean isDescending() {
				return descending;
			}

			public void setDescending(final boolean descending) {
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
		 * Create a {@link MangoQuery} from criteria used to select documents.
		 *
		 * @param selector See http://docs.couchdb.org/en/stable/api/database/find.html#selector-syntax.
		 */
		public MangoQuery(final Map<String, Object> selector) {
			this.selector = selector;
		}

		@JsonInclude(JsonInclude.Include.ALWAYS)
		@JsonView(View.Persistence.class)
		public Map<String, ?> getSelector() {
			return selector;
		}

		/**
		 * Set a Map with criteria used to select documents.
		 *
		 * @param selector See http://docs.couchdb.org/en/stable/api/database/find.html#selector-syntax.
		 */
		public void setSelector(final Map<String, Object> selector) {
			this.selector = selector;
		}

		@JsonView(View.Persistence.class)
		public List<String> getFields() {
			return fields;
		}

		public void setFields(final List<String> fields) {
			this.fields = fields;
		}

		@JsonView(View.Persistence.class)
		public List<Sort> getSort() {
			return sort;
		}

		public void setSort(final List<Sort> sort) {
			this.sort = sort;
		}

		@JsonView(View.Persistence.class)
		public int getLimit() {
			return limit;
		}

		public void setLimit(final int limit) {
			this.limit = limit;
		}

		@JsonView(View.Persistence.class)
		public int getSkip() {
			return skip;
		}

		public void setSkip(final int skip) {
			this.skip = skip;
		}

		public String getIndexDocument() {
			return indexDocument;
		}

		public void setIndexDocument(final String indexDocument) {
			this.indexDocument = indexDocument;
		}

		public String getIndexName() {
			return indexName;
		}

		public void setIndexName(final String indexName) {
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

		public void setUpdate(final boolean update) {
			this.update = update;
		}

		@JsonView(View.Persistence.class)
		public boolean isStable() {
			return stable;
		}

		public void setStable(final boolean stable) {
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
	private ApplicationEventPublisher applicationEventPublisher;

	public MangoCouchDbConnector(final String databaseName, final CouchDbInstance dbInstance) {
		super(databaseName, dbInstance);
	}

	public MangoCouchDbConnector(final String databaseName, final CouchDbInstance dbi, final ObjectMapperFactory om) {
		super(databaseName, dbi, om);
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public <T> T get(final Class<T> c, final String id, final Options options) {
		publishQueryEvent(c);
		return super.get(c, id, options);
	}

	@Override
	protected <T> T executeQuery(final ViewQuery query, final ResponseCallback<T> rh) {
		publishQueryEvent(
				query.getDesignDocId(),
				query.hasMultipleKeys() || query.getStartKey() != null || query.getEndKey() != null,
				query.isIncludeDocs(),
				query.isReduce());
		return super.executeQuery(query, rh);
	}

	@Override
	public List<DocumentOperationResult> executeBulk(final Collection<?> objects, final boolean allOrNothing) {
		return super.executeBulk(objects, allOrNothing);
	}

	/**
	 * Retrieves entities from the database selected by the query.
	 *
	 * @param query The query sent to CouchDB's Mango API
	 * @param rh Handler for the response to the query
	 * @return List of retrieved entities
	 */
	public <T> List<T> query(final MangoQuery query, final MangoResponseHandler<T> rh) {
		final String queryString;
		try {
			queryString = objectMapper.writeValueAsString(query);
			logger.debug("Querying CouchDB using Mango API: {}", queryString);
		} catch (final JsonProcessingException e) {
			throw new DbAccessException("Could not serialize Mango query.");
		}
		final List<T> result = restTemplate.postUncached(dbURI.append("_find").toString(), queryString, rh);
		//List<T> result = restTemplate.post(dbURI.append("_find").toString(),
		//		new JacksonableEntity(query, objectMapper), rh);

		logger.debug("Answer from CouchDB Mango query: {}", result);

		return result;
	}

	/**
	 * Retrieves entities from the database selected by the query.
	 *
	 * @param query The query sent to CouchDB's Mango API
	 * @param type Type for deserialization of retrieved entities
	 * @return List of retrieved entities
	 */
	public <T> List<T> query(final MangoQuery query, final Class<T> type) {
		final MangoResponseHandler<T> rh = new MangoResponseHandler<>(type, objectMapper, true);
		return query(query, rh);
	}

	/**
	 * Retrieves entities from the database selected by the query.
	 *
	 * @param query The query sent to CouchDB's Mango API
	 * @param propertyName Name of the entity's property to be parsed
	 * @param type Type for deserialization of retrieved entities
	 * @return List of retrieved entities
	 */
	public <T> List<T> query(final MangoQuery query, final String propertyName, final Class<T> type) {
		query.setFields(Arrays.asList(new String[] {propertyName}));
		final MangoResponseHandler<T> rh = new MangoResponseHandler<>(propertyName, type, objectMapper);

		return query(query, rh);
	}

	/**
	 * Retrieves entities with pagination metadata from the database selected by the query.
	 *
	 * @param query The query sent to CouchDB's Mango API
	 * @param type Type for deserialization of retrieved entities
	 * @return List of retrieved entities wrapped with pagination metadata
	 */
	public <T> PagedMangoResponse<T> queryForPage(final MangoQuery query, final Class<T> type) {
		final MangoResponseHandler<T> rh = new MangoResponseHandler<>(type, objectMapper, true);
		return new PagedMangoResponse<T>(query(query, rh), rh.getBookmark());
	}

	public void createPartialJsonIndex(
			final String name, final List<MangoQuery.Sort> fields, final Map<String, Object> filterSelector) {
		final Map<String, Object> query = new HashMap<>();
		final Map<String, Object> index = new HashMap<>();
		query.put("ddoc", name);
		query.put("type", "json");
		query.put("index", index);
		index.put("fields", fields);
		if (filterSelector != null) {
			index.put("partial_filter_selector", filterSelector);
		}
		final String queryString;
		try {
			queryString = objectMapper.writeValueAsString(query);
			logger.debug("Creating CouchDB index using Mango API: {}", queryString);
		} catch (final JsonProcessingException e) {
			throw new DbAccessException(e);
		}
		final HttpResponse response = restTemplate.postUncached(dbURI.append("_index").toString(), queryString);
		response.releaseConnection();
	}

	public void createJsonIndex(final String name, final List<MangoQuery.Sort> fields) {
		createPartialJsonIndex(name, fields, null);
	}

	public boolean initializeIndex(final String name) {
		final MangoQuery query = new MangoQuery(Collections.EMPTY_MAP);
		query.setIndexDocument(name);
		query.setLimit(0);
		try {
			final String queryString = objectMapper.writeValueAsString(query);
			logger.debug("Using Mango API query to initialize CouchDB index: {}", queryString);
			final HttpResponse response = restTemplate.postUncached(dbURI.append("_find").toString(), queryString);
			response.releaseConnection();
		} catch (final JsonProcessingException e) {
			throw new DbAccessException("Could not serialize Mango query.");
		} catch (final DbAccessException e) {
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
					public DbInfo success(final HttpResponse hr) throws Exception {
						return objectMapper.readValue(hr.getContent(),
								DbInfo.class);
					}
				}
		);
	}

	protected void publishQueryEvent(final Class<?> clazz) {
		final String repositoryName = Entity.class.isAssignableFrom(clazz)
				? clazz.getSimpleName()
				: "n/a";
		this.applicationEventPublisher.publishEvent(
				new ReadRepositoryEvent(this, repositoryName, false, true, false));
	}

	protected void publishQueryEvent(
			final String designDocId, final boolean multiple, final boolean includeDocs, final boolean reduce) {
		final String repositoryName = designDocId.substring("_design/".length());
		this.applicationEventPublisher.publishEvent(
				new ReadRepositoryEvent(this, repositoryName, multiple, !includeDocs, reduce));
	}

	@JsonIgnoreProperties("purge_seq")
	public static class DbInfo extends org.ektorp.DbInfo {
		private String purgeSeq;

		@JsonCreator
		public DbInfo(@JsonProperty("db_name") final String dbName) {
			super(dbName);
		}
	}
}
