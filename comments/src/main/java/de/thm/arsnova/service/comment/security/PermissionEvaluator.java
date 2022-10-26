package de.thm.arsnova.service.comment.security;

import java.util.Map;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.Vote;

@Component
public class PermissionEvaluator {
    final static String OWNER_ROLE_STRING = "ROLE_CREATOR";
    final static String EDITING_MODERATOR_ROLE_STRING = "ROLE_EDITING_MODERATOR";
    final static String EXECUTIVE_MODERATOR_ROLE_STRING = "ROLE_EXECUTIVE_MODERATOR";

    public Boolean isOwnerOrAnyTypeOfModeratorForRoom(
            final String roomId
    ) {
        final AuthenticatedUser authenticatedUser =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return authenticatedUser.getAuthorities().stream().anyMatch(authority -> authority.equals(
                new SimpleGrantedAuthority(OWNER_ROLE_STRING + "-" + roomId)) ||
                authority.equals(new SimpleGrantedAuthority(EDITING_MODERATOR_ROLE_STRING + "-" + roomId)) ||
                authority.equals(new SimpleGrantedAuthority(EXECUTIVE_MODERATOR_ROLE_STRING + "-" + roomId)));
    }

    public Boolean isOwnerOrEditingModeratorForRoom(
            final String roomId
    ) {
        final AuthenticatedUser authenticatedUser =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return authenticatedUser.getAuthorities().stream().anyMatch(authority -> authority.equals(
                new SimpleGrantedAuthority(OWNER_ROLE_STRING + "-" + roomId)) ||
                authority.equals(new SimpleGrantedAuthority(EDITING_MODERATOR_ROLE_STRING + "-" + roomId)));
    }

    public Boolean checkCommentOwnerPermission(
            final Comment comment
    ) {
        final AuthenticatedUser authenticatedUser =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return authenticatedUser.getId().equals(comment.getCreatorId());
    }

    public Boolean checkCommentUpdatePermission(
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

    public Boolean checkCommentPatchPermission(
            final Comment comment,
            final Map<String, Object> changes
    ) {
        final Boolean needsModerator = changes.keySet().stream().anyMatch(changeKey -> changeKey.equals("favorite") ||
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

    public Boolean checkCommentDeletePermission(
            final Comment comment
    ) {
        return checkCommentOwnerPermission(comment) ||
                isOwnerOrEditingModeratorForRoom(comment.getRoomId());
    }

    public Boolean checkVoteOwnerPermission(
            final Vote v
    ) {
        final AuthenticatedUser authenticatedUser =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return authenticatedUser.getId().equals(v.getUserId());
    }
}
