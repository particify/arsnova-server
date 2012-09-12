package de.thm.arsnova;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Controller
public class WebSocketController {
	
	@Autowired
	ARSnovaSocketIOServer server;
	
	@Autowired
	IUserService userService;

	public static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

	@RequestMapping(method = RequestMethod.POST, value = "/authorize")
	public void authorize(@RequestBody String session, HttpServletResponse response) {
		boolean result = server.authorize(session, userService.getUser(SecurityContextHolder.getContext().getAuthentication()));
		response.setStatus(result ? HttpStatus.CREATED.value() : HttpStatus.SERVICE_UNAVAILABLE.value());
	}

}
