package de.thm.arsnova.security.jwt;

import de.thm.arsnova.security.User;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationProvider implements AuthenticationProvider {
	private JwtService jwtService;

	public JwtAuthenticationProvider(final JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
		final String token = (String) authentication.getCredentials();
		final User user = jwtService.verifyToken((String) authentication.getCredentials());

		return new JwtToken(token, user, user.getAuthorities());
	}

	@Override
	public boolean supports(final Class<?> aClass) {
		return JwtToken.class.isAssignableFrom(aClass);
	}
}
