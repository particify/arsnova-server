package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class AfterCreationEvent<E extends Entity> extends CrudEvent<E> {
	public AfterCreationEvent(final E source) {
		super(source);
	}
}
