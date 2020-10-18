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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

@ConfigurationProperties(SecurityProperties.PREFIX)
public class SecurityProperties {
	public static final String PREFIX = "security";

	public static class Jwt {
		private String serverId;
		private String secret;
		private String legacyServerId;
		private String legacySecret;

		@DurationUnit(ChronoUnit.MINUTES)
		private Duration validityPeriod;

		public String getServerId() {
			return serverId;
		}

		public void setServerId(final String serverId) {
			this.serverId = serverId;
		}

		public String getSecret() {
			return secret;
		}

		public void setSecret(final String secret) {
			this.secret = secret;
		}

		public Duration getValidityPeriod() {
			return validityPeriod;
		}

		public void setValidityPeriod(final Duration validityPeriod) {
			this.validityPeriod = validityPeriod;
		}

		public String getLegacyServerId() {
			return legacyServerId;
		}

		public void setLegacyServerId(final String legacyServerId) {
			this.legacyServerId = legacyServerId;
		}

		public String getLegacySecret() {
			return legacySecret;
		}

		public void setLegacySecret(final String legacySecret) {
			this.legacySecret = legacySecret;
		}
	}

	private Jwt jwt;
	private List<String> adminAccounts;
	private int loginTryLimit;
	private int resendMailLimit;
	private List<String> corsOrigins;

	public Jwt getJwt() {
		return jwt;
	}

	public void setJwt(final Jwt jwt) {
		this.jwt = jwt;
	}

	public List<String> getAdminAccounts() {
		return adminAccounts;
	}

	public void setAdminAccounts(final List<String> adminAccounts) {
		this.adminAccounts = adminAccounts;
	}

	public int getLoginTryLimit() {
		return loginTryLimit;
	}

	public void setLoginTryLimit(final int loginTryLimit) {
		this.loginTryLimit = loginTryLimit;
	}

	public int getResendMailLimit() {
		return resendMailLimit;
	}

	public void setResendMailLimit(final int resendMailLimit) {
		this.resendMailLimit = resendMailLimit;
	}

	public List<String> getCorsOrigins() {
		return corsOrigins;
	}

	public void setCorsOrigins(final List<String> corsOrigins) {
		this.corsOrigins = corsOrigins;
	}
}
