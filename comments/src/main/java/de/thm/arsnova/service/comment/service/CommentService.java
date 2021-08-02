package de.thm.arsnova.service.comment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import de.thm.arsnova.service.comment.exception.BadRequestException;
import de.thm.arsnova.service.comment.service.persistence.CommentRepository;
import de.thm.arsnova.service.comment.service.persistence.VoteRepository;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.Vote;

import java.util.stream.Collectors;
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
    private static final String NIL_UUID = "00000000-0000-0000-0000-000000000000";
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    final CommentRepository repository;
    final VoteRepository voteRepository;
    final VoteService voteService;
    private ObjectMapper objectMapper;

    @Autowired
    public CommentService(
            CommentRepository repository,
            VoteRepository voteRepository,
            VoteService voteService,
            MappingJackson2MessageConverter jackson2Converter) {
        this.repository = repository;
        this.voteRepository = voteRepository;
        this.voteService = voteService;
        this.objectMapper = jackson2Converter.getObjectMapper();
    }

    public Comment get(String id) {
        // ToDo: error handling
        Comment c = repository.findById(id).orElse(new Comment());
        c.setScore(voteService.getSumByCommentId(id));

        return c;
    }

    public List<Comment> get(final List<String> ids) {
        final List<Comment> list = new ArrayList<>();
        final Map<String, Integer> voteSums = voteService.getSumsByCommentIds(ids);
        repository.findAllById(ids).forEach(c -> {
            c.setScore(voteSums.getOrDefault(c.getId(), 0));
            list.add(c);
        });

        return list;
    }

    public Comment create(Comment c) {
        c.setId(generateUuidStringForDb());
        logger.trace("Creating new comment: " + c.toString());
        repository.save(c);

        return c;
    }

    public Iterable<Comment> create(final Iterable<Comment> comments) {
        for (final Comment comment : comments) {
            comment.setId(generateUuidStringForDb());
        }
        return repository.saveAll(comments);
    }

    public Comment patch(final Comment entity, final Map<String, Object> changes) throws IOException {
        return patch(entity, changes, Function.identity());
    }

    public Comment patch(final Comment entity, final Map<String, Object> changes,
                   final Function<Comment, ? extends Object> propertyGetter) throws IOException {
        // Archived comments should not be changed anymore
        if (entity.getArchiveId() != null) {
            logger.debug("Tried changing an archived comment.");
            throw new BadRequestException("Tried changing an archived comment.");
        }
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
            // Archived comments should not be changed anymore
            if (entity.getArchiveId() != null) {
                logger.debug("Tried changing an archived comment.");
                throw new BadRequestException("Tried changing an archived comment.");
            }
            Object obj = propertyGetter.apply(entity);
            ObjectReader reader = objectMapper.readerForUpdating(obj);
            reader.readValue(tree);
        }

        Iterable<Comment> patchedEntities = repository.saveAll(entities);

        return patchedEntities;
    }

    public Comment update(final Comment c) {
        final Optional<Comment> entity = repository.findById(c.getId());
        // Archived comments should not be changed anymore
        if (entity.isPresent() && entity.get().getArchiveId() != null) {
            logger.debug("Tried changing an archived comment.");
            throw new BadRequestException("Tried changing an archived comment.");
        }
        final Comment updatedEntity = repository.save(c);

        return updatedEntity;
    }

    public List<Comment> getByRoomIdAndArchiveIdNull(String roomId) {
        return repository.findByRoomIdAndArchiveIdNull(roomId);
    }

    public long countByRoomIdAndAck(String roomId, Boolean ack) {
        return repository.countByRoomIdAndAckAndArchiveIdNull(roomId, ack);
    }

    public void delete(String id) {
        List<Vote> voteList = voteRepository.findByCommentId(id);
        voteRepository.deleteAll(voteList);
        repository.deleteById(id);
    }

    public List<Comment> deleteByRoomId(String roomId) {
        return repository.deleteByRoomId(roomId);
    }

    public Map<String, Comment> duplicateComments(final String originalRoomId, final String duplicatedRoomId) {
        final Map<String, Comment> commentMapping = new HashMap<>();
        final List<Comment> comments = getByRoomIdAndArchiveIdNull(originalRoomId);
        final List<Comment> commentCopies = comments.stream().map(c -> {
            final Comment commentCopy = new Comment(c);
            commentMapping.put(c.getId(), commentCopy);
            commentCopy.setCreatorId(NIL_UUID);
            commentCopy.setRoomId(duplicatedRoomId);
            return commentCopy;
        }).collect(Collectors.toList());
        create(commentCopies);

        return commentMapping;
    }

    private String generateUuidStringForDb() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
