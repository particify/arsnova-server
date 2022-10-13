package de.thm.arsnova.service.comment.handler;

import de.thm.arsnova.service.comment.CommentEventSource;
import de.thm.arsnova.service.comment.exception.ForbiddenException;
import de.thm.arsnova.service.comment.model.command.ResetVote;
import de.thm.arsnova.service.comment.security.AuthenticatedUser;
import de.thm.arsnova.service.comment.security.PermissionEvaluator;
import de.thm.arsnova.service.comment.service.VoteService;
import de.thm.arsnova.service.comment.model.Vote;
import de.thm.arsnova.service.comment.model.command.Downvote;
import de.thm.arsnova.service.comment.model.command.Upvote;
import de.thm.arsnova.service.comment.model.command.VotePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
