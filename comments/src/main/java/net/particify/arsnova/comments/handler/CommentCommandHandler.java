package net.particify.arsnova.comments.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.particify.arsnova.comments.config.RabbitConfig;
import net.particify.arsnova.comments.exception.BadRequestException;
import net.particify.arsnova.comments.exception.ForbiddenException;
import net.particify.arsnova.comments.model.BonusToken;
import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.CommentStats;
import net.particify.arsnova.comments.model.Settings;
import net.particify.arsnova.comments.model.command.CalculateStats;
import net.particify.arsnova.comments.model.command.CreateComment;
import net.particify.arsnova.comments.model.command.CreateCommentPayload;
import net.particify.arsnova.comments.model.command.DeleteComment;
import net.particify.arsnova.comments.model.command.DeleteCommentsByRoom;
import net.particify.arsnova.comments.model.command.HighlightComment;
import net.particify.arsnova.comments.model.command.ImportComment;
import net.particify.arsnova.comments.model.command.ImportCommentPayload;
import net.particify.arsnova.comments.model.command.PatchComment;
import net.particify.arsnova.comments.model.command.PatchCommentPayload;
import net.particify.arsnova.comments.model.command.UpdateComment;
import net.particify.arsnova.comments.model.command.UpdateCommentPayload;
import net.particify.arsnova.comments.model.event.CommentCreated;
import net.particify.arsnova.comments.model.event.CommentCreatedPayload;
import net.particify.arsnova.comments.model.event.CommentDeleted;
import net.particify.arsnova.comments.model.event.CommentDeletedPayload;
import net.particify.arsnova.comments.model.event.CommentHighlighted;
import net.particify.arsnova.comments.model.event.CommentHighlightedPayload;
import net.particify.arsnova.comments.model.event.CommentPatched;
import net.particify.arsnova.comments.model.event.CommentPatchedPayload;
import net.particify.arsnova.comments.model.event.CommentUpdated;
import net.particify.arsnova.comments.model.event.CommentUpdatedPayload;
import net.particify.arsnova.comments.security.PermissionEvaluator;
import net.particify.arsnova.comments.service.BonusTokenService;
import net.particify.arsnova.comments.service.CommentService;
import net.particify.arsnova.comments.service.SettingsService;

@Component
public class CommentCommandHandler {
  private static final Logger logger = LoggerFactory.getLogger(CommentCommandHandler.class);

  private final AmqpTemplate messagingTemplate;
  private final CommentService service;
  private final BonusTokenService bonusTokenService;
  private final SettingsService settingsService;
  private final PermissionEvaluator permissionEvaluator;

  @Autowired
  public CommentCommandHandler(
      AmqpTemplate messagingTemplate,
      CommentService service,
      BonusTokenService bonusTokenService,
      SettingsService settingsService,
      PermissionEvaluator permissionEvaluator
  ) {
    this.messagingTemplate = messagingTemplate;
    this.service = service;
    this.bonusTokenService = bonusTokenService;
    this.settingsService = settingsService;
    this.permissionEvaluator = permissionEvaluator;
  }

  private Comment createOrImportComment(Comment comment, Settings settings) {
    Comment saved = service.create(comment);

    CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(saved);
    commentCreatedPayload.setTimestamp(comment.getTimestamp());

    CommentCreated event = new CommentCreated(commentCreatedPayload, comment.getRoomId());

    if (settings.getDirectSend()) {
      logger.debug("Sending event to comment stream: {}", event);

      messagingTemplate.convertAndSend(
          "amq.topic",
          comment.getRoomId() + ".comment.stream",
          event
      );
    } else {
      logger.debug("Sending event to moderated stream: {}", event);
      messagingTemplate.convertAndSend(
          "amq.topic",
          comment.getRoomId() + ".comment.moderator.stream",
          event
      );
    }

    return saved;
  }

  public Comment handle(CreateComment command) {
    logger.debug("Got new command: {}", command);

    Date now = new Date();

    Comment newComment = new Comment();
    CreateCommentPayload payload = command.getPayload();

    Settings settings = settingsService.get(payload.getRoomId());

    newComment.setRoomId(payload.getRoomId());
    newComment.setCreatorId(payload.getCreatorId());
    newComment.setBody(payload.getBody());
    newComment.setTag(payload.getTag());
    newComment.setTimestamp(now);
    newComment.setRead(false);
    newComment.setCorrect(0);
    newComment.setFavorite(false);
    newComment.setAck(settings.getDirectSend());

    if (!permissionEvaluator.checkCommentOwnerPermission(newComment)) {
      throw new BadRequestException();
    }

    return createOrImportComment(newComment, settings);
  }

  public Comment handle(ImportComment command) {
    logger.debug("Got new command: {}", command);

    Comment newComment = new Comment();
    ImportCommentPayload payload = command.getPayload();

    Settings settings = settingsService.get(payload.getRoomId());

    newComment.setRoomId(payload.getRoomId());
    newComment.setCreatorId(payload.getCreatorId());
    newComment.setBody(payload.getBody());
    newComment.setTag(payload.getTag());
    newComment.setTimestamp(payload.getTimestamp());
    newComment.setRead(payload.isRead());
    newComment.setCorrect(0);
    newComment.setFavorite(false);
    newComment.setAck(settings.getDirectSend());

    return createOrImportComment(newComment, settings);
  }

  public Comment handle(PatchComment command) throws IOException {
    logger.debug("Got new command: {}", command);

    PatchCommentPayload p = command.getPayload();
    Comment c = this.service.get(p.getId());

    if (!permissionEvaluator.checkCommentPatchPermission(c, p.getChanges())) {
      throw new ForbiddenException();
    }

    boolean wasAck = c.isAck();
    boolean wasFavorited = c.isFavorite();

    if (c.getId() != null) {
      try {
        Comment patched = this.service.patch(c, p.getChanges());

        CommentPatchedPayload payload = new CommentPatchedPayload(patched.getId(), p.getChanges());
        CommentPatched event = new CommentPatched(payload, patched.getRoomId());

        if (!wasFavorited && patched.isFavorite()) {
          BonusToken bt = new BonusToken();
          Date now = new Date();
          bt.setRoomId(patched.getRoomId());
          bt.setCommentId(patched.getId());
          bt.setUserId(patched.getCreatorId());
          bt.setTimestamp(now);

          logger.debug("Creating token as a side effect: {}", bt);

          bonusTokenService.create(bt);

        } else if (wasFavorited && !patched.isFavorite()) {
          bonusTokenService.deleteByPK(patched.getRoomId(), patched.getId(), patched.getCreatorId());
        }

        if (!wasAck && patched.isAck()) {
          CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(patched);
          commentCreatedPayload.setTimestamp(new Date());
          CommentCreated quoteOnQuoteNew = new CommentCreated(commentCreatedPayload, patched.getRoomId());

          logger.debug("Sending event to comment stream: {}", quoteOnQuoteNew);

          messagingTemplate.convertAndSend(
              "amq.topic",
              c.getRoomId() + ".comment.stream",
              quoteOnQuoteNew
          );
        } else if (wasAck && !patched.isAck()) {
          CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(patched);
          commentCreatedPayload.setTimestamp(new Date());
          CommentCreated quoteOnQuoteNew = new CommentCreated(commentCreatedPayload, patched.getRoomId());

          logger.debug("Sending event to moderated stream: {}", quoteOnQuoteNew);

          messagingTemplate.convertAndSend(
              "amq.topic",
              c.getRoomId() + ".comment.moderator.stream",
              quoteOnQuoteNew
          );
        }

        logger.debug("Sending event to moderated stream: {}", event);

        messagingTemplate.convertAndSend(
            "amq.topic",
            c.getRoomId() + ".comment.stream",
            event
        );

        return patched;
      } catch (IOException e) {
        logger.error("Patching of comment {} failed.", c.getId(), e);
      }
    } else {
      logger.debug("No comment found for patch command {}", command);
    }
    return c;
  }

  public Comment handle(UpdateComment command) {
    logger.debug("Got new command: {}", command);

    UpdateCommentPayload p = command.getPayload();
    Comment old = this.service.get(p.getId());
    Comment newComment = this.service.get(p.getId());
    newComment.setBody(p.getBody());
    newComment.setRead(p.isRead());
    newComment.setFavorite(p.isFavorite());
    newComment.setCorrect(p.getCorrect());
    newComment.setTag(p.getTag());
    newComment.setAnswer(p.getAnswer());

    if (!permissionEvaluator.checkCommentUpdatePermission(newComment, old)) {
      throw new ForbiddenException();
    }

    Comment updated = this.service.update(newComment);

    CommentUpdatedPayload payload = new CommentUpdatedPayload(updated);
    CommentUpdated event = new CommentUpdated(payload, updated.getRoomId());

    if (!old.isAck() && updated.isAck()) {
      CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(updated);
      commentCreatedPayload.setTimestamp(new Date());
      CommentCreated quoteOnQuoteNew = new CommentCreated(commentCreatedPayload, updated.getRoomId());

      logger.debug("Sending event to comment stream: {}", quoteOnQuoteNew);

      messagingTemplate.convertAndSend(
          "amq.topic",
          old.getRoomId() + ".comment.stream",
          quoteOnQuoteNew
      );
    } else if (old.isAck() && !updated.isAck()) {
      CommentCreatedPayload commentCreatedPayload = new CommentCreatedPayload(updated);
      commentCreatedPayload.setTimestamp(new Date());
      CommentCreated quoteOnQuoteNew = new CommentCreated(commentCreatedPayload, updated.getRoomId());

      logger.debug("Sending event to moderated stream: {}", quoteOnQuoteNew);

      messagingTemplate.convertAndSend(
          "amq.topic",
          old.getRoomId() + ".comment.moderator.stream",
          quoteOnQuoteNew
      );
    }

    logger.debug("Sending event to comment stream: {}", event);

    messagingTemplate.convertAndSend(
        "amq.topic",
        old.getRoomId() + ".comment.stream",
        event
    );

    return updated;
  }

  public void handle(DeleteComment command) {
    logger.debug("Got new command: {}", command);

    String id = command.getPayload().getId();
    Comment c = service.get(id);

    if (!permissionEvaluator.checkCommentDeletePermission(c)) {
      throw new ForbiddenException();
    }

    if (c.getId() != null) {
      service.delete(id);

      CommentDeletedPayload p = new CommentDeletedPayload();
      p.setId(c.getId());
      CommentDeleted event = new CommentDeleted(p, c.getRoomId());

      logger.debug("Sending event to comment stream: {}", event);

      messagingTemplate.convertAndSend(
          "amq.topic",
          c.getRoomId() + ".comment.stream",
          event
      );

      messagingTemplate.convertAndSend(
          RabbitConfig.COMMENT_SERVICE_COMMENT_DELETE_FANOUT_NAME,
          "",
          event
      );
    }
  }

  public void handle(HighlightComment command) {
    logger.debug("Got new command: {}", command);

    String id = command.getPayload().getId();
    Comment c = service.get(id);

    if (!permissionEvaluator.isOwnerOrAnyTypeOfModeratorForRoom(c.getRoomId())) {
      throw new ForbiddenException();
    }

    if (c.getId() != null) {
      CommentHighlightedPayload p = new CommentHighlightedPayload(c, command.getPayload().getLights());
      CommentHighlighted event = new CommentHighlighted(p, c.getRoomId());

      logger.debug("Sending event to comment stream: {}", event);

      messagingTemplate.convertAndSend(
          "amq.topic",
          c.getRoomId() + ".comment.stream",
          event
      );
    }
  }

  public void handle(DeleteCommentsByRoom command) {
    logger.debug("Got new command: {}", command);

    String roomId = command.getPayload().getRoomId();

    if (!permissionEvaluator.isOwnerOrEditingModeratorForRoom(roomId)) {
      throw new ForbiddenException();
    }

    List<Comment> deletedComments = service.deleteByRoomId(roomId);
    for (Comment c : deletedComments) {
      CommentDeletedPayload p = new CommentDeletedPayload();
      p.setId(c.getId());
      CommentDeleted event = new CommentDeleted(p, c.getRoomId());

      logger.debug("Sending event to comment stream: {}", event);

      messagingTemplate.convertAndSend(
          "amq.topic",
          c.getRoomId() + ".comment.stream",
          event
      );

      messagingTemplate.convertAndSend(
          RabbitConfig.COMMENT_SERVICE_COMMENT_DELETE_FANOUT_NAME,
          "",
          event
      );
    }
  }

  public List<CommentStats> handle(CalculateStats command) {
    logger.debug("Got new command: {}", command);

    final List<String> roomIds = command.getPayload().getRoomIds();
    final List<CommentStats> stats = new ArrayList<>();

    for (final String roomId : roomIds) {
      CommentStats roomStatistics = new CommentStats();
      int ackCommentcount = (int) service.countByRoomIdAndAck(roomId, true);
      // ToDo: Implement view to show unacknowledge counter to owner / moderators
      // int unackCommentcount = (int) service.countByRoomIdAndAck(roomId, false);
      // roomStatistics.setUnackCommentCount(unackCommentcount);
      roomStatistics.setRoomId(roomId);
      roomStatistics.setAckCommentCount(ackCommentcount);
      stats.add(roomStatistics);
    }

    return stats;
  }

}
