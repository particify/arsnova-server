package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class AfterDeletionEvent<E extends Entity> extends CrudEvent<E> {
	public AfterDeletionEvent(final Object source, final E entity) {
		super(source, entity);
	}
}
