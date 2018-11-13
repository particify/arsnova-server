package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public abstract class AfterUpdateEvent<E extends Entity> extends CrudEvent<E> {
	public AfterUpdateEvent(final Object source, final E entity) {
		super(source, entity);
	}
}
