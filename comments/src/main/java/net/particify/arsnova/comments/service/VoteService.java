package net.particify.arsnova.comments.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.Vote;
import net.particify.arsnova.comments.model.VotePK;
import net.particify.arsnova.comments.service.persistence.VoteRepository;

@Service
public class VoteService {
  private static final UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

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
  public Vote delete(UUID commentId, UUID userId) {
    Vote v = repository.findById(new VotePK(userId, commentId)).orElse(null);

    if (v != null) {
      repository.delete(v);
    }

    return v;
  }

  public List<Vote> getForCommentsAndUser(List<UUID> commentIds, UUID userId) {
    List<Vote> voteList = new ArrayList<>();

    commentIds.forEach((id) -> {
      Vote tmp = repository.findById(new VotePK(userId, id)).orElse(null);
      if (tmp != null) {
        voteList.add(tmp);
      }
    });

    return voteList;
  }

  public Vote getForCommentAndUser(UUID commentId, UUID userId) {
    return repository.findById(new VotePK(commentId, userId)).orElse(null);
  }

  public int getSumByCommentId(final UUID commentId) {
    return repository.sumByCommentId(commentId);
  }

  public Map<UUID, Integer> getSumsByCommentIds(final List<UUID> commentIds) {
    return repository.sumByCommentIdFindByCommentIdAndArchiveIdNull(commentIds).stream().collect(Collectors.toMap(
        voteSum -> voteSum.getCommentId(),
        voteSum -> voteSum.getSum()
    ));
  }

  public Map<UUID, Integer> getSumByCommentForRoom(final UUID roomId) {
    return repository.sumByCommentIdFindByRoomIdAndArchiveIdNull(roomId).stream().collect(Collectors.toMap(
        voteSum -> voteSum.getCommentId(),
        voteSum -> voteSum.getSum()
    ));
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Vote resetVote(UUID commentId, UUID userId) {
    Vote v = repository.findById(new VotePK(userId, commentId)).orElse(null);

    if (v != null) {
      repository.delete(v);
    }

    return v;
  }

  public void duplicateVotes(final UUID originalRoomId, Map<UUID, Comment> commentMapping) {
    final Map<UUID, Integer> voteSums = getSumByCommentForRoom(originalRoomId);
    for (final Map.Entry<UUID, Integer> voteSum : voteSums.entrySet()) {
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
