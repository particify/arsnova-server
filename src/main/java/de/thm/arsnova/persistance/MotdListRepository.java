package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.migration.v2.MotdList;

public interface MotdListRepository {
	MotdList findByUsername(String username);
	MotdList save(MotdList motdlist);
}
