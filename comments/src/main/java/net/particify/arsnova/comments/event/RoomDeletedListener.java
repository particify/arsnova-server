package net.particify.arsnova.comments.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import net.particify.arsnova.comments.service.CommentService;
import net.particify.arsnova.comments.service.SettingsService;

@Component
public class RoomDeletedListener {

  private static final Logger logger = LoggerFactory.getLogger(RoomDeletedListener.class);
  private final CommentService commentService;
  private final SettingsService settingsService;

  @Autowired
  public RoomDeletedListener(
      final CommentService commentService,
      final SettingsService settingsService
  ) {
    this.commentService = commentService;
    this.settingsService = settingsService;
  }

  @EventListener
  public void handleRoomDeletedEvent(final RoomDeletedEvent event) {
    logger.debug("Received room deleted event {}", event);
    commentService.deleteByRoomId(event.getId());
    settingsService.delete(event.getId());
  }
}
