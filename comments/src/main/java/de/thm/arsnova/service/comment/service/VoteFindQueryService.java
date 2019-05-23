package de.thm.arsnova.service.comment.service;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VoteFindQueryService {
    private final VoteService voteService;
    private final CommentService commentService;

    @Autowired
    public VoteFindQueryService(
            final VoteService voteService,
            final CommentService commentService
    ) {
        this.voteService = voteService;
        this.commentService = commentService;
    }

    public Set<String> resolveQuery(final FindQuery<Vote> findQuery) {
        Set<String> voteIds = new HashSet<>();
        if (findQuery.getExternalFilters().get("roomId") instanceof String && findQuery.getProperties().getUserId() != null) {
            String roomId = (String) findQuery.getExternalFilters().get("roomId");
            String userId = findQuery.getProperties().getUserId();
            List<Comment> commentList = commentService.getByRoomId(roomId);
            List<String> commentIds = commentList.stream().map(Comment::getId).collect(Collectors.toList());
            List<Vote> voteList = voteService.getForCommentsAndUser(commentIds, userId);

            voteList.forEach((v) -> voteIds.add(v.getId()));
        }

        return voteIds;
    }
}
