package de.thm.arsnova.service.comment.service;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.VotePK;
import de.thm.arsnova.service.comment.service.persistence.VoteRepository;
import de.thm.arsnova.service.comment.model.Vote;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;

import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class VoteService {
    private static final String NIL_UUID = "00000000-0000-0000-0000-000000000000";

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
        return repository.createOrUpdate(v.getCommentId(), v.getUserId(), v.getVote(), v.getVote());
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

    public int getSumByCommentId(final String commentId) {
        return repository.sumByCommentId(commentId);
    }

    public Map<String, Integer> getSumsByCommentIds(final List<String> commentIds) {
        return repository.sumByCommentIdFindByCommentIdAndArchiveIdNull(commentIds).stream().collect(Collectors.toMap(
                voteSum -> voteSum.getCommentId(),
                voteSum -> voteSum.getSum()
        ));
    }

    public Map<String, Integer> getSumByCommentForRoom(final String roomId) {
        return repository.sumByCommentIdFindByRoomIdAndArchiveIdNull(roomId).stream().collect(Collectors.toMap(
                voteSum -> voteSum.getCommentId(),
                voteSum -> voteSum.getSum()
        ));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Vote resetVote(String commentId, String userId) {
        Vote v = repository.findById(new VotePK(userId, commentId)).orElse(null);

        if (v != null) {
            repository.delete(v);
        }

        return v;
    }

    public void duplicateVotes(final String originalRoomId, Map<String, Comment> commentMapping) {
        final Map<String, Integer> voteSums = getSumByCommentForRoom(originalRoomId);
        for (final Map.Entry<String, Integer> voteSum : voteSums.entrySet()) {
            if (!commentMapping.containsKey(voteSum.getKey())) {
                continue;
            }
            final Vote vote = new Vote();
            vote.setUserId(NIL_UUID);
            vote.setCommentId(commentMapping.get(voteSum.getKey()).getId());
            vote.setVote(voteSum.getValue());
            create(vote);
        }
    }
}
