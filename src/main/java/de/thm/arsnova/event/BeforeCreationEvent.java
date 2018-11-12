package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class BeforeCreationEvent<E extends Entity> extends CrudEvent<E> {
	public BeforeCreationEvent(final Object source, final E entity) {
		super(source, entity);
	}
}
