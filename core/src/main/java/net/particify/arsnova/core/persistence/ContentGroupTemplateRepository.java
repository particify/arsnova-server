package net.particify.arsnova.core.persistence;

import java.util.List;

import net.particify.arsnova.core.model.ContentGroupTemplate;

public interface ContentGroupTemplateRepository extends CrudRepository<ContentGroupTemplate, String> {
  List<ContentGroupTemplate> findTopByVerifiedAndLanguageOrderByCreationTimestampDesc(
      boolean verified,
      String language,
      int topCount);

  List<ContentGroupTemplate> findByVerifiedAndTagIds(boolean verified, List<String> tags);

  List<ContentGroupTemplate> findByCreatorId(String creatorId);
}
