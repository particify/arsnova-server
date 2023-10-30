package net.particify.arsnova.core.service;

import java.util.List;

import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentTemplate;

public interface ContentGroupService extends EntityService<ContentGroup> {
  ContentGroup getByRoomIdAndName(String roomId, String name);

  List<ContentGroup> getByRoomId(String roomId);

  List<ContentGroup> getByRoomIdAndContainingContentId(String roomId, String contentId);

  void addContentToGroup(String roomId, String groupName, String contentId);

  void removeContentFromGroup(String groupId, String contentId);

  ContentGroup createOrUpdateContentGroup(ContentGroup contentGroup);

  void importFromCsv(byte[] csv, ContentGroup contentGroup);

  ContentGroup createFromTemplate(String roomId, ContentGroupTemplate template, List<ContentTemplate> contentTemplates);
}
