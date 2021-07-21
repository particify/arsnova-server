package de.thm.arsnova.service.comment.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.thm.arsnova.service.comment.config.RabbitConfig;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.Vote;
import de.thm.arsnova.service.comment.service.CommentService;
import de.thm.arsnova.service.comment.service.VoteService;

@Service
public class RoomDuplicatedListener {
    private static final String NIL_UUID = "00000000-0000-0000-0000-000000000000";
    private CommentService commentService;
    private VoteService voteService;

    public RoomDuplicatedListener(
            final CommentService commentService,
            final VoteService voteService) {
        this.commentService = commentService;
        this.voteService = voteService;
    }

    @RabbitListener(queues = RabbitConfig.BACKEND_ROOM_DUPLICATED_QUEUE_NAME)
    @Transactional
    public void receiveMessage(final RoomDuplicatedMessage message) {
        Map<String, Comment> commentMapping = duplicateComments(message.originalRoomId, message.duplicatedRoomId);
        duplicateVotes(message.originalRoomId, commentMapping);
    }

    private Map<String, Comment> duplicateComments(final String originalRoomId, final String duplicatedRoomId) {
        final Map<String, Comment> commentMapping = new HashMap<>();
        final List<Comment> comments = commentService.getByRoomIdAndArchiveIdNull(originalRoomId);
        final List<Comment> commentCopies = comments.stream().map(c -> {
            final Comment commentCopy = new Comment(c);
            commentMapping.put(c.getId(), commentCopy);
            commentCopy.setCreatorId(NIL_UUID);
            commentCopy.setRoomId(duplicatedRoomId);
            return commentCopy;
        }).collect(Collectors.toList());
        commentService.create(commentCopies);

        return commentMapping;
    }

    private void duplicateVotes(final String originalRoomId, Map<String, Comment> commentMapping) {
        final Map<String, Integer> voteSums = voteService.getSumByCommentForRoom(originalRoomId);
        for (final Map.Entry<String, Integer> voteSum : voteSums.entrySet()) {
            if (!commentMapping.containsKey(voteSum.getKey())) {
                continue;
            }
            final Vote vote = new Vote();
            vote.setUserId(NIL_UUID);
            vote.setCommentId(commentMapping.get(voteSum.getKey()).getId());
            vote.setVote(voteSum.getValue());
            voteService.create(vote);
        }
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
