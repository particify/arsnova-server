package de.thm.arsnova.service.comment.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.thm.arsnova.service.comment.CommentEventSource;
import de.thm.arsnova.service.comment.handler.VoteCommandHandler;
import de.thm.arsnova.service.comment.model.Vote;
import de.thm.arsnova.service.comment.model.command.Downvote;
import de.thm.arsnova.service.comment.model.command.ResetVote;
import de.thm.arsnova.service.comment.model.command.Upvote;
import de.thm.arsnova.service.comment.model.command.VotePayload;
import de.thm.arsnova.service.comment.service.VoteService;

@RunWith(MockitoJUnitRunner.class)
public class VoteCommandHandlerTest {
    @Mock
    private VoteService voteService;

    @Mock
    private CommentEventSource commentEventSource;

    private VoteCommandHandler commandHandler;

    @Before
    public void setup() {
        this.commandHandler = new VoteCommandHandler(voteService, commentEventSource);
    }

    @Test
    public void testHandleUpvote() {
        String commentId = "52f08e8314aba247c50faacef600254c";
        String creatorId = "52f08e8314aba247c50faacef600254c";
        VotePayload payload = new VotePayload(creatorId, commentId);
        Upvote command = new Upvote(payload);
        Vote expectedVote = new Vote();
        expectedVote.setCommentId(commentId);
        expectedVote.setUserId(creatorId);
        expectedVote.setVote(1);

        when(voteService.create(any())).thenReturn(expectedVote);

        Vote returned = commandHandler.handle(command);

        ArgumentCaptor<String> commentIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(commentEventSource, times(1)).ScoreChanged(commentIdCaptor.capture());
        assertThat(returned).isEqualTo(expectedVote);
        assertThat(commentIdCaptor.getValue()).isEqualTo(commentId);
    }

    @Test
    public void testHandleDownvote() {
        String commentId = "52f08e8314aba247c50faacef600254c";
        String creatorId = "52f08e8314aba247c50faacef600254c";
        VotePayload payload = new VotePayload(creatorId, commentId);
        Downvote command = new Downvote(payload);
        Vote expectedVote = new Vote();
        expectedVote.setCommentId(commentId);
        expectedVote.setUserId(creatorId);
        expectedVote.setVote(-1);

        when(voteService.create(any())).thenReturn(expectedVote);

        Vote returned = commandHandler.handle(command);

        ArgumentCaptor<String> commentIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(commentEventSource, times(1)).ScoreChanged(commentIdCaptor.capture());
        assertThat(returned).isEqualTo(expectedVote);
        assertThat(commentIdCaptor.getValue()).isEqualTo(commentId);
    }

    @Test
    public void testHandleResetVote() {
        String commentId = "52f08e8314aba247c50faacef600254c";
        String creatorId = "52f08e8314aba247c50faacef600254c";
        VotePayload payload = new VotePayload(creatorId, commentId);
        ResetVote command = new ResetVote(payload);
        Vote currentVote = new Vote();
        currentVote.setCommentId(commentId);
        currentVote.setUserId(creatorId);
        currentVote.setVote(-1);

        when(voteService.resetVote(commentId, creatorId)).thenReturn(currentVote);

        commandHandler.handle(command);

        ArgumentCaptor<String> commentIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(commentEventSource, times(1)).ScoreChanged(commentIdCaptor.capture());
        assertThat(commentIdCaptor.getValue()).isEqualTo(commentId);
    }

    @Test
    public void testHandleResetVoteWithoutCurrentVote() {
        String commentId = "52f08e8314aba247c50faacef600254c";
        String creatorId = "52f08e8314aba247c50faacef600254c";
        VotePayload payload = new VotePayload(creatorId, commentId);
        ResetVote command = new ResetVote(payload);

        when(voteService.resetVote(commentId, creatorId)).thenReturn(null);

        commandHandler.handle(command);

        ArgumentCaptor<String> commentIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(commentEventSource, never()).ScoreChanged(commentIdCaptor.capture());
    }
}
