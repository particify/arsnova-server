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

/**
 * The ConfigurationController provides frontend clients with information necessary to correctly interact with the 
 * backend and other frontends as well as settings for ARSnova. The the alternative /arsnova-config route is necessary
 * in case the backend application is deployed as root context.
 */
@Controller
@RequestMapping({"/configuration", "/arsnova-config"})
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

	@Value("${links.documentation.url}")
	private String documentationUrl;

	@Value("${features.mathjax.enabled:true}")
	private String mathJaxEnabled;

	@Value("${features.markdown.enabled:false}")
	private String markdownEnabled;

	@Value("${features.question-format.flashcard.enabled:false}")
	private String flashcardEnabled;

	@Value("${features.question-format.grid-square.enabled:false}")
	private String gridSquareEnabled;

	@Value("${question.answer-option-limit:8}")
	private String answerOptionLimit;

	@Value("${question.parse-answer-option-formatting:false}")
	private String parseAnswerOptionFormatting;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public final HashMap<String, Object> getConfiguration(HttpServletRequest request) {
		HashMap<String, Object> config = new HashMap<String, Object>();
		HashMap<String, Boolean> features = new HashMap<String, Boolean>();

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

		if (!"".equals(documentationUrl)) {
			config.put("documentationUrl", documentationUrl);
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

		config.put("answerOptionLimit", Integer.valueOf(answerOptionLimit));
		config.put("parseAnswerOptionFormatting", Boolean.valueOf(parseAnswerOptionFormatting));

		config.put("features", features);

		features.put("mathJax", "true".equals(mathJaxEnabled));
		features.put("markdown", "true".equals(markdownEnabled));
		features.put("flashcard", "true".equals(flashcardEnabled));
		features.put("gridSquare", "true".equals(gridSquareEnabled));

		return config;
	}
}
