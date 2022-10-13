package de.thm.arsnova.service.comment.service.persistence;

import de.thm.arsnova.service.comment.model.Vote;
import de.thm.arsnova.service.comment.model.VotePK;
import de.thm.arsnova.service.comment.model.VoteSum;

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

  @Query("SELECT COALESCE(SUM(vote), 0) AS sum " +
      "FROM Vote " +
      "WHERE commentId = :commentId")
  int sumByCommentId(@Param("commentId") String commentId);

  @Query("SELECT new de.thm.arsnova.service.comment.model.VoteSum(commentId AS key, SUM(vote) AS sum) " +
      "FROM Vote " +
      "WHERE commentId IN (:commentIds) " +
      "GROUP BY commentId")
  List<VoteSum> sumByCommentIdFindByCommentIdAndArchiveIdNull(@Param("commentIds") List<String> commentIds);

  @Query("SELECT new de.thm.arsnova.service.comment.model.VoteSum(v.commentId AS key, SUM(v.vote) AS sum) " +
      "FROM Vote AS v JOIN Comment AS c ON v.commentId = c.id " +
      "WHERE c.roomId = :roomId " +
      "AND c.archiveId IS NULL " +
      "GROUP BY v.commentId")
  List<VoteSum> sumByCommentIdFindByRoomIdAndArchiveIdNull(
      @Param("roomId") String roomId
  );
}
