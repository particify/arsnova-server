package net.particify.arsnova.comments.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.comments.handler.VoteCommandHandler;
import net.particify.arsnova.comments.model.Vote;
import net.particify.arsnova.comments.model.VotePK;
import net.particify.arsnova.comments.model.command.Downvote;
import net.particify.arsnova.comments.model.command.ResetVote;
import net.particify.arsnova.comments.model.command.Upvote;
import net.particify.arsnova.comments.model.command.VotePayload;
import net.particify.arsnova.comments.service.FindQuery;
import net.particify.arsnova.comments.service.VoteFindQueryService;
import net.particify.arsnova.comments.service.VoteService;

@RestController("VoteController")
@RequestMapping(VoteController.REQUEST_MAPPING)
public class VoteController extends AbstractEntityController {
  private static final Logger logger = LoggerFactory.getLogger(VoteController.class);

  protected static final String REQUEST_MAPPING = "/room/{roomId}/vote";
  protected static final String DELETE_MAPPING = "/{commentId}/{userId}";

  private final VoteCommandHandler commandHandler;
  private final VoteService service;
  private final VoteFindQueryService findQueryService;

  @Autowired
  public VoteController(
      final VoteCommandHandler commandHandler,
      final VoteService service,
      final VoteFindQueryService findQueryService
  ) {
    this.commandHandler = commandHandler;
    this.service = service;
    this.findQueryService = findQueryService;
  }

  /*@GetMapping(GET_MAPPING)
  public Vote get(@PathVariable String id) {
    return service.get(id);
  }*/

  @PostMapping(POST_MAPPING)
  @ResponseStatus(HttpStatus.CREATED)
  public Vote post(
      @RequestBody final Vote vote,
      final HttpServletResponse httpServletResponse
  ) {
    Vote v = null;
    final VotePayload payload = new VotePayload(vote.getUserId(), vote.getCommentId());
    switch (vote.getVote()) {
      case 1:
        final Upvote upvote = new Upvote(payload);
        v = commandHandler.handle(upvote);
        break;
      case -1:
        final Downvote downvote = new Downvote(payload);
        v = commandHandler.handle(downvote);
        break;
      default:
    }

    if (v != null) {
      /*final String uri = UriComponentsBuilder.fromPath(REQUEST_MAPPING).path(GET_MAPPING)
          .buildAndExpand(v.getId()).toUriString();
      httpServletResponse.setHeader(HttpHeaders.LOCATION, uri);
      httpServletResponse.setHeader(ENTITY_ID_HEADER, v.getId());*/

      return v;
    } else {
      logger.warn("Vote is neither up- nor downvote: " + vote.toString());
      throw new HttpMessageNotReadableException("Invalid request");
    }
  }

  @DeleteMapping(DELETE_MAPPING)
  public void delete(
      @PathVariable final String commentId,
      @PathVariable final String userId
  ) {
    logger.debug("Resolving delete request with commentId: {}, userId: {}", commentId, userId);
    final VotePayload payload = new VotePayload(userId, commentId);
    final ResetVote resetVote = new ResetVote(payload);
    commandHandler.handle(resetVote);
  }

  @PostMapping(FIND_MAPPING)
  public List<Vote> find(@RequestBody final FindQuery<Vote> findQuery) {
    logger.debug("Resolving find query: {}", findQuery);

    Set<VotePK> ids = findQueryService.resolveQuery(findQuery);

    logger.debug("Resolved find query to IDs: {}", ids);

    return service.get(new ArrayList<>(ids));
  }
}
