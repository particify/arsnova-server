package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

public abstract class CrudEvent<E extends Entity> extends ApplicationEvent implements ResolvableTypeProvider {
	public CrudEvent(final E source) {
		super(source);
	}

	@Override
	public E getSource() {
		return (E) super.getSource();
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), source.getClass());
	}
}
