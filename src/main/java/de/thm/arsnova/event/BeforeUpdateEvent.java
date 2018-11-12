package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class BeforeUpdateEvent<E extends Entity> extends CrudEvent<E> {
	public BeforeUpdateEvent(final Object source, final E entity) {
		super(source, entity);
	}
}
