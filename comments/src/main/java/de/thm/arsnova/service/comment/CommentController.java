package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.command.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController("CommentController")
@RequestMapping(CommentController.REQUEST_MAPPING)
public class CommentController extends AbstractEntityController {
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    protected static final String REQUEST_MAPPING = "/comment";
    private static final String BULK_DELETE_MAPPING = POST_MAPPING + "bulkdelete";
    private static final String DELETE_BY_ROOM_MAPPING = POST_MAPPING + "byRoom";

    private final CommentCommandHandler commandHandler;
    private final CommentService service;
    private final CommentFindQueryService findQueryService;

    @Autowired
    public CommentController(
            CommentCommandHandler commandHandler,
            CommentService service,
            CommentFindQueryService findQueryService
    ) {
        this.commandHandler = commandHandler;
        this.service = service;
        this.findQueryService = findQueryService;
    }

    @GetMapping(GET_MAPPING)
    public Comment get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping(POST_MAPPING)
    @ResponseStatus(HttpStatus.CREATED)
    public Comment post(
            @RequestBody final Comment comment,
            final HttpServletResponse httpServletResponse
    ) {
        // convert into command for the command handler
        final CreateCommentPayload payload = new CreateCommentPayload(comment);
        final CreateComment command = new CreateComment(payload);

        Comment c = commandHandler.handle(command);

        final String uri = UriComponentsBuilder.fromPath(REQUEST_MAPPING).path(GET_MAPPING)
                .buildAndExpand(c.getId()).toUriString();
        httpServletResponse.setHeader(HttpHeaders.LOCATION, uri);
        httpServletResponse.setHeader(ENTITY_ID_HEADER, c.getId());

        return c;
    }

    @PutMapping(PUT_MAPPING)
    public Comment put(@RequestBody final Comment entity, final HttpServletResponse httpServletResponse) {
        UpdateCommentPayload p = new UpdateCommentPayload(entity);
        UpdateComment command = new UpdateComment(p);

        Comment c = this.commandHandler.handle(command);

        httpServletResponse.setHeader(ENTITY_ID_HEADER, c.getId());

        return c;
    }

    @PostMapping(FIND_MAPPING)
    public List<Comment> find(@RequestBody final FindQuery<Comment> findQuery) {
        logger.debug("Resolving find query: {}", findQuery);

        Set<String> ids = findQueryService.resolveQuery(findQuery);

        logger.debug("Resolved find query to IDs: {}", ids);

        return service.get(new ArrayList<>(ids));
    }

    @PatchMapping(PATCH_MAPPING)
    public Comment patch(@PathVariable final String id, @RequestBody final Map<String, Object> changes,
                   final HttpServletResponse httpServletResponse) throws IOException {
        PatchCommentPayload p = new PatchCommentPayload(id, changes);
        PatchComment command = new PatchComment(p);

        Comment c = this.commandHandler.handle(command);

        httpServletResponse.setHeader(ENTITY_ID_HEADER, c.getId());

        return c;
    }

    @DeleteMapping(DELETE_MAPPING)
    public void delete(@PathVariable String id) {
        DeleteCommentPayload p = new DeleteCommentPayload(id);
        DeleteComment command = new DeleteComment(p);

        commandHandler.handle(command);
    }

    @PostMapping(BULK_DELETE_MAPPING)
    public void bulkDelete(
            @RequestBody final String[] ids
    ) {
        for (String id : ids) {
            DeleteCommentPayload p = new DeleteCommentPayload(id);
            DeleteComment command = new DeleteComment(p);

            commandHandler.handle(command);
        }
    }

    @DeleteMapping(DELETE_BY_ROOM_MAPPING)
    public void deleteByRoom(
            @RequestParam final String roomId
    ) {
        DeleteCommentsByRoomPayload p = new DeleteCommentsByRoomPayload(roomId);
        DeleteCommentsByRoom command = new DeleteCommentsByRoom(p);

        commandHandler.handle(command);
    }

}
