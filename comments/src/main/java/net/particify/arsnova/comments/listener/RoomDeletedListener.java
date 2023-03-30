package net.particify.arsnova.comments.listener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.particify.arsnova.comments.config.RabbitConfig;
import net.particify.arsnova.comments.service.CommentService;
import net.particify.arsnova.comments.service.SettingsService;

@Service
public class RoomDeletedListener {
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class RoomDeletedEvent {
    private UUID id;

    public UUID getId() {
      return id;
    }

    public void setId(final UUID id) {
      this.id = id;
    }

    @Override
    public String toString() {
      return "RoomDeletedEvent{" +
          "id='" + id + '\'' +
          '}';
    }
  }

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

  @RabbitListener(queues = RabbitConfig.ROOM_DELETED_QUEUE_NAME)
  public void receiveMessage(final RoomDeletedEvent event) {
    logger.info("Reveiced room deleted event {}", event);
    commentService.deleteByRoomId(event.getId());
    settingsService.delete(event.getId());
  }
}
