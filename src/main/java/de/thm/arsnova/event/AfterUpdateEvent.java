package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class AfterUpdateEvent<E extends Entity> extends CrudEvent<E> {
	public AfterUpdateEvent(final E source) {
		super(source);
	}
}
