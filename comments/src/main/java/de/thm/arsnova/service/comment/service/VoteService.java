package de.thm.arsnova.service.comment.service;

import de.thm.arsnova.service.comment.model.VotePK;
import de.thm.arsnova.service.comment.service.persistence.VoteRepository;
import de.thm.arsnova.service.comment.model.Vote;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;

import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class VoteService {
    final VoteRepository repository;

    @Autowired
    public VoteService(VoteRepository repository) {
        this.repository = repository;
    }

    public Vote get(VotePK id) {
        // ToDo: error handling
        return repository.findById(id).orElse(null);
    }

    public List<Vote> get(List<VotePK> ids) {
        Iterable<Vote> it = repository.findAllById(ids);
        List<Vote> list = new ArrayList<Vote>();
        it.forEach(list::add);

        return list;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Vote create(Vote v) {
        Vote oldVote = repository.findById(new VotePK(v.getCommentId(), v.getUserId())).orElse(null);

        if (repository.existsById(new VotePK(v.getCommentId(), v.getUserId()))) {
            oldVote.setVote(v.getVote());
            repository.save(oldVote);
            return oldVote;
        } else {
            repository.save(v);
            return v;
        }
    }

    public void delete(Vote v) {
        repository.delete(v);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Vote delete(String commentId, String userId) {
        Vote v = repository.findById(new VotePK(userId, commentId)).orElse(null);

        if (v != null) {
            repository.delete(v);
        }

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
            Vote tmp = repository.findById(new VotePK(userId, id)).orElse(null);
            if (tmp != null) {
                voteList.add(tmp);
            }
        });

        return voteList;
    }

    public Vote getForCommentAndUser(String commentId, String userId) {
        return repository.findById(new VotePK(commentId, userId)).orElse(null);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Vote resetVote(String commentId, String userId) {
        Vote v = repository.findById(new VotePK(userId, commentId)).orElse(null);

        if (v != null) {
            repository.delete(v);
        }

        return v;
    }
}
