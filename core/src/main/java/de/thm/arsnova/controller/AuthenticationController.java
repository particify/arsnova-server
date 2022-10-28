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

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.saml.client.SAML2Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import de.thm.arsnova.config.SecurityConfig;
import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.model.ClientAuthentication;
import de.thm.arsnova.model.LoginCredentials;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.security.LoginAuthenticationSucessHandler;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.web.exceptions.NotImplementedException;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
	private UserService userService;
	private OidcClient oidcClient;
	private SAML2Client saml2Client;
	private CasAuthenticationEntryPoint casEntryPoint;
	private String apiPath;

	public AuthenticationController(
			@Qualifier("securedUserService") final UserService userService,
			final SystemProperties systemProperties,
			final ServletContext servletContext) {
		this.userService = userService;
		final String proxyPath = systemProperties.getApi().getProxyPath();
		this.apiPath = proxyPath != null && !proxyPath.isEmpty() ? proxyPath : servletContext.getContextPath();
	}

	@Autowired(required = false)
	public void setOidcClient(final OidcClient oidcClient) {
		this.oidcClient = oidcClient;
	}

	@Autowired(required = false)
	public void setSaml2Client(final SAML2Client saml2Client) {
		this.saml2Client = saml2Client;
	}

	@Autowired(required = false)
	public void setCasEntryPoint(final CasAuthenticationEntryPoint casEntryPoint) {
		this.casEntryPoint = casEntryPoint;
	}

	@PostMapping("/login")
	public ClientAuthentication login(@RequestParam(defaultValue = "false") final boolean refresh,
			final HttpServletRequest request, final HttpServletResponse response) {
		if (request.getCookies() != null && Arrays.stream(request.getCookies())
				.anyMatch(c -> c.getName().equalsIgnoreCase(LoginAuthenticationSucessHandler.AUTH_COOKIE_NAME))) {
			/* Delete cookie */
			final Cookie cookie = new Cookie(LoginAuthenticationSucessHandler.AUTH_COOKIE_NAME, null);
			cookie.setPath(apiPath);
			cookie.setMaxAge(0);
			response.addCookie(cookie);
		}

		final ClientAuthentication authentication = userService.getCurrentClientAuthentication(refresh);
		if (authentication == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		}
		return authentication;
	}

	@PostMapping("/login/guest")
	public ClientAuthentication loginGuest(
			final HttpServletRequest request,
			@RequestBody(required = false) final LoginCredentials loginCredentials) {
		final String guestId = loginCredentials != null ? loginCredentials.getLoginId() : null;
		if (guestId == null) {
			final ClientAuthentication currentAuthentication = userService.getCurrentClientAuthentication(false);
			if (currentAuthentication != null
					&& currentAuthentication.getAuthProvider() == UserProfile.AuthProvider.ARSNOVA_GUEST) {
				return currentAuthentication;
			}
		}
		userService.authenticate(new UsernamePasswordAuthenticationToken(guestId, null),
				UserProfile.AuthProvider.ARSNOVA_GUEST, request.getRemoteAddr());

		return userService.getCurrentClientAuthentication(false);
	}

	@PostMapping("/login/{providerId}")
	public ClientAuthentication loginViaProvider(
			@PathVariable final String providerId,
			@RequestBody final LoginCredentials loginCredentials,
			final HttpServletRequest request) {
		switch (providerId) {
			case "registered":
				final String loginId = loginCredentials.getLoginId().toLowerCase();
				userService.authenticate(new UsernamePasswordAuthenticationToken(
						loginId, loginCredentials.getPassword()),
						UserProfile.AuthProvider.ARSNOVA, request.getRemoteAddr());

				return userService.getCurrentClientAuthentication(false);
			case SecurityConfig.LDAP_PROVIDER_ID:
				userService.authenticate(new UsernamePasswordAuthenticationToken(
						loginCredentials.getLoginId(), loginCredentials.getPassword()),
						UserProfile.AuthProvider.LDAP, request.getRemoteAddr());

				return userService.getCurrentClientAuthentication(false);
			default:
				throw new IllegalArgumentException("Invalid provider ID.");
		}
	}

	@GetMapping("/sso/{providerId}")
	public View redirectToSso(@PathVariable final String providerId,
			final HttpServletRequest request, final HttpServletResponse response)
			throws IOException {
		switch (providerId) {
			case SecurityConfig.OIDC_PROVIDER_ID:
				if (oidcClient == null) {
					throw new IllegalArgumentException("Invalid provider ID.");
				}
				return buildSsoRedirectView(oidcClient, request, response);
			case SecurityConfig.SAML_PROVIDER_ID:
				if (saml2Client == null) {
					throw new IllegalArgumentException("Invalid provider ID.");
				}
				return buildSsoRedirectView(saml2Client, request, response);
			case SecurityConfig.CAS_PROVIDER_ID:
				if (casEntryPoint == null) {
					throw new IllegalArgumentException("Invalid provider ID.");
				}
				casEntryPoint.commence(request, response, null);
				return null;
			default:
				throw new IllegalArgumentException("Invalid provider ID.");
		}
	}

	private RedirectView buildSsoRedirectView(
			final IndirectClient client,
			final HttpServletRequest request,
			final HttpServletResponse response) {
		final JEEContext context = new JEEContext(request, response);
		final Optional<RedirectView> view = client.getRedirectionAction(context).map(action -> {
			if (action instanceof WithLocationAction) {
				return new RedirectView(((WithLocationAction) action).getLocation());
			}
			return null;
		});
		return view.orElseThrow(() -> {
			throw new IllegalStateException("No URL for redirect found.");
		});
	}

	@GetMapping(value = "/config/saml/sp-metadata.xml", produces = MediaType.APPLICATION_XML_VALUE)
	public String samlSpMetadata() throws IOException {
		if (saml2Client == null) {
			throw new NotImplementedException("SAML authentication is disabled.");
		}

		return saml2Client.getServiceProviderMetadataResolver().getMetadata();
	}
}
