package de.thm.arsnova.persistence;

import java.util.List;

import de.thm.arsnova.model.Announcement;

public interface AnnouncementRepository extends CrudRepository<Announcement, String> {
	List<String> findIdsByRoomId(String roomId);

	List<String> findIdsByRoomIds(List<String> roomIds);
}
