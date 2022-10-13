package de.thm.arsnova.service.comment.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationProvider implements AuthenticationProvider {
  private JwtService jwtService;

  @Override
  public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
    final String token = (String) authentication.getCredentials();
    final AuthenticatedUser user = jwtService.verifyToken((String) authentication.getCredentials());
    return new JwtToken(token, user, user.getAuthorities());
  }

  @Override
  public boolean supports(final Class<?> authentication) {
    return JwtToken.class.isAssignableFrom(authentication);
  }

  @Autowired
  public void setJwtService(final JwtService jwtService) {
    this.jwtService = jwtService;
  }
}
