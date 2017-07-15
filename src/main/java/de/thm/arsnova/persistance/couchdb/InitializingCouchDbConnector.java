package de.thm.arsnova.persistance.couchdb;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ektorp.CouchDbInstance;
import org.ektorp.impl.ObjectMapperFactory;
import org.ektorp.impl.StdCouchDbConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
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

public class InitializingCouchDbConnector extends StdCouchDbConnector implements InitializingBean, ResourceLoaderAware {
	private static final Logger logger = LoggerFactory.getLogger(InitializingCouchDbConnector.class);
	private final List<Bindings> docs = new ArrayList<>();

	private ResourceLoader resourceLoader;

	public InitializingCouchDbConnector(String databaseName, CouchDbInstance dbInstance) {
		super(databaseName, dbInstance);
	}

	public InitializingCouchDbConnector(String databaseName, CouchDbInstance dbi, ObjectMapperFactory om) {
		super(databaseName, dbi, om);
	}

	protected void loadDesignDocFiles() throws IOException, ScriptException {
		final ScriptEngine engine = new ScriptEngineManager().getEngineByMimeType("application/javascript");
		engine.eval(new InputStreamReader(new ClassPathResource("couchdb/jsToJson.js").getInputStream()));

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources("classpath:couchdb/*.design.js");
		for (Resource resource : resources) {
			logger.debug("Loading CouchDB design doc: {}", resource.getFilename());
			String js = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
			/* Reset designDoc before parsing a new one. */
			engine.eval("var designDoc = null;" + js);
			Bindings jsonObject = (Bindings) engine.eval("jsToJson(designDoc)");
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
			String rev = getCurrentRevision((String) doc.get("_id"));
			if (rev == null) {
				create(doc);
			} else {
				doc.put("_rev", rev);
				update(doc);
			}
		});
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		loadDesignDocFiles();
		createDesignDocs();
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
