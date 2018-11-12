package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

import java.util.Map;

public class BeforePatchEvent<E extends Entity> extends BeforeUpdateEvent<E> {
	private final Map<String, Object> changes;

	public BeforePatchEvent(final Object source, final E entity, final Map<String, Object> changes) {
		super(source, entity);
		this.changes = changes;
	}

	public Map<String, Object> getChanges() {
		return changes;
	}
}
