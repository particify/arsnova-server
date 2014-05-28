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
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.UnauthorizedException;

public class ApplicationPermissionEvaluator implements PermissionEvaluator {

	@Autowired
	private IDatabaseDao dao;

	@Override
	public boolean hasPermission(final Authentication authentication, final Object targetDomainObject, final Object permission) {
		final String username = getUsername(authentication);

		if (
				targetDomainObject instanceof Session
				&& ! checkSessionPermission(username, ((Session)targetDomainObject).getKeyword(), permission)
				) {
			throw new ForbiddenException();
		}
		return true;
	}

	@Override
	public boolean hasPermission(final Authentication authentication, final Serializable targetId, final String targetType, final Object permission) {
		final String username = getUsername(authentication);

		if ("session".equals(targetType) && ! checkSessionPermission(username, targetId, permission)) {
			throw new ForbiddenException();
		} else if ("question".equals(targetType) && ! checkQuestionPermission(username, targetId, permission)) {
			throw new ForbiddenException();
		} else if ("interposedquestion".equals(targetType) && ! checkInterposedQuestionPermission(username, targetId, permission)) {
			throw new ForbiddenException();
		}
		return true;
	}

	private boolean checkSessionPermission(final String username, final Serializable targetId, final Object permission) {
		if (permission instanceof String && permission.equals("owner")) {
			return dao.getSession(targetId.toString()).getCreator().equals(username);
		}
		return false;
	}

	private boolean checkQuestionPermission(final String username, final Serializable targetId, final Object permission) {
		if (permission instanceof String && permission.equals("owner")) {
			final Question question = dao.getQuestion(targetId.toString());
			if (question != null) {
				final Session session = dao.getSessionFromId(question.getSessionId());
				if (session == null) {
					return false;
				}
				return session.getCreator().equals(username);
			}
		}
		return false;
	}

	private boolean checkInterposedQuestionPermission(final String username, final Serializable targetId, final Object permission) {
		if (permission instanceof String && permission.equals("owner")) {
			final InterposedQuestion question = dao.getInterposedQuestion(targetId.toString());
			if (question != null) {
				final Session session = dao.getSessionFromId(question.getSessionId());
				if (session == null) {
					return false;
				}
				return session.getCreator().equals(username);
			}
		}
		return false;
	}

	private String getUsername(final Authentication authentication) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new UnauthorizedException();
		}

		if (authentication instanceof OAuthAuthenticationToken) {
			User user = null;

			final OAuthAuthenticationToken token = (OAuthAuthenticationToken) authentication;
			if (token.getUserProfile() instanceof Google2Profile) {
				final Google2Profile profile = (Google2Profile) token.getUserProfile();
				user = new User(profile);
			} else if (token.getUserProfile() instanceof TwitterProfile) {
				final TwitterProfile profile = (TwitterProfile) token.getUserProfile();
				user = new User(profile);
			} else if (token.getUserProfile() instanceof FacebookProfile) {
				final FacebookProfile profile = (FacebookProfile) token.getUserProfile();
				user = new User(profile);
			}

			if (user != null) {
				return user.getUsername();
			}
		}

		return authentication.getName();
	}
}
