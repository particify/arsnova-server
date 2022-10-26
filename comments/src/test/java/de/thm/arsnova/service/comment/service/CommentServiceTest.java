package de.thm.arsnova.service.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.Vote;
import de.thm.arsnova.service.comment.service.persistence.CommentRepository;
import de.thm.arsnova.service.comment.service.persistence.VoteRepository;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    CommentRepository repository;

    @Mock
    VoteRepository voteRepository;

    @Mock
    MappingJackson2MessageConverter converter;

    private CommentService service;

    @BeforeEach
    public void setup() {
        service = new CommentService(repository, voteRepository, converter);
    }

    @Test
    public void testShouldCreateComment() {
        String roomId = "52f08e8314aba247c50faacef600254c";
        String creatorId = "52f08e8314aba247c50faacef600254c";

        Comment c = new Comment();
        c.setRoomId(roomId);
        c.setCreatorId(creatorId);

        Comment saved = service.create(c);

        verify(repository, times(1)).save(c);
        assertNotNull(saved.getId());
    }

    @Test
    public void testShouldGetCommentById() {
        String id = "52f08e8314aba247c50faacef60025ff";
        String roomId = "52f08e8314aba247c50faacef600254c";
        String creatorId = "52f08e8314aba247c50faacef600254c";
        Comment c = new Comment();
        c.setId(id);
        c.setRoomId(roomId);
        c.setCreatorId(creatorId);

        when(repository.findById(id)).thenReturn(Optional.of(c));

        Comment comment = service.get(id);

        assertEquals(roomId, comment.getRoomId());
        assertEquals(creatorId, comment.getCreatorId());
        assertNotNull(comment.getScore());
    }

    @Test
    public void testShouldDelete() {
        String id = "52f08e8314aba247c50faacef60025ff";

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
