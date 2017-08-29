package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.MotdList;

public interface MotdListRepository {
	MotdList findByUsername(String username);
	MotdList save(MotdList motdlist);
}
