package de.thm.arsnova.service.comment.listener;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import de.thm.arsnova.service.comment.config.RabbitConfig;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.service.CommentService;

@Service
public class RoomDuplicatedListener {
    private static final String NIL_UUID = "00000000-0000-0000-0000-000000000000";
    private CommentService commentService;

    public RoomDuplicatedListener(final CommentService commentService) {
        this.commentService = commentService;
    }

    @RabbitListener(queues = RabbitConfig.BACKEND_ROOM_DUPLICATED_QUEUE_NAME)
    public void receiveMessage(final RoomDuplicatedMessage message) {
        final List<Comment> comments = commentService.getByRoomIdAndArchiveIdNull(message.originalRoomId);
        final List<Comment> commentCopies = comments.stream().map(c -> {
            final Comment commentCopy = new Comment(c);
            commentCopy.setCreatorId(NIL_UUID);
            commentCopy.setRoomId(message.duplicatedRoomId);
            return commentCopy;
        }).collect(Collectors.toList());
        commentService.create(commentCopies);
    }

    private static class RoomDuplicatedMessage {
        private String originalRoomId;
        private String duplicatedRoomId;

        public String getOriginalRoomId() {
            return originalRoomId;
        }

        public void setOriginalRoomId(final String originalRoomId) {
            this.originalRoomId = originalRoomId;
        }

        public String getDuplicatedRoomId() {
            return duplicatedRoomId;
        }

        public void setDuplicatedRoomId(final String duplicatedRoomId) {
            this.duplicatedRoomId = duplicatedRoomId;
        }
    }
}
