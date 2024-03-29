package net.particify.arsnova.comments.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

import net.particify.arsnova.common.uuid.UuidHelper;

public class AuthenticatedUser implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private UUID id;
  private Collection<? extends GrantedAuthority> authorities;
  private String token;

  public AuthenticatedUser(
      final UUID userId,
      final Collection<? extends GrantedAuthority> authorities,
      final String token
  ) {
    this.id = userId;
    this.authorities = authorities;
    this.token = token;
  }

  public AuthenticatedUser(
    final String userId,
    final Collection<? extends GrantedAuthority> authorities,
    final String token
  ) {
    this(UuidHelper.stringToUuid(userId), authorities, token);
  }

  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  public UUID getId() {
    return id;
  }

  public boolean hasRole(final String role) {
    return getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_" + role));
  }

  public boolean isAdmin() {
    return hasRole("ADMIN");
  }

  public String getToken() {
    return token;
  }

  public void setToken(final String token) {
    this.token = token;
  }

  @Override
  public String toString() {
    return "AuthenticatedUser{" +
        "id='" + id + '\'' +
        ", authorities=" + authorities +
        ", token='" + token + '\'' +
        '}';
  }
}
