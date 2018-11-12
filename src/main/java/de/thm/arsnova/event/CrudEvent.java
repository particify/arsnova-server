package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public abstract class CrudEvent<E extends Entity> extends ApplicationEvent implements ResolvableTypeProvider {
	private E entity;

	public CrudEvent(final Object source, final E entity) {
		super(source);
		this.entity = entity;
	}

	public E getEntity() {
		return entity;
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), source.getClass());
	}
}
