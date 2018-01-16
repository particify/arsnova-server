package de.thm.arsnova.persistance.couchdb.support;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import de.thm.arsnova.entities.serialization.View;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.impl.ObjectMapperFactory;
import org.ektorp.impl.StdCouchDbConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

		public MangoQuery() {
			this.selector = new HashMap<>();
		}

		/**
		 * @param selector See http://docs.couchdb.org/en/stable/api/database/find.html#selector-syntax.
		 */
		public MangoQuery(Map<String, Object> selector) {
			this.selector = selector;
		}

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
		//List<T> result = restTemplate.post(dbURI.append("_find").toString(), new JacksonableEntity(query, objectMapper), rh);

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

	public void createPartialJsonIndex(final String name, final List<MangoQuery.Sort> fields, final Map<String, Object> filterSelector) {
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
}
