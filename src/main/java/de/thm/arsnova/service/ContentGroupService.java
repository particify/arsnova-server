package de.thm.arsnova.service;

import java.util.List;

import de.thm.arsnova.model.ContentGroup;

public interface ContentGroupService extends EntityService<ContentGroup> {
	ContentGroup getByRoomIdAndName(String roomId, String name);

	List<ContentGroup> getByRoomId(String roomId);

	void addContentToGroup(String roomId, String groupName, String contentId);

	void removeContentFromGroup(String roomId, String groupId, String contentId);

	ContentGroup createOrUpdateContentGroup(ContentGroup contentGroup);
}
