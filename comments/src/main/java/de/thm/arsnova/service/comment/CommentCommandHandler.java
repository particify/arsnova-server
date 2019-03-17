package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.message.CommentCreated;
import de.thm.arsnova.service.comment.message.CommentCreatedPayload;
import de.thm.arsnova.service.comment.message.CreateComment;
import de.thm.arsnova.service.comment.message.CreateCommentPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Component
public class CommentCommandHandler {
    private HashMap<String, List<Comment>> commentList = new HashMap<>();

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public CommentCommandHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    synchronized private void addComment(Comment comment) {
        commentList.getOrDefault(comment.getRoomId(), new ArrayList<>()).add(comment);
    }

    public void handle(CreateComment command) {
        Date now = new Date();

        Comment newComment = new Comment();
        CreateCommentPayload payload = command.getPayload();
        newComment.setRoomId(payload.getRoomId());
        newComment.setCreatorId(payload.getCreatorId());
        newComment.setSubject(payload.getSubject());
        newComment.setBody(payload.getBody());
        newComment.setTimestamp(now);
        newComment.setRead(false);

        addComment(newComment);
        CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload();
        commentCreatedPayload.setSubject(newComment.getSubject());
        commentCreatedPayload.setBody(newComment.getBody());
        commentCreatedPayload.setTimestamp(now);

        CommentCreated commentCreated = new CommentCreated();
        commentCreated.setPayload(commentCreatedPayload);

        messagingTemplate.convertAndSend(
                "/queue/" + payload.getRoomId() + ".comment.stream",
                commentCreated
        );
    }
}
