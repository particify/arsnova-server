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
	
	@RequestMapping(method = RequestMethod.GET, value = "/openIdLogin")
	public ModelAndView openIdLogin() {
		return new ModelAndView("openidlogin");
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/doOpenIdLogin")
	public ModelAndView doOpenIdLogin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		User user = (User) authentication.getPrincipal();
		String userHash = new ShaPasswordEncoder(256).encodePassword(user.getUsername(), "");
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
