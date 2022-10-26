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

package de.thm.arsnova.controller.v2;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.controller.AbstractController;

/**
 * The ConfigurationController provides frontend clients with information necessary to correctly interact with the
 * backend and other frontends as well as settings for ARSnova. The the alternative /arsnova-config route is necessary
 * in case the backend application is deployed as root context.
 */
@Controller("v2ConfigurationController")
@RequestMapping({"/v2/configuration", "/v2/arsnova-config"})
public class ConfigurationController extends AbstractController {
	private static final Logger logger = LoggerFactory
			.getLogger(ConfigurationController.class);

	private ServletContext servletContext;
	private String apiPath;
	private String socketioPath;

	@Value("${customization.path}")
	private String customizationPath;

	@Value("${mobile.path}")
	private String mobilePath;

	@Value("${presenter.path}")
	private String presenterPath;

	@Value("${ui.links.overlay.url}")
	private String overlayUrl;

	@Value("${ui.links.organization.url}")
	private String organizationUrl;

	@Value("${ui.links.imprint.url}")
	private String imprintUrl;

	@Value("${ui.links.blog.url:}")
	private String blogUrl;

	@Value("${ui.links.privacy-policy.url}")
	private String privacyPolicyUrl;

	@Value("${ui.links.documentation.url}")
	private String documentationUrl;

	@Value("${ui.links.presenter-documentation.url}")
	private String presenterDocumentationUrl;

	@Value("${ui.feedback.warning:5}")
	private String feedbackWarningOffset;

	@Value("${ui.mathjax.enabled:true}")
	private String mathJaxEnabled;

	@Value("${ui.mathjax.src:}")
	private String mathJaxSrc;

	@Value("${features.contents.formats.freetext.imageanswer.enabled:false}")
	private String imageAnswerEnabled;

	@Value("${features.contents.formats.grid-square.enabled:false}")
	private String gridSquareEnabled;

	private String roomImportExportEnabled = "true";

	@Value("${features.content-pool.enabled:false}")
	private String publicPoolEnabled;

	@Value("${features.contents.answer-option-limit:8}")
	private String answerOptionLimit;

	@Value("${system.uploads.max-filesize:}")
	private String maxUploadFilesize;

	@Value("${ui.parse-answer-option-formatting:false}")
	private String parseAnswerOptionFormatting;

	@Value("${features.content-pool.subjects}")
	private String ppSubjects;

	@Value("${features.content-pool.licenses}")
	private String ppLicenses;

	@Value("${features.content-pool.logo-max-filesize}")
	private String ppLogoMaxFilesize;

	@Value("${system.uploads.max-filesize}")
	private String gridImageMaxFileSize;

	@Value("${ui.tracking.provider}")
	private String trackingProvider;

	@Value("${ui.tracking.tracker-url}")
	private String trackingTrackerUrl;

	@Value("${ui.tracking.site-id}")
	private String trackingSiteId;

	@Value("${ui.demo-room-id:}")
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

	@Value("${features.content-pool.session-levels.de}")
	private String ppLevelsDe;

	@Value("${features.content-pool.session-levels.en}")
	private String ppLevelsEn;

	public ConfigurationController(final SystemProperties systemProperties, final ServletContext servletContext) {
		apiPath = systemProperties.getApi().getProxyPath();
		socketioPath = systemProperties.getSocketio().getProxyPath();
		this.servletContext = servletContext;
	}

	@PostConstruct
	private void init() {
		if (apiPath == null || "".equals(apiPath)) {
			apiPath = servletContext.getContextPath();
		}
	}

	@GetMapping
	@ResponseBody
	public Map<String, Object> getConfiguration(final HttpServletRequest request) {
		final Map<String, Object> config = new HashMap<>();
		final Map<String, Boolean> features = new HashMap<>();
		final Map<String, String> publicPool = new HashMap<>();
		final Map<String, Object> splashscreen = new HashMap<>();

		config.put("apiPath", apiPath + "/v2");

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
		features.put("exportToClick", false);

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
