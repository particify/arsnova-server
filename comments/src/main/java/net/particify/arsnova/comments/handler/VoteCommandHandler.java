package net.particify.arsnova.comments.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.particify.arsnova.comments.CommentEventSource;
import net.particify.arsnova.comments.exception.ForbiddenException;
import net.particify.arsnova.comments.model.Vote;
import net.particify.arsnova.comments.model.command.Downvote;
import net.particify.arsnova.comments.model.command.ResetVote;
import net.particify.arsnova.comments.model.command.Upvote;
import net.particify.arsnova.comments.model.command.VotePayload;
import net.particify.arsnova.comments.security.PermissionEvaluator;
import net.particify.arsnova.comments.service.VoteService;

@Component
public class VoteCommandHandler {
  private static final Logger logger = LoggerFactory.getLogger(VoteCommandHandler.class);

  private final VoteService service;
  private final CommentEventSource eventer;
  private final PermissionEvaluator permissionEvaluator;

  @Autowired
  public VoteCommandHandler(
      VoteService service,
      CommentEventSource eventer,
      PermissionEvaluator permissionEvaluator
  ) {
    this.service = service;
    this.eventer = eventer;
    this.permissionEvaluator = permissionEvaluator;
  }

  public Vote handle(Upvote vote) {
    logger.debug("Got new command: {}", vote);

    VotePayload p = vote.getPayload();
    Vote v = new Vote();
    v.setCommentId(p.getCommentId());
    v.setVote(1);
    v.setUserId(p.getUserId());

    if (!permissionEvaluator.checkVoteOwnerPermission(v)) {
      throw new ForbiddenException();
    }

    Vote saved = service.create(v);

    eventer.ScoreChanged(p.getCommentId());

    return saved;
  }

  public Vote handle(Downvote vote) {
    logger.debug("Got new command: {}", vote);

    VotePayload p = vote.getPayload();
    Vote v = new Vote();
    v.setCommentId(p.getCommentId());
    v.setVote(-1);
    v.setUserId(p.getUserId());

    if (!permissionEvaluator.checkVoteOwnerPermission(v)) {
      throw new ForbiddenException();
    }

    Vote saved = service.create(v);

    eventer.ScoreChanged(p.getCommentId());

    return saved;
  }

  public void handle(ResetVote vote) {
    logger.debug("Got new command: {}", vote);

    VotePayload p = vote.getPayload();

    Vote v = new Vote();
    v.setUserId(p.getUserId());

    if (!permissionEvaluator.checkVoteOwnerPermission(v)) {
      throw new ForbiddenException();
    }

    v = service.resetVote(p.getCommentId(), p.getUserId());

    if (v == null) {
      logger.trace("No vote to reset");
    } else {
      logger.trace("Initialize sending the new score");
      eventer.ScoreChanged(p.getCommentId());
    }
  }
}
