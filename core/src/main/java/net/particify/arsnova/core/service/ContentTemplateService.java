package net.particify.arsnova.core.service;

import java.util.List;

import net.particify.arsnova.core.model.ContentTemplate;

public interface ContentTemplateService extends EntityService<ContentTemplate> {
  List<String> createTemplatesFromContents(List<String> ids);
}
