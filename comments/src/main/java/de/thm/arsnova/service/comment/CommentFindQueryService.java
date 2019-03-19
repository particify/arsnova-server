package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CommentFindQueryService {

    private final CommentService commentService;

    @Autowired
    public CommentFindQueryService(final CommentService commentService) {
        this.commentService = commentService;
    }

    public Set<String> resolveQuery(final FindQuery findQuery) {
        Set<String> commentIds = new HashSet<>();
        if (findQuery.getProperties().getRoomId() != null) {
            List<Comment> contentList = commentService.getByRoomId(findQuery.getProperties().getRoomId());
            for (Comment c : contentList) {
                commentIds.add(c.getId());
            }
        }

        return commentIds;
    }
}
