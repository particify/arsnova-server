package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.ContentTemplate;

@Service
public class SecuredContentTemplateService
    extends AbstractSecuredEntityServiceImpl<ContentTemplate>
    implements ContentTemplateService, SecuredService {
  private ContentTemplateService contentTemplateService;

  public SecuredContentTemplateService(final ContentTemplateService contentTemplateService) {
    super(ContentTemplate.class, contentTemplateService);
    this.contentTemplateService = contentTemplateService;
  }

  @Override
  @PreFilter(value = "hasPermission(filterObject, 'content', 'owner') and !hasRole('GUEST_USER')", filterTarget = "ids")
  public List<String> createTemplatesFromContents(final List<String> ids) {
    return contentTemplateService.createTemplatesFromContents(ids);
  }
}
