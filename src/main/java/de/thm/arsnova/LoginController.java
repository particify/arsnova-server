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
package de.thm.arsnova;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {
	
	
	public static final Logger logger = LoggerFactory.getLogger(LoginController.class); 
	
	@RequestMapping(method = RequestMethod.GET, value = "/doCasLogin")
	public ModelAndView doCasLogin(HttpServletRequest request) {
		String referer = request.getHeader("referer");
		String target = "";
		if (referer != null && referer.endsWith("dojo-index.html")) {
			target = "dojo-index.html";
		}
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User user = (User) authentication.getPrincipal();
		
		logger.info("CAS Login for: " + user.getUsername());
		return new ModelAndView("redirect:/" + target + "#auth/checkCasLogin/" + user.getUsername());
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/doOpenIdLogin")
	public ModelAndView doOpenIdLogin() {
		String userHash = hashUser();
		
		logger.info("OpenID Login for user with hash " + userHash);
		return new ModelAndView("redirect:/#auth/checkCasLogin/" + userHash);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/doGuestLogin")
	public ModelAndView doGuestLogin(final HttpServletRequest request) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new GrantedAuthorityImpl("ROLE_GUEST"));
		String username = "Guest" + Sha512DigestUtils.shaHex(request.getSession().getId()).substring(0, 10);
		User user = new User(username, "", true, true, true, true, authorities);
		Authentication token = new UsernamePasswordAuthenticationToken(user, null, authorities);
		
		SecurityContextHolder.getContext().setAuthentication(token);
		request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

		String userHash = hashUser();
		logger.info("Guest Login for user " + user);
		return new ModelAndView("redirect:/#auth/checkCasLogin/" + userHash);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/whoami")
	@ResponseBody
	public String whoami() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication == null || authentication.getPrincipal() == null) {
			return null;			
		}
		return authentication.getPrincipal().toString();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/logout")
	public ModelAndView doLogout(final HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		request.getSession().invalidate();
		if(auth instanceof CasAuthenticationToken) {
			return new ModelAndView("redirect:/j_spring_cas_security_logout");
		}
		return new ModelAndView("redirect:/");
	}
	
	private String hashUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String userHash;
		try {
			User user = (User) authentication.getPrincipal();
			userHash = new ShaPasswordEncoder(256).encodePassword(user.getUsername(), "");
		} catch (ClassCastException e) {
			// Principal is of type String
			userHash = new ShaPasswordEncoder(256).encodePassword(
				(String)authentication.getPrincipal(),
				""
			);
		}
		return userHash;
	}
}
