/*
 * Copyright (C) 2014 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.controller;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/configuration")
public class ConfigurationController extends AbstractController {
	@Value("${security.guest.enabled}")
	private String guestEnabled;

	public static final Logger LOGGER = LoggerFactory
			.getLogger(ConfigurationController.class);

	@Value("${customization.path}")
	private String customizationPath;
	@Value("${mobile.path}")
	private String mobilePath;
	@Value("${presenter.path}")
	private String presenterPath;
	@Value("${links.overlay.url}")
	private String overlayUrl;
	@Value("${links.organization.url}")
	private String organizationUrl;
	@Value("${links.imprint.url}")
	private String imprintUrl;
	@Value("${links.privacy-policy.url}")
	private String privacyPolicyUrl;
	
	@RequestMapping(value = { "/" }, method = RequestMethod.GET)
	@ResponseBody
	public final HashMap<String, String> getConfiguration(HttpServletRequest request) {
		HashMap<String, String> config = new HashMap<String, String>();
		/* The API path could be unknown to the client in case the request was forwarded */
		config.put("apiPath", request.getContextPath());

		if (!"".equals(customizationPath)) {
			config.put("customizationPath", customizationPath);
		}
		if (!"".equals(mobilePath)) {
			config.put("mobilePath", mobilePath);
		}
		if (!"".equals(presenterPath)) {
			config.put("presenterPath", presenterPath);
		}
		if (!"".equals(overlayUrl)) {
			config.put("overlayUrl", overlayUrl);
		}
		if (!"".equals(organizationUrl)) {
			config.put("organizationUrl", organizationUrl);
		}
		if (!"".equals(imprintUrl)) {
			config.put("imprintUrl", imprintUrl);
		}
		if (!"".equals(privacyPolicyUrl)) {
			config.put("privacyPolicyUrl", privacyPolicyUrl);
		}
		
		return config;
	}
}
