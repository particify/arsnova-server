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

package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class UserProfile extends Entity {
	public enum AuthProvider {
		NONE,
		UNKNOWN,
		ANONYMIZED,
		ARSNOVA,
		ARSNOVA_GUEST,
		LDAP,
		SAML,
		CAS,
		OIDC,
		GOOGLE,
		FACEBOOK,
		TWITTER
	}

	public static class Account {
		// An empty value is fine because an encrypted value is expected, so an
		// empty value will fail verification.
		private String password;

		private String activationKey;
		private String passwordResetKey;
		private Date passwordResetTime;
		private int failedVerifications;

		@JsonView(View.Persistence.class)
		public String getPassword() {
			return password;
		}

		@JsonView(View.Persistence.class)
		public void setPassword(final String password) {
			this.password = password;
		}

		@JsonView(View.Persistence.class)
		public String getActivationKey() {
			return activationKey;
		}

		@JsonView(View.Persistence.class)
		public void setActivationKey(final String activationKey) {
			this.activationKey = activationKey;
		}

		@JsonView(View.Persistence.class)
		public String getPasswordResetKey() {
			return passwordResetKey;
		}

		@JsonView(View.Persistence.class)
		public void setPasswordResetKey(final String passwordResetKey) {
			this.passwordResetKey = passwordResetKey;
		}

		@JsonView({View.Persistence.class, View.Admin.class})
		public Date getPasswordResetTime() {
			return passwordResetTime;
		}

		@JsonView(View.Persistence.class)
		public void setPasswordResetTime(final Date passwordResetTime) {
			this.passwordResetTime = passwordResetTime;
		}

		@JsonView(View.Persistence.class)
		public int getFailedVerifications() {
			return failedVerifications;
		}

		@JsonView(View.Persistence.class)
		public void setFailedVerifications(final int failedVerifications) {
			this.failedVerifications = failedVerifications;
		}

		@JsonView(View.Admin.class)
		public boolean isActivated() {
			return this.activationKey == null;
		}

		@Override
		public int hashCode() {
			return Objects.hash(password, activationKey, passwordResetKey, passwordResetTime);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (!super.equals(o)) {
				return false;
			}
			final Account account = (Account) o;

			return Objects.equals(password, account.password)
					&& Objects.equals(activationKey, account.activationKey)
					&& Objects.equals(passwordResetKey, account.passwordResetKey)
					&& Objects.equals(passwordResetTime, account.passwordResetTime);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("password", password)
					.append("activationKey", activationKey)
					.append("passwordResetKey", passwordResetKey)
					.append("passwordResetTime", passwordResetTime)
					.toString();
		}
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public static class Person {
		private String displayName;
		private String firstName;
		private String lastName;
		private String organization;
		private String department;

		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(final String displayName) {
			this.displayName = displayName;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(final String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(final String lastName) {
			this.lastName = lastName;
		}

		public String getOrganization() {
			return organization;
		}

		public void setOrganization(final String organization) {
			this.organization = organization;
		}

		public String getDepartment() {
			return department;
		}

		public void setDepartment(final String department) {
			this.department = department;
		}
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public static class Settings {
		private boolean contentAnswersDirectlyBelowChart;
		private boolean contentVisualizationUnitPercent;
		private boolean showContentResultsDirectly;

		public boolean isContentAnswersDirectlyBelowChart() {
			return contentAnswersDirectlyBelowChart;
		}

		public void setContentAnswersDirectlyBelowChart(final boolean contentAnswersDirectlyBelowChart) {
			this.contentAnswersDirectlyBelowChart = contentAnswersDirectlyBelowChart;
		}

		public boolean isContentVisualizationUnitPercent() {
			return contentVisualizationUnitPercent;
		}

		public void setContentVisualizationUnitPercent(final boolean contentVisualizationUnitPercent) {
			this.contentVisualizationUnitPercent = contentVisualizationUnitPercent;
		}

		public boolean isShowContentResultsDirectly() {
			return showContentResultsDirectly;
		}

		public void setShowContentResultsDirectly(final boolean showContentResultsDirectly) {
			this.showContentResultsDirectly = showContentResultsDirectly;
		}
	}

	@NotNull
	private AuthProvider authProvider;

	@NotEmpty
	private String loginId;

	private Date lastLoginTimestamp;
	private Account account;
	private Person person;
	private Settings settings;
	private Date announcementReadTimestamp;
	private Set<String> acknowledgedMotds = new HashSet<>();

	public UserProfile() {

	}

	public UserProfile(final AuthProvider authProvider, final String loginId) {
		this.authProvider = authProvider;
		this.loginId = loginId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public AuthProvider getAuthProvider() {
		return authProvider;
	}

	@JsonView(View.Persistence.class)
	public void setAuthProvider(final AuthProvider authProvider) {
		this.authProvider = authProvider;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getLoginId() {
		return loginId;
	}

	@JsonView(View.Persistence.class)
	public void setLoginId(final String loginId) {
		this.loginId = loginId;
	}

	@JsonView({View.Persistence.class, View.Owner.class})
	public Date getLastLoginTimestamp() {
		return lastLoginTimestamp;
	}

	@JsonView(View.Persistence.class)
	public void setLastLoginTimestamp(final Date lastLoginTimestamp) {
		this.lastLoginTimestamp = lastLoginTimestamp;
	}

	@JsonView({View.Persistence.class, View.Admin.class})
	public Account getAccount() {
		if (account == null) {
			account = new Account();
		}
		return account;
	}

	@JsonView(View.Persistence.class)
	public void setAccount(final Account account) {
		this.account = account;
	}

	@JsonView({View.Persistence.class, View.Owner.class})
	public Person getPerson() {
		return person;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setPerson(final Person person) {
		this.person = person;
	}

	@JsonView({View.Persistence.class, View.Owner.class})
	public Settings getSettings() {
		return settings;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSettings(final Settings settings) {
		this.settings = settings;
	}

	@JsonView({View.Persistence.class, View.Owner.class})
	public Date getAnnouncementReadTimestamp() {
		return announcementReadTimestamp;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAnnouncementReadTimestamp(final Date announcementReadTimestamp) {
		this.announcementReadTimestamp = announcementReadTimestamp;
	}

	@JsonView({View.Persistence.class, View.Owner.class})
	public Set<String> getAcknowledgedMotds() {
		return acknowledgedMotds;
	}

	@JsonView(View.Persistence.class)
	public void setAcknowledgedMotds(final Set<String> acknowledgedMotds) {
		this.acknowledgedMotds = acknowledgedMotds;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The following fields of <tt>UserProfile</tt> are excluded from equality checks:
	 * {@link #account}, {@link #acknowledgedMotds}.
	 * </p>
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final UserProfile that = (UserProfile) o;

		return authProvider == that.authProvider
				&& Objects.equals(loginId, that.loginId)
				&& Objects.equals(lastLoginTimestamp, that.lastLoginTimestamp);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("authProvider", authProvider)
				.append("loginId", loginId)
				.append("lastLoginTimestamp", lastLoginTimestamp)
				.append("account", account)
				.append("acknowledgedMotds", acknowledgedMotds);
	}
}
