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

package de.thm.arsnova.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.config.SecurityConfig;
import de.thm.arsnova.config.properties.AuthenticationProviderProperties;
import de.thm.arsnova.config.properties.FeatureProperties;
import de.thm.arsnova.config.properties.UiProperties;
import de.thm.arsnova.model.AuthenticationProvider;
import de.thm.arsnova.model.Configuration;

@RestController
@RequestMapping(ConfigurationController.REQUEST_MAPPING)
@EnableConfigurationProperties(UiProperties.class)
public class ConfigurationController {
	protected static final String REQUEST_MAPPING = "/configuration";

	private AuthenticationProviderProperties providerProperties;
	private UiProperties uiProperties;
	private FeatureProperties featureProperties;
	private List<AuthenticationProvider> authenticationProviders;
	private Map<String, Object> featureConfig;

	public ConfigurationController(
			final AuthenticationProviderProperties authenticationProviderProperties,
			final UiProperties uiProperties,
			final FeatureProperties featureProperties) {
		this.providerProperties = authenticationProviderProperties;
		this.uiProperties = uiProperties;
		this.featureProperties = featureProperties;
		buildAuthenticationProviderConfig();
		buildFeatureConfig();
	}

	@RequestMapping
	public Configuration get() {
		final Configuration configuration = new Configuration();
		configuration.setAuthenticationProviders(authenticationProviders);
		configuration.setUi(uiProperties.getUi());
		configuration.setFeatures(featureConfig);

		return configuration;
	}

	private void buildAuthenticationProviderConfig() {
		this.authenticationProviders = new ArrayList<>();
		if (providerProperties.getGuest().isEnabled()) {
			authenticationProviders.add(new AuthenticationProvider("guest", providerProperties.getGuest()));
		}
		if (providerProperties.getRegistered().isEnabled()) {
			authenticationProviders.add(new AuthenticationProvider(
					SecurityConfig.INTERNAL_PROVIDER_ID, providerProperties.getRegistered()));
		}
		if (!providerProperties.getLdap().isEmpty() && providerProperties.getLdap().get(0).isEnabled()) {
			authenticationProviders.add(new AuthenticationProvider(
					SecurityConfig.LDAP_PROVIDER_ID, providerProperties.getLdap().get(0)));
		}
		if (!providerProperties.getOidc().isEmpty() && providerProperties.getOidc().get(0).isEnabled()) {
			authenticationProviders.add(new AuthenticationProvider(
					SecurityConfig.OIDC_PROVIDER_ID, providerProperties.getOidc().get(0)));
		}
		if (providerProperties.getCas().isEnabled()) {
			authenticationProviders.add(new AuthenticationProvider(
					SecurityConfig.CAS_PROVIDER_ID, providerProperties.getCas()));
		}
	}

	private void buildFeatureConfig() {
		this.featureConfig = new HashMap<>();
		featureConfig.put("contents", featureProperties.getContents());
		featureConfig.put("comments", featureProperties.getComments());
		featureConfig.put("liveFeedback", featureProperties.getLiveFeedback());
		featureConfig.put("contentPool", featureProperties.getContentPool());
	}
}
