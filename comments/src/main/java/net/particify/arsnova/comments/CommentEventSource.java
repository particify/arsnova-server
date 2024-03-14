package net.particify.arsnova.comments;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.event.CommentPatched;
import net.particify.arsnova.comments.model.event.CommentPatchedPayload;
import net.particify.arsnova.common.uuid.UuidHelper;
import net.particify.arsnova.comments.service.CommentService;
import net.particify.arsnova.comments.service.VoteService;

@Component
public class CommentEventSource {
  private static final Logger logger = LoggerFactory.getLogger(CommentEventSource.class);

  private final AmqpTemplate messagingTemplate;
  private final CommentService service;
  private final VoteService voteService;

  @Autowired
  public CommentEventSource(
      AmqpTemplate messagingTemplate,
      CommentService service,
      VoteService voteService
  ) {
    this.messagingTemplate = messagingTemplate;
    this.service = service;
    this.voteService = voteService;
  }

  public void scoreChanged(UUID id) {
    Comment c = service.getWithScore(id);
    int score = c.getScore();

    Map<String, Object> changeMap = new HashMap<>();
    changeMap.put("score", score);

    CommentPatchedPayload p = new CommentPatchedPayload(id, changeMap);
    CommentPatched event = new CommentPatched(p, c.getRoomId());

    logger.debug("Sending event to comment stream: {}", event);

    messagingTemplate.convertAndSend(
        "amq.topic",
        UuidHelper.uuidToString(event.getRoomId()) + ".comment.stream",
        event
    );
  }
}
