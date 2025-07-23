package net.particify.arsnova.comments.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import net.particify.arsnova.comments.handler.CommentCommandHandler;
import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.command.ImportComment;
import net.particify.arsnova.comments.model.command.ImportCommentPayload;

@Component
public class CommentImportListener {

  private static final Logger logger = LoggerFactory.getLogger(CommentImportListener.class);

  private CommentCommandHandler commentCommandHandler;

  public CommentImportListener(final CommentCommandHandler commentCommandHandler) {
    this.commentCommandHandler = commentCommandHandler;
  }

  @EventListener
  public void handleImportEvent(final ImportEvent entity) {
    logger.debug("Received comment creation message for room ID {} from backend queue.", entity.getRoomId());
    ImportCommentPayload payload = new ImportCommentPayload();
    payload.setRoomId(entity.getRoomId());
    payload.setCreatorId(entity.getCreatorId());
    payload.setBody(entity.getBody());
    payload.setTimestamp(entity.getTimestamp());
    payload.setRead(entity.isRead());
    Comment comment = commentCommandHandler.handle(new ImportComment(payload));
    logger.debug("Created comment with ID {} for creation message from backend queue.", comment.getId());
  }
}
