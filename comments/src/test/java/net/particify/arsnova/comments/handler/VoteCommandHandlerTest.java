package net.particify.arsnova.comments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.particify.arsnova.comments.CommentEventSource;
import net.particify.arsnova.comments.model.Vote;
import net.particify.arsnova.comments.model.command.Downvote;
import net.particify.arsnova.comments.model.command.ResetVote;
import net.particify.arsnova.comments.model.command.Upvote;
import net.particify.arsnova.comments.model.command.VotePayload;
import net.particify.arsnova.comments.security.PermissionEvaluator;
import net.particify.arsnova.comments.service.VoteService;

@ExtendWith(MockitoExtension.class)
public class VoteCommandHandlerTest {
  @Mock
  private VoteService voteService;

  @Mock
  private CommentEventSource commentEventSource;

  @Mock
  private PermissionEvaluator permissionEvaluator;

  private VoteCommandHandler commandHandler;

  @BeforeEach
  public void setup() {
    this.commandHandler = new VoteCommandHandler(voteService, commentEventSource, permissionEvaluator);
  }

  @Test
  public void testHandleUpvote() {
    UUID commentId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UUID creatorId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    VotePayload payload = new VotePayload(creatorId, commentId);
    Upvote command = new Upvote(payload);
    Vote expectedVote = new Vote();
    expectedVote.setCommentId(commentId);
    expectedVote.setUserId(creatorId);
    expectedVote.setVote(1);

    when(voteService.create(any())).thenReturn(expectedVote);
    when(permissionEvaluator.checkVoteOwnerPermission(any())).thenReturn(true);

    Vote returned = commandHandler.handle(command);

    ArgumentCaptor<UUID> commentIdCaptor = ArgumentCaptor.forClass(UUID.class);

    verify(commentEventSource, times(1)).scoreChanged(commentIdCaptor.capture());
    assertThat(returned).isEqualTo(expectedVote);
    assertThat(commentIdCaptor.getValue()).isEqualTo(commentId);
  }

  @Test
  public void testHandleDownvote() {
    UUID commentId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UUID creatorId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    VotePayload payload = new VotePayload(creatorId, commentId);
    Downvote command = new Downvote(payload);
    Vote expectedVote = new Vote();
    expectedVote.setCommentId(commentId);
    expectedVote.setUserId(creatorId);
    expectedVote.setVote(-1);

    when(voteService.create(any())).thenReturn(expectedVote);
    when(permissionEvaluator.checkVoteOwnerPermission(any())).thenReturn(true);

    Vote returned = commandHandler.handle(command);

    ArgumentCaptor<UUID> commentIdCaptor = ArgumentCaptor.forClass(UUID.class);

    verify(commentEventSource, times(1)).scoreChanged(commentIdCaptor.capture());
    assertThat(returned).isEqualTo(expectedVote);
    assertThat(commentIdCaptor.getValue()).isEqualTo(commentId);
  }

  @Test
  public void testHandleResetVote() {
    UUID commentId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UUID creatorId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    VotePayload payload = new VotePayload(creatorId, commentId);
    ResetVote command = new ResetVote(payload);
    Vote currentVote = new Vote();
    currentVote.setCommentId(commentId);
    currentVote.setUserId(creatorId);
    currentVote.setVote(-1);

    when(voteService.resetVote(commentId, creatorId)).thenReturn(currentVote);
    when(permissionEvaluator.checkVoteOwnerPermission(any())).thenReturn(true);

    commandHandler.handle(command);

    ArgumentCaptor<UUID> commentIdCaptor = ArgumentCaptor.forClass(UUID.class);

    verify(commentEventSource, times(1)).scoreChanged(commentIdCaptor.capture());
    assertThat(commentIdCaptor.getValue()).isEqualTo(commentId);
  }

  @Test
  public void testHandleResetVoteWithoutCurrentVote() {
    UUID commentId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UUID creatorId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    VotePayload payload = new VotePayload(creatorId, commentId);
    ResetVote command = new ResetVote(payload);

    when(voteService.resetVote(commentId, creatorId)).thenReturn(null);
    when(permissionEvaluator.checkVoteOwnerPermission(any())).thenReturn(true);

    commandHandler.handle(command);

    ArgumentCaptor<UUID> commentIdCaptor = ArgumentCaptor.forClass(UUID.class);

    verify(commentEventSource, never()).scoreChanged(commentIdCaptor.capture());
  }
}
