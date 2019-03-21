package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.event.CommentPatched;
import de.thm.arsnova.service.comment.model.event.CommentPatchedPayload;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CommentEventSource {
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

    public void ScoreChanged(String id) {
        Comment c = service.get(id);
        int score = c.getScore();

        Map<String, Object> changeMap = new HashMap<>();
        changeMap.put("score", score);

        CommentPatchedPayload p = new CommentPatchedPayload(id, changeMap);
        CommentPatched event = new CommentPatched(p, c.getRoomId());

        messagingTemplate.convertAndSend(
                "amq.topic",
                event.getRoomId() + ".comment.stream",
                event
        );
    }
}
