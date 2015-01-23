/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
import de.thm.arsnova.exceptions.UnauthorizedException;

public class ApplicationPermissionEvaluator implements PermissionEvaluator {

	@Autowired
	private IDatabaseDao dao;

	@Override
	public boolean hasPermission(
			final Authentication authentication,
			final Object targetDomainObject,
			final Object permission
			) {
		final String username = getUsername(authentication);

		if (
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

		if (
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

	private boolean checkSessionPermission(
			final String username,
			final Serializable targetId,
			final Object permission
			) {
		if (permission instanceof String && (permission.equals("owner") || permission.equals("write"))) {
			return dao.getSession(targetId.toString()).getCreator().equals(username);
		} else if (permission instanceof String && permission.equals("read")) {
			return dao.getSession(targetId.toString()).isActive();
		}
		return false;
	}

	private boolean checkQuestionPermission(
			final String username,
			final Serializable targetId,
			final Object permission
			) {
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

	private boolean checkInterposedQuestionPermission(
			final String username,
			final Serializable targetId,
			final Object permission
			) {
		if (permission instanceof String && permission.equals("owner")) {
			final InterposedQuestion question = dao.getInterposedQuestion(targetId.toString());
			if (question != null) {
				// Does the creator want to delete his own question?
				if (question.getCreator() != null && question.getCreator().equals(username)) {
					return true;
				}
				// Allow deletion if requested by session owner
				final Session session = dao.getSessionFromKeyword(question.getSessionId());
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
