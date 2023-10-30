package net.particify.arsnova.core.service;

import java.util.List;

import net.particify.arsnova.core.model.TemplateTag;

public interface TemplateTagService extends EntityService<TemplateTag> {
  List<TemplateTag> getTagsByVerifiedAndLanguage(boolean verified, String language);

  List<TemplateTag> getTagsByNameAndLanguage(List<String> tagNames, String language);
}
