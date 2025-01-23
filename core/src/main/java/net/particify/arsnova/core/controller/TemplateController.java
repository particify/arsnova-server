package net.particify.arsnova.core.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.TemplateTag;
import net.particify.arsnova.core.service.ContentGroupService;
import net.particify.arsnova.core.service.ContentGroupTemplateService;
import net.particify.arsnova.core.service.ContentTemplateService;

@RestController
@EntityRequestMapping(TemplateController.REQUEST_MAPPING)
public class TemplateController extends AbstractEntityController<ContentGroupTemplate> {
  public static final String REQUEST_MAPPING = "/template/contentgroup";
  private static final String FROM_EXISTING_MAPPING = "/from-existing";

  private final ContentGroupTemplateService contentGroupTemplateService;

  protected TemplateController(
      @Qualifier("securedContentGroupTemplateService") final ContentGroupTemplateService contentGroupTemplateService,
      @Qualifier("securedContentTemplateService") final ContentTemplateService contentTemplateService,
      @Qualifier("securedContentGroupService") final ContentGroupService contentGroupService) {
    super(contentGroupTemplateService);
    this.contentGroupTemplateService = contentGroupTemplateService;
  }

  @GetMapping(value = DEFAULT_ROOT_MAPPING, params = {"language"})
  public List<ContentGroupTemplate> getContentGroupTemplatesByLanguage(@RequestParam final String language) {
    return contentGroupTemplateService.getTopByLanguageOrderedByCreationTimestampDesc(language, 50);
  }

  @GetMapping(value = DEFAULT_ROOT_MAPPING, params = {"tagIds"})
  public List<ContentGroupTemplate> getContentGroupTemplates(@RequestParam final List<String> tagIds) {
    return contentGroupTemplateService.getByTagIds(tagIds);
  }

  @GetMapping(value = DEFAULT_ROOT_MAPPING, params = {"creatorId"})
  public List<ContentGroupTemplate> getContentGroupTemplatesByCreatorId(@RequestParam final String creatorId) {
    return contentGroupTemplateService.getByCreatorId(creatorId);
  }

  @PostMapping(value = FROM_EXISTING_MAPPING)
  public ContentGroupTemplate createFromContentGroup(
      @RequestBody final CreateFromContentGroupRequestEntity requestEntity) {
    final ContentGroupTemplate contentGroupTemplate = new ContentGroupTemplate();
    contentGroupTemplate.setName(requestEntity.name);
    contentGroupTemplate.setDescription(requestEntity.description);
    contentGroupTemplate.setLanguage(requestEntity.language);
    contentGroupTemplate.setLicense(requestEntity.license);
    contentGroupTemplate.setAttribution(requestEntity.attribution);
    contentGroupTemplate.setAiGenerated(requestEntity.aiGenerated);
    contentGroupTemplate.setTags(requestEntity.tags);
    contentGroupTemplate.setPublished(requestEntity.published);
    return contentGroupTemplateService.createFromContentGroup(requestEntity.contentGroupId, contentGroupTemplate);
  }

  @Override
  protected String getMapping() {
    return REQUEST_MAPPING;
  }

  public record CreateFromContentGroupRequestEntity(
      String name,
      String description,
      String language,
      String license,
      String attribution,
      boolean aiGenerated,
      String contentGroupId,
      List<TemplateTag> tags,
      boolean published) {
  }
}
