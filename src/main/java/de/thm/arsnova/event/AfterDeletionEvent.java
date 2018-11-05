package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class AfterDeletionEvent<E extends Entity> extends CrudEvent<E> {
	public AfterDeletionEvent(final E source) {
		super(source);
	}
}
