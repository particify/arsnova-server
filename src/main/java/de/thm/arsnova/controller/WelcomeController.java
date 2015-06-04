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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

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

import springfox.documentation.annotations.ApiIgnore;

import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.NoContentException;

/**
 * Default controller that handles requests which have not set a path.
 */
@Controller
@ApiIgnore
public class WelcomeController extends AbstractController {

	@Value("${mobile.path}")
	private String mobileContextPath;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public View home(final HttpServletRequest request) {
		return new RedirectView(mobileContextPath + "/", false);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public HashMap<String, Object> jsonHome(final HttpServletRequest request) {
		return new HashMap<String, Object>();
	}

	@RequestMapping(value = "/checkframeoptionsheader", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	public void checkFrameOptionsHeader(
			@RequestParam(required = true) final String url,
			final HttpServletRequest request
		) {
		/* Block requests from the server itself to prevent DoS attacks caused by request loops */
		if ("127.0.0.1".equals(request.getRemoteAddr())) {
			throw new BadRequestException();
		}
		/* Block requests to servers in private networks */
		try {
			final InetAddress addr = InetAddress.getByName(new URL(url).getHost());
			if (addr.isSiteLocalAddress()) {
				throw new BadRequestException();
			}
		} catch (UnknownHostException | MalformedURLException e) {
			throw new BadRequestException();
		}

		RestTemplate restTemplate = new RestTemplate();
		SimpleClientHttpRequestFactory rf = (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();
		rf.setConnectTimeout(2000);
		rf.setReadTimeout(2000);

		try {
			HttpHeaders headers = restTemplate.headForHeaders(url);
			if (headers.isEmpty() || headers.containsKey("x-frame-options")) {
				throw new NoContentException();
			}
		} catch (RestClientException e) {
			throw new NoContentException();
		}
	}
}
