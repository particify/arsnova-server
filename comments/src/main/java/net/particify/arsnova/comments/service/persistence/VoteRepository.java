package net.particify.arsnova.comments.service.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import net.particify.arsnova.comments.model.Vote;
import net.particify.arsnova.comments.model.VotePK;
import net.particify.arsnova.comments.model.VoteSum;

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

  @Query("SELECT new net.particify.arsnova.comments.model.VoteSum(commentId AS key, SUM(vote) AS sum) " +
      "FROM Vote " +
      "WHERE commentId IN (:commentIds) " +
      "GROUP BY commentId")
  List<VoteSum> sumByCommentIdFindByCommentIdAndArchiveIdNull(@Param("commentIds") List<String> commentIds);

  @Query("SELECT new net.particify.arsnova.comments.model.VoteSum(v.commentId AS key, SUM(v.vote) AS sum) " +
      "FROM Vote AS v JOIN Comment AS c ON v.commentId = c.id " +
      "WHERE c.roomId = :roomId " +
      "AND c.archiveId IS NULL " +
      "GROUP BY v.commentId")
  List<VoteSum> sumByCommentIdFindByRoomIdAndArchiveIdNull(
      @Param("roomId") String roomId
  );
}
