/*
 * Copyright (C) 2012 THM webMedia
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.scribe.up.provider.impl.FacebookProvider;
import org.scribe.up.provider.impl.Google2Provider;
import org.scribe.up.provider.impl.TwitterProvider;
import org.scribe.up.session.HttpUserSession;
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

import de.thm.arsnova.entities.DbUser;
import de.thm.arsnova.entities.ServiceDescription;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.UserSessionService;

@Controller
public class LoginController extends AbstractController {

	private static final int MAX_USERNAME_LENGTH = 15;
	private static final int MAX_GUESTHASH_LENGTH = 10;

	@Value("${security.guest.enabled}")
	private String guestEnabled;
	@Value("${security.guest.lecturer.enabled}")
	private String guestLecturerEnabled;
	@Value("${security.cas.enabled}")
	private String casEnabled;
	@Value("${security.ldap.enabled}")
	private String ldapEnabled;
	@Value("${security.facebook.enabled}")
	private String facebookEnabled;
	@Value("${security.google.enabled}")
	private String googleEnabled;
	@Value("${security.twitter.enabled}")
	private String twitterEnabled;

	@Autowired
	private DaoAuthenticationProvider daoProvider;

	@Autowired
	private TwitterProvider twitterProvider;

	@Autowired
	private Google2Provider googleProvider;

	@Autowired
	private FacebookProvider facebookProvider;
	
	@Autowired
	private LdapAuthenticationProvider ldapAuthenticationProvider;

	@Autowired
	private CasAuthenticationEntryPoint casEntryPoint;

	@Autowired
	private IUserService userService;

	@Autowired
	private UserSessionService userSessionService;

	public static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

	@RequestMapping(value = { "/auth/login", "/doLogin" }, method = { RequestMethod.POST, RequestMethod.GET })
	public final View doLogin(
			@RequestParam("type") final String type,
			@RequestParam(value = "user", required = false) String username,
			@RequestParam(required = false) final String password,
			@RequestParam(value = "referer", required = false) final String forcedReferer,
			@RequestParam(value = "successurl", required = false) final String successUrl,
			@RequestParam(value = "failureurl", required = false) final String failureUrl,
			@RequestParam(value = "role", required = false) UserSessionService.Role role,
			final HttpServletRequest request,
			final HttpServletResponse response
	) throws IOException, ServletException {
		userSessionService.setRole(role);

		String referer = request.getHeader("referer");
		if (null != forcedReferer && null != referer && !UrlUtils.isAbsoluteUrl(referer)) {
			/* Use a url from a request parameter as referer as long as the url is not absolute (to prevent
			 * abuse of the redirection). */
			referer = forcedReferer;
		}
		if (null == referer) {
			referer = "/";
		}

		request.getSession().setAttribute("ars-login-success-url",
			null == successUrl ? referer + "#auth/checkLogin" : successUrl
		);
		request.getSession().setAttribute("ars-login-failure-url",
			null == failureUrl ? referer : failureUrl
		);

		View result = null;

		if ("arsnova".equals(type)) {
			Authentication authRequest = new UsernamePasswordAuthenticationToken(username, password);
			Authentication auth = daoProvider.authenticate(authRequest);
			if (auth.isAuthenticated()) {
				SecurityContextHolder.getContext().setAuthentication(auth);
				request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
						SecurityContextHolder.getContext());
				
				return null;
			}

			response.setStatus(HttpStatus.UNAUTHORIZED.value());

			return null;
		} else if ("ldap".equals(type)) {
			if (!"".equals(username) && !"".equals(password)) {
				org.springframework.security.core.userdetails.User user =
						new org.springframework.security.core.userdetails.User(
							username, password, true, true, true, true, this.getAuthorities()
						);

				Authentication token = new UsernamePasswordAuthenticationToken(user, password, getAuthorities());
				try {
					Authentication auth = ldapAuthenticationProvider.authenticate(token);
					if (auth.isAuthenticated()) {
						SecurityContextHolder.getContext().setAuthentication(token);
						request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
								SecurityContextHolder.getContext());

						return null;
					}
					LOGGER.info("LDAPLOGIN: {}", auth.isAuthenticated());
				}
				catch (AuthenticationException e) {
					LOGGER.info("No LDAP login: {}", e);
				}

				response.setStatus(HttpStatus.UNAUTHORIZED.value());

				return null;
			}
		} else if ("cas".equals(type)) {
			casEntryPoint.commence(request, response, null);
		} else if ("twitter".equals(type)) {
			String authUrl = twitterProvider.getAuthorizationUrl(new HttpUserSession(request));
			result = new RedirectView(authUrl);
		} else if ("facebook".equals(type)) {
			String authUrl = facebookProvider.getAuthorizationUrl(new HttpUserSession(request));
			result = new RedirectView(authUrl);
		} else if ("google".equals(type)) {
			String authUrl = googleProvider.getAuthorizationUrl(new HttpUserSession(request));
			result = new RedirectView(authUrl);
		} else if ("guest".equals(type)) {
			List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
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
			result = new RedirectView(null == successUrl ? referer + "#auth/checkLogin" : successUrl);
		}

		return result;
	}

	@RequestMapping(value = { "/auth/", "/whoami" }, method = RequestMethod.GET)
	@ResponseBody
	public final User whoami() {
		userSessionService.setUser(userService.getCurrentUser());		
		return userService.getCurrentUser();
	}

	@RequestMapping(value = { "/auth/logout", "/logout" }, method = { RequestMethod.POST, RequestMethod.GET } )
	public final View doLogout(final HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		userService.removeUserFromMaps(userService.getCurrentUser());
		request.getSession().invalidate();
		SecurityContextHolder.clearContext();
		if (auth instanceof CasAuthenticationToken) {
			return new RedirectView("/j_spring_cas_security_logout");
		}
		return new RedirectView(request.getHeader("referer") != null ? request.getHeader("referer") : "/");
	}
	
	@RequestMapping(value = { "/auth/services" }, method = RequestMethod.GET)
	@ResponseBody
	public final List<ServiceDescription> getServices(final HttpServletRequest request) {
		List<ServiceDescription> services = new ArrayList<ServiceDescription>();

		if ("true".equals(guestEnabled)) {
			ServiceDescription sdesc = new ServiceDescription(
				"Guest",
				null
			);
			if (!"true".equals(guestLecturerEnabled)) {
				sdesc.setAllowLecturer(false);
			}
			services.add(sdesc);
		}

		if ("true".equals(casEnabled)) {
			try {
				services.add(new ServiceDescription(
						"CAS",
						casEntryPoint.getLoginUrl()
							+ "?" + casEntryPoint.getServiceProperties().getServiceParameter()
							+ "=" + URLEncoder.encode(casEntryPoint.getServiceProperties().getService(), "UTF-8")
					));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if ("true".equals(facebookEnabled)) {
			services.add(new ServiceDescription(
				"Facebook",
				facebookProvider.getAuthorizationUrl(new HttpUserSession(request))
			));
		}

		if ("true".equals(googleEnabled)) {
			services.add(new ServiceDescription(
				"Google",
				googleProvider.getAuthorizationUrl(new HttpUserSession(request))
			));
		}

		if ("true".equals(twitterEnabled)) {
			services.add(new ServiceDescription(
				"Twitter",
				twitterProvider.getAuthorizationUrl(new HttpUserSession(request))
			));
		}

		return services;
	}

	@RequestMapping(value = { "/auth/register" }, method = RequestMethod.POST)
	public final void register(
			@RequestParam final String username,
			@RequestParam final String password,
			final HttpServletRequest request,
			final HttpServletResponse response
	) {
		if (null != userService.createDbUser(username, password)) {
			return;
		}

		/* TODO: Improve error handling: send reason to client */
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

	@RequestMapping(value = { "/auth/activate" }, method = {RequestMethod.POST, RequestMethod.GET})
	public final void activate(
			@RequestParam final String username,
			@RequestParam final String key,
			final HttpServletRequest request,
			final HttpServletResponse response
	) {
		DbUser dbUser = userService.getDbUser(username);
		if (null != dbUser && key.equals(dbUser.getActivationKey())) {
			dbUser.setActivationKey(null);
			userService.updateDbUser(dbUser);

			return;
		}

		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

	private Collection<GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();
		authList.add(new SimpleGrantedAuthority("ROLE_USER"));
		return authList;
	}

	@RequestMapping(value = { "/test/me" }, method = RequestMethod.GET)
	@ResponseBody
	public final User me() {
		User me = userSessionService.getUser();
		if (me == null) {
			throw new UnauthorizedException();
		}
		return me;
	}

	@RequestMapping(value = { "/test/mysession" }, method = RequestMethod.GET)
	@ResponseBody
	public final Session mysession() {
		Session mysession = userSessionService.getSession();
		if (mysession == null) {
			throw new UnauthorizedException();
		}
		return mysession;
	}

	@RequestMapping(value = { "/test/myrole" }, method = RequestMethod.GET)
	@ResponseBody
	public final UserSessionService.Role myrole() {
		UserSessionService.Role myrole = userSessionService.getRole();
		if (myrole == null) {
			throw new UnauthorizedException();
		}
		return myrole;
	}
}
