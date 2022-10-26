package de.thm.arsnova.service.comment.service.persistence;

import de.thm.arsnova.service.comment.model.Vote;
import de.thm.arsnova.service.comment.model.VotePK;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface VoteRepository extends CrudRepository<Vote, VotePK> {
    List<Vote> findByCommentId(String commentId);
}
