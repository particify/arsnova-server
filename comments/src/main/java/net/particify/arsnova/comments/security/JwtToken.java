package net.particify.arsnova.comments.security;

import java.util.Collection;
import java.util.Collections;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtToken extends AbstractAuthenticationToken {
  private String token;
  private AuthenticatedUser principal;

  public JwtToken(final String token, final AuthenticatedUser principal,
      final Collection<? extends GrantedAuthority> grantedAuthorities) {
    super(grantedAuthorities);
    this.token = token;
    this.principal = principal;
    setAuthenticated(!grantedAuthorities.isEmpty());
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
