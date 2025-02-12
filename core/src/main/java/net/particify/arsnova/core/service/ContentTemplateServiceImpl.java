package net.particify.arsnova.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.event.BeforeDeletionEvent;
import net.particify.arsnova.core.event.BeforeUpdateEvent;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentTemplate;
import net.particify.arsnova.core.model.Deletion;
import net.particify.arsnova.core.persistence.ContentTemplateRepository;
import net.particify.arsnova.core.persistence.DeletionRepository;

@Service
@Primary
public class ContentTemplateServiceImpl
    extends DefaultEntityServiceImpl<ContentTemplate>
    implements ContentTemplateService {
  private final ContentTemplateRepository contentTemplateRepository;
  private final ContentService contentService;

  public ContentTemplateServiceImpl(
      final ContentTemplateRepository repository,
      final ContentService contentService,
      final DeletionRepository deletionRepository,
      final ObjectMapper objectMapper,
      final Validator validator) {
    super(ContentTemplate.class, repository, deletionRepository, objectMapper, validator);
    this.contentTemplateRepository = repository;
    this.contentService = contentService;
  }

  @Override
  public List<String> createTemplatesFromContents(final List<String> ids) {
    final List<Content> contents = contentService.get(ids);
    final List<ContentTemplate> templates = contents.stream()
        .map(c -> new ContentTemplate(c.copy()))
        .collect(Collectors.toList());
    return create(templates).stream()
        .map(t -> t.getId())
        .collect(Collectors.toList());
  }

  @EventListener
  public void handleUpdate(final BeforeUpdateEvent<ContentGroupTemplate> event) {
    event.getEntity().setGroupType(event.getOldEntity().getGroupType());
    event.getEntity().setLicense(event.getOldEntity().getLicense());
    event.getEntity().setTemplateIds(event.getOldEntity().getTemplateIds());
  }

  @EventListener
  public void handleContentGroupTemplateDeletion(final BeforeDeletionEvent<ContentGroupTemplate> event) {
    final List<ContentTemplate> templates = contentTemplateRepository.findAllById(event.getEntity().getTemplateIds());
    delete(templates, Deletion.Initiator.CASCADE);
  }
}
