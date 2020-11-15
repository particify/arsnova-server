package de.thm.arsnova.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import de.thm.arsnova.model.UserProfile;

@ConfigurationProperties(CouchDbMigrationProperties.PREFIX)
public class CouchDbMigrationProperties extends CouchDbProperties {
	public static final String PREFIX = SystemProperties.PREFIX + ".couchdb.migration";

	private boolean enabled;
	private UserProfile.AuthProvider authenticationProviderFallback;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public UserProfile.AuthProvider getAuthenticationProviderFallback() {
		return authenticationProviderFallback;
	}

	public void setAuthenticationProviderFallback(final UserProfile.AuthProvider authenticationProviderFallback) {
		this.authenticationProviderFallback = authenticationProviderFallback;
	}
}
