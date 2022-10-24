package de.thm.arsnova.service.comment.listener;

import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.thm.arsnova.service.comment.config.RabbitConfig;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.service.CommentService;
import de.thm.arsnova.service.comment.service.VoteService;

@Service
public class RoomDuplicatedListener {
  private CommentService commentService;
  private VoteService voteService;

  public RoomDuplicatedListener(
      final CommentService commentService,
      final VoteService voteService) {
    this.commentService = commentService;
    this.voteService = voteService;
  }

  @RabbitListener(queues = RabbitConfig.BACKEND_ROOM_DUPLICATED_QUEUE_NAME)
  @Transactional
  public void receiveMessage(final RoomDuplicatedMessage message) {
    Map<String, Comment> commentMapping = commentService.duplicateComments(
        message.originalRoomId,
        message.duplicatedRoomId);
    voteService.duplicateVotes(message.originalRoomId, commentMapping);
  }

  private static class RoomDuplicatedMessage {
    private String originalRoomId;
    private String duplicatedRoomId;

    public String getOriginalRoomId() {
      return originalRoomId;
    }

    public void setOriginalRoomId(final String originalRoomId) {
      this.originalRoomId = originalRoomId;
    }

    public String getDuplicatedRoomId() {
      return duplicatedRoomId;
    }

    public void setDuplicatedRoomId(final String duplicatedRoomId) {
      this.duplicatedRoomId = duplicatedRoomId;
    }
  }
}
