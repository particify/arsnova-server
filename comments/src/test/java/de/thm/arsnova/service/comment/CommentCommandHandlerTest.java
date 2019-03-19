package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Comment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import de.thm.arsnova.service.comment.model.message.CreateComment;
import de.thm.arsnova.service.comment.model.message.CreateCommentPayload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CommentCommandHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private CommentService commentService;

    private CommentCommandHandler commandHandler;

    @Before
    public void setup() {
        commandHandler = new CommentCommandHandler(messagingTemplate, commentService);
    }

    @Test
    public void testShouldHandleCreateComment() {

        // Arrange
        String roomId = "52f08e8314aba247c50faacef600254c";
        String creatorId = "52f08e8314aba247c50faacef600254c";
        Comment newComment = new Comment();
        newComment.setCreatorId(creatorId);
        newComment.setRoomId(roomId);
        newComment.setSubject("subject");
        newComment.setBody("body");
        CreateCommentPayload payload = new CreateCommentPayload(newComment);
        CreateComment command = new CreateComment();
        command.setPayload(payload);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CreateComment> messageCaptor =
                ArgumentCaptor.forClass(CreateComment.class);
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);

        // Act
        commandHandler.handle(command);

        //Assert
        verify(commentService, times(1)).create(commentCaptor.capture());
        verify(messagingTemplate, times(1)).convertAndSend(topicCaptor.capture(), messageCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo("/queue/" + roomId + ".comment.stream");
    }

}