package net.particify.arsnova.core.persistence;

import java.util.List;

import net.particify.arsnova.core.model.ContentGroupTemplate;

public interface ContentGroupTemplateRepository extends CrudRepository<ContentGroupTemplate, String> {
  List<ContentGroupTemplate> findByTagIds(List<String> tags);

  List<ContentGroupTemplate> findByCreatorId(String creatorId);
}
