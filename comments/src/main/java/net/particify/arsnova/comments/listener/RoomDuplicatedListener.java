package net.particify.arsnova.comments.listener;

import java.util.Map;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.particify.arsnova.comments.config.RabbitConfig;
import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.service.CommentService;
import net.particify.arsnova.comments.service.VoteService;

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
    Map<UUID, Comment> commentMapping = commentService.duplicateComments(
        message.originalRoomId,
        message.duplicatedRoomId);
    voteService.duplicateVotes(message.originalRoomId, commentMapping);
  }

  private static class RoomDuplicatedMessage {
    private UUID originalRoomId;
    private UUID duplicatedRoomId;

    public UUID getOriginalRoomId() {
      return originalRoomId;
    }

    public void setOriginalRoomId(final UUID originalRoomId) {
      this.originalRoomId = originalRoomId;
    }

    public UUID getDuplicatedRoomId() {
      return duplicatedRoomId;
    }

    public void setDuplicatedRoomId(final UUID duplicatedRoomId) {
      this.duplicatedRoomId = duplicatedRoomId;
    }
  }
}
