package de.thm.arsnova.service.comment;

import de.thm.arsnova.service.comment.model.Comment;
import de.thm.arsnova.service.comment.model.message.*;
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
@RequestMapping("/comment")
public class CommentController {
    protected static final String ENTITY_ID_HEADER = "Arsnova-Entity-Id";
    protected static final String ENTITY_REVISION_HEADER = "Arsnova-Entity-Revision";
    protected static final String DEFAULT_ID_MAPPING = "/{id}";
    protected static final String PATCH_MAPPING = DEFAULT_ID_MAPPING;
    protected static final String GET_MAPPING = "/{id}";
    protected static final String POST_MAPPING = "/";
    protected static final String PUT_MAPPING = DEFAULT_ID_MAPPING;
    protected static final String REQUEST_MAPPING = "/comment";
    protected static final String FIND_MAPPING = "/find";

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
    public Comment getComment(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public Comment post(
            @RequestBody final Comment comment,
            final HttpServletResponse httpServletResponse
    ) {
        // convert into command for the command handler
        final CreateCommentPayload payload = new CreateCommentPayload(comment);
        final CreateComment command = new CreateComment();
        command.setPayload(payload);

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
    public List<Comment> find(@RequestBody final FindQuery findQuery) {
        Set<String> ids = findQueryService.resolveQuery(findQuery);

        return service.get(new ArrayList<String>(ids));
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
}
