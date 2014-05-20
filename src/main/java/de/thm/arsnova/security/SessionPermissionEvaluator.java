package de.thm.arsnova.security;

import java.io.Serializable;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import de.thm.arsnova.exceptions.UnauthorizedException;

public class SessionPermissionEvaluator implements PermissionEvaluator {

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		UserDetails user = getUserDetails(authentication);
		return false;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
		UserDetails user = getUserDetails(authentication);
		return false;
	}

	private UserDetails getUserDetails(Authentication authentication)
			throws UnauthorizedException {
		if (authentication.getPrincipal() instanceof String) {
			throw new UnauthorizedException();
		}

		return (UserDetails)authentication.getPrincipal();
	}
}
