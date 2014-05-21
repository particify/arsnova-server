package de.thm.arsnova.security;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.exceptions.UnauthorizedException;

public class SessionPermissionEvaluator implements PermissionEvaluator {

	@Autowired
	IDatabaseDao dao;

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		UserDetails user = getUserDetails(authentication);
		return false;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
		UserDetails user = getUserDetails(authentication);

		if ("session".equals(targetType)) {
			return checkSessionPermission(user, targetId, permission);
		}
		return false;
	}

	private boolean checkSessionPermission(UserDetails user, Serializable targetId, Object permission) {
		if (permission instanceof String && permission.equals("owner")) {
			return dao.getSession(targetId.toString()).getCreator().equals(user.getUsername());
		}
		return false;
	}

	private UserDetails getUserDetails(Authentication authentication)
			throws UnauthorizedException {
		if (authentication.getPrincipal() == null || authentication.getPrincipal() instanceof String) {
			throw new UnauthorizedException();
		}

		return (UserDetails)authentication.getPrincipal();
	}
}
