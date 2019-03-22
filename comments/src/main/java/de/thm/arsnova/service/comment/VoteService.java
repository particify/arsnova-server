package de.thm.arsnova.service.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.thm.arsnova.service.comment.model.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
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
        return repository.findById(id).orElse(new Vote());
    }

    public List<Vote> get(List<String> ids) {
        Iterable<Vote> it = repository.findAllById(ids);
        List<Vote> list = new ArrayList<Vote>();
        it.forEach(list::add);

        return list;
    }

    public Vote create(Vote v) {

        Vote eventualOldVote = repository.findByCommentIdAndUserId(v.getCommentId(), v.getUserId());
        if (eventualOldVote.getId() != null) {
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
}
