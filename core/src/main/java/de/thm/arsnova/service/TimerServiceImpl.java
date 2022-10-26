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

package de.thm.arsnova.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.security.User;

@Service
public class TimerServiceImpl implements TimerService {
	private HashMap<String, Timer> timerList = new HashMap<>();
	private UserService userService;
	private RoomService roomService;
	private ContentService contentService;
	private AnswerService answerService;
	private AnswerRepository answerRepository;

	public TimerServiceImpl(final UserService userService, final RoomService roomService,
			final ContentService contentService, final AnswerService answerService,
			final AnswerRepository answerRepository) {
		this.userService = userService;
		this.roomService = roomService;
		this.contentService = contentService;
		this.answerService = answerService;
		this.answerRepository = answerRepository;
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#contentId, 'content', 'owner')")
	public void startNewRound(final String contentId) {
		final Content content = contentService.get(contentId);
		final Room room = roomService.get(content.getRoomId());

		cancelDelayedRoundChange(contentId);

		content.getState().setRoundEndTimestamp(null);
		content.getState().setResponsesEnabled(false);
		updateRoundManagementState(content);
		contentService.update(content);
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	public void startNewRoundDelayed(final String contentId, final int time) {
		final User user = userService.getCurrentUser();
		final Content content = contentService.get(contentId);
		final Room room = roomService.get(content.getRoomId());

		final Date date = new Date();
		final Timer timer = new Timer();
		final Date endDate = new Date(date.getTime() + (time * 1000));
		updateRoundStartVariables(content, date, endDate);
		contentService.update(content);

		timerList.put(contentId, timer);

		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				startNewRound(contentId);
			}
		}, endDate);
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	public void cancelRoundChange(final String contentId) {
		final Content content = contentService.get(contentId);
		final Room room = roomService.get(content.getRoomId());

		cancelDelayedRoundChange(contentId);
		resetRoundManagementState(content);

		if (content.getState().getRound() > 1) {
			content.getState().setRound(content.getState().getRound() - 1);
		}
		content.getState().setRoundEndTimestamp(null);

		contentService.update(content);
	}

	@Override
	public void cancelDelayedRoundChange(final String contentId) {
		final Timer timer = timerList.get(contentId);

		if (null != timer) {
			timer.cancel();
			timerList.remove(contentId);
			timer.purge();
		}
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	@CacheEvict("answerlists")
	public void resetRoundState(final String contentId) {
		final Content content = contentService.get(contentId);
		final Room room = roomService.get(content.getRoomId());
		cancelDelayedRoundChange(contentId);

		if (Content.Format.TEXT == content.getFormat()) {
			content.getState().setRound(0);
		} else {
			content.getState().setRound(1);
		}

		resetRoundManagementState(content);
		answerRepository.findStubsByContentId(content.getId());
		contentService.update(content);
	}

	private void updateRoundStartVariables(final Content content, final Date start, final Date end) {
		if (content.getState().getRound() == 1 && content.getState().getRoundEndTimestamp() == null) {
			content.getState().setRound(2);
		}

		content.getState().setVisible(true);
		content.getState().setAdditionalTextVisible(false);
		content.getState().setResponsesVisible(false);
		content.getState().setResponsesEnabled(true);
		content.getState().setRoundEndTimestamp(end);
	}

	private void updateRoundManagementState(final Content content) {
		if (content.getState().getRoundEndTimestamp() != null
				&& new Date().compareTo(content.getState().getRoundEndTimestamp()) > 0) {
			content.getState().setRoundEndTimestamp(null);
		}
	}

	private void resetRoundManagementState(final Content content) {
		content.getState().setAdditionalTextVisible(false);
		content.getState().setResponsesVisible(false);
		content.getState().setResponsesEnabled(false);
		content.getState().setRoundEndTimestamp(null);
	}

	private void resetContentState(final Content content) {
		content.getState().setResponsesEnabled(true);
		content.getState().setRound(1);
		content.getState().setRoundEndTimestamp(null);
	}
}
