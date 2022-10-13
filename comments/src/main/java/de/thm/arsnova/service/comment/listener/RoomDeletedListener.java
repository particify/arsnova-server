package de.thm.arsnova.service.comment.listener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.thm.arsnova.service.comment.config.RabbitConfig;
import de.thm.arsnova.service.comment.service.CommentService;
import de.thm.arsnova.service.comment.service.SettingsService;

@Service
public class RoomDeletedListener {
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class RoomDeletedEvent {
    private String id;

    public String getId() {
      return id;
    }

    public void setId(final String id) {
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
