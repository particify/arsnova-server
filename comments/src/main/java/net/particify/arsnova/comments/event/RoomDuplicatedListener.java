package net.particify.arsnova.comments.event;

import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.service.CommentService;
import net.particify.arsnova.comments.service.VoteService;

@Component
public class RoomDuplicatedListener {
  private CommentService commentService;
  private VoteService voteService;

  public RoomDuplicatedListener(
      final CommentService commentService,
      final VoteService voteService) {
    this.commentService = commentService;
    this.voteService = voteService;
  }

  @EventListener
  @Transactional
  public void receiveMessage(final RoomDuplicatedEvent message) {
    Map<UUID, Comment> commentMapping = commentService.duplicateComments(
        message.getOriginalRoomId(),
        message.getDuplicatedRoomId());
    voteService.duplicateVotes(message.getOriginalRoomId(), commentMapping);
  }
}
