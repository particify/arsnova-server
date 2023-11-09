package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentLicenseAttribution;

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
  public List<ContentGroupTemplate> getTopByLanguageOrderedByCreationTimestampDesc(
      final String language,
      final int topCount) {
    return contentGroupTemplateService.getTopByLanguageOrderedByCreationTimestampDesc(language, topCount);
  }

  @Override
  @PreAuthorize("isAuthenticated")
  public List<ContentGroupTemplate> getByTagIds(final List<String> tags) {
    return contentGroupTemplateService.getByTagIds(tags);
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

  @Override
  @PreFilter(value = "hasPermission(filterObject, 'content', 'read')", filterTarget = "contentIds")
  public List<ContentLicenseAttribution> getAttributionsByContentIds(final List<String> contentIds) {
    return contentGroupTemplateService.getAttributionsByContentIds(contentIds);
  }
}
