package net.particify.arsnova.core.service;

import java.util.List;

import net.particify.arsnova.core.model.ContentGroupTemplate;

public interface ContentGroupTemplateService extends EntityService<ContentGroupTemplate> {
  List<ContentGroupTemplate> getTopByLanguageOrderedByCreationTimestampDesc(String language, int topCount);

  List<ContentGroupTemplate> getByTagIds(List<String> tags);

  List<ContentGroupTemplate> getByCreatorId(String creatorId);

  ContentGroupTemplate createFromContentGroup(String id, ContentGroupTemplate contentGroupTemplate);
}
