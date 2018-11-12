package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class AfterPatchEvent<E extends Entity> extends AfterUpdateEvent<E> {
	public AfterPatchEvent(final Object source, final E entity) {
		super(source, entity);
	}
}
