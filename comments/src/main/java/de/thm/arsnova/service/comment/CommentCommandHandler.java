package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.message.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Component
public class CommentCommandHandler {
    private HashMap<String, List<Comment>> commentList = new HashMap<>();

    private final SimpMessagingTemplate messagingTemplate;
    private final CommentService service;

    @Autowired
    public CommentCommandHandler(
            SimpMessagingTemplate messagingTemplate,
            CommentService service) {
        this.messagingTemplate = messagingTemplate;
        this.service = service;
    }

    public Comment handle(CreateComment command) {
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

        CommentCreated commentCreated = new CommentCreated();
        commentCreated.setPayload(commentCreatedPayload);

        messagingTemplate.convertAndSend(
                "/queue/" + payload.getRoomId() + ".comment.stream",
                commentCreated
        );

        return saved;
    }

    public Comment handle(PatchComment command) throws IOException {
        PatchCommentPayload p = command.getPayload();
        Comment c = this.service.get(p.getId());

        Comment patched = this.service.patch(c, p.getChanges());

        CommentPatchedPayload payload = new CommentPatchedPayload(patched.getId(), p.getChanges());
        CommentPatched event = new CommentPatched();
        event.setPayload(payload);

        messagingTemplate.convertAndSend(
                "/queue/" + c.getRoomId() + ".comment.stream",
                event
        );

        return patched;
    }

    public Comment handle(UpdateComment command) {
        UpdateCommentPayload p = command.getPayload();
        Comment old = this.service.get(p.getId());
        old.setBody(p.getBody());
        old.setRead(p.isRead());
        old.setFavorite(p.isFavorite());
        old.setCorrect(p.isCorrect());

        Comment updated = this.service.update(old);

        CommentUpdatedPayload payload = new CommentUpdatedPayload(updated);
        CommentUpdated event = new CommentUpdated(payload);

        messagingTemplate.convertAndSend(
                "/queue/" + updated.getRoomId() + ".comment.stream",
                updated
        );

        return updated;
    }
}
