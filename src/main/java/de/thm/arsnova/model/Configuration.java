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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.thm.arsnova.model.serialization.View;

public class Configuration {
	private List<AuthenticationProvider> authenticationProviders;
	private Map<String, Object> ui;

	@JsonView(View.Public.class)
	public List<AuthenticationProvider> getAuthenticationProviders() {
		return authenticationProviders;
	}

	public void setAuthenticationProviders(final List<AuthenticationProvider> authenticationProviders) {
		this.authenticationProviders = authenticationProviders;
	}

	@JsonView(View.Public.class)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public Map<String, Object> getFeatures() {
		return Collections.emptyMap();
	}

	@JsonView(View.Public.class)
	public Map<String, Object> getUi() {
		return ui;
	}

	public void setUi(final Map<String, Object> ui) {
		this.ui = ui;
	}
}
