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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import de.thm.arsnova.config.properties.SystemProperties;

/**
 * The ConfigurationController provides frontend clients with information necessary to correctly interact with the
 * backend and other frontends as well as settings for ARSnova. The the alternative /arsnova-config route is necessary
 * in case the backend application is deployed as root context.
 */
@Controller
@RequestMapping({"/configuration", "/arsnova-config"})
public class ConfigurationController extends AbstractController {
	private static final Logger logger = LoggerFactory
			.getLogger(ConfigurationController.class);

	private String apiPath;
	private String socketioPath;

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

	@Value("${feedback.warning:5}")
	private String feedbackWarningOffset;

	@Value("${features.mathjax.enabled:true}")
	private String mathJaxEnabled;

	@Value("${features.mathjax.src:}")
	private String mathJaxSrc;

	@Value("${features.freetext-imageanswer.enabled:false}")
	private String imageAnswerEnabled;

	@Value("${features.question-format.grid-square.enabled:false}")
	private String gridSquareEnabled;

	@Value("${features.session-import-export.enabled:false}")
	private String roomImportExportEnabled;

	@Value("${features.public-pool.enabled:false}")
	private String publicPoolEnabled;

	@Value("${features.export-to-click.enabled:false}")
	private String exportToClickEnabled;

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
	private String demoRoomShortId;

	@Value("${ui.slogan:}")
	private String arsnovaSlogan;

	@Value("${ui.splashscreen.logo-path:}")
	private String splashscreenLogo;

	@Value("${ui.splashscreen.slogan:}")
	private String splashscreenSlogan;

	@Value("${ui.splashscreen.slogan-color:}")
	private String splashscreenSloganColor;

	@Value("${ui.splashscreen.background-color:}")
	private String splashscreenBgColor;

	@Value("${ui.splashscreen.loading-ind-color:}")
	private String splashscreenLoadingIndColor;

	@Value("${ui.splashscreen.min-delay:}")
	private String splashscreenDelay;

	@Value("${pp.session-levels.de}")
	private String ppLevelsDe;

	@Value("${pp.session-levels.en}")
	private String ppLevelsEn;

	public ConfigurationController(final SystemProperties systemProperties) {
		apiPath = systemProperties.getApi().getPath();
		socketioPath = systemProperties.getSocketio().getProxyPath();
	}

	@GetMapping
	@ResponseBody
	public Map<String, Object> getConfiguration(final HttpServletRequest request) {
		final Map<String, Object> config = new HashMap<>();
		final Map<String, Boolean> features = new HashMap<>();
		final Map<String, String> publicPool = new HashMap<>();
		final Map<String, Object> splashscreen = new HashMap<>();

		/* The API path could be unknown to the client in case the request was forwarded */
		if ("".equals(apiPath)) {
			apiPath = request.getContextPath();
		}
		config.put("apiPath", apiPath);


		if (!"".equals(socketioPath)) {
			config.put("socketioPath", socketioPath);
		}
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
		if (!"".equals(demoRoomShortId)) {
			config.put("demoRoomShortId", demoRoomShortId);
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
		config.put("feedbackWarningOffset", Integer.valueOf(feedbackWarningOffset));
		config.put("parseAnswerOptionFormatting", Boolean.valueOf(parseAnswerOptionFormatting));

		config.put("features", features);

		features.put("mathJax", "true".equals(mathJaxEnabled));
		/* Keep the markdown property for now since the frontend still depends on it */
		features.put("markdown", true);
		features.put("imageAnswer", "true".equals(imageAnswerEnabled));
		features.put("gridSquare", "true".equals(gridSquareEnabled));
		features.put("sessionImportExport", "true".equals(roomImportExportEnabled));
		features.put("publicPool", "true".equals(publicPoolEnabled));
		features.put("exportToClick", "true".equals(exportToClickEnabled));

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
		if (!"".equals(splashscreenSloganColor)) {
			splashscreen.put("sloganColor", splashscreenSloganColor);
		}
		if (!"".equals(splashscreenBgColor)) {
			splashscreen.put("bgcolor", splashscreenBgColor);
		}
		if (!"".equals(splashscreenLoadingIndColor)) {
			splashscreen.put("loadIndColor", splashscreenLoadingIndColor);
		}
		if (!"".equals(splashscreenDelay)) {
			splashscreen.put("minDelay", Integer.valueOf(splashscreenDelay));
		}

		if (!"".equals(trackingTrackerUrl)) {
			final Map<String, String> tracking = new HashMap<>();
			config.put("tracking", tracking);

			tracking.put("provider", trackingProvider);
			tracking.put("trackerUrl", trackingTrackerUrl);
			tracking.put("siteId", trackingSiteId);
		}

		config.put("grid", gridImageMaxFileSize);

		return config;
	}
}
