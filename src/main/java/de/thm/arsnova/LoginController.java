/*
 * ARSnova
 * 
 * Copyright (C) 2012 THM Web Media
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova;

import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {
	
	@RequestMapping(method = RequestMethod.GET, value = "/doCasLogin")
	public ModelAndView doCasLogin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User user = (User) authentication.getPrincipal();
		return new ModelAndView("redirect:/#auth/checkCasLogin/" + user.getUsername());
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/doOpenIdLogin")
	public ModelAndView doOpenIdLogin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		String userHash = null;
		
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

		return new ModelAndView("redirect:/#auth/checkCasLogin/" + userHash);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/doFacebookLogin")
	public ModelAndView doFacebookLogin() {
		return new ModelAndView("redirect:/#auth/checkCasLogin/");
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/doTwitterLogin")
	public ModelAndView doTwitterLogin() {
		return new ModelAndView("redirect:/#auth/checkCasLogin/");
	}
}
