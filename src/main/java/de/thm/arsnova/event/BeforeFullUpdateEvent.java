package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class BeforeFullUpdateEvent<E extends Entity> extends BeforeUpdateEvent<E> {
	private final E oldEntity;

	public BeforeFullUpdateEvent(final Object source, final E entity, final E oldEntity) {
		super(source, entity);
		this.oldEntity = oldEntity;
	}

	public E getOldEntity() {
		return oldEntity;
	}
}
