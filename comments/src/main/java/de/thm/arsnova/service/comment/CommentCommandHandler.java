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
        newComment.setSubject(payload.getSubject());
        newComment.setBody(payload.getBody());
        newComment.setTimestamp(now);
        newComment.setRead(false);

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

        Comment updated = this.service.patch(c, p.getChanges());

        CommentPatchedPayload payload = new CommentPatchedPayload(updated.getId(), p.getChanges());
        CommentPatched event = new CommentPatched();
        event.setPayload(payload);

        messagingTemplate.convertAndSend(
                "/queue/" + c.getRoomId() + ".comment.stream",
                event
        );

        return updated;
    }
}
