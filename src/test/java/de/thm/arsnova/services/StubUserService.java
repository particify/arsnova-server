package de.thm.arsnova.services;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import de.thm.arsnova.entities.User;

public class StubUserService extends UserService {

	private User stubUser = null;

	public void setUserAuthenticated(boolean isAuthenticated) {
		if (isAuthenticated) {
			stubUser = new User(new UsernamePasswordAuthenticationToken(
					"ptsr00", "testpassword"));
			return;
		}
		stubUser = null;
	}

	public void useAnonymousUser() {
		stubUser = new User(new UsernamePasswordAuthenticationToken(
				"anonymous", ""));
	}

	@Override
	public User getCurrentUser() {
		return stubUser;
	}
}
