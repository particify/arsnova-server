package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.ContentGroupTemplate;

@Service
public class SecuredContentGroupTemplateService
    extends AbstractSecuredEntityServiceImpl<ContentGroupTemplate>
    implements ContentGroupTemplateService, SecuredService {
  private ContentGroupTemplateService contentGroupTemplateService;

  public SecuredContentGroupTemplateService(final ContentGroupTemplateService contentGroupTemplateService) {
    super(ContentGroupTemplate.class, contentGroupTemplateService);
    this.contentGroupTemplateService = contentGroupTemplateService;
  }

  // Restrict access to admin role until fine-grained permission handling has
  // been implemented.

  @Override
  @PreAuthorize("isAuthenticated")
  public List<ContentGroupTemplate> getByTags(final List<String> tags) {
    return contentGroupTemplateService.getByTags(tags);
  }

  @Override
  @PreAuthorize("isAuthenticated")
  public List<ContentGroupTemplate> getByCreatorId(final String creatorId) {
    return contentGroupTemplateService.getByCreatorId(creatorId);
  }

  @Override
  @PreAuthorize("!hasRole('GUEST_USER')")
  public ContentGroupTemplate createFromContentGroup(final String id, final ContentGroupTemplate contentGroupTemplate) {
    return contentGroupTemplateService.createFromContentGroup(id, contentGroupTemplate);
  }
}
