package de.thm.arsnova.persistance.couchdb;

import de.thm.arsnova.entities.migration.v2.MotdList;
import de.thm.arsnova.persistance.MotdListRepository;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.support.CouchDbRepositorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CouchDbMotdListRepository extends CouchDbRepositorySupport<MotdList> implements MotdListRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbMotdListRepository.class);

	public CouchDbMotdListRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(MotdList.class, db, createIfNotExists);
	}

	@Override
	public MotdList findByUsername(final String username) {
		final List<MotdList> motdListList = queryView("by_username", username);
		return motdListList.isEmpty() ? new MotdList() : motdListList.get(0);
	}

	/* TODO: Move to service layer. */
	@Override
	public MotdList save(final MotdList motdlist) {
		try {
			if (motdlist.getId() != null) {
				update(motdlist);
			} else {
				db.create(motdlist);
			}

			return motdlist;
		} catch (final DbAccessException e) {
			logger.error("Could not save MotD list {}.", motdlist, e);
		}

		return null;
	}
}
