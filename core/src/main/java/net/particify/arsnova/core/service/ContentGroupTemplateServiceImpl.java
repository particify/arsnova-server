package net.particify.arsnova.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.persistence.ContentGroupTemplateRepository;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.security.AuthenticationService;

@Service
@Primary
public class ContentGroupTemplateServiceImpl
    extends DefaultEntityServiceImpl<ContentGroupTemplate>
    implements ContentGroupTemplateService {
  private final ContentGroupTemplateRepository contentGroupTemplateRepository;
  private final ContentGroupService contentGroupService;
  private final ContentTemplateService contentTemplateService;
  private final AuthenticationService authenticationService;

  public ContentGroupTemplateServiceImpl(
      final ContentGroupTemplateRepository repository,
      final ContentGroupService contentGroupService,
      final ContentTemplateService contentTemplateService,
      final AuthenticationService authenticationService,
      final DeletionRepository deletionRepository,
      final ObjectMapper objectMapper,
      final Validator validator) {
    super(ContentGroupTemplate.class, repository, deletionRepository, objectMapper, validator);
    this.contentGroupTemplateRepository = repository;
    this.contentGroupService = contentGroupService;
    this.contentTemplateService = contentTemplateService;
    this.authenticationService = authenticationService;
  }

  @Override
  public List<ContentGroupTemplate> getByTags(final List<String> tags) {
    return contentGroupTemplateRepository.findByTags(tags);
  }

  @Override
  protected void prepareCreate(final ContentGroupTemplate entity) {
    super.prepareCreate(entity);
    entity.setCreatorId(authenticationService.getCurrentUser().getId());
  }

  @Override
  public List<ContentGroupTemplate> getByCreatorId(final String creatorId) {
    final List<ContentGroupTemplate> templates = contentGroupTemplateRepository.findByCreatorId(creatorId);
    templates.forEach(t -> t.setTags(templateTagService.get(t.getTagIds())));
    return templates;
  }

  @Override
  public ContentGroupTemplate createFromContentGroup(final String id, final ContentGroupTemplate template) {
    final ContentGroup contentGroup = contentGroupService.get(id);
    final List<String> ids = contentTemplateService.createTemplatesFromContents(contentGroup.getContentIds());
    template.setTemplateIds(ids);
    return create(template);
  }
}
