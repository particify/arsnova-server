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

import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.persistance.CommentRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Provides access control methods that can be used in annotations.
 */
@Component
public class ApplicationPermissionEvaluator implements PermissionEvaluator {
	private static final Logger logger = LoggerFactory.getLogger(ApplicationPermissionEvaluator.class);

	@Value("${security.admin-accounts}")
	private String[] adminAccounts;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private ContentRepository contentRepository;

	@Override
	public boolean hasPermission(
			final Authentication authentication,
			final Object targetDomainObject,
			final Object permission) {
		logger.debug("Evaluating permission: hasPermission({}, {}. {})", authentication, targetDomainObject, permission);
		if (authentication == null || targetDomainObject == null || !(permission instanceof String)) {
			return false;
		}

		final String userId = getUserId(authentication);

		return hasAdminRole(userId)
				|| (targetDomainObject instanceof UserProfile
						&& hasUserProfilePermission(userId, ((UserProfile) targetDomainObject), permission.toString()))
				|| (targetDomainObject instanceof Room
						&& hasRoomPermission(userId, ((Room) targetDomainObject), permission.toString()))
				|| (targetDomainObject instanceof Content
						&& hasContentPermission(userId, ((Content) targetDomainObject), permission.toString()))
				|| (targetDomainObject instanceof Comment
						&& hasCommentPermission(userId, ((Comment) targetDomainObject), permission.toString()));
	}

	@Override
	public boolean hasPermission(
			final Authentication authentication,
			final Serializable targetId,
			final String targetType,
			final Object permission) {
		logger.debug("Evaluating permission: hasPermission({}, {}, {}, {})", authentication, targetId, targetType, permission);
		if (authentication == null || targetId == null || targetType == null || !(permission instanceof String)) {
			return false;
		}

		final String userId = getUserId(authentication);
		if (hasAdminRole(userId)) {
			return true;
		}

		switch (targetType) {
			case "userprofile":
				final UserProfile targetUserProfile = new UserProfile();
				targetUserProfile.setId(targetId.toString());
				return hasUserProfilePermission(userId, targetUserProfile, permission.toString());
			case "room":
				final Room targetRoom = roomRepository.findOne(targetId.toString());
				return targetRoom != null && hasRoomPermission(userId, targetRoom, permission.toString());
			case "content":
				final Content targetContent = contentRepository.findOne(targetId.toString());
				return targetContent != null && hasContentPermission(userId, targetContent, permission.toString());
			case "comment":
				final Comment targetComment = commentRepository.findOne(targetId.toString());
				return targetComment != null && hasCommentPermission(userId, targetComment, permission.toString());
			default:
				return false;
		}
	}

	private boolean hasUserProfilePermission(
			final String userId,
			final UserProfile targetUserProfile,
			final String permission) {
		switch (permission) {
			case "read":
				return userId.equals(targetUserProfile.getId());
			case "create":
				return true;
			case "owner":
			case "update":
			case "delete":
				return userId.equals(targetUserProfile.getId());
			default:
				return false;
		}
	}

	private boolean hasRoomPermission(
			final String userId,
			final Room targetRoom,
			final String permission) {
		switch (permission) {
			case "read":
				return !targetRoom.isClosed();
			case "create":
				return !userId.isEmpty();
			case "owner":
			case "update":
			case "delete":
				return targetRoom.getOwnerId().equals(userId);
			default:
				return false;
		}
	}

	private boolean hasContentPermission(
			final String userId,
			final Content targetContent,
			final String permission) {
		switch (permission) {
			case "read":
				return !roomRepository.findOne(targetContent.getRoomId()).isClosed();
			case "create":
			case "owner":
			case "update":
			case "delete":
				final Room room = roomRepository.findOne(targetContent.getRoomId());
				return room != null && room.getOwnerId().equals(userId);
			default:
				return false;
		}
	}

	private boolean hasCommentPermission(
			final String userId,
			final Comment targetComment,
			final String permission) {
		switch (permission) {
			case "create":
				return !userId.isEmpty() && !roomRepository.findOne(targetComment.getRoomId()).isClosed();
			case "owner":
			case "update":
				return targetComment.getCreatorId() != null && targetComment.getCreatorId().equals(userId);
			case "read":
			case "delete":
				if (targetComment.getCreatorId() != null && targetComment.getCreatorId().equals(userId)) {
					return true;
				}

				/* Allow reading & deletion by session owner */
				final Room room = roomRepository.findOne(targetComment.getRoomId());

				return room != null && room.getOwnerId().equals(userId);
			default:
				return false;
		}
	}

	private boolean hasAdminRole(final String username) {
		/* TODO: only allow accounts from arsnova db */
		return Arrays.asList(adminAccounts).contains(username);
	}

	private String getUserId(final Authentication authentication) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken ||
				!(authentication.getPrincipal() instanceof User)) {
			return "";
		}
		User user = (User) authentication.getPrincipal();

		return user.getId();
	}

	private boolean isWebsocketAccess(Authentication auth) {
		return auth instanceof AnonymousAuthenticationToken && auth.getAuthorities().contains("ROLE_WEBSOCKET_ACCESS");
	}
}
