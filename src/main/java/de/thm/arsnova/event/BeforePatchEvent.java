package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

import java.util.Map;
import java.util.function.Function;

public class BeforePatchEvent<E extends Entity> extends BeforeUpdateEvent<E> {
	private final Function<E, ? extends Object> propertyGetter;
	private final Map<String, Object> changes;

	public BeforePatchEvent(final Object source, final E entity, final Function<E, ? extends Object> propertyGetter,
			final Map<String, Object> changes) {
		super(source, entity);
		this.propertyGetter = propertyGetter;
		this.changes = changes;
	}

	public Function<E, ? extends Object> getPropertyGetter() {
		return propertyGetter;
	}

	public Map<String, Object> getChanges() {
		return changes;
	}
}
