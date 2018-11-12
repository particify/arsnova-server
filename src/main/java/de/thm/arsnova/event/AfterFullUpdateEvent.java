package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

public class AfterFullUpdateEvent<E extends Entity> extends AfterUpdateEvent<E> {
	private final E oldEntity;

	public AfterFullUpdateEvent(final Object source, final E entity, final E oldEntity) {
		super(source, entity);
		this.oldEntity = oldEntity;
	}

	public E getOldEntity() {
		return oldEntity;
	}
}
