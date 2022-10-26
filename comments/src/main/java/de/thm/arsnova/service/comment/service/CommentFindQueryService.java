package de.thm.arsnova.service.comment.service;

import de.thm.arsnova.service.comment.model.Comment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CommentFindQueryService {

    private static final Logger logger = LoggerFactory.getLogger(CommentFindQueryService.class);

    private final CommentService commentService;

    @Autowired
    public CommentFindQueryService(final CommentService commentService) {
        this.commentService = commentService;
    }

    public Set<String> resolveQuery(final FindQuery<Comment> findQuery) {
        Set<String> commentIds = new HashSet<>();
        if (findQuery.getProperties().getRoomId() != null) {
            List<Comment> contentList = commentService.getByRoomId(findQuery.getProperties().getRoomId());
            for (Comment c : contentList) {
                if (findQuery.getProperties().isAck() == c.isAck()) {
                    commentIds.add(c.getId());
                }
            }
        }

        return commentIds;
    }
}
