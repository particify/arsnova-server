package net.particify.arsnova.core.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import net.particify.arsnova.core.model.AnswerStatisticsUserSummary;
import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.service.AnswerService;
import net.particify.arsnova.core.service.ContentGroupService;

@RestController
@EntityRequestMapping(ContentGroupController.REQUEST_MAPPING)
public class ContentGroupController extends AbstractEntityController<ContentGroup> {
  protected static final String REQUEST_MAPPING = "/contentgroup";
  private static final String ADD_CONTENT_MAPPING = "/-/content/";
  private static final String REMOVE_CONTENT_MAPPING = DEFAULT_ID_MAPPING + "/content/{contentId}";
  private static final String IMPORT_MAPPING = DEFAULT_ID_MAPPING + "/import";
  private static final String ANSWER_STATISTICS_USER_SUMMARY_MAPPING = DEFAULT_ID_MAPPING + "/stats/user/{userId}";

  private ContentGroupService contentGroupService;
  private AnswerService answerService;

  public ContentGroupController(
      @Qualifier("securedContentGroupService") final ContentGroupService contentGroupService,
      @Qualifier("securedAnswerService") final AnswerService answerService) {
    super(contentGroupService);
    this.contentGroupService = contentGroupService;
    this.answerService = answerService;
  }

  @Override
  protected String getMapping() {
    return REQUEST_MAPPING;
  }

  @Override
  public ContentGroup post(
      @RequestBody final ContentGroup entity,
      final HttpServletResponse httpServletResponse) {
    contentGroupService.createOrUpdateContentGroup(entity);
    final String uri = UriComponentsBuilder.fromPath(getMapping()).path(GET_MAPPING)
        .buildAndExpand(entity.getRoomId(), entity.getName()).toUriString();
    httpServletResponse.setHeader(HttpHeaders.LOCATION, uri);
    httpServletResponse.setHeader(ENTITY_ID_HEADER, entity.getId());
    httpServletResponse.setHeader(ENTITY_REVISION_HEADER, entity.getRevision());

    return entity;
  }

  @Override
  public ContentGroup put(
      @RequestBody final ContentGroup contentGroup,
      final HttpServletResponse httpServletResponse) {
    final ContentGroup updatedContentGroup = contentGroupService.createOrUpdateContentGroup(contentGroup);

    if (updatedContentGroup.getId() != null) {
      final String uri = UriComponentsBuilder.fromPath(getMapping()).path(GET_MAPPING)
          .buildAndExpand(contentGroup.getRoomId(), contentGroup.getName()).toUriString();
      httpServletResponse.setHeader(HttpHeaders.LOCATION, uri);
      httpServletResponse.setHeader(ENTITY_ID_HEADER, contentGroup.getId());
      httpServletResponse.setHeader(ENTITY_REVISION_HEADER, contentGroup.getRevision());
    }

    return updatedContentGroup;
  }

  @PostMapping(ADD_CONTENT_MAPPING)
  public void addContentToGroup(@RequestBody final AddContentToGroupRequestEntity addContentToGroupRequestEntity) {
    contentGroupService.addContentToGroup(
        addContentToGroupRequestEntity.getRoomId(),
        addContentToGroupRequestEntity.getContentGroupName(),
        addContentToGroupRequestEntity.getContentId());
  }

  @DeleteMapping(REMOVE_CONTENT_MAPPING)
  public void removeContentFromGroup(@PathVariable final String id, @PathVariable final String contentId) {
    contentGroupService.removeContentFromGroup(id, contentId);
  }

  @PostMapping(IMPORT_MAPPING)
  public void importFromFile(@PathVariable final String id, @RequestParam final MultipartFile file)
      throws IOException {
    final ContentGroup contentGroup = get(id);
    contentGroupService.importFromCsv(file.getBytes(), contentGroup);
  }

  @GetMapping(ANSWER_STATISTICS_USER_SUMMARY_MAPPING)
  public AnswerStatisticsUserSummary getAnswerStatisticsUserSummary(
      @PathVariable final String id, @PathVariable final String userId) {
    return answerService.getStatisticsByUserIdAndContentIds(
        userId, contentGroupService.get(id).getContentIds());
  }

  static class AddContentToGroupRequestEntity {
    private String roomId;
    private String contentGroupName;
    private String contentId;

    public String getRoomId() {
      return roomId;
    }

    @JsonView(View.Public.class)
    public void setRoomId(final String roomId) {
      this.roomId = roomId;
    }

    public String getContentGroupName() {
      return contentGroupName;
    }

    @JsonView(View.Public.class)
    public void setContentGroupName(final String contentGroupName) {
      this.contentGroupName = contentGroupName;
    }

    public String getContentId() {
      return contentId;
    }

    @JsonView(View.Public.class)
    public void setContentId(final String contentId) {
      this.contentId = contentId;
    }
  }
}
