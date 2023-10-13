package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.TemplateTag;

@Service
public class SecuredTemplateTagService
    extends AbstractSecuredEntityServiceImpl<TemplateTag>
    implements TemplateTagService, SecuredService {
  private TemplateTagService templateTagService;

  public SecuredTemplateTagService(final TemplateTagService templateTagService) {
    super(TemplateTag.class, templateTagService);
    this.templateTagService = templateTagService;
  }

  @Override
  @PreAuthorize("isAuthenticated")
  public List<TemplateTag> getTagsByVerifiedAndLanguage(final boolean verified, final String language) {
    return templateTagService.getTagsByVerifiedAndLanguage(verified, language);
  }

  @Override
  @PreAuthorize("isAuthenticated")
  public List<TemplateTag> getTagsByNameAndLanguage(final List<String> tagNames, final String language) {
    return templateTagService.getTagsByNameAndLanguage(tagNames, language);
  }
}
