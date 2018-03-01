/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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

import de.thm.arsnova.config.SecurityConfig;
import de.thm.arsnova.controller.AbstractController;
import de.thm.arsnova.entities.ServiceDescription;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.security.User;
import de.thm.arsnova.services.UserService;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.Google2Client;
import org.pac4j.oauth.client.TwitterClient;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Handles authentication specific requests.
 */
@Controller("v2AuthenticationController")
@RequestMapping("/v2/auth")
public class AuthenticationController extends AbstractController {
	@Value("${api.path:}") private String apiPath;
	@Value("${customization.path}") private String customizationPath;

	@Value("${security.guest.enabled}") private boolean guestEnabled;
	@Value("${security.guest.allowed-roles:speaker,student}") private String[] guestRoles;
	@Value("${security.guest.order}") private int guestOrder;

	@Value("${security.custom-login.enabled}") private boolean customLoginEnabled;
	@Value("${security.custom-login.allowed-roles:speaker,student}") private String[] customLoginRoles;
	@Value("${security.custom-login.title:University}") private String customLoginTitle;
	@Value("${security.custom-login.login-dialog-path}") private String customLoginDialog;
	@Value("${security.custom-login.image:}") private String customLoginImage;
	@Value("${security.custom-login.order}") private int customLoginOrder;

	@Value("${security.user-db.enabled}") private boolean dbAuthEnabled;
	@Value("${security.user-db.allowed-roles:speaker,student}") private String[] dbAuthRoles;
	@Value("${security.user-db.title:ARSnova}") private String dbAuthTitle;
	@Value("${security.user-db.login-dialog-path}") private String dbAuthDialog;
	@Value("${security.user-db.image:}") private String dbAuthImage;
	@Value("${security.user-db.order}") private int dbAuthOrder;

	@Value("${security.ldap.enabled}") private boolean ldapEnabled;
	@Value("${security.ldap.allowed-roles:speaker,student}") private String[] ldapRoles;
	@Value("${security.ldap.title:LDAP}") private String ldapTitle;
	@Value("${security.ldap.login-dialog-path}") private String ldapDialog;
	@Value("${security.ldap.image:}") private String ldapImage;
	@Value("${security.ldap.order}") private int ldapOrder;

	@Value("${security.cas.enabled}") private boolean casEnabled;
	@Value("${security.cas.allowed-roles:speaker,student}") private String[] casRoles;
	@Value("${security.cas.title:CAS}") private String casTitle;
	@Value("${security.cas.image:}") private String casImage;
	@Value("${security.cas.order}") private int casOrder;

	@Value("${security.facebook.enabled}") private boolean facebookEnabled;
	@Value("${security.facebook.allowed-roles:speaker,student}") private String[] facebookRoles;
	@Value("${security.facebook.order}") private int facebookOrder;

	@Value("${security.google.enabled}") private boolean googleEnabled;
	@Value("${security.google.allowed-roles:speaker,student}") private String[] googleRoles;
	@Value("${security.google.order}") private int googleOrder;

	@Value("${security.twitter.enabled}") private boolean twitterEnabled;
	@Value("${security.twitter.allowed-roles:speaker,student}") private String[] twitterRoles;
	@Value("${security.twitter.order}") private int twitterOrder;

	@Autowired
	private ServletContext servletContext;

	@Autowired(required = false)
	private TwitterClient twitterClient;

	@Autowired(required = false)
	private Google2Client google2Client;

	@Autowired(required = false)
	private FacebookClient facebookClient;

	@Autowired(required = false)
	private CasAuthenticationEntryPoint casEntryPoint;

	@Autowired
	private UserService userService;

	private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

	@PostConstruct
	private void init() {
		if ("".equals(apiPath)) {
			apiPath = servletContext.getContextPath();
		}
	}

	@RequestMapping(value = { "/login", "/doLogin" }, method = { RequestMethod.POST, RequestMethod.GET })
	public void doLogin(
			@RequestParam("type") final String type,
			@RequestParam(value = "user", required = false) String username,
			@RequestParam(required = false) final String password,
			final HttpServletRequest request,
			final HttpServletResponse response
	) throws IOException {
		String addr = request.getRemoteAddr();
		if (userService.isBannedFromLogin(addr)) {
			response.sendError(429, "Too Many Requests");

			return;
		}
		final UsernamePasswordAuthenticationToken authRequest =
				new UsernamePasswordAuthenticationToken(username, password);

		if (dbAuthEnabled && "arsnova".equals(type)) {
			try {
				userService.authenticate(authRequest, UserProfile.AuthProvider.ARSNOVA);
			} catch (AuthenticationException e) {
				logger.info("Database authentication failed.", e);
				userService.increaseFailedLoginCount(addr);
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
			}
		} else if (ldapEnabled && "ldap".equals(type)) {
			try {
				userService.authenticate(authRequest, UserProfile.AuthProvider.LDAP);
			} catch (AuthenticationException e) {
				logger.info("LDAP authentication failed.", e);
				userService.increaseFailedLoginCount(addr);
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
			}
		} else if (guestEnabled && "guest".equals(type)) {
			try {
				userService.authenticate(authRequest, UserProfile.AuthProvider.ARSNOVA_GUEST);
			} catch (final AuthenticationException e) {
				logger.debug("Guest authentication failed.", e);
				userService.increaseFailedLoginCount(addr);
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
			}
		} else {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
		}
	}

	@RequestMapping(value = { "/dialog" }, method = RequestMethod.GET)
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

		request.getSession().setAttribute("ars-login-success-url", serverUrl + successUrl);
		request.getSession().setAttribute("ars-login-failure-url", serverUrl + failureUrl);

		if (casEnabled && "cas".equals(type)) {
			casEntryPoint.commence(request, response, null);
		} else if (twitterEnabled && "twitter".equals(type)) {
			result = new RedirectView(
					twitterClient.getRedirectAction(new J2EContext(request, response)).getLocation());
		} else if (facebookEnabled && "facebook".equals(type)) {
			facebookClient.setFields("id");
			facebookClient.setScope("");
			result = new RedirectView(
					facebookClient.getRedirectAction(new J2EContext(request, response)).getLocation());
		} else if (googleEnabled && "google".equals(type)) {
			google2Client.setScope(Google2Client.Google2Scope.EMAIL);
			result = new RedirectView(
					google2Client.getRedirectAction(new J2EContext(request, response)).getLocation());
		} else {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
		}

		return result;
	}

	@RequestMapping(value = { "/", "/whoami" }, method = RequestMethod.GET)
	@ResponseBody
	public ClientAuthentication whoami(@AuthenticationPrincipal User user) {
		if (user == null) {
			throw new UnauthorizedException();
		}
		return new ClientAuthentication(user);
	}

	@RequestMapping(value = { "/logout" }, method = { RequestMethod.POST, RequestMethod.GET })
	public String doLogout(final HttpServletRequest request) {
		final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		userService.removeUserFromMaps(userService.getCurrentUser());
		request.getSession().invalidate();
		SecurityContextHolder.clearContext();
		if (auth instanceof CasAuthenticationToken) {
			return "redirect:" + apiPath + SecurityConfig.CAS_LOGOUT_PATH_SUFFIX;
		}
		return "redirect:" + request.getHeader("referer") != null ? request.getHeader("referer") : "/";
	}

	@RequestMapping(value = { "/services" }, method = RequestMethod.GET)
	@ResponseBody
	public List<ServiceDescription> getServices(final HttpServletRequest request) {
		List<ServiceDescription> services = new ArrayList<>();

		/* The first parameter is replaced by the backend, the second one by the frondend */
		String dialogUrl = apiPath + "/auth/dialog?type={0}&successurl='{0}'";

		if (guestEnabled) {
			ServiceDescription sdesc = new ServiceDescription(
				"guest",
				"Guest",
				null,
				guestRoles
			);
			sdesc.setOrder(guestOrder);
			services.add(sdesc);
		}

		if (customLoginEnabled && !"".equals(customLoginDialog)) {
			ServiceDescription sdesc = new ServiceDescription(
				"custom",
				customLoginTitle,
				customizationPath + "/" + customLoginDialog + "?redirect={0}",
				customLoginRoles,
				customLoginImage
			);
			sdesc.setOrder(customLoginOrder);
			services.add(sdesc);
		}

		if (dbAuthEnabled && !"".equals(dbAuthDialog)) {
			ServiceDescription sdesc = new ServiceDescription(
				"arsnova",
				dbAuthTitle,
				customizationPath + "/" + dbAuthDialog + "?redirect={0}",
				dbAuthRoles,
				dbAuthImage
			);
			sdesc.setOrder(dbAuthOrder);
			services.add(sdesc);
		}

		if (ldapEnabled && !"".equals(ldapDialog)) {
			ServiceDescription sdesc = new ServiceDescription(
				"ldap",
				ldapTitle,
				customizationPath + "/" + ldapDialog + "?redirect={0}",
				ldapRoles,
				ldapImage
			);
			sdesc.setOrder(ldapOrder);
			services.add(sdesc);
		}

		if (casEnabled) {
			ServiceDescription sdesc = new ServiceDescription(
				"cas",
				casTitle,
				MessageFormat.format(dialogUrl, "cas"),
				casRoles
			);
			sdesc.setOrder(casOrder);
			services.add(sdesc);
		}

		if (facebookEnabled) {
			ServiceDescription sdesc = new ServiceDescription(
				"facebook",
				"Facebook",
				MessageFormat.format(dialogUrl, "facebook"),
				facebookRoles
			);
			sdesc.setOrder(facebookOrder);
			services.add(sdesc);
		}

		if (googleEnabled) {
			ServiceDescription sdesc = new ServiceDescription(
				"google",
				"Google",
				MessageFormat.format(dialogUrl, "google"),
				googleRoles
			);
			sdesc.setOrder(googleOrder);
			services.add(sdesc);
		}

		if (twitterEnabled) {
			ServiceDescription sdesc = new ServiceDescription(
				"twitter",
				"Twitter",
				MessageFormat.format(dialogUrl, "twitter"),
				twitterRoles
			);
			sdesc.setOrder(twitterOrder);
			services.add(sdesc);
		}

		return services;
	}

	private Collection<GrantedAuthority> getAuthorities(final boolean admin) {
		List<GrantedAuthority> authList = new ArrayList<>();
		authList.add(new SimpleGrantedAuthority("ROLE_USER"));
		if (admin) {
			authList.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}

		return authList;
	}
}
