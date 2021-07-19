/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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
import java.util.ArrayList;
import java.util.List;
import org.ektorp.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.Motd;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.service.AnswerService;
import de.thm.arsnova.service.ContentGroupService;
import de.thm.arsnova.service.ContentService;
import de.thm.arsnova.service.RoomService;

/**
 * Provides access control methods that can be used in annotations.
 */
@Component
public class ApplicationPermissionEvaluator implements PermissionEvaluator {
	/* common permissions */
	public static final String READ_PERMISSION = "read";
	public static final String READ_EXTENDED_PERMISSION = "read-extended";
	public static final String CREATE_PERMISSION = "create";
	public static final String UPDATE_PERMISSION = "update";
	public static final String DELETE_PERMISSION = "delete";
	public static final String OWNER_PERMISSION = "owner";
	public static final String DUPLICATE_PERMISSION = "duplicate";

	/* specialized permissions */
	public static final String READ_CORRECT_OPTIONS_PERMISSION = "read-correct-options";

	private static final String ROOM_ROLE_PATTERN = "ROLE_%s__%s";

	private static final Logger logger = LoggerFactory.getLogger(ApplicationPermissionEvaluator.class);

	private final RoomService roomService;
	private final ContentService contentService;
	private final ContentGroupService contentGroupService;
	private final AnswerService answerService;

	public ApplicationPermissionEvaluator(
			final RoomService roomService,
			final ContentService contentService,
			final ContentGroupService contentGroupService,
			final AnswerService answerService
	) {
		this.roomService = roomService;
		this.contentService = contentService;
		this.contentGroupService = contentGroupService;
		this.answerService = answerService;
	}

	@Override
	public boolean hasPermission(
			final Authentication authentication,
			final Object targetDomainObject,
			final Object permission) {
		logger.debug(
				"Evaluating permission: hasPermission({}, {}. {})",
				authentication,
				targetDomainObject,
				permission);
		if (authentication == null || targetDomainObject == null || !(permission instanceof String)) {
			return false;
		}

		return isSystemAccess(authentication) || isAdminAccess(authentication)
				|| (targetDomainObject instanceof UserProfile
				&& (isAccountManagementAccess(authentication)
				|| hasUserProfilePermission(authentication,
				((UserProfile) targetDomainObject), permission.toString())))
				|| (targetDomainObject instanceof Room
				&& hasRoomPermission(authentication,
				((Room) targetDomainObject), permission.toString()))
				|| (targetDomainObject instanceof Content
				&& hasContentPermission(authentication,
				((Content) targetDomainObject), permission.toString()))
				|| (targetDomainObject instanceof ContentGroup
				&& hasContentGroupPermission(authentication,
				((ContentGroup) targetDomainObject), permission.toString()))
				|| (targetDomainObject instanceof Answer
				&& hasAnswerPermission(authentication,
				((Answer) targetDomainObject), permission.toString()))
				|| (targetDomainObject instanceof Motd
				&& hasMotdPermission(authentication,
				((Motd) targetDomainObject), permission.toString()));
	}

	@Override
	public boolean hasPermission(
			final Authentication authentication,
			final Serializable targetId,
			final String targetType,
			final Object permission) {
		logger.debug("Evaluating permission: hasPermission({}, {}, {}, {})",
				authentication, targetId, targetType, permission);
		if (authentication == null || targetId == null || targetType == null || !(permission instanceof String)) {
			return false;
		}

		if (isSystemAccess(authentication) || isAdminAccess(authentication)) {
			return true;
		}

		try {
			switch (targetType) {
				case "userprofile":
					final UserProfile targetUserProfile = new UserProfile();
					targetUserProfile.setId(targetId.toString());
					return isAccountManagementAccess(authentication)
							|| hasUserProfilePermission(authentication, targetUserProfile, permission.toString());
				case "room":
					final Room targetRoom = roomService.get(targetId.toString());
					return targetRoom != null
							&& hasRoomPermission(authentication, targetRoom, permission.toString());
				case "content":
					final Content targetContent = contentService.get(targetId.toString());
					return targetContent != null
							&& hasContentPermission(authentication, targetContent, permission.toString());
				case "contentgroup":
					final ContentGroup targetContentGroup = contentGroupService.get(targetId.toString());
					return targetContentGroup != null
							&& hasContentGroupPermission(authentication, targetContentGroup, permission.toString());
				case "answer":
					final Answer targetAnswer = answerService.get(targetId.toString());
					return targetAnswer != null
							&& hasAnswerPermission(authentication, targetAnswer, permission.toString());
				default:
					return false;
			}
		} catch (final DocumentNotFoundException e) {
			logger.debug("Denying access for non-existent {} with ID {}.", targetType, targetId);
			return false;
		}
	}

	private boolean hasUserProfilePermission(
			final Authentication auth,
			final UserProfile targetUserProfile,
			final String permission) {
		switch (permission) {
			case READ_PERMISSION:
				/* While the profile is readable for all authenticated users, it
				 * only contains a limited amount of properties for the default
				 * view. */
				return true;
			case CREATE_PERMISSION:
				return true;
			case OWNER_PERMISSION:
			case UPDATE_PERMISSION:
			case DELETE_PERMISSION:
				return getUserId(auth).equals(targetUserProfile.getId());
			default:
				return false;
		}
	}

	private boolean hasRoomPermission(
			final Authentication auth,
			final Room targetRoom,
			final String permission) {
		switch (permission) {
			case READ_PERMISSION:
				return !targetRoom.isClosed() || hasAuthenticationRoomModeratingRole(auth, targetRoom);
			case READ_EXTENDED_PERMISSION:
				return hasAuthenticationRoomModeratingRole(auth, targetRoom);
			case CREATE_PERMISSION:
				return !getUserId(auth).isEmpty();
			case UPDATE_PERMISSION:
				return hasAuthenticationRoomRole(auth, targetRoom, RoomRole.EDITING_MODERATOR);
			case OWNER_PERMISSION:
			case DELETE_PERMISSION:
				return hasAuthenticationRoomRole(auth, targetRoom, RoomRole.OWNER);
			case DUPLICATE_PERMISSION:
				return hasAuthenticationRoomRole(auth, targetRoom, RoomRole.OWNER)
						|| targetRoom.isTemplate();
			default:
				return false;
		}
	}

	private boolean hasContentPermission(
			final Authentication auth,
			final Content targetContent,
			final String permission) {
		final Room room = roomService.get(targetContent.getRoomId());
		if (room == null) {
			return false;
		}

		switch (permission) {
			case READ_PERMISSION:
				if (hasAuthenticationRoomModeratingRole(auth, room)) {
					return true;
				}
				return !room.isClosed() && contentGroupService.getByRoomIdAndContainingContentId(
						targetContent.getRoomId(), targetContent.getId()).stream()
						.anyMatch(cg -> cg.isContentPublished(targetContent.getId()));
			case READ_EXTENDED_PERMISSION:
				return hasAuthenticationRoomModeratingRole(auth, room);
			case READ_CORRECT_OPTIONS_PERMISSION:
				if (hasAuthenticationRoomModeratingRole(auth, room)) {
					return true;
				}
				return !room.isClosed() && contentGroupService.getByRoomIdAndContainingContentId(
						targetContent.getRoomId(), targetContent.getId()).stream()
						.anyMatch(cg -> cg.isContentPublished(targetContent.getId()) && cg.isCorrectOptionsPublished());
			case CREATE_PERMISSION:
			case UPDATE_PERMISSION:
			case DELETE_PERMISSION:
			case OWNER_PERMISSION:
			case DUPLICATE_PERMISSION:
				/* TODO: Remove owner permission for content. Use create/update/delete instead. */
				return hasAuthenticationRoomRole(auth, room, RoomRole.EDITING_MODERATOR);
			default:
				return false;
		}
	}

	private boolean hasContentGroupPermission(
			final Authentication auth,
			final ContentGroup targetContentGroup,
			final String permission) {
		final Room room = roomService.get(targetContentGroup.getRoomId());
		if (room == null) {
			return false;
		}

		switch (permission) {
			case "read":
				return (!room.isClosed() && targetContentGroup.isPublished())
						|| hasAuthenticationRoomModeratingRole(auth, room);
			case "create":
			case "update":
			case "delete":
				return hasAuthenticationRoomRole(auth, room, RoomRole.EDITING_MODERATOR);
			default:
				return false;
		}
	}

	private boolean hasAnswerPermission(
			final Authentication auth,
			final Answer targetAnswer,
			final String permission) {
		final Content content = contentService.get(targetAnswer.getContentId());
		if (!hasContentPermission(auth, content, "read")) {
			return false;
		}
		final Room room;
		switch (permission) {
			case READ_PERMISSION:
				if (content.getState().isAnswersPublished() || getUserId(auth).equals(targetAnswer.getCreatorId())) {
					return true;
				}
				room = roomService.get(targetAnswer.getRoomId());
				return room != null && hasAuthenticationRoomModeratingRole(auth, room);
			case CREATE_PERMISSION:
				return content.getState().isAnswerable();
			case OWNER_PERMISSION:
				return getUserId(auth).equals(targetAnswer.getCreatorId());
			case UPDATE_PERMISSION:
				/* TODO */
				return false;
			case DELETE_PERMISSION:
				room = roomService.get(targetAnswer.getRoomId());
				return room != null && hasAuthenticationRoomModeratingRole(auth, room);
			default:
				return false;
		}
	}

	private boolean hasMotdPermission(
			final Authentication auth,
			final Motd targetMotd,
			final String permission) {
		final Room room;
		switch (permission) {
			case CREATE_PERMISSION:
			case UPDATE_PERMISSION:
			case DELETE_PERMISSION:
				if (getUserId(auth).isEmpty() || targetMotd.getRoomId() == null
						|| targetMotd.getAudience() != Motd.Audience.ROOM) {
					return false;
				}
				room = roomService.get(targetMotd.getRoomId());
				if (room == null) {
					return false;
				}

				return hasAuthenticationRoomRole(auth, room, RoomRole.EDITING_MODERATOR);
			case READ_PERMISSION:
				if (targetMotd.getAudience() != Motd.Audience.ROOM) {
					return true;
				}
				room = roomService.get(targetMotd.getRoomId());

				return room != null && (!room.isClosed() || hasAuthenticationRoomModeratingRole(auth, room));
			default:
				return false;
		}
	}

	/**
	 * Checks if the authentication has the owner or has any moderating role for
	 * the room.
	 */
	private boolean hasAuthenticationRoomModeratingRole(final Authentication auth, final Room room) {
		return hasAuthenticationRoomRole(auth, room, RoomRole.EXECUTIVE_MODERATOR);
	}

	/**
	 * Checks if the authentication has a specific role for the room.
	 *
	 * @param room The room to check the role for.
	 * @param auth The authentication to check the role for.
	 * @param role The role that is checked.
	 * @return Returns true if the user has the moderator role for the room.
	 */
	private boolean hasAuthenticationRoomRole(
			final Authentication auth,
			final Room room,
			final RoomRole role) {
		final List<String> allowedRoles = determineRoleSuperset(room, role);
		return auth.getAuthorities().stream().anyMatch(ga -> allowedRoles.contains(ga.getAuthority()));
	}

	/**
	 * Returns a superset of roles which contains the passed role and roles with
	 * a superset of permissions.
	 */
	private List<String> determineRoleSuperset(final Room room, final RoomRole role) {
		final List<String> roles = new ArrayList<>();

		roles.add(
				String.format(ROOM_ROLE_PATTERN, RoomRole.OWNER, room.getId()));
		if (role == RoomRole.OWNER) {
			return roles;
		}

		roles.add(
				String.format(ROOM_ROLE_PATTERN, RoomRole.EDITING_MODERATOR, room.getId()));
		if (role == RoomRole.EDITING_MODERATOR) {
			return roles;
		}

		roles.add(
				String.format(ROOM_ROLE_PATTERN, RoomRole.EXECUTIVE_MODERATOR, room.getId()));
		if (role == RoomRole.EXECUTIVE_MODERATOR) {
			return roles;
		}

		roles.add(
				String.format(ROOM_ROLE_PATTERN, RoomRole.PARTICIPANT, room.getId()));

		return roles;
	}

	private String getUserId(final Authentication authentication) {
		if (authentication == null || authentication instanceof AnonymousAuthenticationToken
				|| !(authentication.getPrincipal() instanceof User)) {
			return "";
		}
		final User user = (User) authentication.getPrincipal();

		return user.getId();
	}

	private boolean isAdminAccess(final Authentication auth) {
		return auth.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));
	}

	private boolean isSystemAccess(final Authentication auth) {
		return (auth instanceof UsernamePasswordAuthenticationToken
				&& auth.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_SYSTEM"))
				) || (
				auth instanceof RunAsUserToken
				&& auth.getAuthorities().stream().anyMatch(ga -> ga.getAuthority().equals("ROLE_RUN_AS_SYSTEM")));
	}

	private boolean isAccountManagementAccess(final Authentication auth) {
		return auth instanceof RunAsUserToken
				&& auth.getAuthorities().stream()
					.anyMatch(ga -> ga.getAuthority().equals("ROLE_RUN_AS_ACCOUNT_MANAGEMENT"));
	}
}
