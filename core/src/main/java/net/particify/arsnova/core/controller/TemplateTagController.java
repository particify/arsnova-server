package net.particify.arsnova.core.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.TemplateTag;
import net.particify.arsnova.core.service.TemplateTagService;

@RestController
@EntityRequestMapping(TemplateTagController.REQUEST_MAPPING)
public class TemplateTagController extends AbstractEntityController<TemplateTag> {
  public static final String REQUEST_MAPPING = "/template/tag";

  private final TemplateTagService templateTagService;

  protected TemplateTagController(@Qualifier("securedTemplateTagService") final TemplateTagService templateTagService) {
    super(templateTagService);
    this.templateTagService = templateTagService;
  }

  @GetMapping(value = DEFAULT_ROOT_MAPPING, params = "language")
  public List<TemplateTag> getAllByLanguage(
      @RequestParam(defaultValue = "true") final boolean verified,
      @RequestParam final String language) {
    return templateTagService.getTagsByVerifiedAndLanguage(verified, language);
  }

  @Override
  protected String getMapping() {
    return null;
  }
}
