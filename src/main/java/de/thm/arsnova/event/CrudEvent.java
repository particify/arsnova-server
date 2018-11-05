package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;
import org.springframework.context.ApplicationEvent;

public abstract class CrudEvent<E extends Entity> extends ApplicationEvent {
	public CrudEvent(final E source) {
		super(source);
	}

	@Override
	public E getSource() {
		return (E) super.getSource();
	}
}
