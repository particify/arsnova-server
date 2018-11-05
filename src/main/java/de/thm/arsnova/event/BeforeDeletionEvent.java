package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class BeforeDeletionEvent<E extends Entity> extends CrudEvent<E> {
	public BeforeDeletionEvent(final E source) {
		super(source);
	}
}
