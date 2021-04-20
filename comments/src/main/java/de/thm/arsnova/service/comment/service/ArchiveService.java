package de.thm.arsnova.service.comment.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.thm.arsnova.service.comment.exception.BadRequestException;
import de.thm.arsnova.service.comment.exception.ForbiddenException;
import de.thm.arsnova.service.comment.model.Archive;
import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.command.CreateArchiveCommand;
import de.thm.arsnova.service.comment.security.PermissionEvaluator;
import de.thm.arsnova.service.comment.service.persistence.ArchiveRepository;
import de.thm.arsnova.service.comment.service.persistence.CommentRepository;

@Service
public class ArchiveService {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveService.class);

    private final ArchiveRepository repository;
    private final CommentRepository commentRepository;
    private final PermissionEvaluator permissionEvaluator;

    @Autowired
    public ArchiveService(
            ArchiveRepository repository,
            CommentRepository commentRepository,
            PermissionEvaluator permissionEvaluator
    ) {
        this.repository = repository;
        this.commentRepository = commentRepository;
        this.permissionEvaluator = permissionEvaluator;
    }

    public Optional<Archive> get(final String id) {
        final Optional<Archive> archive = repository.findById(id);
        if (archive.isPresent()) {
            if (!permissionEvaluator.isOwnerOrEditingModeratorForRoom(archive.get().getRoomId())) {
                throw new ForbiddenException();
            }
        }

        final Optional<Archive> enrichedArchive = archive.map(a -> {
            final List<Comment> comments = commentRepository.findByArchiveId(a.getId());
            a.setComments(new HashSet<>(comments));
            a.setCount(comments.size());
            return a;
        });
        return enrichedArchive;
    }

    public Optional<Archive> getByName(final String name) {
        final List<Archive> list = repository.findByName(name);
        if (list.isEmpty()) {
            return Optional.empty();
        } else {
            final Archive archive = list.iterator().next();
            if (!permissionEvaluator.isOwnerOrEditingModeratorForRoom(archive.getRoomId())) {
                throw new ForbiddenException();
            }
            archive.setCount(commentRepository.countByArchiveId(archive.getId()));

            return Optional.of(archive);
        }
    }

    public List<Archive> getByRoomId(final String roomId) {
        if (!permissionEvaluator.isOwnerOrEditingModeratorForRoom(roomId)) {
            throw new ForbiddenException();
        }
        List<Archive> archives = repository.findByRoomId(roomId);
        for (Archive archive : archives) {
            archive.setCount(commentRepository.countByArchiveId(archive.getId()));
        }
        return archives;
    }

    public Archive create(final CreateArchiveCommand cmd) {
        if (!permissionEvaluator.isOwnerOrEditingModeratorForRoom(cmd.getRoomId())) {
            throw new ForbiddenException();
        }

        Set<Comment> comments;

        if (cmd.getCommentIds() != null && !cmd.getCommentIds().isEmpty()) {
            comments = StreamSupport.stream(
                    commentRepository.findByIdInAndRoomIdAndArchiveIdNull(cmd.getCommentIds(), cmd.getRoomId()).spliterator(), false
            ).collect(Collectors.toSet());

            if (cmd.getCommentIds().size() != comments.size()) {
                throw new BadRequestException("Could not find comments for all given ids");
            }
        } else {
            comments = StreamSupport.stream(
                    commentRepository.findByRoomIdAndArchiveIdNull(cmd.getRoomId()).spliterator(), false
            ).collect(Collectors.toSet());
        }

        String newId = UUID.randomUUID().toString().replace("-", "");
        final Archive archive = new Archive();
        archive.setId(newId);
        archive.setRoomId(cmd.getRoomId());
        archive.setName(cmd.getName());
        archive.setComments(comments);
        final Archive savedArchive = repository.save(archive);

        final Set<Comment> updatedComments = comments.stream().map(
                comment -> {
                    comment.setArchiveId(savedArchive.getId());
                    return comment;
                }
        ).collect(Collectors.toSet());

        commentRepository.saveAll(updatedComments);
        savedArchive.setComments(updatedComments);

        return savedArchive;
    }

    public void delete(final String id) {
        final Optional<Archive> archive = repository.findById(id);
        if (archive.isPresent()) {
            if (!permissionEvaluator.isOwnerOrEditingModeratorForRoom(archive.get().getRoomId())) {
                throw new ForbiddenException();
            }
            repository.delete(archive.get());
        }
    }
}
