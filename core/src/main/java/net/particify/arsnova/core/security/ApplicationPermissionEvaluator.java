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

package net.particify.arsnova.core.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.ektorp.DocumentNotFoundException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.Announcement;
import net.particify.arsnova.core.model.Answer;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentTemplate;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.RoomSettings;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.service.AnnouncementService;
import net.particify.arsnova.core.service.AnswerService;
import net.particify.arsnova.core.service.ContentGroupService;
import net.particify.arsnova.core.service.ContentGroupTemplateService;
import net.particify.arsnova.core.service.ContentService;
import net.particify.arsnova.core.service.ContentTemplateService;
import net.particify.arsnova.core.service.RoomSettingsService;

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
  public static final String MODERATE_PERMISSION = "moderate";
  public static final String DUPLICATE_PERMISSION = "duplicate";

  /* specialized permissions */
  public static final String READ_CORRECT_OPTIONS_PERMISSION = "read-correct-options";

  private static final String ROOM_ROLE_PATTERN = "ROLE_%s__%s";

  private static final Logger logger = LoggerFactory.getLogger(ApplicationPermissionEvaluator.class);

  private final RoomSettingsService roomSettingsService;
  private final ContentService contentService;
  private final ContentGroupService contentGroupService;
  private final AnswerService answerService;
  private final AnnouncementService announcementService;
  private final ContentGroupTemplateService contentGroupTemplateService;
  private final ContentTemplateService contentTemplateService;

  public ApplicationPermissionEvaluator(
      final RoomSettingsService roomSettingsService,
      final ContentService contentService,
      final ContentGroupService contentGroupService,
      final AnswerService answerService,
      @Nullable final AnnouncementService announcementService,
      final ContentGroupTemplateService contentGroupTemplateService,
      final ContentTemplateService contentTemplateService,
      final SystemProperties systemProperties
  ) {
    if (announcementService == null && !systemProperties.isExternalDataManagement()) {
      throw new IllegalStateException("AnnouncementService is null but external data management is disabled.");
    }
    this.roomSettingsService = roomSettingsService;
    this.contentService = contentService;
    this.contentGroupService = contentGroupService;
    this.answerService = answerService;
    this.announcementService = announcementService;
    this.contentGroupTemplateService = contentGroupTemplateService;
    this.contentTemplateService = contentTemplateService;
  }

  @Override
  public boolean hasPermission(
      final Authentication authentication,
      final Object targetDomainObject,
      final Object permission) {
    logger.debug(
        "Evaluating permission: hasPermission({}, {}, {})",
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
        || (targetDomainObject instanceof RoomSettings
        && hasRoomSettingsPermission(authentication,
        ((RoomSettings) targetDomainObject), permission.toString()))
        || (targetDomainObject instanceof Content
        && hasContentPermission(authentication,
        ((Content) targetDomainObject), permission.toString()))
        || (targetDomainObject instanceof ContentGroup
        && hasContentGroupPermission(authentication,
        ((ContentGroup) targetDomainObject), permission.toString()))
        || (targetDomainObject instanceof Answer
        && hasAnswerPermission(authentication,
        ((Answer) targetDomainObject), permission.toString()))
        || (targetDomainObject instanceof Announcement
        && hasAnnouncementPermission(authentication,
        ((Announcement) targetDomainObject), permission.toString()))
        || (targetDomainObject instanceof ContentGroupTemplate
        && hasContentGroupTemplatePermission(authentication,
        ((ContentGroupTemplate) targetDomainObject), permission.toString()))
        || (targetDomainObject instanceof ContentTemplate
        && hasContentTemplatePermission(authentication,
        ((ContentTemplate) targetDomainObject), permission.toString()));
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
      switch (targetType.toLowerCase()) {
        case "userprofile":
          final UserProfile targetUserProfile = new UserProfile();
          targetUserProfile.setId(targetId.toString());
          return isAccountManagementAccess(authentication)
              || hasUserProfilePermission(authentication, targetUserProfile, permission.toString());
        case "room":
          return hasRoomPermission(authentication, targetId.toString(), permission.toString());
        case "roomsettings":
          final RoomSettings targetRoomSettings = roomSettingsService.get(targetId.toString());
          return targetRoomSettings != null
              && hasRoomSettingsPermission(authentication, targetRoomSettings, permission.toString());
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
        case "announcement":
          final Announcement targetAnnouncement = announcementService.get(targetId.toString());
          return targetAnnouncement != null
              && hasAnnouncementPermission(authentication, targetAnnouncement, permission.toString());
        case "contentgrouptemplate":
          final ContentGroupTemplate targetContentGroupTemplate = contentGroupTemplateService.get(targetId.toString());
          return targetContentGroupTemplate != null
              && hasContentGroupTemplatePermission(authentication, targetContentGroupTemplate, permission.toString());
        case "contenttemplate":
          final ContentTemplate targetContentTemplate = contentTemplateService.get(targetId.toString());
          return targetContentTemplate != null
              && hasContentTemplatePermission(authentication, targetContentTemplate, permission.toString());
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
      final String targetRoomId,
      final String permission) {
    switch (permission) {
      case READ_PERMISSION:
        return true;
      case READ_EXTENDED_PERMISSION:
        return hasAuthenticationRoomModeratingRole(auth, targetRoomId);
      case CREATE_PERMISSION:
        return !getUserId(auth).isEmpty();
      case UPDATE_PERMISSION:
        return hasAuthenticationRoomRole(auth, targetRoomId, RoomRole.EDITOR);
      case OWNER_PERMISSION:
      case DELETE_PERMISSION:
        return hasAuthenticationRoomRole(auth, targetRoomId, RoomRole.OWNER);
      case DUPLICATE_PERMISSION:
        return hasAuthenticationRoomRole(auth, targetRoomId, RoomRole.OWNER);
      default:
        return false;
    }
  }

  private boolean hasRoomPermission(
      final Authentication auth,
      final Room targetRoom,
      final String permission) {
    if (permission.equals(READ_PERMISSION) || permission.equals(DUPLICATE_PERMISSION) && targetRoom.isTemplate()) {
      return true;
    }
    return hasRoomPermission(auth, targetRoom.getId(), permission);
  }

  private boolean hasRoomSettingsPermission(
      final Authentication auth,
      final RoomSettings targetRoomSettings,
      final String permission) {
    if (permission.equals(READ_PERMISSION)) {
      return hasAuthenticationRoomRole(auth, targetRoomSettings.getRoomId(), RoomRole.PARTICIPANT);
    }
    return hasAuthenticationRoomModeratingRole(auth, targetRoomSettings.getRoomId());
  }

  private boolean hasContentPermission(
      final Authentication auth,
      final Content targetContent,
      final String permission) {
    final String roomId = targetContent.getRoomId();

    switch (permission) {
      case READ_PERMISSION:
        if (hasAuthenticationRoomModeratingRole(auth, roomId)) {
          return true;
        }
        return hasAuthenticationRoomRole(auth, roomId, RoomRole.PARTICIPANT)
            && contentGroupService.getByRoomIdAndContainingContentId(
            targetContent.getRoomId(), targetContent.getId()).stream()
            .anyMatch(cg -> cg.isContentPublished(targetContent.getId()));
      case READ_EXTENDED_PERMISSION:
        return hasAuthenticationRoomModeratingRole(auth, roomId);
      case READ_CORRECT_OPTIONS_PERMISSION:
        if (hasAuthenticationRoomModeratingRole(auth, roomId)) {
          return true;
        }
        return hasAuthenticationRoomRole(auth, roomId, RoomRole.PARTICIPANT)
            && contentGroupService.getByRoomIdAndContainingContentId(
            targetContent.getRoomId(), targetContent.getId()).stream()
            .anyMatch(cg -> cg.isContentPublished(targetContent.getId()) && cg.isCorrectOptionsPublished());
      case MODERATE_PERMISSION:
        return hasAuthenticationRoomModeratingRole(auth, roomId);
      case CREATE_PERMISSION:
      case UPDATE_PERMISSION:
      case DELETE_PERMISSION:
      case OWNER_PERMISSION:
      case DUPLICATE_PERMISSION:
        /* TODO: Remove owner permission for content. Use create/update/delete instead. */
        return hasAuthenticationRoomRole(auth, roomId, RoomRole.EDITOR);
      default:
        return false;
    }
  }

  private boolean hasContentGroupPermission(
      final Authentication auth,
      final ContentGroup targetContentGroup,
      final String permission) {
    final String roomId = targetContentGroup.getRoomId();

    switch (permission) {
      case "read":
        return (hasAuthenticationRoomRole(auth, roomId, RoomRole.PARTICIPANT) && targetContentGroup.isPublished())
            || hasAuthenticationRoomModeratingRole(auth, roomId);
      case "create":
      case "update":
      case "delete":
        return hasAuthenticationRoomRole(auth, roomId, RoomRole.EDITOR);
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
    final String roomId = targetAnswer.getRoomId();
    switch (permission) {
      case READ_PERMISSION:
        if (content.getState().isAnswersPublished() || getUserId(auth).equals(targetAnswer.getCreatorId())) {
          return true;
        }
        return hasAuthenticationRoomModeratingRole(auth, roomId);
      case CREATE_PERMISSION:
        return content.getState().isAnswerable();
      case OWNER_PERMISSION:
        return getUserId(auth).equals(targetAnswer.getCreatorId());
      case UPDATE_PERMISSION:
        /* TODO */
        return false;
      case MODERATE_PERMISSION:
        return hasAuthenticationRoomModeratingRole(auth, roomId);
      case DELETE_PERMISSION:
        return hasAuthenticationRoomRole(auth, roomId, RoomRole.EDITOR);
      default:
        return false;
    }
  }

  private boolean hasAnnouncementPermission(
      final Authentication auth,
      final Announcement targetAnnouncement,
      final String permission) {
    if (getUserId(auth).isEmpty() || targetAnnouncement.getRoomId() == null) {
      return false;
    }
    final String roomId = targetAnnouncement.getRoomId();
    switch (permission) {
      case CREATE_PERMISSION:
      case UPDATE_PERMISSION:
      case DELETE_PERMISSION:
        return hasAuthenticationRoomRole(auth, roomId, RoomRole.EDITOR);
      case READ_PERMISSION:
        return hasAuthenticationRoomRole(auth, roomId, RoomRole.PARTICIPANT);
      default:
        return false;
    }
  }

  private boolean hasContentGroupTemplatePermission(
      final Authentication auth,
      final ContentGroupTemplate contentGroupTemplate,
      final String permission) {
    switch (permission) {
      case READ_PERMISSION:
        return true;
      case UPDATE_PERMISSION:
      case DELETE_PERMISSION:
        final String userId = getUserId(auth);
        if (userId.isEmpty()) {
          return false;
        }

        return userId.equals(contentGroupTemplate.getCreatorId());
      default:
        return false;
    }
  }

  private boolean hasContentTemplatePermission(
      final Authentication auth,
      final ContentTemplate contentTemplate,
      final String permission) {
    switch (permission) {
      case READ_PERMISSION:
        return true;
      default:
        return false;
    }
  }

  /**
   * Checks if the authentication has the owner or has any moderating role for
   * the room.
   */
  private boolean hasAuthenticationRoomModeratingRole(final Authentication auth, final String roomId) {
    return hasAuthenticationRoomRole(auth, roomId, RoomRole.MODERATOR);
  }

  /**
   * Checks if the authentication has a specific role for the room.
   *
   * @param auth The authentication to check the role for.
   * @param roomId The ID of the room to check the role for.
   * @param role The role that is checked.
   * @return Returns true if the user has the moderator role for the room.
   */
  private boolean hasAuthenticationRoomRole(
      final Authentication auth,
      final String roomId,
      final RoomRole role) {
    final List<String> allowedRoles = determineRoleSuperset(roomId, role);
    return auth.getAuthorities().stream().anyMatch(ga -> allowedRoles.contains(ga.getAuthority()));
  }

  /**
   * Returns a superset of roles which contains the passed role and roles with
   * a superset of permissions.
   */
  private List<String> determineRoleSuperset(final String roomId, final RoomRole role) {
    final List<String> roles = new ArrayList<>();

    roles.add(String.format(ROOM_ROLE_PATTERN, RoomRole.OWNER, roomId));
    if (role == RoomRole.OWNER) {
      return roles;
    }

    roles.add(String.format(ROOM_ROLE_PATTERN, RoomRole.EDITOR, roomId));
    if (role == RoomRole.EDITOR) {
      return roles;
    }

    roles.add(String.format(ROOM_ROLE_PATTERN, RoomRole.MODERATOR, roomId));
    if (role == RoomRole.MODERATOR) {
      return roles;
    }

    roles.add(String.format(ROOM_ROLE_PATTERN, RoomRole.PARTICIPANT, roomId));

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
