package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.command.CreateComment;
import de.thm.arsnova.service.comment.model.command.DeleteComment;
import de.thm.arsnova.service.comment.model.command.PatchComment;
import de.thm.arsnova.service.comment.model.command.UpdateComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@RabbitListener(queues = "comment.command")
public class CommentHandler {
    private final CommentCommandHandler commandHandler;

    private static final Logger log = LoggerFactory.getLogger(CommentHandler.class);

    @Autowired
    public CommentHandler(CommentCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @RabbitListener(queues = "comment.command.create")
    public void receiveMessage(final CreateComment message) {
        commandHandler.handle(message);
    }

    @RabbitListener(queues = "comment.command.patch")
    public void receiveMessage(final PatchComment message) {
        try {
            commandHandler.handle(message);
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    @RabbitListener(queues = "comment.command.update")
    public void receiveMessage(final UpdateComment message) throws IOException {
        commandHandler.handle(message);
    }

    @RabbitListener(queues = "comment.command.delete")
    public void receiveMessage(final DeleteComment message) {
        commandHandler.handle(message);
    }
}
