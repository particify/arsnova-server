package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class BeforeCreationEvent<E extends Entity> extends CrudEvent<E> {
	public BeforeCreationEvent(final E source) {
		super(source);
	}
}
