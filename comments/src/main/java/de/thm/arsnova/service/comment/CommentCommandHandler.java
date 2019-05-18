package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.command.*;
import de.thm.arsnova.service.comment.model.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Component
public class CommentCommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommentCommandHandler.class);

    private final AmqpTemplate messagingTemplate;
    private final CommentService service;

    @Autowired
    public CommentCommandHandler(
            AmqpTemplate messagingTemplate,
            CommentService service) {
        this.messagingTemplate = messagingTemplate;
        this.service = service;
    }

    public Comment handle(CreateComment command) {
        logger.trace("got new command: " + command.toString());

        Date now = new Date();

        Comment newComment = new Comment();
        CreateCommentPayload payload = command.getPayload();
        newComment.setRoomId(payload.getRoomId());
        newComment.setCreatorId(payload.getCreatorId());
        newComment.setBody(payload.getBody());
        newComment.setTimestamp(now);
        newComment.setRead(false);
        newComment.setCorrect(false);
        newComment.setFavorite(false);

        Comment saved = service.create(newComment);

        CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(saved);
        commentCreatedPayload.setTimestamp(now);

        CommentCreated event = new CommentCreated(commentCreatedPayload, payload.getRoomId());

        messagingTemplate.convertAndSend(
                "amq.topic",
                payload.getRoomId() + ".comment.stream",
                event
        );

        return saved;
    }

    public Comment handle(PatchComment command) throws IOException {
        logger.trace("got new command: " + command.toString());

        PatchCommentPayload p = command.getPayload();
        Comment c = this.service.get(p.getId());

        if (c.getId() != null) {
            Comment patched = this.service.patch(c, p.getChanges());

            CommentPatchedPayload payload = new CommentPatchedPayload(patched.getId(), p.getChanges());
            CommentPatched event = new CommentPatched(payload, patched.getRoomId());

            messagingTemplate.convertAndSend(
                    "amq.topic",
                    c.getRoomId() + ".comment.stream",
                    event
            );

            return patched;
        } else {
            // ToDo: Error handling
            return c;
        }
    }

    public Comment handle(UpdateComment command) {
        logger.trace("got new command: " + command.toString());

        UpdateCommentPayload p = command.getPayload();
        Comment old = this.service.get(p.getId());
        old.setBody(p.getBody());
        old.setRead(p.isRead());
        old.setFavorite(p.isFavorite());
        old.setCorrect(p.isCorrect());

        Comment updated = this.service.update(old);

        CommentUpdatedPayload payload = new CommentUpdatedPayload(updated);
        CommentUpdated event = new CommentUpdated(payload, updated.getRoomId());

        messagingTemplate.convertAndSend(
                "amq.topic",
                old.getRoomId() + ".comment.stream",
                event
        );

        return updated;
    }

    public void handle(DeleteComment command) {
        logger.trace("got new command: " + command.toString());

        String id = command.getPayload().getId();
        Comment c = service.get(id);
        if (c.getId() != null) {
            service.delete(id);

            CommentDeletedPayload p = new CommentDeletedPayload();
            p.setId(c.getId());
            CommentDeleted event = new CommentDeleted(p, c.getRoomId());

            messagingTemplate.convertAndSend(
                    "amq.topic",
                    c.getRoomId() + ".comment.stream",
                    event
            );
        }
    }

    public void handle(HighlightComment command) {
        logger.trace("got new command: " + command.toString());

        String id = command.getPayload().getId();
        Comment c = service.get(id);
        if (c.getId() != null) {
            CommentHighlightedPayload p = new CommentHighlightedPayload(c, command.getPayload().getLights());
            CommentHighlighted event = new CommentHighlighted(p, c.getRoomId());
            messagingTemplate.convertAndSend(
                    "amq.topic",
                    c.getRoomId() + ".comment.stream",
                    event
            );
        }
    }

    public void handle(DeleteCommentsByRoom command) {
        logger.trace("got new command: " + command.toString());

        String roomId = command.getPayload().getRoomId();
        List<Comment> deletedComments = service.deleteByRoomId(roomId);
        for (Comment c : deletedComments) {
            CommentDeletedPayload p = new CommentDeletedPayload();
            p.setId(c.getId());
            CommentDeleted event = new CommentDeleted(p, c.getRoomId());

            messagingTemplate.convertAndSend(
                    "amq.topic",
                    c.getRoomId() + ".comment.stream",
                    event
            );
        }
    }

}
