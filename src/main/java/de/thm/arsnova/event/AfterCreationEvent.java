package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class AfterCreationEvent<E extends Entity> extends CrudEvent<E> {
	public AfterCreationEvent(final Object source, final E entity) {
		super(source, entity);
	}
}
