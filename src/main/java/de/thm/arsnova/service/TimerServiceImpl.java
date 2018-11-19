package de.thm.arsnova.service;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.persistence.RoomRepository;
import de.thm.arsnova.security.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class TimerServiceImpl implements TimerService {
	private HashMap<String, Timer> timerList = new HashMap<>();
	private UserService userService;
	private RoomRepository roomRepository;
	private ContentService contentService;
	private AnswerRepository answerRepository;

	public TimerServiceImpl(final UserService userService, final RoomRepository roomRepository,
			final ContentService contentService, final AnswerRepository answerRepository) {
		this.userService = userService;
		this.roomRepository = roomRepository;
		this.contentService = contentService;
		this.answerRepository = answerRepository;
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#contentId, 'content', 'owner')")
	public void startNewRound(final String contentId) {
		final Content content = contentService.get(contentId);
		final Room room = roomRepository.findOne(content.getRoomId());

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
		final Room room = roomRepository.findOne(content.getRoomId());

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
		final Room room = roomRepository.findOne(content.getRoomId());

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
		Timer timer = timerList.get(contentId);

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
		final Room room = roomRepository.findOne(content.getRoomId());
		cancelDelayedRoundChange(contentId);

		if (Content.Format.TEXT == content.getFormat()) {
			content.getState().setRound(0);
		} else {
			content.getState().setRound(1);
		}

		resetRoundManagementState(content);
		answerRepository.deleteByContentId(content.getId());
		contentService.update(content);
	}

	private void updateRoundStartVariables(final Content content, final Date start, final Date end) {
		if (content.getState().getRound() == 1 && content.getState().getRoundEndTimestamp() == null) {
			content.getState().setRound(2);
		}

		content.getState().setVisible(true);
		content.getState().setSolutionVisible(false);
		content.getState().setResponsesVisible(false);
		content.getState().setResponsesEnabled(true);
		content.getState().setRoundEndTimestamp(end);
	}

	private void updateRoundManagementState(final Content content) {
		if (content.getState().getRoundEndTimestamp() != null && new Date().compareTo(content.getState().getRoundEndTimestamp()) > 0) {
			content.getState().setRoundEndTimestamp(null);
		}
	}

	private void resetRoundManagementState(final Content content) {
		content.getState().setSolutionVisible(false);
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
