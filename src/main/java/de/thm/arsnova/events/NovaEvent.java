package de.thm.arsnova.events;

import org.springframework.context.ApplicationEvent;

public abstract class NovaEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	public NovaEvent(Object source) {
		super(source);
	}

	public abstract void accept(NovaEventVisitor visitor);

}
