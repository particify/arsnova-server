package de.thm.arsnova.service;

import java.util.List;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.ContentGroup;

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
	@PreAuthorize("hasPermission(#contentGroup, 'update')")
	public ContentGroup createOrUpdateContentGroup(final ContentGroup contentGroup) {
		return contentGroupService.createOrUpdateContentGroup(contentGroup);
	}
}
