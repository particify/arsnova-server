package de.thm.arsnova.services;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.IUserService;

public class StubUserService implements IUserService {

	private User stubUser = null;
	
	public void setUserAuthenticated(boolean isAuthenticated) {
		if (isAuthenticated) {
			stubUser = new User(new UsernamePasswordAuthenticationToken("ptsr00","testpassword"));
			return;
		}
		stubUser = null;
	}
	
	@Override
	public User getUser(Authentication authentication) {
		return stubUser;
	}
}
