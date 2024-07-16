package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentTemplate;

@Service
public class SecuredContentGroupService extends AbstractSecuredEntityServiceImpl<ContentGroup>
    implements ContentGroupService, SecuredService {
  private final ContentGroupService contentGroupService;

  public SecuredContentGroupService(final ContentGroupService contentGroupService) {
    super(ContentGroup.class, contentGroupService);
    this.contentGroupService = contentGroupService;
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'read')")
  @PostAuthorize("hasPermission(returnObject, 'read')")
  public ContentGroup getByRoomIdAndName(final String roomId, final String name) {
    return contentGroupService.getByRoomIdAndName(roomId, name);
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'read')")
  @PostFilter("hasPermission(filterObject, 'read')")
  public List<ContentGroup> getByRoomId(final String roomId) {
    return contentGroupService.getByRoomId(roomId);
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'read') and hasPermission(#contentId, 'content', 'read')\"")
  public List<ContentGroup> getByRoomIdAndContainingContentId(final String roomId, final String contentId) {
    return contentGroupService.getByRoomIdAndContainingContentId(roomId, contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'update') and hasPermission(#contentId, 'content', 'update')")
  public void addContentToGroup(final String roomId, final String groupName, final String contentId) {
    contentGroupService.addContentToGroup(roomId, groupName, contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#groupId, 'contentgroup', 'update') and hasPermission(#contentId, 'content', 'update')")
  public void removeContentFromGroup(final String groupId, final String contentId) {
    contentGroupService.removeContentFromGroup(groupId, contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#groupId, 'contentgroup', 'update') and hasPermission(#contentId, 'content', 'update')")
  public void startContent(final String groupId, final String contentId, final int round) {
    contentGroupService.startContent(groupId, contentId, round);
  }

  @Override
  @PreAuthorize("hasPermission(#contentGroup, 'update')")
  public ContentGroup createOrUpdateContentGroup(final ContentGroup contentGroup) {
    return contentGroupService.createOrUpdateContentGroup(contentGroup);
  }

  @Override
  @PreAuthorize("hasPermission(#contentGroup, 'update')")
  public void importFromCsv(final byte[] csv, final ContentGroup contentGroup) {
    contentGroupService.importFromCsv(csv, contentGroup);
  }

  @PreAuthorize("hasPermission(#roomId, 'room', 'update')")
  @Override
  public ContentGroup createFromTemplate(
      final String roomId,
      final ContentGroupTemplate template,
      final List<ContentTemplate> contentTemplates) {
    return contentGroupService.createFromTemplate(roomId, template, contentTemplates);
  }
}
