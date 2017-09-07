package de.thm.arsnova.entities.migration;

import de.thm.arsnova.entities.Entity;

public class V2Migrator {
	private void copyCommonProperties(final Entity from, final Entity to) {
		to.setId(from.getId());
		to.setRevision(from.getRevision());
	}
}
