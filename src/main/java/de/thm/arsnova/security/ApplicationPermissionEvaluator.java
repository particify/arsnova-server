/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.security;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.UnauthorizedException;
import org.pac4j.oauth.profile.facebook.FacebookProfile;
import org.pac4j.oauth.profile.google2.Google2Profile;
import org.pac4j.oauth.profile.twitter.TwitterProfile;
import org.pac4j.springframework.security.authentication.Pac4jAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Provides access control methods that can be used in annotations.
 */
public class ApplicationPermissionEvaluator implements PermissionEvaluator {

	@Value("${security.admin-accounts}")
	private String[] adminAccounts;

	@Autowired
	private IDatabaseDao dao;

	@Override
	public boolean hasPermission(
			final Authentication authentication,
			final Object targetDomainObject,
			final Object permission
			) {
		final String username = getUsername(authentication);
		if (checkAdminPermission(username)) {
			return true;
		} else if (
				targetDomainObject instanceof Session
				&& checkSessionPermission(username, ((Session) targetDomainObject).getKeyword(), permission)
				) {
			return true;
		}
		return false;
	}

	@Override
	public boolean hasPermission(
			final Authentication authentication,
			final Serializable targetId,
			final String targetType,
			final Object permission
			) {
		final String username = getUsername(authentication);
		if (checkAdminPermission(username)) {
			return true;
		} else if (
				"session".equals(targetType)
				&& checkSessionPermission(username, targetId, permission)) {
			return true;
		} else if (
				"question".equals(targetType)
				&& checkQuestionPermission(username, targetId, permission)
				) {
			return true;
		} else if (
				"interposedquestion".equals(targetType)
				&& checkInterposedQuestionPermission(username, targetId, permission)
				) {
			return true;
		}
		return false;
	}

	private boolean checkAdminPermission(final String username) {
		/* TODO: only allow accounts from arsnova db */
		return Arrays.asList(adminAccounts).contains(username);
	}

	private boolean checkSessionPermission(
			final String username,
			final Serializable targetId,
			final Object permission
			) {
		if (permission instanceof String && ("owner".equals(permission) || "write".equals(permission))) {
			return dao.getSessionFromKeyword(targetId.toString()).getCreator().equals(username);
		} else if (permission instanceof String && "read".equals(permission)) {
			return dao.getSessionFromKeyword(targetId.toString()).isActive();
		}
		return false;
	}

	private boolean checkQuestionPermission(
			final String username,
			final Serializable targetId,
			final Object permission
			) {
		if (permission instanceof String && "owner".equals(permission)) {
			final Question question = dao.getQuestion(targetId.toString());
			if (question != null) {
				final Session session = dao.getSessionFromId(question.getSessionId());

				return session != null && session.getCreator().equals(username);
			}
		}
		return false;
	}

	private boolean checkInterposedQuestionPermission(
			final String username,
			final Serializable targetId,
			final Object permission
			) {
		if (permission instanceof String && "owner".equals(permission)) {
			final InterposedQuestion question = dao.getInterposedQuestion(targetId.toString());
			if (question != null) {
				// Does the creator want to delete his own question?
				if (question.getCreator() != null && question.getCreator().equals(username)) {
					return true;
				}
				// Allow deletion if requested by session owner
				final Session session = dao.getSessionFromKeyword(question.getSessionId());

				return session != null && session.getCreator().equals(username);
			}
		}
		return false;
	}

	private String getUsername(final Authentication authentication) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			throw new UnauthorizedException();
		}

		if (authentication instanceof Pac4jAuthenticationToken) {
			User user = null;

			final Pac4jAuthenticationToken token = (Pac4jAuthenticationToken) authentication;
			if (token.getProfile() instanceof Google2Profile) {
				final Google2Profile profile = (Google2Profile) token.getProfile();
				user = new User(profile);
			} else if (token.getProfile() instanceof TwitterProfile) {
				final TwitterProfile profile = (TwitterProfile) token.getProfile();
				user = new User(profile);
			} else if (token.getProfile() instanceof FacebookProfile) {
				final FacebookProfile profile = (FacebookProfile) token.getProfile();
				user = new User(profile);
			}

			if (user != null) {
				return user.getUsername();
			}
		}

		return authentication.getName();
	}
}
