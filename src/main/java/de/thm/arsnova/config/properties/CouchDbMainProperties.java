package de.thm.arsnova.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(CouchDbMainProperties.PREFIX)
public class CouchDbMainProperties extends CouchDbProperties {
	public static final String PREFIX = SystemProperties.PREFIX + ".couchdb";

	private boolean createDb;

	public boolean isCreateDb() {
		return createDb;
	}

	public void setCreateDb(final boolean createDb) {
		this.createDb = createDb;
	}
}
