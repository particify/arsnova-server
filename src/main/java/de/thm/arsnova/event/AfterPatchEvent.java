package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class AfterPatchEvent<E extends Entity> extends AfterUpdateEvent<E> {
	public AfterPatchEvent(final E source) {
		super(source);
	}
}
