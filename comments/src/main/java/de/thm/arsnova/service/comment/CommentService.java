package de.thm.arsnova.service.comment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

@Service
public class CommentService {
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    final CommentRepository repository;
    final VoteRepository voteRepository;
    private ObjectMapper objectMapper;

    @Autowired
    public CommentService(
            CommentRepository repository,
            VoteRepository voteRepository,
            MappingJackson2MessageConverter jackson2Converter) {
        this.repository = repository;
        this.voteRepository = voteRepository;
        this.objectMapper = jackson2Converter.getObjectMapper();
    }

    public Comment get(String id) {
        // ToDo: error handling
        Comment c = repository.findById(id).orElse(new Comment());
        List<Vote> voteList = voteRepository.findByCommentId(c.getId());
        int voteSum = 0;
        for (Vote v : voteList) {
            voteSum = voteSum + v.getVote();
        }
        c.setScore(voteSum);

        return c;
    }

    public List<Comment> get(List<String> ids) {
        List<Comment> list = new ArrayList<Comment>();
        ids.forEach((c) -> list.add(get(c)));

        return list;
    }

    public Comment create(Comment c) {
        String newId = UUID.randomUUID().toString().replace("-", "");
        c.setId(newId);
        logger.trace("Creating new comment: " + c.toString());
        repository.save(c);

        return c;
    }

    public Comment patch(final Comment entity, final Map<String, Object> changes) throws IOException {
        return patch(entity, changes, Function.identity());
    }

    public Comment patch(final Comment entity, final Map<String, Object> changes,
                   final Function<Comment, ? extends Object> propertyGetter) throws IOException {
        Object obj = propertyGetter.apply(entity);
        ObjectReader reader = objectMapper.readerForUpdating(obj);
        JsonNode tree = objectMapper.valueToTree(changes);
        reader.readValue(tree);
        final Comment patchedEntity = repository.save(entity);

        return patchedEntity;
    }

    public Iterable<Comment> patch(final Iterable<Comment> entities, final Map<String, Object> changes) throws IOException {
        return patch(entities, changes, Function.identity());
    }

    public Iterable<Comment> patch(final Iterable<Comment> entities, final Map<String, Object> changes,
                             final Function<Comment, ? extends Object> propertyGetter) throws IOException {
        final JsonNode tree = objectMapper.valueToTree(changes);
        for (Comment entity : entities) {
            Object obj = propertyGetter.apply(entity);
            ObjectReader reader = objectMapper.readerForUpdating(obj);
            reader.readValue(tree);
        }

        Iterable<Comment> patchedEntities = repository.saveAll(entities);

        return patchedEntities;
    }

    public Comment update(final Comment c) {
        final Comment updatedEntity = repository.save(c);

        return updatedEntity;
    }

    public List<Comment> getByRoomId(String roomId) {
        return repository.findByRoomId(roomId);
    }

    public void delete(String id) {
        List<Vote> voteList = voteRepository.findByCommentId(id);
        voteRepository.deleteAll(voteList);
        repository.deleteById(id);
    }

    public List<Comment> deleteByRoomId(String roomId) {
        return repository.deleteByRoomId(roomId);
    }

}
