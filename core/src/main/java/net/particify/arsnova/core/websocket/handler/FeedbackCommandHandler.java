package net.particify.arsnova.core.websocket.handler;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import net.particify.arsnova.core.config.RabbitConfig;
import net.particify.arsnova.core.config.properties.MessageBrokerProperties;
import net.particify.arsnova.core.event.AfterPatchEvent;
import net.particify.arsnova.core.model.Feedback;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.RoomSettings;
import net.particify.arsnova.core.service.FeedbackStorageService;
import net.particify.arsnova.core.service.RoomSettingsService;
import net.particify.arsnova.core.websocket.message.CreateFeedback;
import net.particify.arsnova.core.websocket.message.CreateFeedbackPayload;
import net.particify.arsnova.core.websocket.message.FeedbackChanged;
import net.particify.arsnova.core.websocket.message.FeedbackChangedPayload;
import net.particify.arsnova.core.websocket.message.FeedbackReset;
import net.particify.arsnova.core.websocket.message.FeedbackStarted;
import net.particify.arsnova.core.websocket.message.FeedbackStopped;
import net.particify.arsnova.core.websocket.message.ResetFeedback;

@Component
@EnableConfigurationProperties(MessageBrokerProperties.class)
@ConditionalOnProperty(
    name = RabbitConfig.RabbitConfigProperties.RABBIT_ENABLED,
    prefix = MessageBrokerProperties.PREFIX,
    havingValue = "true")
public class FeedbackCommandHandler {
  private final RabbitTemplate messagingTemplate;
  private final FeedbackStorageService feedbackStorage;
  private final RoomSettingsService roomSettingsService;

  public FeedbackCommandHandler(
      final RabbitTemplate messagingTemplate,
      final FeedbackStorageService feedbackStorage,
      final RoomSettingsService roomSettingsService
  ) {
    this.messagingTemplate = messagingTemplate;
    this.feedbackStorage = feedbackStorage;
    this.roomSettingsService = roomSettingsService;
  }

  /* ToDo: Listen to a more specific event */
  @EventListener
  public void handleLockFeedback(final AfterPatchEvent<Room> event) {
    if (event.getRequestedChanges().containsKey("settings")) {
      final String roomId = event.getEntity().getId();
      final RoomSettings settings = roomSettingsService.getByRoomId(roomId);
      if (!settings.isSurveyEnabled()) {
        final FeedbackStopped stompEvent = new FeedbackStopped();

        messagingTemplate.convertAndSend(
            "amq.topic",
            roomId + ".feedback.stream",
            stompEvent
        );

      } else {
        final FeedbackStarted stompEvent = new FeedbackStarted();

        messagingTemplate.convertAndSend(
            "amq.topic",
            roomId + ".feedback.stream",
            stompEvent
        );
      }
    }
  }

  public void handle(final CreateFeedback command) {
    final String roomId = command.getPayload().getRoomId();
    final RoomSettings roomSettings = roomSettingsService.getByRoomId(roomId);
    final Room room = new Room();
    room.setId(roomId);
    if (roomSettings.isSurveyEnabled()) {
      final CreateFeedbackPayload p = command.getPayload();

      feedbackStorage.save(room, p.getValue(), p.getUserId());
      final Feedback feedback = feedbackStorage.getByRoom(room);
      final int[] newVals = feedback.getValues().stream().mapToInt(i -> i).toArray();

      final FeedbackChanged feedbackChanged = new FeedbackChanged();
      final FeedbackChangedPayload feedbackChangedPayload = new FeedbackChangedPayload();
      feedbackChangedPayload.setValues(newVals);
      feedbackChanged.setPayload(feedbackChangedPayload);

      messagingTemplate.convertAndSend(
          "amq.topic",
          roomId + ".feedback.stream",
          feedbackChanged
      );
    }
  }

  public void handle(final ResetFeedback command) {
    final String roomId = command.getPayload().getRoomId();
    final Room room = new Room();
    room.setId(roomId);
    feedbackStorage.cleanVotesByRoom(room, 0);

    final FeedbackReset event = new FeedbackReset();

    messagingTemplate.convertAndSend(
        "amq.topic",
        roomId + ".feedback.stream",
        event
    );
  }

}
