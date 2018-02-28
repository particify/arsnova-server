package de.thm.arsnova.security.jwt;

import de.thm.arsnova.security.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class JwtToken extends AbstractAuthenticationToken {
	private String token;
	private User principal;

	public JwtToken(final String token, final User principal,
			final Collection<? extends GrantedAuthority> grantedAuthorities) {
		super(grantedAuthorities);
		this.token = token;
		this.principal = principal;
	}

	public JwtToken(final String token) {
		this(token, null, Collections.emptyList());
	}

	@Override
	public Object getCredentials() {
		return token;
	}

	@Override
	public Object getPrincipal() {
		return principal;
	}
}
