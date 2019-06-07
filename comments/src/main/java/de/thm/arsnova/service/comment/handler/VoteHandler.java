package de.thm.arsnova.service.comment.handler;

import de.thm.arsnova.service.comment.model.command.Downvote;
import de.thm.arsnova.service.comment.model.command.ResetVote;
import de.thm.arsnova.service.comment.model.command.Upvote;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RabbitListener(queues = "vote.command")
public class VoteHandler {
    private final VoteCommandHandler commandHandler;

    @Autowired
    public VoteHandler(VoteCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @RabbitListener(queues = "vote.command.upvote")
    public void receiveUpvote(final Upvote vote) {
        commandHandler.handle(vote);
    }

    @RabbitListener(queues = "vote.command.downvote")
    public void receiveDownvote(final Downvote vote) {
        commandHandler.handle(vote);
    }

    @RabbitListener(queues = "vote.command.resetvote")
    public void receiveResetVote(final ResetVote vote) {
        commandHandler.handle(vote);
    }
}
