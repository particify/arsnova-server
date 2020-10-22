package de.thm.arsnova.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(CouchDbMigrationProperties.PREFIX)
public class CouchDbMigrationProperties extends CouchDbProperties {
	public static final String PREFIX = SystemProperties.PREFIX + ".couchdb.migration";

	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}
}
