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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import de.thm.arsnova.management.VersionInfoContributor;
import de.thm.arsnova.web.exceptions.BadRequestException;
import de.thm.arsnova.web.exceptions.NoContentException;

/**
 * Default controller that handles requests which have not set a path.
 */
@Controller
public class WelcomeController extends AbstractController {

	@Value("${mobile.path}")
	private String mobileContextPath;

	@Autowired
	private VersionInfoContributor versionInfoContributor;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public View home() {
		return new RedirectView(mobileContextPath + "/", false);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public Map<String, Object> jsonHome() {
		return versionInfoContributor.getInfoDetails();
	}

	@RequestMapping(value = "/checkframeoptionsheader", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void checkFrameOptionsHeader(
			@RequestParam final String url,
			final HttpServletRequest request) {
		/* Block requests from the server itself to prevent DoS attacks caused by request loops */
		if ("127.0.0.1".equals(request.getRemoteAddr()) || "::1".equals(request.getRemoteAddr())) {
			throw new BadRequestException("Access to localhost not allowed.");
		}
		/* Block requests to servers in private networks */
		try {
			final InetAddress addr = InetAddress.getByName(new URL(url).getHost());
			if (addr.isSiteLocalAddress()) {
				throw new BadRequestException("Access to site-local addresses not allowed.");
			}
		} catch (final UnknownHostException | MalformedURLException e) {
			throw new BadRequestException();
		}

		final RestTemplate restTemplate = new RestTemplate();
		final SimpleClientHttpRequestFactory rf = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
		rf.setConnectTimeout(2000);
		rf.setReadTimeout(2000);

		try {
			final HttpHeaders headers = restTemplate.headForHeaders(url);
			if (headers.isEmpty() || headers.containsKey("x-frame-options")) {
				throw new NoContentException();
			}
		} catch (final RestClientException e) {
			throw new NoContentException();
		}
	}
}
