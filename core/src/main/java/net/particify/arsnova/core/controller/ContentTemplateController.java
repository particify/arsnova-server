package net.particify.arsnova.core.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.ContentTemplate;
import net.particify.arsnova.core.service.ContentTemplateService;

@RestController
@EntityRequestMapping(ContentTemplateController.REQUEST_MAPPING)
public class ContentTemplateController extends AbstractEntityController<ContentTemplate> {
  public static final String REQUEST_MAPPING = "/template/content";

  protected ContentTemplateController(
      @Qualifier("securedContentTemplateService") final ContentTemplateService contentTemplateService) {
    super(contentTemplateService);
  }

  @Override
  protected String getMapping() {
    return REQUEST_MAPPING;
  }
}
