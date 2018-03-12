package de.thm.arsnova.controller;

import de.thm.arsnova.entities.LoginCredentials;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.services.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UserController.REQUEST_MAPPING)
public class UserController extends AbstractEntityController<UserProfile> {
	protected static final String REQUEST_MAPPING = "/user";

	private UserService userService;

	public UserController(final UserService userService) {
		super(userService);
		this.userService = userService;
	}

	@Override
	protected String getMapping() {
		return REQUEST_MAPPING;
	}

	@PostMapping("/register")
	public void register(@RequestBody LoginCredentials loginCredentials) {
		userService.create(loginCredentials.getLoginId(), loginCredentials.getPassword());
	}
}
