package de.thm.arsnova.controller;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import de.thm.arsnova.services.StubUserService;

public abstract class AbstractControllerTest {

	@Autowired protected StubUserService userService;

	public AbstractControllerTest() {
		super();
	}

	protected void setAuthenticated(final boolean isAuthenticated, final String username) {
		final List<GrantedAuthority> ga = new ArrayList<GrantedAuthority>();
		if (isAuthenticated) {
			final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, "secret", ga);
			SecurityContextHolder.getContext().setAuthentication(token);
			userService.setUserAuthenticated(isAuthenticated, username);
		} else {
			userService.setUserAuthenticated(isAuthenticated);
		}
	}

	@After
	public void cleanup() {
		SecurityContextHolder.clearContext();
		userService.setUserAuthenticated(false);
	}

}