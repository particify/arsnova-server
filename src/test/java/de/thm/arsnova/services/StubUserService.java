package de.thm.arsnova.services;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import de.thm.arsnova.entities.User;

public class StubUserService extends UserService {

	private User stubUser = null;
	
	public void setUserAuthenticated(boolean isAuthenticated) {
		this.setUserAuthenticated(isAuthenticated, "ptsr00");
	}
	
	public void setUserAuthenticated(boolean isAuthenticated, String username) {
		if (isAuthenticated) {
			stubUser = new User(new UsernamePasswordAuthenticationToken(username, "testpassword"));
			return;
		}
		stubUser = null;
	}

	public void useAnonymousUser() {
		stubUser = new User(new UsernamePasswordAuthenticationToken("anonymous", ""));
	}

	@Override
	public User getCurrentUser() {
		return stubUser;
	}
}
