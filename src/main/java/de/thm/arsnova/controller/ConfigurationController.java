/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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

	@Value("${api.path:}")
	private String apiPath;

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

	@Value("${links.blog.url:}")
	private String blogUrl;

	@Value("${links.privacy-policy.url}")
	private String privacyPolicyUrl;

	@Value("${links.documentation.url}")
	private String documentationUrl;

	@Value("${links.presenter-documentation.url}")
	private String presenterDocumentationUrl;

	@Value("${features.mathjax.enabled:true}")
	private String mathJaxEnabled;

	@Value("${features.mathjax.src:}")
	private String mathJaxSrc;

	@Value("${features.markdown.enabled:false}")
	private String markdownEnabled;

	@Value("${features.learning-progress.enabled:false}")
	private String learningProgressEnabled;

	@Value("${features.students-own-questions.enabled:false}")
	private String studentsOwnQuestions;

	@Value("${features.freetext-imageanswer.enabled:false}")
	private String imageAnswerEnabled;

	@Value("${features.question-format.flashcard.enabled:false}")
	private String flashcardEnabled;

	@Value("${features.question-format.grid-square.enabled:false}")
	private String gridSquareEnabled;

	@Value("${features.session-import-export.enabled:false}")
	private String sessionImportExportEnabled;

	@Value("${features.public-pool.enabled:false}")
	private String publicPoolEnabled;

	@Value("${question.answer-option-limit:8}")
	private String answerOptionLimit;

	@Value("${upload.filesize_b:}")
	private String maxUploadFilesize;

	@Value("${question.parse-answer-option-formatting:false}")
	private String parseAnswerOptionFormatting;

	@Value("${pp.subjects}")
	private String ppSubjects;

	@Value("${pp.licenses}")
	private String ppLicenses;

	@Value("${pp.logofilesize_b}")
	private String ppLogoMaxFilesize;

	@Value("${upload.filesize_b}")
	private String gridImageMaxFileSize;

	@Value("${tracking.provider}")
	private String trackingProvider;

	@Value("${tracking.tracker-url}")
	private String trackingTrackerUrl;

	@Value("${tracking.site-id}")
	private String trackingSiteId;

	@Value("${session.demo-id:}")
	private String demoSessionKey;

	@Value("${ui.slogan:}")
	private String arsnovaSlogan;

	@Value("${splashscreen.logo-path:}")
	private String splashscreenLogo;

	@Value("${splashscreen.slogan:}")
	private String splashscreenSlogan;

	@Value("${splashscreen.background-color:}")
	private String splashscreenBgColor;

	@Value("${splashscreen.loading-ind-color:}")
	private String splashscreenLoadingIndColor;

	@Value("${pp.session-levels.de}")
	private String ppLevelsDe;

	@Value("${pp.session-levels.en}")
	private String ppLevelsEn;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public HashMap<String, Object> getConfiguration(HttpServletRequest request) {
		HashMap<String, Object> config = new HashMap<String, Object>();
		HashMap<String, Boolean> features = new HashMap<String, Boolean>();
		HashMap<String, String> publicPool = new HashMap<String, String>();
		HashMap<String, String> splashscreen = new HashMap<String, String>();

		/* The API path could be unknown to the client in case the request was forwarded */
		if ("".equals(apiPath)) {
			apiPath = request.getContextPath();
		}
		config.put("apiPath", apiPath);

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
		if (!"".equals(blogUrl)) {
			config.put("blogUrl", blogUrl);
		}
		if (!"".equals(presenterDocumentationUrl)) {
			config.put("presenterDocumentationUrl", presenterDocumentationUrl);
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
		if (!"".equals(demoSessionKey)) {
			config.put("demoSessionKey", demoSessionKey);
		}
		if (!"".equals(arsnovaSlogan)) {
			config.put("arsnovaSlogan", arsnovaSlogan);
		}
		if (!"".equals(maxUploadFilesize)) {
			config.put("maxUploadFilesize", maxUploadFilesize);
		}
		if (!"".equals(mathJaxSrc) && "true".equals(mathJaxEnabled)) {
			config.put("mathJaxSrc", mathJaxSrc);
		}

		config.put("answerOptionLimit", Integer.valueOf(answerOptionLimit));
		config.put("parseAnswerOptionFormatting", Boolean.valueOf(parseAnswerOptionFormatting));

		config.put("features", features);

		features.put("mathJax", "true".equals(mathJaxEnabled));
		features.put("markdown", "true".equals(markdownEnabled));
		features.put("learningProgress", "true".equals(learningProgressEnabled));
		features.put("studentsOwnQuestions", "true".equals(studentsOwnQuestions));
		features.put("imageAnswer", "true".equals(imageAnswerEnabled));
		features.put("flashcard", "true".equals(flashcardEnabled));
		features.put("gridSquare", "true".equals(gridSquareEnabled));
		features.put("sessionImportExport", "true".equals(sessionImportExportEnabled));
		features.put("publicPool", "true".equals(publicPoolEnabled));

		// add public pool configuration on demand
		if (features.get("publicPool")) {
			config.put("publicPool", publicPool);
			publicPool.put("subjects", ppSubjects);
			publicPool.put("licenses", ppLicenses);
			publicPool.put("logoMaxFilesize", ppLogoMaxFilesize);
			publicPool.put("levelsDe", ppLevelsDe);
			publicPool.put("levelsEn", ppLevelsEn);
		}

		config.put("splashscreen", splashscreen);

		if (!"".equals(splashscreenLogo)) {
			splashscreen.put("logo", splashscreenLogo);
		}
		if (!"".equals(splashscreenSlogan)) {
			splashscreen.put("slogan", splashscreenSlogan);
		}
		if (!"".equals(splashscreenBgColor)) {
			splashscreen.put("bgcolor", splashscreenBgColor);
		}
		if (!"".equals(splashscreenLoadingIndColor)) {
			splashscreen.put("loadIndColor", splashscreenLoadingIndColor);
		}

		if (!"".equals(trackingTrackerUrl)) {
			HashMap<String, String> tracking = new HashMap<String, String>();
			config.put("tracking", tracking);

			tracking.put("provider", trackingProvider);
			tracking.put("trackerUrl", trackingTrackerUrl);
			tracking.put("siteId", trackingSiteId);
		}

		config.put("grid", gridImageMaxFileSize);

		return config;
	}
}
