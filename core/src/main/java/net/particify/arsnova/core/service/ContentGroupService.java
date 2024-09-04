package net.particify.arsnova.core.service;

import java.io.IOException;
import java.util.List;

import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentTemplate;
import net.particify.arsnova.core.model.export.ContentCsvImportSummary;

public interface ContentGroupService extends EntityService<ContentGroup> {
  ContentGroup getByRoomIdAndName(String roomId, String name);

  List<ContentGroup> getByRoomId(String roomId);

  List<ContentGroup> getByRoomIdAndContainingContentId(String roomId, String contentId);

  void addContentToGroup(String roomId, String groupName, String contentId);

  void removeContentFromGroup(String groupId, String contentId);

  ContentGroup createOrUpdateContentGroup(ContentGroup contentGroup);

  ContentCsvImportSummary importFromCsv(byte[] csv, ContentGroup contentGroup);

  ContentGroup createFromTemplate(String roomId, ContentGroupTemplate template, List<ContentTemplate> contentTemplates);

  void startContent(String groupId, String contentId, int round) throws IOException;
}
