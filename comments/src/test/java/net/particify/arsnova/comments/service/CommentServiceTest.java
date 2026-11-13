package net.particify.arsnova.comments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import net.particify.arsnova.comments.model.Comment;
import net.particify.arsnova.comments.model.Vote;
import net.particify.arsnova.comments.service.persistence.CommentRepository;
import net.particify.arsnova.comments.service.persistence.VoteRepository;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

  @Mock
  CommentRepository repository;

  @Mock
  VoteRepository voteRepository;

  @Mock
  VoteService voteService;

  @Mock
  JsonMapper jsonMapper;

  private CommentService service;

  @BeforeEach
  public void setup() {
    service = new CommentService(repository, voteRepository, voteService, jsonMapper);
  }

  @Test
  public void testShouldCreateComment() {
    UUID roomId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UUID creatorId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");

    Comment c = new Comment();
    c.setRoomId(roomId);
    c.setCreatorId(creatorId);

    Comment saved = service.create(c);

    verify(repository, times(1)).save(c);
    assertNotNull(saved.getId());
  }

  @Test
  public void testShouldGetCommentById() {
    UUID id = UUID.fromString("52f08e83-14ab-a247-c50f-aacef60025ff");
    UUID roomId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    UUID creatorId = UUID.fromString("52f08e83-14ab-a247-c50f-aacef600254c");
    Comment c = new Comment();
    c.setId(id);
    c.setRoomId(roomId);
    c.setCreatorId(creatorId);

    when(repository.findById(id)).thenReturn(Optional.of(c));

    Comment comment = service.getWithScore(id);

    assertEquals(roomId, comment.getRoomId());
    assertEquals(creatorId, comment.getCreatorId());
    assertNotNull(comment.getScore());
  }

  @Test
  public void testShouldDelete() {
    UUID id = UUID.fromString("52f08e83-14ab-a247-c50f-aacef60025ff");

    Vote v1 = new Vote();
    Vote v2 = new Vote();
    List<Vote> voteList = new ArrayList<>();
    voteList.add(v1);
    voteList.add(v2);

    when(voteRepository.findByCommentId(id)).thenReturn(voteList);

    service.delete(id);

    @SuppressWarnings("unchecked")
    Class<ArrayList<Vote>> listClass =
        (Class<ArrayList<Vote>>)(Class)ArrayList.class;
    ArgumentCaptor<List<Vote>> voteListCaptor = ArgumentCaptor.forClass(listClass);

    verify(voteRepository, times(1)).deleteAll(voteListCaptor.capture());
    assertThat(voteListCaptor.getValue()).isEqualTo(voteList);
  }

}
