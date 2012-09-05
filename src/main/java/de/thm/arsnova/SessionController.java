package de.thm.arsnova;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import de.thm.arsnova.entities.Session;
import de.thm.arsnova.services.IAuthenticationService;
import de.thm.arsnova.services.ISessionService;

@Controller
public class SessionController {
	
	@Autowired
	ISessionService sessionService;
	
	@Autowired
	IAuthenticationService authenticationService;
	
	@RequestMapping("/session/{sessionkey}")
	public Session getSession(@PathVariable String sessionkey, HttpServletResponse response) {
		Session session = sessionService.getSession(sessionkey);
		if (session != null) return session;
		
		authenticationService.getUsername();
		
		response.setStatus(HttpStatus.NOT_FOUND.value());
		return null;
	}
}
