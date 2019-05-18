package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VoteService {
    final VoteRepository repository;

    @Autowired
    public VoteService(VoteRepository repository) {
        this.repository = repository;
    }

    public Vote get(String id) {
        // ToDo: error handling
        return repository.findById(id).orElse(null);
    }

    public List<Vote> get(List<String> ids) {
        Iterable<Vote> it = repository.findAllById(ids);
        List<Vote> list = new ArrayList<Vote>();
        it.forEach(list::add);

        return list;
    }

    public Vote create(Vote v) {

        Vote eventualOldVote = repository.findByCommentIdAndUserId(v.getCommentId(), v.getUserId());
        if (eventualOldVote != null) {
            v.setId(eventualOldVote.getId());
        } else {
            String newId = UUID.randomUUID().toString().replace("-", "");
            v.setId(newId);
        }
        repository.save(v);

        return v;
    }

    public int scoreForComment(String commentId) {
        List<Vote> l = repository.findByCommentId(commentId);
        int score = 0;
        for (Vote v : l) {
            score = score + v.getVote();
        }

        return score;
    }

    public List<Vote> getForCommentsAndUser(List<String> commentIds, String userId) {
        List<Vote> voteList = new ArrayList<>();

        commentIds.forEach((id) -> {
            Vote tmp = repository.findByCommentIdAndUserId(id, userId);
            if (tmp != null) {
                voteList.add(tmp);
            }
        });

        return voteList;
    }
}
