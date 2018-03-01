package de.thm.arsnova.services;

import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Room;
import de.thm.arsnova.entities.migration.v2.ClientAuthentication;
import de.thm.arsnova.events.PiRoundCancelEvent;
import de.thm.arsnova.events.PiRoundDelayedStartEvent;
import de.thm.arsnova.events.PiRoundEndEvent;
import de.thm.arsnova.events.PiRoundResetEvent;
import de.thm.arsnova.persistance.AnswerRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.RoomRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class TimerServiceImpl implements TimerService, ApplicationEventPublisherAware {
	private HashMap<String, Timer> timerList = new HashMap<>();
	private UserService userService;
	private RoomRepository roomRepository;
	private ContentRepository contentRepository;
	private AnswerRepository answerRepository;
	private ApplicationEventPublisher publisher;

	public TimerServiceImpl(final UserService userService, final RoomRepository roomRepository,
			final ContentRepository contentRepository, final AnswerRepository answerRepository) {
		this.userService = userService;
		this.roomRepository = roomRepository;
		this.contentRepository = contentRepository;
		this.answerRepository = answerRepository;
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#contentId, 'content', 'owner')")
	public void startNewRound(final String contentId, ClientAuthentication user) {
		final Content content = contentRepository.findOne(contentId);
		final Room room = roomRepository.findOne(content.getRoomId());

		if (null == user) {
			user = userService.getCurrentUser();
		}

		cancelDelayedRoundChange(contentId);

		content.getState().setRoundEndTimestamp(null);
		content.getState().setResponsesEnabled(false);
		updateRoundManagementState(content);
		contentRepository.save(content);

		this.publisher.publishEvent(new PiRoundEndEvent(this, room, content));
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	public void startNewRoundDelayed(final String contentId, final int time) {
		final ClientAuthentication user = userService.getCurrentUser();
		final Content content = contentRepository.findOne(contentId);
		final Room room = roomRepository.findOne(content.getRoomId());

		final Date date = new Date();
		final Timer timer = new Timer();
		final Date endDate = new Date(date.getTime() + (time * 1000));
		updateRoundStartVariables(content, date, endDate);
		contentRepository.save(content);

		this.publisher.publishEvent(new PiRoundDelayedStartEvent(this, room, content));
		timerList.put(contentId, timer);

		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				startNewRound(contentId, user);
			}
		}, endDate);
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	public void cancelRoundChange(final String contentId) {
		final Content content = contentRepository.findOne(contentId);
		final Room room = roomRepository.findOne(content.getRoomId());

		cancelDelayedRoundChange(contentId);
		resetRoundManagementState(content);

		if (content.getState().getRound() > 1) {
			content.getState().setRound(content.getState().getRound() - 1);
		}
		content.getState().setRoundEndTimestamp(null);

		contentRepository.save(content);
		this.publisher.publishEvent(new PiRoundCancelEvent(this, room, content));
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
		final Content content = contentRepository.findOne(contentId);
		final Room room = roomRepository.findOne(content.getRoomId());
		cancelDelayedRoundChange(contentId);

		if ("freetext".equals(content.getFormat())) {
			content.getState().setRound(0);
		} else {
			content.getState().setRound(1);
		}

		resetRoundManagementState(content);
		answerRepository.deleteByContentId(content.getId());
		contentRepository.save(content);
		this.publisher.publishEvent(new PiRoundResetEvent(this, room, content));
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

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.publisher = applicationEventPublisher;
	}
}
