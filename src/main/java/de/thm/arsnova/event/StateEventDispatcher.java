/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.event;

import java.util.Map;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Entity;
import de.thm.arsnova.model.Room;

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
		publishEventIfPropertyChanged(event, Function.identity(), "closed", "closed");
		publishEventIfPropertyChanged(event, Function.identity(), "settings", "settings");
		publishEventIfPropertyChanged(event, Room::getSettings, null, "settings");
	}

	@EventListener
	public void dispatchContentStateEvent(final AfterFullUpdateEvent<Content> event) {
		final Content newContent = event.getEntity();
		final Content oldContent = event.getOldEntity();
		Function<Content, Content.State> f = Content::getState;
		f.apply(newContent);
		publishEventIfPropertyChanged(newContent, oldContent, Content::getState, "state");
	}

	@EventListener
	public void dispatchContentStateEvent(final AfterPatchEvent<Content> event) {
		publishEventIfPropertyChanged(event, Function.identity(), "state", "state");
		publishEventIfPropertyChanged(event, Content::getState, null, "state");
	}

	private <E extends Entity, T extends Object> void publishEventIfPropertyChanged(
			final E newEntity, final E oldEntity, final Function<E, T> propertyGetter, final String stateName) {
		T newValue = propertyGetter.apply(newEntity);
		T oldValue = propertyGetter.apply(oldEntity);
		if (!newValue.equals(oldValue)) {
			eventPublisher.publishEvent(new StateChangeEvent<>(this, newEntity, stateName, newValue, oldValue));
		}
	}

	private <E extends Entity, T extends Object> void publishEventIfPropertyChanged(
			final AfterPatchEvent<E> event, final Function<E, T> propertyGetter,
			final String property, final String stateName) {
		final E entity = event.getEntity();
		final Map<String, Object> changes = event.getChanges();
		if (event.getPropertyGetter().apply(entity) == propertyGetter.apply(entity)
				&& (property == null || changes.containsKey(property))) {
			final Object value = property == null ? event.getPropertyGetter().apply(entity) : changes.get(property);
			eventPublisher.publishEvent(
					new StateChangeEvent<>(this, entity, stateName, value, null));
		}
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}
}
