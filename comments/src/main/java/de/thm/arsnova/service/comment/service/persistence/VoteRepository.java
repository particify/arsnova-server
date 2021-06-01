package de.thm.arsnova.service.comment.service.persistence;

import de.thm.arsnova.service.comment.model.Vote;
import de.thm.arsnova.service.comment.model.VotePK;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends CrudRepository<Vote, VotePK> {
    List<Vote> findByCommentId(String commentId);
    // The two vote params are needed because otherwise Hibernate can't find the second usage
    @Query(value = "INSERT INTO vote (comment_id, user_id, vote) " +
            "VALUES (:commentId, :userId, :vote) " +
            "ON CONFLICT ON CONSTRAINT vote_pkey DO UPDATE SET vote = :updateVote " +
            "RETURNING *;",
            nativeQuery = true)
    Vote createOrUpdate(
            @Param("commentId") String commentId,
            @Param("userId") String userId,
            @Param("vote") int vote,
            @Param("updateVote") int updateVote
    );
}
