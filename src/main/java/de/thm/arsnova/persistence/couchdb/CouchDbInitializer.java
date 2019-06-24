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

package de.thm.arsnova.persistence.couchdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.impl.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import de.thm.arsnova.model.MigrationState;
import de.thm.arsnova.persistence.couchdb.migrations.MigrationExecutor;
import de.thm.arsnova.service.StatusService;

@Component
public class CouchDbInitializer implements ResourceLoaderAware {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbInitializer.class);
	private final List<Bindings> docs = new ArrayList<>();

	private ResourceLoader resourceLoader;
	private MigrationExecutor migrationExecutor;
	private CouchDbConnector connector;
	private ObjectMapper objectMapper;
	private StatusService statusService;
	private boolean migrationStarted = false;

	public CouchDbInitializer(final CouchDbConnector couchDbConnector, final ObjectMapperFactory objectMapperFactory,
			final StatusService statusService) {
		connector = couchDbConnector;
		objectMapper = objectMapperFactory.createObjectMapper(couchDbConnector);
		this.statusService = statusService;
	}

	protected void loadDesignDocFiles() throws IOException, ScriptException {
		final ScriptEngine engine = new ScriptEngineManager().getEngineByMimeType("application/javascript");
		engine.eval(new InputStreamReader(new ClassPathResource("couchdb/jsToJson.js").getInputStream()));

		final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		final Resource[] resources = resolver.getResources("classpath:couchdb/*.design.js");
		for (Resource resource : resources) {
			logger.debug("Loading CouchDB design doc: {}", resource.getFilename());
			final String js = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
			/* Reset designDoc before parsing a new one. */
			engine.eval("var designDoc = null;" + js);
			final Bindings jsonObject = (Bindings) engine.eval("jsToJson(designDoc)");
			docs.add(jsonObject);
		}
	}

	protected void createDesignDocs() {
		connector.executeBulk(docs.stream().filter(doc -> {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Checking design doc {}:\n{}", doc.get("_id"), objectMapper.writeValueAsString(doc));
				}
				final Map<String, Object> existingDoc = connector.get(HashMap.class, doc.get("_id").toString());
				final String existingViews = objectMapper.writeValueAsString(existingDoc.get("views"));
				final String currentViews = objectMapper.writeValueAsString(doc.get("views"));
				if (existingViews.equals(currentViews)) {
					logger.debug("Design doc {} already exists.", doc.get("_id"));
					return false;
				} else {
					logger.debug("Design doc {} will be updated.", doc.get("_id"));
					doc.put("_rev", existingDoc.get("_rev"));
					return true;
				}
			} catch (final DocumentNotFoundException e) {
				logger.debug("Design doc {} will be created.", doc.get("_id"));
				return true;
			} catch (JsonProcessingException e) {
				logger.warn("Failed to serialize design doc {}.", doc.get("_id"), e);
				return false;
			}
		}).collect(Collectors.toList()));
	}

	private MigrationState checkMigrationState() {
		MigrationState state;
		try {
			state = connector.get(MigrationState.class, MigrationState.ID);
		} catch (DocumentNotFoundException e) {
			logger.debug("No migration state found in database.", e);
			if (connector.getDbInfo().getDocCount() > 0) {
				/* TODO: use a custom exception */
				throw new DbAccessException("Database is not empty.");
			}
			state = new MigrationState();
			connector.create(state);
		}

		return state;
	}

	protected void migrate(final MigrationState state) {
		if (migrationExecutor != null && migrationExecutor.runMigrations(state)) {
			connector.update(state);
		}
	}

	@PostConstruct
	private void init() {
		statusService.putMaintenanceReason(this.getClass(), "Database not initialized");
	}

	@EventListener
	private void onApplicationEvent(ContextRefreshedEvent event) throws IOException, ScriptException {
		/* Event is triggered more than once */
		if (migrationStarted) {
			return;
		}
		migrationStarted = true;

		try {
			final MigrationState state = checkMigrationState();
			statusService.putMaintenanceReason(this.getClass(), "Data migration active");
			loadDesignDocFiles();
			createDesignDocs();
			migrate(state);
			statusService.removeMaintenanceReason(this.getClass());
		} catch (DbAccessException e) {
			logger.error("Database is invalid.", e);
			statusService.putMaintenanceReason(this.getClass(), "Invalid database");
		}
	}

	@Override
	public void setResourceLoader(final ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Autowired
	public void setMigrationExecutor(final MigrationExecutor migrationExecutor) {
		this.migrationExecutor = migrationExecutor;
	}
}
