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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.client.OidcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import de.thm.arsnova.config.SecurityConfig;
import de.thm.arsnova.config.properties.AuthenticationProviderProperties;
import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.controller.AbstractController;
import de.thm.arsnova.model.ServiceDescription;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.model.migration.v2.ClientAuthentication;
import de.thm.arsnova.security.LoginAuthenticationFailureHandler;
import de.thm.arsnova.security.LoginAuthenticationSucessHandler;
import de.thm.arsnova.security.User;
import de.thm.arsnova.service.UserService;
import de.thm.arsnova.web.exceptions.UnauthorizedException;

/**
 * Handles authentication specific requests.
 */
@Controller("v2AuthenticationController")
@RequestMapping("/v2/auth")
public class AuthenticationController extends AbstractController {
	private String apiPath;
	@Value("${customization.path}") private String customizationPath;

	private AuthenticationProviderProperties.Guest guestProperties;
	private AuthenticationProviderProperties.Registered registeredProperties;
	private List<AuthenticationProviderProperties.Ldap> ldapProperties;
	private List<AuthenticationProviderProperties.Oidc> oidcProperties;
	private AuthenticationProviderProperties.Cas casProperties;
	private Map<String, AuthenticationProviderProperties.Oauth> oauthProperties;

	@Autowired
	private ServletContext servletContext;

	@Autowired
	private AuthenticationProviderProperties providerProperties;

	@Autowired(required = false)
	private OidcClient oidcClient;

	@Autowired(required = false)
	private TwitterClient twitterClient;

	@Autowired(required = false)
	private GoogleOidcClient googleOidcClient;

	@Autowired(required = false)
	private FacebookClient facebookClient;

	@Autowired(required = false)
	private CasAuthenticationEntryPoint casEntryPoint;

	@Autowired
	private UserService userService;

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

	public AuthenticationController(final SystemProperties systemProperties,
			final AuthenticationProviderProperties authenticationProviderProperties) {
		apiPath = systemProperties.getApi().getProxyPath();
		guestProperties = authenticationProviderProperties.getGuest();
		registeredProperties = authenticationProviderProperties.getRegistered();
		ldapProperties = authenticationProviderProperties.getLdap();
		oidcProperties = authenticationProviderProperties.getOidc();
		casProperties = authenticationProviderProperties.getCas();
		oauthProperties = authenticationProviderProperties.getOauth();
	}

	@PostConstruct
	private void init() {
		if (apiPath == null || "".equals(apiPath)) {
			apiPath = servletContext.getContextPath();
		}
	}

	@PostMapping({ "/login", "/doLogin" })
	public void doLogin(
			@RequestParam("type") final String type,
			@RequestParam(value = "user", required = false) final String username,
			@RequestParam(required = false) final String password,
			final HttpServletRequest request,
			final HttpServletResponse response
	) throws IOException {
		final String address = request.getRemoteAddr();
		if (userService.isBannedFromLogin(address)) {
			response.sendError(429, "Too Many Requests");

			return;
		}
		final UsernamePasswordAuthenticationToken authRequest =
				new UsernamePasswordAuthenticationToken(username, password);

		if (registeredProperties.isEnabled() && "arsnova".equals(type)) {
			try {
				userService.authenticate(authRequest, UserProfile.AuthProvider.ARSNOVA, address);
			} catch (final AuthenticationException e) {
				logger.info("Database authentication failed.", e);
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
			}
		} else if (ldapProperties.stream().anyMatch(p -> p.isEnabled()) && "ldap".equals(type)) {
			try {
				userService.authenticate(authRequest, UserProfile.AuthProvider.LDAP, address);
			} catch (final AuthenticationException e) {
				logger.info("LDAP authentication failed.", e);
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
			}
		} else if (guestProperties.isEnabled() && "guest".equals(type)) {
			try {
				userService.authenticate(authRequest, UserProfile.AuthProvider.ARSNOVA_GUEST, address);
			} catch (final AuthenticationException e) {
				logger.debug("Guest authentication failed.", e);
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
			}
		} else {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
		}
	}

	@GetMapping("/dialog")
	@ResponseBody
	public View dialog(
			@RequestParam("type") final String type,
			@RequestParam(value = "successurl", defaultValue = "/") String successUrl,
			@RequestParam(value = "failureurl", defaultValue = "/") String failureUrl,
			final HttpServletRequest request,
			final HttpServletResponse response
	) throws HttpAction, IOException, ServletException {
		View result = null;

		/* Use URLs from a request parameters for redirection as long as the
		 * URL is not absolute (to prevent abuse of the redirection). */
		if (UrlUtils.isAbsoluteUrl(successUrl)) {
			successUrl = "/";
		}
		if (UrlUtils.isAbsoluteUrl(failureUrl)) {
			failureUrl = "/";
		}

		final String host = request.getServerName();
		final int port = request.getServerPort();
		final String scheme = request.getScheme();

		String serverUrl = scheme + "://" + host;
		if ("https".equals(scheme)) {
			if (443 != port) {
				serverUrl = serverUrl + ":" + String.valueOf(port);
			}
		} else {
			if (80 != port) {
				serverUrl = serverUrl + ":" + String.valueOf(port);
			}
		}

		request.getSession().setAttribute(LoginAuthenticationSucessHandler.URL_ATTRIBUTE, serverUrl + successUrl);
		request.getSession().setAttribute(LoginAuthenticationFailureHandler.URL_ATTRIBUTE, serverUrl + failureUrl);

		if (casProperties.isEnabled() && "cas".equals(type)) {
			casEntryPoint.commence(request, response, null);
		} else if (oidcProperties.stream().anyMatch(p -> p.isEnabled()) && "oidc".equals(type)) {
			result = new RedirectView(
					oidcClient.getRedirectAction(new J2EContext(request, response)).getLocation());
		} else if (twitterClient != null && "twitter".equals(type)) {
			result = new RedirectView(
					twitterClient.getRedirectAction(new J2EContext(request, response)).getLocation());
		} else if (facebookClient != null && "facebook".equals(type)) {
			facebookClient.setFields("id");
			facebookClient.setScope("");
			result = new RedirectView(
					facebookClient.getRedirectAction(new J2EContext(request, response)).getLocation());
		} else if (googleOidcClient != null && "google".equals(type)) {
			result = new RedirectView(
					googleOidcClient.getRedirectAction(new J2EContext(request, response)).getLocation());
		} else {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
		}

		return result;
	}

	@GetMapping({ "/", "/whoami" })
	@ResponseBody
	public ClientAuthentication whoami(@AuthenticationPrincipal final User user) {
		if (user == null) {
			throw new UnauthorizedException();
		}
		return new ClientAuthentication(user);
	}

	@PostMapping("/logout")
	public String doLogout(final HttpServletRequest request) {
		final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		userService.removeUserIdFromMaps(userService.getCurrentUser().getId());
		request.getSession().invalidate();
		SecurityContextHolder.clearContext();
		if (auth instanceof CasAuthenticationToken) {
			return "redirect:" + apiPath + SecurityConfig.CAS_LOGOUT_PATH;
		}
		return "redirect:" + (request.getHeader("referer") != null ? request.getHeader("referer") : "/");
	}

	@GetMapping("/services")
	@ResponseBody
	public List<ServiceDescription> getServices(final HttpServletRequest request) {
		final List<ServiceDescription> services = new ArrayList<>();

		/* The first parameter is replaced by the backend, the second one by the frondend */
		final String dialogUrl = apiPath + "/v2/auth/dialog?type={0}&successurl='{0}'";

		if (guestProperties.isEnabled()) {
			final ServiceDescription sdesc = new ServiceDescription(
					"guest",
					"Guest",
					null,
					guestProperties.getAllowedRoles()
			);
			sdesc.setOrder(guestProperties.getOrder());
			services.add(sdesc);
		}

		if (registeredProperties.isEnabled()) {
			final ServiceDescription sdesc = new ServiceDescription(
					"arsnova",
					registeredProperties.getTitle(),
					customizationPath + "/login?provider=arsnova&redirect={0}",
					registeredProperties.getAllowedRoles()
			);
			sdesc.setOrder(registeredProperties.getOrder());
			services.add(sdesc);
		}

		if (ldapProperties.get(0).isEnabled()) {
			final ServiceDescription sdesc = new ServiceDescription(
					SecurityConfig.LDAP_PROVIDER_ID,
					ldapProperties.get(0).getTitle(),
					customizationPath + "/login?provider=" + SecurityConfig.LDAP_PROVIDER_ID + "&redirect={0}",
					ldapProperties.get(0).getAllowedRoles()
			);
			sdesc.setOrder(ldapProperties.get(0).getOrder());
			services.add(sdesc);
		}

		if (casProperties.isEnabled()) {
			final ServiceDescription sdesc = new ServiceDescription(
					SecurityConfig.CAS_PROVIDER_ID,
					casProperties.getTitle(),
					MessageFormat.format(dialogUrl, SecurityConfig.CAS_PROVIDER_ID),
					casProperties.getAllowedRoles()
			);
			sdesc.setOrder(casProperties.getOrder());
			services.add(sdesc);
		}

		if (oidcProperties.get(0).isEnabled()) {
			final ServiceDescription sdesc = new ServiceDescription(
					SecurityConfig.OIDC_PROVIDER_ID,
					oidcProperties.get(0).getTitle(),
					MessageFormat.format(dialogUrl, SecurityConfig.OIDC_PROVIDER_ID),
					oidcProperties.get(0).getAllowedRoles()
			);
			sdesc.setOrder(oidcProperties.get(0).getOrder());
			services.add(sdesc);
		}

		final AuthenticationProviderProperties.Oauth facebookProperties =
				oauthProperties.get(SecurityConfig.FACEBOOK_PROVIDER_ID);
		if (facebookProperties != null && facebookProperties.isEnabled()) {
			final ServiceDescription sdesc = new ServiceDescription(
					SecurityConfig.FACEBOOK_PROVIDER_ID,
					"Facebook",
					MessageFormat.format(dialogUrl, SecurityConfig.FACEBOOK_PROVIDER_ID),
					facebookProperties.getAllowedRoles()
			);
			sdesc.setOrder(facebookProperties.getOrder());
			services.add(sdesc);
		}

		final AuthenticationProviderProperties.Oauth googleProperties =
				oauthProperties.get(SecurityConfig.GOOGLE_PROVIDER_ID);
		if (googleProperties != null && googleProperties.isEnabled()) {
			final ServiceDescription sdesc = new ServiceDescription(
					SecurityConfig.GOOGLE_PROVIDER_ID,
					"Google",
					MessageFormat.format(dialogUrl, SecurityConfig.GOOGLE_PROVIDER_ID),
					googleProperties.getAllowedRoles()
			);
			sdesc.setOrder(googleProperties.getOrder());
			services.add(sdesc);
		}

		final AuthenticationProviderProperties.Oauth twitterProperties =
				oauthProperties.get(SecurityConfig.TWITTER_PROVIDER_ID);
		if (twitterProperties != null && twitterProperties.isEnabled()) {
			final ServiceDescription sdesc = new ServiceDescription(
					SecurityConfig.TWITTER_PROVIDER_ID,
					"Twitter",
					MessageFormat.format(dialogUrl, SecurityConfig.TWITTER_PROVIDER_ID),
					twitterProperties.getAllowedRoles()
			);
			sdesc.setOrder(twitterProperties.getOrder());
			services.add(sdesc);
		}

		return services;
	}

	private Collection<GrantedAuthority> getAuthorities(final boolean admin) {
		final List<GrantedAuthority> authList = new ArrayList<>();
		authList.add(new SimpleGrantedAuthority("ROLE_USER"));
		if (admin) {
			authList.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}

		return authList;
	}
}
