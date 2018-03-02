package de.thm.arsnova.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Component
public class JwtTokenFilter extends GenericFilterBean {
	private static final String JWT_HEADER_NAME = "Arsnova-Auth-Token";
	private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);
	private JwtAuthenticationProvider jwtAuthenticationProvider;

	@Override
	public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
		String jwtHeader = ((HttpServletRequest) servletRequest).getHeader(JWT_HEADER_NAME);
		if (jwtHeader != null) {
			JwtToken token = new JwtToken(jwtHeader);
			try {
				Authentication authenticatedToken = jwtAuthenticationProvider.authenticate(token);
				if (authenticatedToken != null) {
					logger.debug("Storing JWT to SecurityContext: {}", authenticatedToken);
					SecurityContextHolder.getContext().setAuthentication(authenticatedToken);
				} else {
					logger.debug("Could not authenticate JWT.");
				}
			} catch (final Exception e) {
				logger.debug("JWT authentication failed", e);
			}
		} else {
			logger.debug("No authentication header present.");
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Autowired
	public void setJwtAuthenticationProvider(final JwtAuthenticationProvider jwtAuthenticationProvider) {
		this.jwtAuthenticationProvider = jwtAuthenticationProvider;
	}
}
