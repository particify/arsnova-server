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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.core.userdetails.UserDetails;
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

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.services.UserSessionService;

@Controller
public class LoginController extends AbstractController {

	private static final int MAX_USERNAME_LENGTH = 15;
	private static final int MAX_GUESTHASH_LENGTH = 10;

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

	@RequestMapping(value = { "/auth/login", "/doLogin" }, method = RequestMethod.GET)
	public final View doLogin(
			@RequestParam("type") final String type,
			@RequestParam(value = "user", required = false) final String guestName,
			@RequestParam(value = "referer", required = false) final String forcedReferer,
			@RequestParam(value = "successurl", required = false) final String successUrl,
			@RequestParam(value = "failureurl", required = false) final String failureUrl,
			final HttpServletRequest request,
			final HttpServletResponse response
	) throws IOException, ServletException {
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

		if ("cas".equals(type)) {
			casEntryPoint.commence(request, response, null);
		} else if ("twitter".equals(type)) {
			String authUrl = twitterProvider.getAuthorizationUrl(new HttpUserSession(request));
			return new RedirectView(authUrl);
		} else if ("facebook".equals(type)) {
			String authUrl = facebookProvider.getAuthorizationUrl(new HttpUserSession(request));
			return new RedirectView(authUrl);
		} else if ("google".equals(type)) {
			String authUrl = googleProvider.getAuthorizationUrl(new HttpUserSession(request));
			return new RedirectView(authUrl);
		} else if ("guest".equals(type)) {
			List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
			authorities.add(new SimpleGrantedAuthority("ROLE_GUEST"));
			String username = "";
			if (guestName != null && guestName.startsWith("Guest") && guestName.length() == MAX_USERNAME_LENGTH) {
				username = guestName;
			} else {
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
			return new RedirectView(null == successUrl ? referer + "#auth/checkLogin" : successUrl);
		}
		return null;
	}
	
	@RequestMapping(value = { "/auth/ldaplogin" }, method = RequestMethod.POST)
	public final View doLdapLogin(
			@RequestParam("type") final String type,
			@RequestParam(value = "user") final String userName,
			@RequestParam(value = "referer", required = false) final String forcedReferer,
			@RequestParam(value = "password") final String password,
			final HttpServletRequest request,
			final HttpServletResponse response
	) {
		if ("ldap".equals(type) && password != null) {
			String referer = request.getHeader("referer");
			if (null != forcedReferer && null != referer && !UrlUtils.isAbsoluteUrl(referer)) {
				referer = forcedReferer;
			}
			if (null == referer) {
				referer = "/";
			}
			org.springframework.security.core.userdetails.User user =
					new org.springframework.security.core.userdetails.User(
						userName, password, true, true, true, true, this.getAuthorities()
					);
			
			Authentication token = new UsernamePasswordAuthenticationToken(user, password, getAuthorities());
			try {
				Authentication auth = ldapAuthenticationProvider.authenticate(token);
				LOGGER.info("LDAPLOGIN: {}", auth.isAuthenticated());
				return new RedirectView(referer + "#auth/checkLogin");
			}
			catch (AuthenticationException e) {
				LOGGER.info("No LDAP login: {}", e);
				return new RedirectView("/login.html");
			}
		}
		return null;
	}

	@RequestMapping(value = { "/auth/", "/whoami" }, method = RequestMethod.GET)
	@ResponseBody
	public final User whoami() {
		return userService.getCurrentUser();
	}

	@RequestMapping(value = { "/auth/logout", "/logout" }, method = RequestMethod.GET)
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
		
	private Collection<GrantedAuthority> getAuthorities() {
		List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();
		authList.add(new GrantedAuthorityImpl("ROLE_USER"));
		return authList;
	}
	
	@RequestMapping(value = { "/test/me" }, method = RequestMethod.GET)
	@ResponseBody
	public final User me() {
		return userSessionService.getUser();
	}

	@RequestMapping(value = { "/test/mysession" }, method = RequestMethod.GET)
	@ResponseBody
	public final Session mysession() {
		return userSessionService.getSession();
	}
}
