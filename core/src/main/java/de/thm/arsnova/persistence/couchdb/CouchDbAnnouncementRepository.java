package de.thm.arsnova.persistence.couchdb;

import java.util.List;
import java.util.stream.Collectors;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;

import de.thm.arsnova.model.Announcement;
import de.thm.arsnova.persistence.AnnouncementRepository;

public class CouchDbAnnouncementRepository extends CouchDbCrudRepository<Announcement>
		implements AnnouncementRepository {
	public CouchDbAnnouncementRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Announcement.class, db, "by_id", createIfNotExists);
	}

	@Override
	public List<String> findIdsByRoomId(final String roomId) {
		final ViewResult result = db.queryView(createQuery("by_roomid").reduce(false).key(roomId));

		return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
	}

	@Override
	public List<String> findIdsByRoomIds(final List<String> roomIds) {
		final ViewResult result = db.queryView(createQuery("by_roomid").reduce(false).keys(roomIds));

		return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
	}
}
