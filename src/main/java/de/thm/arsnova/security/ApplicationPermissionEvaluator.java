/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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

import de.thm.arsnova.entities.UserAuthentication;
import de.thm.arsnova.entities.migration.v2.Comment;
import de.thm.arsnova.entities.migration.v2.Content;
import de.thm.arsnova.entities.migration.v2.Session;
import de.thm.arsnova.persistance.CommentRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.SessionRepository;
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
	private SessionRepository sessionRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private ContentRepository contentRepository;

	@Override
	public boolean hasPermission(
			final Authentication authentication,
			final Object targetDomainObject,
			final Object permission) {
		if (authentication == null || targetDomainObject == null || !(permission instanceof String)) {
			return false;
		}

		final String username = getUsername(authentication);

		return hasAdminRole(username)
				|| (targetDomainObject instanceof Session
						&& hasSessionPermission(username, ((Session) targetDomainObject), permission.toString()))
				|| (targetDomainObject instanceof Content
						&& hasContentPermission(username, ((Content) targetDomainObject), permission.toString()))
				|| (targetDomainObject instanceof Comment
						&& hasCommentPermission(username, ((Comment) targetDomainObject), permission.toString()));
	}

	@Override
	public boolean hasPermission(
			final Authentication authentication,
			final Serializable targetId,
			final String targetType,
			final Object permission) {
		if (authentication == null || targetId == null || targetType == null || !(permission instanceof String)) {
			return false;
		}

		final String username = getUsername(authentication);
		if (hasAdminRole(username)) {
			return true;
		}

		switch (targetType) {
			case "session":
				final Session targetSession = sessionRepository.findByKeyword(targetId.toString());
				return targetSession != null && hasSessionPermission(username, targetSession, permission.toString());
			case "content":
				final Content targetContent = contentRepository.findOne(targetId.toString());
				return targetContent != null && hasContentPermission(username, targetContent, permission.toString());
			case "comment":
				final Comment targetComment = commentRepository.findOne(targetId.toString());
				return targetComment != null && hasCommentPermission(username, targetComment, permission.toString());
			default:
				return false;
		}
	}

	private boolean hasSessionPermission(
			final String username,
			final Session targetSession,
			final String permission) {
		switch (permission) {
			case "read":
				return targetSession.isActive();
			case "create":
				return !username.isEmpty();
			case "owner":
			case "update":
			case "delete":
				return targetSession.getCreator().equals(username);
			default:
				return false;
		}
	}

	private boolean hasContentPermission(
			final String username,
			final Content targetContent,
			final String permission) {
		switch (permission) {
			case "read":
				return sessionRepository.findOne(targetContent.getSessionId()).isActive();
			case "create":
			case "owner":
			case "update":
			case "delete":
				final Session session = sessionRepository.findOne(targetContent.getSessionId());
				return session != null && session.getCreator().equals(username);
			default:
				return false;
		}
	}

	private boolean hasCommentPermission(
			final String username,
			final Comment targetComment,
			final String permission) {
		switch (permission) {
			case "create":
				return !username.isEmpty() && sessionRepository.findOne(targetComment.getSessionId()).isActive();
			case "owner":
			case "update":
				return targetComment.getCreator() != null && targetComment.getCreator().equals(username);
			case "read":
			case "delete":
				if (targetComment.getCreator() != null && targetComment.getCreator().equals(username)) {
					return true;
				}

				/* Allow reading & deletion by session owner */
				final Session session = sessionRepository.findOne(targetComment.getSessionId());

				return session != null && session.getCreator().equals(username);
			default:
				return false;
		}
	}

	private boolean hasAdminRole(final String username) {
		/* TODO: only allow accounts from arsnova db */
		return Arrays.asList(adminAccounts).contains(username);
	}

	private String getUsername(final Authentication authentication) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
			return "";
		}

		if (authentication instanceof Pac4jAuthenticationToken) {
			UserAuthentication user = null;

			final Pac4jAuthenticationToken token = (Pac4jAuthenticationToken) authentication;
			if (token.getProfile() instanceof Google2Profile) {
				final Google2Profile profile = (Google2Profile) token.getProfile();
				user = new UserAuthentication(profile);
			} else if (token.getProfile() instanceof TwitterProfile) {
				final TwitterProfile profile = (TwitterProfile) token.getProfile();
				user = new UserAuthentication(profile);
			} else if (token.getProfile() instanceof FacebookProfile) {
				final FacebookProfile profile = (FacebookProfile) token.getProfile();
				user = new UserAuthentication(profile);
			}

			if (user != null) {
				return user.getUsername();
			}
		}

		return authentication.getName();
	}

	private boolean isWebsocketAccess(Authentication auth) {
		return auth instanceof AnonymousAuthenticationToken && auth.getAuthorities().contains("ROLE_WEBSOCKET_ACCESS");
	}
}
