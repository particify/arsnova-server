package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Vote;
import de.thm.arsnova.service.comment.model.command.Downvote;
import de.thm.arsnova.service.comment.model.command.Upvote;
import de.thm.arsnova.service.comment.model.command.VotePayload;
import de.thm.arsnova.service.comment.model.event.CommentPatchedPayload;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VoteCommandHandler {
    private final AmqpTemplate messagingTemplate;
    private final VoteService service;
    private final CommentEventSource eventer;

    @Autowired
    public VoteCommandHandler(
            AmqpTemplate messagingTemplate,
            VoteService service,
            CommentEventSource eventer
    ) {
        this.messagingTemplate = messagingTemplate;
        this.service = service;
        this.eventer = eventer;
    }

    public Vote handle(Upvote vote) {
        VotePayload p = vote.getPayload();
        Vote v = new Vote();
        v.setCommentId(p.getCommentId());
        v.setVote(1);
        v.setUserId(p.getUserId());

        Vote saved = service.create(v);

        eventer.ScoreChanged(p.getCommentId());

        return saved;
    }

    public Vote handle(Downvote vote) {
        VotePayload p = vote.getPayload();
        Vote v = new Vote();
        v.setCommentId(p.getCommentId());
        v.setVote(-1);
        v.setUserId(p.getUserId());

        Vote saved = service.create(v);

        eventer.ScoreChanged(p.getCommentId());

        return saved;
    }
}
