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

package net.particify.arsnova.core.persistence.couchdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.http.NoHttpResponseException;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.DbInfo;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.impl.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import net.particify.arsnova.core.event.DatabaseInitializedEvent;
import net.particify.arsnova.core.model.MigrationState;
import net.particify.arsnova.core.persistence.couchdb.migrations.MigrationException;
import net.particify.arsnova.core.persistence.couchdb.migrations.MigrationExecutor;
import net.particify.arsnova.core.service.StatusService;

@Component
@Profile("!test")
public class CouchDbInitializer implements ApplicationEventPublisherAware {
  private static final Logger logger = LoggerFactory.getLogger(CouchDbInitializer.class);
  private final List<Map<String, Object>> docs = new ArrayList<>();

  private ApplicationEventPublisher applicationEventPublisher;
  private MigrationExecutor migrationExecutor;
  private CouchDbConnector connector;
  private ObjectMapper objectMapper;
  private StatusService statusService;
  private boolean initEventHandled = false;

  public CouchDbInitializer(final CouchDbConnector couchDbConnector, final ObjectMapperFactory objectMapperFactory,
      final StatusService statusService) {
    connector = couchDbConnector;
    objectMapper = objectMapperFactory.createObjectMapper(couchDbConnector);
    this.statusService = statusService;
  }

  @Override
  public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  protected void loadDesignDocFiles() throws IOException, ScriptException {
    final ScriptEngine engine = new ScriptEngineManager().getEngineByMimeType("application/javascript");
    engine.eval(new InputStreamReader(new ClassPathResource("couchdb/jsToJson.js").getInputStream()));

    final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    final Resource[] resources = resolver.getResources("classpath:couchdb/*.design.js");
    for (final Resource resource : resources) {
      logger.debug("Loading CouchDB design doc: {}", resource.getFilename());
      final String js = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
      /* Reset designDoc before parsing a new one. */
      engine.eval("var designDoc = null;" + js);
      final Map<String, Object> jsonObject = (Map<String, Object>) engine.eval("jsToJson(designDoc)");
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
      } catch (final JsonProcessingException e) {
        logger.warn("Failed to serialize design doc {}.", doc.get("_id"), e);
        return false;
      }
    }).collect(Collectors.toList()));
  }

  private MigrationState checkMigrationState() {
    MigrationState state;
    try {
      state = connector.get(MigrationState.class, MigrationState.ID);
    } catch (final DocumentNotFoundException e) {
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
    if (migrationExecutor != null) {
      try {
        migrationExecutor.runMigrations(state, () -> connector.update(state));
      } catch (final MigrationException e) {
        logger.error("Migration failed.", e);
      }
    }
  }

  protected void waitForDb() {
    DbInfo info = null;
    logger.info("Waiting for database...");
    boolean firstTry = true;
    do {
      try {
        info = connector.getDbInfo();
        logger.info("Database ready.");
      } catch (final DbAccessException e1) {
        /* Break out of loop if the exception is not related to connection issues. */
        if (e1.getCause() == null
            || (e1.getCause().getClass() != NoHttpResponseException.class
            && e1.getCause().getClass() != UnknownHostException.class
            && !SocketException.class.isAssignableFrom(e1.getCause().getClass()))) {
          throw e1;
        }
        if (firstTry) {
          logger.error("Database not ready.", e1);
          logger.info("Retrying...");
          firstTry = false;
        }
        try {
          Thread.sleep(10000);
        } catch (final InterruptedException e2) {
          logger.warn("Database waiting loop was interrupted.", e2);
        }
      }
    } while (info == null);
  }

  @PostConstruct
  private void init() {
    statusService.putMaintenanceReason(this.getClass(), "Database not initialized");
  }

  @EventListener
  private void onApplicationEvent(final ContextRefreshedEvent event) throws IOException, ScriptException {
    /* Event is triggered more than once */
    if (initEventHandled) {
      return;
    }
    initEventHandled = true;

    waitForDb();

    try {
      final MigrationState state = checkMigrationState();
      statusService.putMaintenanceReason(this.getClass(), "Data migration active");
      loadDesignDocFiles();
      createDesignDocs();
      migrate(state);
      statusService.removeMaintenanceReason(this.getClass());
      logger.info("Database initialization completed.");
      this.applicationEventPublisher.publishEvent(new DatabaseInitializedEvent(this));
    } catch (final DbAccessException e) {
      logger.error("Database initialization failed.", e);
      statusService.putMaintenanceReason(this.getClass(), "Invalid database state");
    }
  }

  @Autowired
  public void setMigrationExecutor(final MigrationExecutor migrationExecutor) {
    this.migrationExecutor = migrationExecutor;
  }
}
