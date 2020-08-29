package de.thm.arsnova.service.comment.handler;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.Settings;
import de.thm.arsnova.service.comment.model.command.DeleteComment;
import de.thm.arsnova.service.comment.model.command.DeleteCommentPayload;
import de.thm.arsnova.service.comment.model.command.DeleteCommentsByRoom;
import de.thm.arsnova.service.comment.model.command.DeleteCommentsByRoomPayload;
import de.thm.arsnova.service.comment.model.command.HighlightComment;
import de.thm.arsnova.service.comment.model.command.HighlightCommentPayload;
import de.thm.arsnova.service.comment.model.event.CommentDeleted;
import de.thm.arsnova.service.comment.model.event.CommentDeletedPayload;
import de.thm.arsnova.service.comment.model.event.CommentHighlighted;
import de.thm.arsnova.service.comment.model.event.CommentHighlightedPayload;
import de.thm.arsnova.service.comment.security.PermissionEvaluator;
import de.thm.arsnova.service.comment.service.BonusTokenService;
import de.thm.arsnova.service.comment.service.CommentService;
import de.thm.arsnova.service.comment.service.SettingsService;

import java.util.ArrayList;
import java.util.List;
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
    private BonusTokenService bonusTokenService;

    @Mock
    private SettingsService settingsService;

    @Mock
    private PermissionEvaluator permissionEvaluator;

    private CommentCommandHandler commandHandler;

    @Before
    public void setup() {
        commandHandler = new CommentCommandHandler(
                messagingTemplate,
                commentService,
                bonusTokenService,
                settingsService,
                permissionEvaluator
        );
    }

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

        Settings settings = new Settings();
        settings.setRoomId(roomId);
        settings.setDirectSend(true);

        when(settingsService.get(roomId)).thenReturn(settings);
        when(commentService.create(any())).thenReturn(newComment);
        when(permissionEvaluator.checkCommentOwnerPermission(any())).thenReturn(true);

        // Act
        commandHandler.handle(command);

        //Assert
        verify(commentService, times(1)).create(commentCaptor.capture());
        verify(messagingTemplate, times(1)).convertAndSend(keyCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(roomId + ".comment.stream");
    }

    @Test
    public void testShouldHandleCreateCommentForNonDirectSend() {

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

        Settings settings = new Settings();
        settings.setRoomId(roomId);
        settings.setDirectSend(false);

        when(settingsService.get(roomId)).thenReturn(settings);
        when(commentService.create(any())).thenReturn(newComment);
        when(permissionEvaluator.checkCommentOwnerPermission(any())).thenReturn(true);

        // Act
        commandHandler.handle(command);

        //Assert
        verify(commentService, times(1)).create(commentCaptor.capture());
        verify(messagingTemplate, times(1)).convertAndSend(keyCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(roomId + ".comment.moderator.stream");
    }

    @Test
    public void testShouldHandleDeleteComment() {
        String id = "52f08e8314aba247c50faacef60025ff";
        String roomId = "52f08e8314aba247c50faacef600254c";
        Comment c = new Comment();
        c.setId(id);
        c.setRoomId(roomId);
        when(commentService.get(id)).thenReturn(c);
        when(permissionEvaluator.checkCommentDeletePermission(any())).thenReturn(true);
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
                keyCaptor.capture(),
                topicCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo(roomId + ".comment.stream");
        assertThat(eventCaptor.getValue()).isEqualTo(expectedEvent);
    }

    @Test
    public void testShouldHandleHighlightComment() {
        String id = "52f08e8314aba247c50faacef60025ff";
        String roomId = "52f08e8314aba247c50faacef600254c";
        Comment c = new Comment();
        c.setId(id);
        c.setRoomId(roomId);
        HighlightCommentPayload payload = new HighlightCommentPayload();
        payload.setLights(true);
        payload.setId(id);
        HighlightComment command = new HighlightComment(payload);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CommentHighlighted> eventCaptor =
                ArgumentCaptor.forClass(CommentHighlighted.class);

        when(commentService.get(id)).thenReturn(c);
        when(permissionEvaluator.isOwnerOrAnyTypeOfModeratorForRoom(any())).thenReturn(true);

        commandHandler.handle(command);

        CommentHighlightedPayload p = new CommentHighlightedPayload(c, true);
        CommentHighlighted expectedEvent = new CommentHighlighted(p, roomId);

        verify(messagingTemplate, times(1)).convertAndSend(
                keyCaptor.capture(),
                topicCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(topicCaptor.getValue()).isEqualTo(roomId + ".comment.stream");
        assertThat(eventCaptor.getValue()).isEqualTo(expectedEvent);
    }

    @Test
    public void handleDeleteCommentsByRoom() {
        String roomId = "52f08e8314aba247c50faacef600254c";
        String firstCommentId = "52f08e8314aba247c50faacef60025ff";
        String secondCommentId = "52f08e8314aba247c50faacef60025fe";
        DeleteCommentsByRoomPayload payload = new DeleteCommentsByRoomPayload(roomId);
        DeleteCommentsByRoom command = new DeleteCommentsByRoom(payload);
        List<Comment> commentList = new ArrayList<>();
        Comment one = new Comment();
        one.setId(firstCommentId);
        one.setRoomId(roomId);
        Comment two = new Comment();
        two.setId(secondCommentId);
        two.setRoomId(roomId);
        commentList.add(one);
        commentList.add(two);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CommentDeleted> eventCaptor =
                ArgumentCaptor.forClass(CommentDeleted.class);

        when(commentService.deleteByRoomId(roomId)).thenReturn(commentList);
        when(permissionEvaluator.isOwnerOrEditingModeratorForRoom(any())).thenReturn(true);

        commandHandler.handle(command);

        CommentDeletedPayload p1 = new CommentDeletedPayload();
        p1.setId(one.getId());
        CommentDeleted e1 = new CommentDeleted(p1, roomId);
        CommentDeletedPayload p2 = new CommentDeletedPayload();
        p2.setId(two.getId());
        CommentDeleted e2 = new CommentDeleted(p2, roomId);


        verify(messagingTemplate, times(2)).convertAndSend(
                keyCaptor.capture(),
                topicCaptor.capture(),
                eventCaptor.capture()
        );

        List<String> capturedTopics = topicCaptor.getAllValues();
        assertThat(capturedTopics.get(0)).isEqualTo(roomId + ".comment.stream");
        assertThat(capturedTopics.get(1)).isEqualTo(roomId + ".comment.stream");
        List<CommentDeleted> capturedEvents = eventCaptor.getAllValues();
        assertThat(capturedEvents.get(0)).isEqualTo(e1);
        assertThat(capturedEvents.get(1)).isEqualTo(e2);
    }

}
