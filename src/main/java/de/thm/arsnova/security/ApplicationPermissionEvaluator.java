package de.thm.arsnova.security;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.UnauthorizedException;

public class ApplicationPermissionEvaluator implements PermissionEvaluator {

	@Autowired
	private IDatabaseDao dao;

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		String username = getUsername(authentication);

		if (
				targetDomainObject instanceof Session
				&& ! checkSessionPermission(username, ((Session)targetDomainObject).getKeyword(), permission)
				) {
			throw new ForbiddenException();
		}
		return true;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
		String username = getUsername(authentication);

		if ("session".equals(targetType) && ! checkSessionPermission(username, targetId, permission)) {
			throw new ForbiddenException();
		}
		return true;
	}

	private boolean checkSessionPermission(String username, Serializable targetId, Object permission) {
		if (permission instanceof String && permission.equals("owner")) {
			return dao.getSession(targetId.toString()).getCreator().equals(username);
		}
		return false;
	}

	private String getUsername(Authentication authentication) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new UnauthorizedException();
		}

		return authentication.getName();
	}
}
