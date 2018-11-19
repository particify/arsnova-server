package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;

import java.util.Map;
import java.util.function.Function;

public class AfterPatchEvent<E extends Entity> extends AfterUpdateEvent<E> {
	private final Function<E, ? extends Object> propertyGetter;
	private final Map<String, Object> changes;

	public AfterPatchEvent(final Object source, final E entity, final Function<E, ? extends Object> propertyGetter,
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
