package de.thm.arsnova.event;

import de.thm.arsnova.model.Entity;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

import java.util.Optional;

public class StateChangeEvent<E extends Entity, T> extends ApplicationEvent implements ResolvableTypeProvider {
	private final E entity;
	private final String stateName;
	private final T newValue;
	private final T oldValue;

	public StateChangeEvent(final Object source, final E entity, final String stateName,
			final T newValue, final T oldValue) {
		super(source);
		this.entity = entity;
		this.stateName = stateName;
		this.newValue = newValue;
		this.oldValue = oldValue;
	}

	public E getEntity() {
		return entity;
	}

	public String getStateName() {
		return stateName;
	}

	public T getNewValue() {
		return newValue;
	}

	public Optional<T> getOldValue() {
		return Optional.ofNullable(oldValue);
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), entity.getClass(), newValue.getClass());
	}
}
