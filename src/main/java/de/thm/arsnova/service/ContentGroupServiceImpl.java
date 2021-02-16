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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import de.thm.arsnova.event.BeforeDeletionEvent;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.persistence.ContentGroupRepository;
import de.thm.arsnova.web.exceptions.BadRequestException;
import de.thm.arsnova.web.exceptions.NotFoundException;

@Service
@Primary
public class ContentGroupServiceImpl extends DefaultEntityServiceImpl<ContentGroup>
		implements ContentGroupService {
	private ContentGroupRepository contentGroupRepository;
	private ContentService contentService;

	public ContentGroupServiceImpl(
			final ContentGroupRepository repository,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator) {
		super(ContentGroup.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
		this.contentGroupRepository = repository;
	}

	@Autowired
	public void setContentService(final ContentService contentService) {
		this.contentService = contentService;
	}

	@Override
	public void prepareCreate(final ContentGroup contentGroup) {
		if (this.getByRoomIdAndName(contentGroup.getRoomId(), contentGroup.getName()) != null) {
			throw new BadRequestException();
		}
	}

	@Override
	public void prepareUpdate(final ContentGroup contentGroup) {
		final ContentGroup oldContentGroup = this.get(contentGroup.getId());
		if (oldContentGroup != null) {
			// Disallow changing the name of a content group for now
			if (!oldContentGroup.getName().equals(contentGroup.getName())) {
				throw new BadRequestException();
			}
		}
	}

	@Override
	public ContentGroup getByRoomIdAndName(final String roomId, final String name) {
		return contentGroupRepository.findByRoomIdAndName(roomId, name);
	}

	@Override
	public List<ContentGroup> getByRoomId(final String roomId) {
		return contentGroupRepository.findByRoomId(roomId);
	}

	@Override
	public void addContentToGroup(final String roomId, final String groupName, final String contentId) {
		ContentGroup contentGroup = getByRoomIdAndName(roomId, groupName);
		if (contentGroup == null) {
			contentGroup = new ContentGroup(roomId, groupName);
			contentGroup.getContentIds().add(contentId);
			create(contentGroup);
		} else {
			contentGroup.getContentIds().add(contentId);
			update(contentGroup);
		}
	}

	@Override
	public void removeContentFromGroup(final String roomId, final String groupId, final String contentId) {
		final Optional<ContentGroup> contentGroup = contentGroupRepository.findById(groupId);
		contentGroup.ifPresentOrElse(
				cg -> {
					if (!cg.getRoomId().equals(roomId)) {
						throw new BadRequestException("Room ID does not match.");
					}
					cg.setContentIds(cg.getContentIds().stream()
							.filter(id -> !id.equals(contentId)).collect(Collectors.toList()));
					if (!cg.getContentIds().isEmpty()) {
						update(cg);
					} else {
						delete(cg);
					}
				}, () -> {
					throw new NotFoundException("ContentGroup does not exist.");
				});
	}

	@Override
	public ContentGroup createOrUpdateContentGroup(final ContentGroup contentGroup) {
		if (contentGroup.getContentIds().isEmpty()) {
			if (contentGroup.getId() != null) {
				delete(contentGroup);
			}

			return new ContentGroup();
		} else {
			final List<String> contentIds = StreamSupport.stream(
					contentService.get(contentGroup.getContentIds()).spliterator(), false)
						.filter(c -> c.getRoomId().equals(contentGroup.getRoomId()))
						.map(Content::getId)
						.distinct()
						.collect(Collectors.toList());
			contentGroup.setContentIds(contentIds);
			if (contentGroup.getId() != null) {
				return update(contentGroup);
			} else {
				return create(contentGroup);
			}
		}
	}

	@EventListener
	@Secured({"ROLE_USER", "RUN_AS_SYSTEM"})
	public void handleRoomDeletion(final BeforeDeletionEvent<Room> event) {
		final Iterable<ContentGroup> contentGroups = contentGroupRepository.findByRoomId(event.getEntity().getId());
		delete(contentGroups);
	}
}
