package de.thm.arsnova.event;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Entity;
import de.thm.arsnova.model.Room;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;

/**
 * StateEventDispatcher publishes additional, more specific events for state changes of entities when
 * {@link AfterUpdateEvent}s are received.
 *
 * @author Daniel Gerhardt
 */
@Component
public class StateEventDispatcher implements ApplicationEventPublisherAware {
	private ApplicationEventPublisher eventPublisher;

	@EventListener
	public void dispatchRoomStateEvent(final AfterFullUpdateEvent<Room> event) {
		final Room newRoom = event.getEntity();
		final Room oldRoom = event.getOldEntity();
		publishEventIfPropertyChanged(newRoom, oldRoom, Room::isClosed, "closed");
		publishEventIfPropertyChanged(newRoom, oldRoom, Room::getSettings, "settings");
	}

	@EventListener
	public void dispatchRoomStateEvent(final AfterPatchEvent<Room> event) {
		final Room room = event.getEntity();
		final Map<String, Object> changes = event.getChanges();
		publishEventIfPropertyChanged(room, changes, "closed", "closed");
		publishEventIfPropertyChanged(room, changes, "settings", "settings");
	}

	@EventListener
	public void dispatchContentStateEvent(final AfterFullUpdateEvent<Content> event) {
		final Content newContent = event.getEntity();
		final Content oldContent = event.getOldEntity();
		publishEventIfPropertyChanged(newContent, oldContent, Content::getState, "state");
	}

	@EventListener
	public void dispatchContentStateEvent(final AfterPatchEvent<Content> event) {
		final Content content = event.getEntity();
		final Map<String, Object> changes = event.getChanges();
		publishEventIfPropertyChanged(content, changes, "state", "state");
	}

	private <E extends Entity, T extends Object> void publishEventIfPropertyChanged(
			final E newEntity, final E oldEntity, final Function<E, T> propertyGetter, final String stateName) {
		T newValue = propertyGetter.apply(newEntity);
		T oldValue = propertyGetter.apply(oldEntity);
		if (!newValue.equals(oldValue)) {
			eventPublisher.publishEvent(new StateChangeEvent<>(this, newEntity, stateName, newValue, oldValue));
		}
	}

	private <E extends Entity> void publishEventIfPropertyChanged(
			final E entity, final Map<String, Object> changes, final String property, final String stateName) {
		if (changes.containsKey(property)) {
			eventPublisher.publishEvent(
					new StateChangeEvent<>(this, entity, stateName, changes.get(property), null));
		}
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}
}
