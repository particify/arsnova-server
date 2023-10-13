package net.particify.arsnova.core.persistence;

import java.util.List;

import net.particify.arsnova.core.model.TemplateTag;

public interface TemplateTagRepository extends CrudRepository<TemplateTag, String> {
  List<TemplateTag> findAllByVerifiedAndLanguage(boolean verified, String language);

  List<TemplateTag> findAllByNameAndLanguage(List<String> names, String language);
}
