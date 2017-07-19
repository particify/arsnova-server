package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.MotdList;

public interface MotdListRepository {
	MotdList getMotdListForUser(String username);
	MotdList createOrUpdateMotdList(MotdList motdlist);
}
