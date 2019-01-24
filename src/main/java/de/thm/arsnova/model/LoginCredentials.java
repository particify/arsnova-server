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

public class LoginCredentials {
	private String loginId;
	private String password;

	public String getLoginId() {
		return loginId;
	}

	@JsonView(View.Public.class)
	public void setLoginId(final String loginId) {
		this.loginId = loginId;
	}

	public String getPassword() {
		return password;
	}

	@JsonView(View.Public.class)
	public void setPassword(final String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("loginId", loginId)
				.append("password", password)
				.toString();
	}
}
