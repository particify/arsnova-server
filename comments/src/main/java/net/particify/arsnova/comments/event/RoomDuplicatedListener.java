package net.particify.arsnova.comments.event;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.Settings;
import net.particify.arsnova.comments.service.CommentService;
import net.particify.arsnova.comments.service.SettingsService;
import net.particify.arsnova.comments.service.VoteService;

@Component
public class RoomDuplicatedListener {
  private final CommentService commentService;
  private final VoteService voteService;
  private final SettingsService settingsService;
  private final EntityManager entityManager;

  public RoomDuplicatedListener(
      final CommentService commentService,
      final VoteService voteService,
      final SettingsService settingsService,
      final EntityManager entityManager) {
    this.commentService = commentService;
    this.voteService = voteService;
    this.settingsService = settingsService;
    this.entityManager = entityManager;
  }

  @EventListener
  @Transactional
  public void receiveMessage(final RoomDuplicatedEvent message) {
    Map<UUID, Comment> commentMapping = commentService.duplicateComments(
        message.getOriginalRoomId(),
        message.getDuplicatedRoomId());
    voteService.duplicateVotes(message.getOriginalRoomId(), commentMapping);
    final Settings settings = settingsService.get(message.getOriginalRoomId());
    if (settings != null) {
      entityManager.detach(settings);
      settings.setRoomId(message.getDuplicatedRoomId());
      settingsService.create(settings);
    }
  }
}
