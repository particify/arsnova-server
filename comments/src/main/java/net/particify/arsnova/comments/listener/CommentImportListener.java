package net.particify.arsnova.comments.listener;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import net.particify.arsnova.comments.config.RabbitConfig;
import net.particify.arsnova.comments.handler.CommentCommandHandler;
import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.command.ImportComment;
import net.particify.arsnova.comments.model.command.ImportCommentPayload;

@Service
public class CommentImportListener {
  static class CommentMessageEntity {
    private String roomId;
    private String creatorId;
    private String body;
    private Date timestamp;
    private boolean read;

    public String getRoomId() {
      return roomId;
    }

    public void setRoomId(String roomId) {
      this.roomId = roomId;
    }

    public String getCreatorId() {
      return creatorId;
    }

    public void setCreatorId(String creatorId) {
      this.creatorId = creatorId;
    }

    public String getBody() {
      return body;
    }

    public void setBody(String body) {
      this.body = body;
    }

    public Date getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Date timestamp) {
      this.timestamp = timestamp;
    }

    public boolean isRead() {
      return read;
    }

    public void setRead(boolean read) {
      this.read = read;
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(CommentImportListener.class);

  private CommentCommandHandler commentCommandHandler;

  public CommentImportListener(final CommentCommandHandler commentCommandHandler) {
    this.commentCommandHandler = commentCommandHandler;
  }

  @RabbitListener(queues = RabbitConfig.BACKEND_COMMENT_QUEUE_NAME)
  public void receiveMessage(final CommentMessageEntity entity) {
    logger.info("Received comment creation message for room ID {} from backend queue.", entity.roomId);
    ImportCommentPayload payload = new ImportCommentPayload();
    payload.setRoomId(entity.roomId);
    payload.setCreatorId(entity.creatorId);
    payload.setBody(entity.body);
    payload.setTimestamp(entity.timestamp);
    payload.setRead(entity.read);
    Comment comment = commentCommandHandler.handle(new ImportComment(payload));
    logger.info("Created comment with ID {} for creation message from backend queue.", comment.getId());
  }
}
