package de.thm.arsnova.security.jwt;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JwtTokenFilter extends AbstractAuthenticationProcessingFilter {
	private static final String JWT_HEADER_NAME = "Arsnova-Auth-Token";

	protected JwtTokenFilter() {
		super(new AntPathRequestMatcher("/**"));
	}

	@Override
	public Authentication attemptAuthentication(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws AuthenticationException {
		String jwtHeader = httpServletRequest.getHeader(JWT_HEADER_NAME);
		if (jwtHeader == null) {
			throw new PreAuthenticatedCredentialsNotFoundException("No authentication header present.");
		}
		JwtToken token = new JwtToken(jwtHeader);

		return getAuthenticationManager().authenticate(token);
	}
}
