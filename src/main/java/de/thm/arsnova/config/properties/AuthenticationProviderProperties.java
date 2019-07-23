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

package de.thm.arsnova.config.properties;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.style.ToStringCreator;

@ConfigurationProperties("security.authentication-providers")
public class AuthenticationProviderProperties {
	public abstract static class Provider {
		public enum Role {
			MODERATOR,
			PARTICIPANT
		}

		private boolean enabled;
		private String title;
		private int order;
		private Set<Role> allowedRoles;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(final String title) {
			this.title = title;
		}

		public int getOrder() {
			return order;
		}

		public void setOrder(final int order) {
			this.order = order;
		}

		public Set<Role> getAllowedRoles() {
			return allowedRoles;
		}

		public void setAllowedRoles(final Set<Role> allowedRoles) {
			this.allowedRoles = allowedRoles;
		}
	}

	public static class Registered extends Provider {
		private List<String> allowedEmailDomains;
		private String registrationMailSubject;
		private String registrationMailBody;
		private String resetPasswordMailSubject;
		private String resetPasswordMailBody;

		public List<String> getAllowedEmailDomains() {
			return allowedEmailDomains;
		}

		public void setAllowedEmailDomains(final List<String> allowedEmailDomains) {
			this.allowedEmailDomains = allowedEmailDomains;
		}

		public String getRegistrationMailSubject() {
			return registrationMailSubject;
		}

		public void setRegistrationMailSubject(final String registrationMailSubject) {
			this.registrationMailSubject = registrationMailSubject;
		}

		public String getRegistrationMailBody() {
			return registrationMailBody;
		}

		public void setRegistrationMailBody(final String registrationMailBody) {
			this.registrationMailBody = registrationMailBody;
		}

		public String getResetPasswordMailSubject() {
			return resetPasswordMailSubject;
		}

		public void setResetPasswordMailSubject(final String resetPasswordMailSubject) {
			this.resetPasswordMailSubject = resetPasswordMailSubject;
		}

		public String getResetPasswordMailBody() {
			return resetPasswordMailBody;
		}

		public void setResetPasswordMailBody(final String resetPasswordMailBody) {
			this.resetPasswordMailBody = resetPasswordMailBody;
		}
	}

	public static class Guest extends Provider {

	}

	public static class Ldap extends Provider {
		private String hostUrl;
		private String userDnPattern;
		private String userIdAttribute;
		private String userSearchFilter;
		private String userSearchBase;
		private String managerUserDn;
		private String managerPassword;

		public String getHostUrl() {
			return hostUrl;
		}

		public void setHostUrl(final String hostUrl) {
			this.hostUrl = hostUrl;
		}

		public String getUserDnPattern() {
			return userDnPattern;
		}

		public void setUserDnPattern(final String userDnPattern) {
			this.userDnPattern = userDnPattern;
		}

		public String getUserIdAttribute() {
			return userIdAttribute;
		}

		public void setUserIdAttribute(final String userIdAttribute) {
			this.userIdAttribute = userIdAttribute;
		}

		public String getUserSearchFilter() {
			return userSearchFilter;
		}

		public void setUserSearchFilter(final String userSearchFilter) {
			this.userSearchFilter = userSearchFilter;
		}

		public String getUserSearchBase() {
			return userSearchBase;
		}

		public void setUserSearchBase(final String userSearchBase) {
			this.userSearchBase = userSearchBase;
		}

		public String getManagerUserDn() {
			return managerUserDn;
		}

		public void setManagerUserDn(final String managerUserDn) {
			this.managerUserDn = managerUserDn;
		}

		public String getManagerPassword() {
			return managerPassword;
		}

		public void setManagerPassword(final String managerPassword) {
			this.managerPassword = managerPassword;
		}
	}

	public static class Oidc extends Provider {
		private String issuer;
		private String clientId;
		private String secret;

		public String getIssuer() {
			return issuer;
		}

		public void setIssuer(final String issuer) {
			this.issuer = issuer;
		}

		public String getClientId() {
			return clientId;
		}

		public void setClientId(final String clientId) {
			this.clientId = clientId;
		}

		public String getSecret() {
			return secret;
		}

		public void setSecret(final String secret) {
			this.secret = secret;
		}
	}

	public static class Cas extends Provider {
		private String hostUrl;

		public String getHostUrl() {
			return hostUrl;
		}

		public void setHostUrl(final String hostUrl) {
			this.hostUrl = hostUrl;
		}
	}

	public static class Oauth extends Provider {
		private String key;
		private String secret;

		public String getKey() {
			return key;
		}

		public void setKey(final String key) {
			this.key = key;
		}

		public String getSecret() {
			return secret;
		}

		public void setSecret(final String secret) {
			this.secret = secret;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("enabled", isEnabled()).append("key", key).toString();
		}
	}

	private Registered registered;
	private Guest guest;
	private List<Ldap> ldap;
	private List<Oidc> oidc;
	private Cas cas;
	private Map<String, Oauth> oauth;

	public Registered getRegistered() {
		return registered;
	}

	public void setRegistered(final Registered registered) {
		this.registered = registered;
	}

	public Guest getGuest() {
		return guest;
	}

	public void setGuest(final Guest guest) {
		this.guest = guest;
	}

	public List<Ldap> getLdap() {
		return ldap;
	}

	public void setLdap(final List<Ldap> ldap) {
		this.ldap = ldap;
	}

	public List<Oidc> getOidc() {
		return oidc;
	}

	public void setOidc(final List<Oidc> oidc) {
		this.oidc = oidc;
	}

	public Cas getCas() {
		return cas;
	}

	public void setCas(final Cas cas) {
		this.cas = cas;
	}

	public Map<String, Oauth> getOauth() {
		return oauth;
	}

	public void setOauth(final Map<String, Oauth> oauth) {
		this.oauth = oauth;
	}
}
