package de.thm.arsnova.service.comment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import de.thm.arsnova.service.comment.message.CreateComment;
import de.thm.arsnova.service.comment.message.CreateCommentPayload;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CommentCommandHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private CommentCommandHandler commandHandler;

    @Before
    public void setup() {
        commandHandler = new CommentCommandHandler(messagingTemplate);
    }

    @Test
    public void testShouldHandleCreateComment() {

        // Arrange
        String roomId = "52f08e8314aba247c50faacef600254c";
        String creatorId = "52f08e8314aba247c50faacef600254c";
        CreateCommentPayload payload = new CreateCommentPayload();
        payload.setCreatorId(creatorId);
        payload.setRoomId(roomId);
        payload.setSubject("subject");
        payload.setBody("body");
        CreateComment command = new CreateComment();
        command.setPayload(payload);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CreateComment> messageCaptor =
                ArgumentCaptor.forClass(CreateComment.class);

        // Act
        commandHandler.handle(command);

        //Assert
        verify(messagingTemplate, times(1)).convertAndSend(topicCaptor.capture(), messageCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo("/queue/" + roomId + ".comment.stream");
    }

}