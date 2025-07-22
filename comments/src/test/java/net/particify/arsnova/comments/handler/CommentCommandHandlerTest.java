package net.particify.arsnova.comments.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

import net.particify.arsnova.comments.config.RabbitConfig;
import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.Settings;
import net.particify.arsnova.comments.model.command.CreateComment;
import net.particify.arsnova.comments.model.command.CreateCommentPayload;
import net.particify.arsnova.comments.model.command.DeleteComment;
import net.particify.arsnova.comments.model.command.DeleteCommentPayload;
import net.particify.arsnova.comments.model.command.DeleteCommentsByRoom;
import net.particify.arsnova.comments.model.command.DeleteCommentsByRoomPayload;
import net.particify.arsnova.comments.model.command.HighlightComment;
import net.particify.arsnova.comments.model.command.HighlightCommentPayload;
import net.particify.arsnova.comments.model.event.CommentCreated;
import net.particify.arsnova.comments.model.event.CommentDeleted;
import net.particify.arsnova.comments.model.event.CommentDeletedPayload;
import net.particify.arsnova.comments.model.event.CommentHighlighted;
import net.particify.arsnova.comments.model.event.CommentHighlightedPayload;
import net.particify.arsnova.comments.security.PermissionEvaluator;
import net.particify.arsnova.comments.service.CommentService;
import net.particify.arsnova.comments.service.SettingsService;

@ExtendWith(MockitoExtension.class)
public class CommentCommandHandlerTest {

  @Mock
  private AmqpTemplate messagingTemplate;

  @Mock
  private CommentService commentService;

  @Mock
  private SettingsService settingsService;

  @Mock
  private PermissionEvaluator permissionEvaluator;

  private CommentCommandHandler commandHandler;

  @BeforeEach
  public void setup() {
    commandHandler = new CommentCommandHandler(
        messagingTemplate,
        commentService,
        settingsService,
        permissionEvaluator,
        false
    );
  }

  @Test
  public void testShouldHandleCreateComment() {

    // Arrange
    UUID roomId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UUID creatorId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    Comment newComment = new Comment();
    newComment.setCreatorId(creatorId);
    newComment.setRoomId(roomId);
    newComment.setBody("body");
    CreateCommentPayload payload = new CreateCommentPayload(newComment);
    CreateComment command = new CreateComment(payload);

    ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<CommentCreated> messageCaptor =
        ArgumentCaptor.forClass(CommentCreated.class);
    ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);

    Settings settings = new Settings();
    settings.setRoomId(roomId);
    settings.setDirectSend(true);
    settings.setReadonly(false);
    settings.setDisabled(false);

    when(settingsService.get(roomId)).thenReturn(settings);
    when(commentService.create(any(Comment.class))).thenReturn(newComment);
    when(permissionEvaluator.checkCommentOwnerPermission(any())).thenReturn(true);

    // Act
    commandHandler.handle(command);

    //Assert
    verify(commentService, times(1)).create(commentCaptor.capture());
    verify(messagingTemplate, times(1)).convertAndSend(keyCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
    assertThat(topicCaptor.getValue()).isEqualTo(
        roomId.toString().replace("-", "") + ".comment.stream");
  }

  @Test
  public void testShouldHandleCreateCommentForNonDirectSend() {

    // Arrange
    UUID roomId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UUID creatorId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    Comment newComment = new Comment();
    newComment.setCreatorId(creatorId);
    newComment.setRoomId(roomId);
    newComment.setBody("body");
    CreateCommentPayload payload = new CreateCommentPayload(newComment);
    CreateComment command = new CreateComment(payload);

    ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<CommentCreated> messageCaptor =
        ArgumentCaptor.forClass(CommentCreated.class);
    ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);

    Settings settings = new Settings();
    settings.setRoomId(roomId);
    settings.setDirectSend(false);
    settings.setReadonly(false);
    settings.setDisabled(false);

    when(settingsService.get(roomId)).thenReturn(settings);
    when(commentService.create(any(Comment.class))).thenReturn(newComment);
    when(permissionEvaluator.checkCommentOwnerPermission(any())).thenReturn(true);

    // Act
    commandHandler.handle(command);

    //Assert
    verify(commentService, times(1)).create(commentCaptor.capture());
    verify(messagingTemplate, times(1)).convertAndSend(keyCaptor.capture(), topicCaptor.capture(), messageCaptor.capture());
    assertThat(topicCaptor.getValue()).isEqualTo(
        roomId.toString().replace("-", "") + ".comment.moderator.stream");
  }

  @Test
  public void testShouldHandleDeleteComment() {
    UUID id = UUID.fromString("52f08e83-14ab-a247-c50f-aacef60025ff");
    UUID roomId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    Comment c = new Comment();
    c.setId(id);
    c.setRoomId(roomId);
    c.setAck(true);
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
    verify(messagingTemplate, times(2)).convertAndSend(
        keyCaptor.capture(),
        topicCaptor.capture(),
        eventCaptor.capture()
    );

    List<String> capturedTopics = topicCaptor.getAllValues();
    List<String> capturedKeys = keyCaptor.getAllValues();
    assertThat(capturedTopics.get(0)).isEqualTo(
        roomId.toString().replace("-", "") + ".comment.stream");
    assertThat(capturedKeys.get(1)).isEqualTo(RabbitConfig.COMMENT_SERVICE_COMMENT_DELETE_FANOUT_NAME);
    List<CommentDeleted> capturedEvents = eventCaptor.getAllValues();
    assertThat(capturedEvents.get(0)).isEqualTo(expectedEvent);
    assertThat(capturedEvents.get(1)).isEqualTo(expectedEvent);
  }

  @Test
  public void testShouldHandleHighlightComment() {
    UUID id = UUID.fromString("52f08e83-14ab-a247-c50f-aacef60025ff");
    UUID roomId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
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

    assertThat(topicCaptor.getValue()).isEqualTo(
        roomId.toString().replace("-", "") + ".comment.stream");
    assertThat(eventCaptor.getValue()).isEqualTo(expectedEvent);
  }

  @Test
  public void handleDeleteCommentsByRoom() {
    UUID roomId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UUID firstCommentId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef60025ff");
    UUID secondCommentId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef60025fe");
    DeleteCommentsByRoomPayload payload = new DeleteCommentsByRoomPayload(roomId);
    DeleteCommentsByRoom command = new DeleteCommentsByRoom(payload);
    List<Comment> commentList = new ArrayList<>();
    Comment one = new Comment();
    one.setId(firstCommentId);
    one.setRoomId(roomId);
    one.setAck(true);
    Comment two = new Comment();
    two.setId(secondCommentId);
    two.setRoomId(roomId);
    two.setAck(false);
    commentList.add(one);
    commentList.add(two);

    ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<CommentDeleted> eventCaptor =
        ArgumentCaptor.forClass(CommentDeleted.class);

    when(commentService.deleteByRoomId(roomId)).thenReturn(commentList);
    when(permissionEvaluator.isOwnerOrEditorForRoom(any())).thenReturn(true);

    commandHandler.handle(command);

    CommentDeletedPayload p1 = new CommentDeletedPayload();
    p1.setId(one.getId());
    CommentDeleted e1 = new CommentDeleted(p1, roomId);
    CommentDeletedPayload p2 = new CommentDeletedPayload();
    p2.setId(two.getId());
    CommentDeleted e2 = new CommentDeleted(p2, roomId);


    verify(messagingTemplate, times(4)).convertAndSend(
        keyCaptor.capture(),
        topicCaptor.capture(),
        eventCaptor.capture()
    );

    List<String> capturedTopics = topicCaptor.getAllValues();
    assertThat(capturedTopics.get(0)).isEqualTo(
        roomId.toString().replace("-", "") + ".comment.stream");
    assertThat(capturedTopics.get(2)).isEqualTo(
        roomId.toString().replace("-", "") + ".comment.moderator.stream");
    List<CommentDeleted> capturedEvents = eventCaptor.getAllValues();
    assertThat(capturedEvents.get(0)).isEqualTo(e1);
    assertThat(capturedEvents.get(2)).isEqualTo(e2);
  }

}
