package de.thm.arsnova.service.comment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import de.thm.arsnova.service.comment.model.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

@Service
public class CommentService {
    final CommentRepository repository;
    private ObjectMapper objectMapper;

    @Autowired
    public CommentService(CommentRepository repository) {
        this.repository = repository;
    }

    public Comment get(String id) {
        // ToDo: error handling
        return repository.findById(id).orElse(new Comment());
    }

    public List<Comment> get(List<String> ids) {
        Iterable<Comment> it = repository.findAllById(ids);
        List<Comment> list = new ArrayList<Comment>();
        it.forEach(list::add);

        return list;
    }

    public Comment create(Comment c) {
        String newId = UUID.randomUUID().toString().replace("-", "");
        c.setId(newId);
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

    public List<Comment> getByRoomId(String roomId) {
        return repository.findByRoomId(roomId);
    }
}
