package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Comment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommentServiceTest {

    @Mock
    CommentRepository repository;

    @Mock
    VoteRepository voteRepository;

    @Mock
    MappingJackson2MessageConverter converter;

    private CommentService service;

    @Before
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

}
