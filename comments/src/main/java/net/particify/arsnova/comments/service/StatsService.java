package net.particify.arsnova.comments.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.particify.arsnova.comments.model.Stats;
import net.particify.arsnova.comments.service.persistence.CommentRepository;
import net.particify.arsnova.comments.service.persistence.VoteRepository;

@Service
public class StatsService {
  private final CommentRepository commentRepository;
  private final VoteRepository voteRepository;

  @Autowired
  public StatsService(
      final CommentRepository commentRepository,
      final VoteRepository voteRepository
  ) {
    this.commentRepository = commentRepository;
    this.voteRepository = voteRepository;
  }

  public Stats get() {
    final long commentCount = commentRepository.count();
    final long voteCount = voteRepository.count();

    final Stats stats = new Stats(commentCount, voteCount);

    return stats;
  }
}
