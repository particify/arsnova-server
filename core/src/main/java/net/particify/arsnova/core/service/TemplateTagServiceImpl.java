package net.particify.arsnova.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.model.TemplateTag;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.persistence.TemplateTagRepository;

@Service
@Primary
public class TemplateTagServiceImpl
    extends DefaultEntityServiceImpl<TemplateTag>
    implements TemplateTagService {
  private TemplateTagRepository templateTagRepository;

  public TemplateTagServiceImpl(
      final TemplateTagRepository repository,
      final DeletionRepository deletionRepository,
      final ObjectMapper objectMapper,
      final Validator validator) {
    super(TemplateTag.class, repository, deletionRepository, objectMapper, validator);
    this.templateTagRepository = repository;
  }

  @Override
  public List<TemplateTag> getTagsByVerifiedAndLanguage(final boolean verified, final String language) {
    return templateTagRepository.findAllByVerifiedAndLanguage(verified, language);
  }

  @Override
  public List<TemplateTag> getTagsByNameAndLanguage(final List<String> tagNames, final String language) {
    return templateTagRepository.findAllByNameAndLanguage(tagNames, language);
  }
}
