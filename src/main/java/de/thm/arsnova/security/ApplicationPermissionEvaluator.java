package de.thm.arsnova.security;

import java.io.Serializable;

import org.scribe.up.profile.facebook.FacebookProfile;
import org.scribe.up.profile.google.Google2Profile;
import org.scribe.up.profile.twitter.TwitterProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.github.leleuj.ss.oauth.client.authentication.OAuthAuthenticationToken;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
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
		} else if ("question".equals(targetType) && ! checkQuestionPermission(username, targetId, permission)) {
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

	private boolean checkQuestionPermission(String username, Serializable targetId, Object permission) {
		if (permission instanceof String && permission.equals("owner")) {
			Question question = dao.getQuestion(targetId.toString());
			if (question != null) {
				Session session = dao.getSessionFromId(question.getSessionId());
				if (session == null) {
					return false;
				}
				return session.getCreator().equals(username);
			}
		}
		return false;
	}

	private String getUsername(Authentication authentication) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new UnauthorizedException();
		}

		if (authentication instanceof OAuthAuthenticationToken) {
			User user = null;

			OAuthAuthenticationToken token = (OAuthAuthenticationToken) authentication;
			if (token.getUserProfile() instanceof Google2Profile) {
				Google2Profile profile = (Google2Profile) token.getUserProfile();
				user = new User(profile);
			} else if (token.getUserProfile() instanceof TwitterProfile) {
				TwitterProfile profile = (TwitterProfile) token.getUserProfile();
				user = new User(profile);
			} else if (token.getUserProfile() instanceof FacebookProfile) {
				FacebookProfile profile = (FacebookProfile) token.getUserProfile();
				user = new User(profile);
			}

			if (user != null) {
				return user.getUsername();
			}
		}

		return authentication.getName();
	}
}
