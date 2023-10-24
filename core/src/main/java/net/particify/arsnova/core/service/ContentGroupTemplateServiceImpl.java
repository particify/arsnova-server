package net.particify.arsnova.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.TemplateTag;
import net.particify.arsnova.core.persistence.ContentGroupTemplateRepository;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.security.AuthenticationService;

@Service
@Primary
public class ContentGroupTemplateServiceImpl
    extends DefaultEntityServiceImpl<ContentGroupTemplate>
    implements ContentGroupTemplateService {
  private static final Logger logger = LoggerFactory.getLogger(ContentGroupTemplateServiceImpl.class);
  private final ContentGroupTemplateRepository contentGroupTemplateRepository;
  private final ContentGroupService contentGroupService;
  private final ContentTemplateService contentTemplateService;
  private final TemplateTagService templateTagService;
  private final AuthenticationService authenticationService;

  public ContentGroupTemplateServiceImpl(
      final ContentGroupTemplateRepository repository,
      final ContentGroupService contentGroupService,
      final ContentTemplateService contentTemplateService,
      final TemplateTagService templateTagService,
      final AuthenticationService authenticationService,
      final DeletionRepository deletionRepository,
      final ObjectMapper objectMapper,
      final Validator validator) {
    super(ContentGroupTemplate.class, repository, deletionRepository, objectMapper, validator);
    this.contentGroupTemplateRepository = repository;
    this.contentGroupService = contentGroupService;
    this.contentTemplateService = contentTemplateService;
    this.templateTagService = templateTagService;
    this.authenticationService = authenticationService;
  }

  @Override
  protected void modifyRetrieved(final ContentGroupTemplate entity) {
    super.modifyRetrieved(entity);
    entity.setTags(templateTagService.get(entity.getTagIds()));
  }

  @Override
  public List<ContentGroupTemplate> getTopByVerifiedAndLanguageOrderedByCreationTimestampDesc(
      final boolean verified,
      final String language,
      final int topCount) {
    final List<ContentGroupTemplate> templates = contentGroupTemplateRepository
        .findTopByVerifiedAndLanguageOrderByCreationTimestampDesc(verified, language, topCount);
    templates.forEach(t -> t.setTags(templateTagService.get(t.getTagIds())));
    return templates;
  }

  @Override
  public List<ContentGroupTemplate> getByVerifiedAndTagIds(final boolean verified, final List<String> tagIds) {
    final List<ContentGroupTemplate> templates = contentGroupTemplateRepository
        .findByVerifiedAndTagIds(verified, tagIds);
    templates.forEach(t -> t.setTags(templateTagService.get(t.getTagIds())));
    return templates;
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
    resolveAndCreateTags(template);
    return create(template);
  }

  @Override
  public ContentGroupTemplate verify(final String id, final boolean verify) {
    final ContentGroupTemplate template = get(id);
    template.setVerified(verify);
    return update(template);
  }

  private void resolveAndCreateTags(final ContentGroupTemplate template) {
    final List<String> tagNames = template.getTags().stream()
        .map(t -> t.getName())
        .collect(Collectors.toList());
    final List<TemplateTag> tags = templateTagService.getTagsByNameAndLanguage(
        tagNames,
        template.getLanguage());
    final List<String> newTagNames = tagNames.stream()
        .filter(name -> tags.stream().noneMatch(t -> t.getName().equals(name)))
        .collect(Collectors.toList());
    final List<TemplateTag> newTags = templateTagService.create(newTagNames.stream()
        .map(name -> new TemplateTag(name, template.getLanguage()))
        .collect(Collectors.toList()));
    logger.debug("Created new template tags: {}", newTags);
    template.setTags(Stream.concat(tags.stream(), newTags.stream()).collect(Collectors.toList()));
    template.setTagIds(template.getTags().stream()
        .map(t -> t.getId())
        .collect(Collectors.toList()));
  }
}
