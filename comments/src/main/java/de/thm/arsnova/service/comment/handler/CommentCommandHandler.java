package de.thm.arsnova.service.comment.handler;

import de.thm.arsnova.service.comment.model.BonusToken;
import de.thm.arsnova.service.comment.model.Settings;
import de.thm.arsnova.service.comment.service.BonusTokenService;
import de.thm.arsnova.service.comment.service.CommentService;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.command.*;
import de.thm.arsnova.service.comment.model.event.*;
import de.thm.arsnova.service.comment.service.SettingsService;
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
    private final BonusTokenService bonusTokenService;
    private final SettingsService settingsService;

    @Autowired
    public CommentCommandHandler(
            AmqpTemplate messagingTemplate,
            CommentService service,
            BonusTokenService bonusTokenService,
            SettingsService settingsService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.service = service;
        this.bonusTokenService = bonusTokenService;
        this.settingsService = settingsService;
    }

    public Comment handle(CreateComment command) {
        logger.trace("got new command: " + command.toString());

        Date now = new Date();

        Comment newComment = new Comment();
        CreateCommentPayload payload = command.getPayload();

        Settings settings = settingsService.get(payload.getRoomId());

        newComment.setRoomId(payload.getRoomId());
        newComment.setCreatorId(payload.getCreatorId());
        newComment.setBody(payload.getBody());
        newComment.setTag(payload.getTag());
        newComment.setTimestamp(now);
        newComment.setRead(false);
        newComment.setCorrect(0);
        newComment.setFavorite(false);
        newComment.setAck(settings.getDirectSend());

        Comment saved = service.create(newComment);

        CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(saved);
        commentCreatedPayload.setTimestamp(now);

        CommentCreated event = new CommentCreated(commentCreatedPayload, payload.getRoomId());

        if (settings.getDirectSend()) {
            messagingTemplate.convertAndSend(
                    "amq.topic",
                    payload.getRoomId() + ".comment.stream",
                    event
            );
        } else {
            messagingTemplate.convertAndSend(
                    "amq.topic",
                    payload.getRoomId() + ".comment.moderator.stream",
                    event
            );
        }

        return saved;
    }

    public Comment handle(PatchComment command) throws IOException {
        logger.trace("got new command: " + command.toString());

        PatchCommentPayload p = command.getPayload();
        Comment c = this.service.get(p.getId());

        boolean wasAck = c.isAck();
        boolean wasFavorited = c.isFavorite();

        if (c.getId() != null) {
            Comment patched = this.service.patch(c, p.getChanges());

            CommentPatchedPayload payload = new CommentPatchedPayload(patched.getId(), p.getChanges());
            CommentPatched event = new CommentPatched(payload, patched.getRoomId());

            if (!wasFavorited && patched.isFavorite()) {
                BonusToken bt = new BonusToken();
                bt.setRoomId(patched.getRoomId());
                bt.setCommentId(patched.getId());
                bt.setUserId(patched.getCreatorId());
                bonusTokenService.create(bt);

            } else if (wasFavorited && !patched.isFavorite()) {
                bonusTokenService.deleteByPK(patched.getRoomId(), patched.getId(), patched.getCreatorId());
            }

            if (!wasAck && patched.isAck()) {
                CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(patched);
                commentCreatedPayload.setTimestamp(new Date());
                CommentCreated quoteOnQuoteNew = new CommentCreated(commentCreatedPayload, patched.getRoomId());
                messagingTemplate.convertAndSend(
                        "amq.topic",
                        c.getRoomId() + ".comment.stream",
                        quoteOnQuoteNew
                );
            } else if (wasAck && !patched.isAck()) {
                CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(patched);
                commentCreatedPayload.setTimestamp(new Date());
                CommentCreated quoteOnQuoteNew = new CommentCreated(commentCreatedPayload, patched.getRoomId());
                messagingTemplate.convertAndSend(
                        "amq.topic",
                        c.getRoomId() + ".comment.moderator.stream",
                        quoteOnQuoteNew
                );
            }

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
        old.setCorrect(p.getCorrect());
        old.setTag(p.getTag());
        old.setAnswer(p.getAnswer());

        Comment updated = this.service.update(old);

        CommentUpdatedPayload payload = new CommentUpdatedPayload(updated);
        CommentUpdated event = new CommentUpdated(payload, updated.getRoomId());


        if (!old.isAck() && updated.isAck()) {
            CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(updated);
            commentCreatedPayload.setTimestamp(new Date());
            CommentCreated quoteOnQuoteNew = new CommentCreated(commentCreatedPayload, updated.getRoomId());
            messagingTemplate.convertAndSend(
                    "amq.topic",
                    old.getRoomId() + ".comment.stream",
                    quoteOnQuoteNew
            );
        } else if (old.isAck() && !updated.isAck()) {
            CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(updated);
            commentCreatedPayload.setTimestamp(new Date());
            CommentCreated quoteOnQuoteNew = new CommentCreated(commentCreatedPayload, updated.getRoomId());
            messagingTemplate.convertAndSend(
                    "amq.topic",
                    old.getRoomId() + ".comment.moderator.stream",
                    quoteOnQuoteNew
            );
        }

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
