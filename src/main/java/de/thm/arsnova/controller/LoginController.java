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
package de.thm.arsnova.controller;

import de.thm.arsnova.entities.ServiceDescription;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.UserService;
import de.thm.arsnova.services.UserSessionService;
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
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
@Controller
public class LoginController extends AbstractController {

	private static final int MAX_USERNAME_LENGTH = 15;
	private static final int MAX_GUESTHASH_LENGTH = 10;

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
	private DaoAuthenticationProvider daoProvider;

	@Autowired(required = false)
	private TwitterClient twitterClient;

	@Autowired(required = false)
	private Google2Client google2Client;

	@Autowired(required = false)
	private FacebookClient facebookClient;

	@Autowired(required = false)
	private LdapAuthenticationProvider ldapAuthenticationProvider;

	@Autowired(required = false)
	private CasAuthenticationEntryPoint casEntryPoint;

	@Autowired
	private UserService userService;

	@Autowired
	private UserSessionService userSessionService;

	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

	@PostConstruct
	private void init() {
		if ("".equals(apiPath)) {
			apiPath = servletContext.getContextPath();
		}
	}

	@RequestMapping(value = { "/auth/login", "/doLogin" }, method = { RequestMethod.POST, RequestMethod.GET })
	public void doLogin(
			@RequestParam("type") final String type,
			@RequestParam(value = "user", required = false) String username,
			@RequestParam(required = false) final String password,
			@RequestParam(value = "role", required = false) final UserSessionService.Role role,
			final HttpServletRequest request,
			final HttpServletResponse response
	) throws IOException {
		String addr = request.getRemoteAddr();
		if (userService.isBannedFromLogin(addr)) {
			response.sendError(429, "Too Many Requests");

			return;
		}

		userSessionService.setRole(role);

		if (dbAuthEnabled && "arsnova".equals(type)) {
			Authentication authRequest = new UsernamePasswordAuthenticationToken(username, password);
			try {
				Authentication auth = daoProvider.authenticate(authRequest);
				if (auth.isAuthenticated()) {
					SecurityContextHolder.getContext().setAuthentication(auth);
					request.getSession(true).setAttribute(
							HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
							SecurityContextHolder.getContext());

					return;
				}
			} catch (AuthenticationException e) {
				logger.info("Database authentication failed.", e);
			}

			userService.increaseFailedLoginCount(addr);
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
		} else if (ldapEnabled && "ldap".equals(type)) {
			if (!"".equals(username) && !"".equals(password)) {
				org.springframework.security.core.userdetails.User user =
						new org.springframework.security.core.userdetails.User(
							username, password, true, true, true, true, this.getAuthorities(userService.isAdmin(username))
						);

				Authentication token = new UsernamePasswordAuthenticationToken(user, password, getAuthorities(userService.isAdmin(username)));
				try {
					Authentication auth = ldapAuthenticationProvider.authenticate(token);
					if (auth.isAuthenticated()) {
						SecurityContextHolder.getContext().setAuthentication(auth);
						request.getSession(true).setAttribute(
								HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
								SecurityContextHolder.getContext());

						return;
					}
					logger.info("LDAP authentication failed.");
				} catch (AuthenticationException e) {
					logger.info("LDAP authentication failed.", e);
				}

				userService.increaseFailedLoginCount(addr);
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
			}
		} else if (guestEnabled && "guest".equals(type)) {
			List<GrantedAuthority> authorities = new ArrayList<>();
			authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
			if (username == null || !username.startsWith("Guest") || username.length() != MAX_USERNAME_LENGTH) {
				username = "Guest" + Sha512DigestUtils.shaHex(request.getSession().getId()).substring(0, MAX_GUESTHASH_LENGTH);
			}
			org.springframework.security.core.userdetails.User user =
					new org.springframework.security.core.userdetails.User(
							username, "", true, true, true, true, authorities
					);
			Authentication token = new UsernamePasswordAuthenticationToken(user, null, authorities);

			SecurityContextHolder.getContext().setAuthentication(token);
			request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
					SecurityContextHolder.getContext());
		} else {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
		}
	}

	@RequestMapping(value = { "/auth/dialog" }, method = RequestMethod.GET)
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

		/* Handle proxy
		 * TODO: It might be better, to support the proposed standard: http://tools.ietf.org/html/rfc7239 */
		String host = null != request.getHeader("X-Forwarded-Host")
				? request.getHeader("X-Forwarded-Host") : request.getServerName();
		int port = null != request.getHeader("X-Forwarded-Port")
				? Integer.valueOf(request.getHeader("X-Forwarded-Port")) : request.getServerPort();
		String scheme = null != request.getHeader("X-Forwarded-Proto")
				? request.getHeader("X-Forwarded-Proto") : request.getScheme();

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
			facebookClient.setFields("id,link");
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

	@RequestMapping(value = { "/auth/", "/whoami" }, method = RequestMethod.GET)
	@ResponseBody
	public User whoami() {
		userSessionService.setUser(userService.getCurrentUser());
		return userService.getCurrentUser();
	}

	@RequestMapping(value = { "/auth/logout", "/logout" }, method = { RequestMethod.POST, RequestMethod.GET })
	public View doLogout(final HttpServletRequest request) {
		final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		userService.removeUserFromMaps(userService.getCurrentUser());
		request.getSession().invalidate();
		SecurityContextHolder.clearContext();
		if (auth instanceof CasAuthenticationToken) {
			return new RedirectView(apiPath + "/j_spring_cas_security_logout");
		}
		return new RedirectView(request.getHeader("referer") != null ? request.getHeader("referer") : "/");
	}

	@RequestMapping(value = { "/auth/services" }, method = RequestMethod.GET)
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

	@RequestMapping(value = { "/test/me" }, method = RequestMethod.GET)
	@ResponseBody
	public User me() {
		final User me = userSessionService.getUser();
		if (me == null) {
			throw new UnauthorizedException();
		}
		return me;
	}

	@RequestMapping(value = { "/test/mysession" }, method = RequestMethod.GET)
	@ResponseBody
	public Session mysession() {
		final Session mysession = userSessionService.getSession();
		if (mysession == null) {
			throw new UnauthorizedException();
		}
		return mysession;
	}

	@RequestMapping(value = { "/test/myrole" }, method = RequestMethod.GET)
	@ResponseBody
	public UserSessionService.Role myrole() {
		final UserSessionService.Role myrole = userSessionService.getRole();
		if (myrole == null) {
			throw new UnauthorizedException();
		}
		return myrole;
	}
}
