package net.particify.arsnova.comments.security;

import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.Vote;
import net.particify.arsnova.common.uuid.UuidHelper;

@Component
public class PermissionEvaluator {
  final static String OWNER_ROLE_STRING = "ROLE_OWNER";
  final static String EDITOR_ROLE_STRING = "ROLE_EDITOR";
  final static String MODERATOR_ROLE_STRING = "ROLE_MODERATOR";

  public boolean isOwnerOrAnyTypeOfModeratorForRoom(
      final UUID roomId
  ) {
    final AuthenticatedUser authenticatedUser =
        (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return authenticatedUser.getAuthorities().stream().anyMatch(authority ->
        authority.equals(buildRoomAuthority(OWNER_ROLE_STRING, roomId)) ||
        authority.equals(buildRoomAuthority(EDITOR_ROLE_STRING, roomId)) ||
        authority.equals(buildRoomAuthority(MODERATOR_ROLE_STRING, roomId)));
  }

  public boolean isOwnerOrEditorForRoom(
      final UUID roomId
  ) {
    final AuthenticatedUser authenticatedUser =
        (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return authenticatedUser.getAuthorities().stream().anyMatch(authority ->
        authority.equals(buildRoomAuthority(OWNER_ROLE_STRING , roomId)) ||
        authority.equals(buildRoomAuthority(EDITOR_ROLE_STRING, roomId)));
  }

  public boolean checkCommentOwnerPermission(
      final Comment comment
  ) {
    final AuthenticatedUser authenticatedUser =
        (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return authenticatedUser.getId().equals(comment.getCreatorId());
  }

  public boolean checkCommentUpdatePermission(
      final Comment comment,
      final Comment oldComment
  ) {
    if (comment.isAck() == oldComment.isAck() ||
        comment.isFavorite() != oldComment.isFavorite() ||
        comment.isRead() != oldComment.isRead() ||
        comment.isAck() != oldComment.isAck() ||
        comment.getCorrect() != oldComment.getCorrect() ||
        !comment.getAnswer().equals(oldComment.getAnswer())
    ) {
      return isOwnerOrAnyTypeOfModeratorForRoom(comment.getRoomId());
    } else {
      return checkCommentOwnerPermission(comment);
    }
  }

  public boolean checkCommentPatchPermission(
      final Comment comment,
      final Map<String, Object> changes
  ) {
    final boolean needsModerator = changes.keySet().stream().anyMatch(changeKey -> changeKey.equals("favorite") ||
          changeKey.equals("read") ||
          changeKey.equals("correct") ||
          changeKey.equals("answer") ||
          changeKey.equals("ack")
    );
    if (needsModerator) {
      return isOwnerOrAnyTypeOfModeratorForRoom(comment.getRoomId());
    } else {
      return checkCommentOwnerPermission(comment);
    }
  }

  public boolean checkCommentDeletePermission(
      final Comment comment
  ) {
    return checkCommentOwnerPermission(comment) ||
        isOwnerOrEditorForRoom(comment.getRoomId());
  }

  public boolean checkVoteOwnerPermission(
      final Vote v
  ) {
    final AuthenticatedUser authenticatedUser =
        (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return authenticatedUser.getId().equals(v.getUserId());
  }

  private GrantedAuthority buildRoomAuthority(final String role, final UUID roomId) {
    return new SimpleGrantedAuthority(role + "-" + UuidHelper.uuidToString(roomId));
  }
}
