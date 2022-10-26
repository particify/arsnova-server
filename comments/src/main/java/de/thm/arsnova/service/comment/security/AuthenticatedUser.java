package de.thm.arsnova.service.comment.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticatedUser {

    private String id;
    private Collection<? extends GrantedAuthority> authorities;
    private String token;

    public AuthenticatedUser(
            final String userId,
            final Collection<? extends GrantedAuthority> authorities,
            final String token
    ) {
        this.id = userId;
        this.authorities = authorities;
        this.token = token;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getId() {
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