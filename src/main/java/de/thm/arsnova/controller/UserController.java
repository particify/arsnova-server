package de.thm.arsnova.controller;

import de.thm.arsnova.entities.LoginCredentials;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.services.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController extends AbstractEntityController<UserProfile> {
	private UserService userService;

	public UserController(final UserService userService) {
		super(userService);
		this.userService = userService;
	}

	@PostMapping("/register")
	public void register(@RequestBody LoginCredentials loginCredentials) {
		userService.create(loginCredentials.getLoginId(), loginCredentials.getPassword());
	}
}
