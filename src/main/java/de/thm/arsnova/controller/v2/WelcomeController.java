package de.thm.arsnova.controller.v2;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller("v2WelcomeController")
@RequestMapping("/v2")
public class WelcomeController {
	@RequestMapping(value = "/")
	public String forwardHome() {
		return "forward:/";
	}
}
