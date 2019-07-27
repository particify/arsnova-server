package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.handler.CommentCommandHandler;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.command.DeleteComment;
import de.thm.arsnova.service.comment.model.command.DeleteCommentPayload;
import de.thm.arsnova.service.comment.model.event.CommentDeleted;
import de.thm.arsnova.service.comment.model.event.CommentDeletedPayload;
import de.thm.arsnova.service.comment.service.CommentService;
import de.thm.arsnova.service.comment.service.SettingsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import de.thm.arsnova.service.comment.model.command.CreateComment;
import de.thm.arsnova.service.comment.model.command.CreateCommentPayload;
import org.springframework.amqp.core.AmqpTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CommentCommandHandlerTest {

    @Mock
    private AmqpTemplate messagingTemplate;

    @Mock
    private CommentService commentService;

    @Mock
    private SettingsService settingsService;

    private CommentCommandHandler commandHandler;

    @Before
    public void setup() {
        commandHandler = new CommentCommandHandler(messagingTemplate, commentService, settingsService);
    }

    /*
    @Test
    public void testShouldHandleCreateComment() {

        // Arrange
        String roomId = "52f08e8314aba247c50faacef600254c";
        String creatorId = "52f08e8314aba247c50faacef600254c";
        Comment newComment = new Comment();
        newComment.setCreatorId(creatorId);
        newComment.setRoomId(roomId);
        newComment.setBody("body");
        CreateCommentPayload payload = new CreateCommentPayload(newComment);
        CreateComment command = new CreateComment(payload);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CreateComment> messageCaptor =
                ArgumentCaptor.forClass(CreateComment.class);
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);

        // Act
        commandHandler.handle(command);

        //Assert
        verify(commentService, times(1)).create(commentCaptor.capture());
        verify(messagingTemplate, times(1)).convertAndSend(topicCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(roomId + ".comment.stream");
    }
    */

    @Test
    public void testShouldHandleDeleteComment() {
        String id = "52f08e8314aba247c50faacef60025ff";
        String roomId = "52f08e8314aba247c50faacef600254c";
        Comment c = new Comment();
        c.setId(id);
        c.setRoomId(roomId);
        when(commentService.get(id)).thenReturn(c);
        DeleteCommentPayload payload = new DeleteCommentPayload(id);
        DeleteComment command = new DeleteComment(payload);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CommentDeleted> eventCaptor =
                ArgumentCaptor.forClass(CommentDeleted.class);

        commandHandler.handle(command);

        CommentDeletedPayload p = new CommentDeletedPayload();
        p.setId(id);
        CommentDeleted expectedEvent = new CommentDeleted(p, roomId);

        verify(commentService, times(1)).get(id);
        verify(commentService, times(1)).delete(id);
        verify(messagingTemplate, times(1)).convertAndSend(
                topicCaptor.capture(),
                keyCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(keyCaptor.getValue()).isEqualTo(roomId + ".comment.stream");
        assertThat(eventCaptor.getValue()).isEqualTo(expectedEvent);
    }

}
