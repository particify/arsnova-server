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
import de.thm.arsnova.model.serialization.View;
import org.springframework.core.style.ToStringCreator;

public class ClientAuthentication {
	private String userId;
	private String loginId;
	private UserProfile.AuthProvider authProvider;
	private String token;

	public ClientAuthentication(final String userId, final String loginId, final UserProfile.AuthProvider authProvider,
			final String token) {
		this.userId = userId;
		this.loginId = loginId;
		this.authProvider = authProvider;
		this.token = token;
	}

	@JsonView(View.Public.class)
	public String getUserId() {
		return userId;
	}

	@JsonView(View.Public.class)
	public String getLoginId() {
		return loginId;
	}

	@JsonView(View.Public.class)
	public UserProfile.AuthProvider getAuthProvider() {
		return authProvider;
	}

	@JsonView(View.Public.class)
	public String getToken() {
		return token;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("userId", userId)
				.append("loginId", loginId)
				.append("authProvider", authProvider)
				.append("token", token)
				.toString();
	}
}
