package de.thm.arsnova.controller;

import de.thm.arsnova.entities.ClientAuthentication;
import de.thm.arsnova.entities.LoginCredentials;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.services.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
	private UserService userService;

	public AuthenticationController(final UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/login")
	public ClientAuthentication login() {
		return userService.getCurrentClientAuthentication();
	}

	@PostMapping("/login/registered")
	public ClientAuthentication loginRegistered(@RequestBody LoginCredentials loginCredentials) {
		final String loginId = loginCredentials.getLoginId().toLowerCase();
		userService.authenticate(new UsernamePasswordAuthenticationToken(loginId, loginCredentials.getPassword()),
				UserProfile.AuthProvider.ARSNOVA);
		return userService.getCurrentClientAuthentication();
	}
}
