package de.thm.arsnova.persistance.couchdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.thm.arsnova.entities.MigrationState;
import de.thm.arsnova.persistance.couchdb.migrations.MigrationExecutor;
import de.thm.arsnova.persistance.couchdb.support.MangoCouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.impl.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.FileCopyUtils;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class InitializingCouchDbConnector extends MangoCouchDbConnector implements InitializingBean, ResourceLoaderAware {
	private static final Logger logger = LoggerFactory.getLogger(InitializingCouchDbConnector.class);
	private final List<Bindings> docs = new ArrayList<>();

	private ResourceLoader resourceLoader;
	private MigrationExecutor migrationExecutor;

	public InitializingCouchDbConnector(final String databaseName, final CouchDbInstance dbInstance) {
		super(databaseName, dbInstance);
	}

	public InitializingCouchDbConnector(final String databaseName, final CouchDbInstance dbi, final ObjectMapperFactory om) {
		super(databaseName, dbi, om);
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
		docs.forEach(doc -> {
			if (logger.isDebugEnabled()) {
				try {
					logger.debug("Creating design doc:\n{}", objectMapper.writeValueAsString(doc));
				} catch (JsonProcessingException e) {
					logger.warn("Failed to serialize design doc.", e);
				}
			}
			try {
				final String rev = getCurrentRevision((String) doc.get("_id"));
				doc.put("_rev", rev);
				update(doc);
			} catch (final DocumentNotFoundException e) {
				create(doc);
			}
		});
	}

	protected void migrate() {
		MigrationState state;
		try {
			state = get(MigrationState.class, MigrationState.ID);
		} catch (DocumentNotFoundException e) {
			logger.debug("No migration state found in database.");
			state = new MigrationState();
		}
		if (migrationExecutor != null && migrationExecutor.runMigrations(state)) {
			update(state);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		loadDesignDocFiles();
		createDesignDocs();
		migrate();
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
