/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
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
package de.thm.arsnova.services;

import de.thm.arsnova.entities.transport.AnswerQueueElement;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.util.ImageUtils;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.*;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.persistance.AnswerRepository;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.SessionRepository;
import org.ektorp.DbAccessException;
import org.ektorp.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Performs all content and answer related operations.
 */
@Service
public class ContentServiceImpl extends DefaultEntityServiceImpl<Content> implements ContentService, ApplicationEventPublisherAware {
	private UserService userService;

	private LogEntryRepository dbLogger;

	private SessionRepository sessionRepository;

	private ContentRepository contentRepository;

	private AnswerRepository answerRepository;

	private ImageUtils imageUtils;

	@Value("${upload.filesize_b}")
	private int uploadFileSizeByte;

	private ApplicationEventPublisher publisher;

	private static final Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

	private HashMap<String, Timer> timerList = new HashMap<>();

	private final Queue<AnswerQueueElement> answerQueue = new ConcurrentLinkedQueue<>();

	public ContentServiceImpl(
			ContentRepository repository,
			AnswerRepository answerRepository,
			SessionRepository sessionRepository,
			LogEntryRepository dbLogger,
			UserService userService,
			ImageUtils imageUtils,
			@Qualifier("defaultJsonMessageConverter") MappingJackson2HttpMessageConverter jackson2HttpMessageConverter) {
		super(Content.class, repository, jackson2HttpMessageConverter.getObjectMapper());
		this.contentRepository = repository;
		this.answerRepository = answerRepository;
		this.sessionRepository = sessionRepository;
		this.dbLogger = dbLogger;
		this.userService = userService;
		this.imageUtils = imageUtils;
	}

	@Scheduled(fixedDelay = 5000)
	public void flushAnswerQueue() {
		if (answerQueue.isEmpty()) {
			// no need to send an empty bulk request.
			return;
		}

		final List<Answer> answerList = new ArrayList<>();
		final List<AnswerQueueElement> elements = new ArrayList<>();
		AnswerQueueElement entry;
		while ((entry = this.answerQueue.poll()) != null) {
			final Answer answer = entry.getAnswer();
			answerList.add(answer);
			elements.add(entry);
		}
		try {
			answerRepository.save(answerList);

			// Send NewAnswerEvents ...
			for (AnswerQueueElement e : elements) {
				this.publisher.publishEvent(new NewAnswerEvent(this, e.getSession(), e.getAnswer(), e.getUser(), e.getQuestion()));
			}
		} catch (final DbAccessException e) {
			logger.error("Could not bulk save answers from queue.", e);
		}
	}

	@Cacheable("contents")
	@Override
	public Content get(final String id) {
		try {
			final Content content = super.get(id);
			if (!"freetext".equals(content.getQuestionType()) && 0 == content.getPiRound()) {
			/* needed for legacy questions whose piRound property has not been set */
				content.setPiRound(1);
			}
			content.updateRoundManagementState();
			//content.setSessionKeyword(sessionRepository.getSessionFromId(content.getSessionId()).getKeyword());

			return content;
		} catch (final DocumentNotFoundException e) {
			logger.error("Could not get question {}.", id, e);
		}

		return null;
	}

	@Override
	@Caching(evict = {@CacheEvict(value = "contentlists", key = "#sessionId"),
			@CacheEvict(value = "lecturecontentlists", key = "#sessionId", condition = "#content.getQuestionVariant().equals('lecture')"),
			@CacheEvict(value = "preparationcontentlists", key = "#sessionId", condition = "#content.getQuestionVariant().equals('preparation')"),
			@CacheEvict(value = "flashcardcontentlists", key = "#sessionId", condition = "#content.getQuestionVariant().equals('flashcard')") },
			put = {@CachePut(value = "contents", key = "#content.id")})
	public Content save(final String sessionId, final Content content) {
		content.setSessionId(sessionId);
		try {
			contentRepository.save(content);

			return content;
		} catch (final IllegalArgumentException e) {
			logger.error("Could not save content {}.", content, e);
		}

		return null;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@Caching(evict = {
			@CacheEvict(value = "contentlists", allEntries = true),
			@CacheEvict(value = "lecturecontentlists", allEntries = true, condition = "#content.getQuestionVariant().equals('lecture')"),
			@CacheEvict(value = "preparationcontentlists", allEntries = true, condition = "#content.getQuestionVariant().equals('preparation')"),
			@CacheEvict(value = "flashcardcontentlists", allEntries = true, condition = "#content.getQuestionVariant().equals('flashcard')") },
			put = {@CachePut(value = "contents", key = "#content.id")})
	public Content update(final Content content) {
		final User user = userService.getCurrentUser();
		final Content oldContent = contentRepository.findOne(content.getId());
		if (null == oldContent) {
			throw new NotFoundException();
		}

		final Session session = sessionRepository.findOne(content.getSessionId());
		if (user == null || session == null || !session.isCreator(user)) {
			throw new UnauthorizedException();
		}

		if ("freetext".equals(content.getQuestionType())) {
			content.setPiRound(0);
		} else if (content.getPiRound() < 1 || content.getPiRound() > 2) {
			content.setPiRound(oldContent.getPiRound() > 0 ? oldContent.getPiRound() : 1);
		}

		content.setId(oldContent.getId());
		content.setRevision(oldContent.getRevision());
		content.updateRoundManagementState();
		contentRepository.save(content);

		if (!oldContent.isActive() && content.isActive()) {
			final UnlockQuestionEvent event = new UnlockQuestionEvent(this, session, content);
			this.publisher.publishEvent(event);
		} else if (oldContent.isActive() && !content.isActive()) {
			final LockQuestionEvent event = new LockQuestionEvent(this, session, content);
			this.publisher.publishEvent(event);
		}
		return content;
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("contentlists")
	public List<Content> getBySessionKey(final String sessionkey) {
		final Session session = getSession(sessionkey);
		final User user = userService.getCurrentUser();
		if (session.isCreator(user)) {
			return contentRepository.findBySessionIdForSpeaker(session.getId());
		} else {
			return contentRepository.findBySessionIdForUsers(session.getId());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countBySessionKey(final String sessionkey) {
		final Session session = sessionRepository.findByKeyword(sessionkey);
		return contentRepository.countBySessionId(session.getId());
	}

	/* FIXME: #content.getSessionKeyword() cannot be checked since keyword is no longer set for content. */
	@Override
	@PreAuthorize("hasPermission(#content.getSessionKeyword(), 'session', 'owner')")
	public Content save(final Content content) {
		final Session session = sessionRepository.findByKeyword(content.getSessionKeyword());
		content.setSessionId(session.getId());
		content.setTimestamp(System.currentTimeMillis() / 1000L);

		if ("freetext".equals(content.getQuestionType())) {
			content.setPiRound(0);
		} else if (content.getPiRound() < 1 || content.getPiRound() > 2) {
			content.setPiRound(1);
		}

		// convert imageurl to base64 if neccessary
		if ("grid".equals(content.getQuestionType()) && !content.getImage().startsWith("http")) {
			// base64 adds offset to filesize, formula taken from: http://en.wikipedia.org/wiki/Base64#MIME
			final int fileSize = (int) ((content.getImage().length() - 814) / 1.37);
			if (fileSize > uploadFileSizeByte) {
				logger.error("Could not save file. File is too large with {} Byte.", fileSize);
				throw new BadRequestException();
			}
		}

		final Content result = save(session.getId(), content);

		final NewQuestionEvent event = new NewQuestionEvent(this, session, result);
		this.publisher.publishEvent(event);

		return result;
	}

	/* TODO: Only evict cache entry for the content's session. This requires some refactoring. */
	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	@Caching(evict = {
			@CacheEvict("answerlists"),
			@CacheEvict(value = "contents", key = "#contentId"),
			@CacheEvict(value = "contentlists", allEntries = true),
			@CacheEvict(value = "lecturecontentlists", allEntries = true /*, condition = "#content.getQuestionVariant().equals('lecture')"*/),
			@CacheEvict(value = "preparationcontentlists", allEntries = true /*, condition = "#content.getQuestionVariant().equals('preparation')"*/),
			@CacheEvict(value = "flashcardcontentlists", allEntries = true /*, condition = "#content.getQuestionVariant().equals('flashcard')"*/) })
	public void delete(final String contentId) {
		final Content content = contentRepository.findOne(contentId);
		if (content == null) {
			throw new NotFoundException();
		}

		final Session session = sessionRepository.findOne(content.getSessionId());
		if (session == null) {
			throw new UnauthorizedException();
		}

		try {
			final int count = answerRepository.deleteByContentId(contentId);
			contentRepository.delete(contentId);
			dbLogger.log("delete", "type", "content", "answerCount", count);
		} catch (final IllegalArgumentException e) {
			logger.error("Could not delete content {}.", contentId, e);
		}

		final DeleteQuestionEvent event = new DeleteQuestionEvent(this, session, content);
		this.publisher.publishEvent(event);
	}

	@PreAuthorize("hasPermission(#session, 'owner')")
	@Caching(evict = {
			@CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#session.getId()"),
			@CacheEvict(value = "lecturecontentlists", key = "#session.getId()", condition = "'lecture'.equals(#variant)"),
			@CacheEvict(value = "preparationcontentlists", key = "#session.getId()", condition = "'preparation'.equals(#variant)"),
			@CacheEvict(value = "flashcardcontentlists", key = "#session.getId()", condition = "'flashcard'.equals(#variant)") })
	private void deleteBySessionAndVariant(final Session session, final String variant) {
		final List<String> contentIds;
		if ("all".equals(variant)) {
			contentIds = contentRepository.findIdsBySessionId(session.getId());
		} else {
			contentIds = contentRepository.findIdsBySessionIdAndVariant(session.getId(), variant);
		}

		final int answerCount = answerRepository.deleteByContentIds(contentIds);
		final int contentCount = contentRepository.deleteBySessionId(session.getId());
		dbLogger.log("delete", "type", "question", "questionCount", contentCount);
		dbLogger.log("delete", "type", "answer", "answerCount", answerCount);

		final DeleteAllQuestionsEvent event = new DeleteAllQuestionsEvent(this, session);
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllContent(final String sessionkey) {
		final Session session = getSessionWithAuthCheck(sessionkey);
		deleteBySessionAndVariant(session, "all");
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteLectureQuestions(final String sessionkey) {
		final Session session = getSessionWithAuthCheck(sessionkey);
		deleteBySessionAndVariant(session, "lecture");
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deletePreparationQuestions(final String sessionkey) {
		final Session session = getSessionWithAuthCheck(sessionkey);
		deleteBySessionAndVariant(session, "preparation");
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteFlashcards(final String sessionkey) {
		final Session session = getSessionWithAuthCheck(sessionkey);
		deleteBySessionAndVariant(session, "flashcard");
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#questionId, 'content', 'owner')")
	public void startNewPiRound(final String questionId, User user) {
		final Content content = contentRepository.findOne(questionId);
		final Session session = sessionRepository.findOne(content.getSessionId());

		if (null == user) {
			user = userService.getCurrentUser();
		}

		cancelDelayedPiRoundChange(questionId);

		content.setPiRoundEndTime(0);
		content.setVotingDisabled(true);
		content.updateRoundManagementState();
		update(content);

		this.publisher.publishEvent(new PiRoundEndEvent(this, session, content));
	}

	@Override
	@PreAuthorize("hasPermission(#questionId, 'content', 'owner')")
	public void startNewPiRoundDelayed(final String questionId, final int time) {
		final ContentService contentService = this;
		final User user = userService.getCurrentUser();
		final Content content = contentRepository.findOne(questionId);
		final Session session = sessionRepository.findOne(content.getSessionId());

		final Date date = new Date();
		final Timer timer = new Timer();
		final Date endDate = new Date(date.getTime() + (time * 1000));
		content.updateRoundStartVariables(date, endDate);
		update(content);

		this.publisher.publishEvent(new PiRoundDelayedStartEvent(this, session, content));
		timerList.put(questionId, timer);

		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				contentService.startNewPiRound(questionId, user);
			}
		}, endDate);
	}

	@Override
	@PreAuthorize("hasPermission(#questionId, 'content', 'owner')")
	public void cancelPiRoundChange(final String questionId) {
		final Content content = contentRepository.findOne(questionId);
		final Session session = sessionRepository.findOne(content.getSessionId());

		cancelDelayedPiRoundChange(questionId);
		content.resetRoundManagementState();

		if (0 == content.getPiRound() || 1 == content.getPiRound()) {
			content.setPiRoundFinished(false);
		} else {
			content.setPiRound(1);
			content.setPiRoundFinished(true);
		}

		update(content);
		this.publisher.publishEvent(new PiRoundCancelEvent(this, session, content));
	}

	@Override
	public void cancelDelayedPiRoundChange(final String questionId) {
		Timer timer = timerList.get(questionId);

		if (null != timer) {
			timer.cancel();
			timerList.remove(questionId);
			timer.purge();
		}
	}

	@Override
	@PreAuthorize("hasPermission(#questionId, 'content', 'owner')")
	@CacheEvict("answerlists")
	public void resetPiRoundState(final String questionId) {
		final Content content = contentRepository.findOne(questionId);
		final Session session = sessionRepository.findOne(content.getSessionId());
		cancelDelayedPiRoundChange(questionId);

		if ("freetext".equals(content.getQuestionType())) {
			content.setPiRound(0);
		} else {
			content.setPiRound(1);
		}

		content.resetRoundManagementState();
		answerRepository.deleteByContentId(content.getId());
		update(content);
		this.publisher.publishEvent(new PiRoundResetEvent(this, session, content));
	}

	@Override
	@PreAuthorize("hasPermission(#questionId, 'content', 'owner')")
	public void setVotingAdmission(final String questionId, final boolean disableVoting) {
		final Content content = contentRepository.findOne(questionId);
		final Session session = sessionRepository.findOne(content.getSessionId());
		content.setVotingDisabled(disableVoting);

		if (!disableVoting && !content.isActive()) {
			content.setActive(true);
			update(content);
		} else {
			update(content);
		}
		ArsnovaEvent event;
		if (disableVoting) {
			event = new LockVoteEvent(this, session, content);
		} else {
			event = new UnlockVoteEvent(this, session, content);
		}
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@Caching(evict = { @CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#sessionId"),
			@CacheEvict(value = "lecturecontentlists", key = "#sessionId"),
			@CacheEvict(value = "preparationcontentlists", key = "#sessionId"),
			@CacheEvict(value = "flashcardcontentlists", key = "#sessionId") })
	public void setVotingAdmissions(final String sessionkey, final boolean disableVoting, List<Content> contents) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		for (final Content q : contents) {
			if (!"flashcard".equals(q.getQuestionType())) {
				q.setVotingDisabled(disableVoting);
			}
		}
		ArsnovaEvent event;
		if (disableVoting) {
			event = new LockVotesEvent(this, session, contents);
		} else {
			event = new UnlockVotesEvent(this, session, contents);
		}
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void setVotingAdmissionForAllQuestions(final String sessionkey, final boolean disableVoting) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		final List<Content> contents = contentRepository.findBySessionId(session.getId());
		setVotingAdmissionForAllQuestions(session.getId(), disableVoting);
	}

	private Session getSessionWithAuthCheck(final String sessionKeyword) {
		final User user = userService.getCurrentUser();
		final Session session = sessionRepository.findByKeyword(sessionKeyword);
		if (user == null || session == null || !session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		return session;
	}

	@Override
	@PreAuthorize("hasPermission(#questionId, 'content', 'owner')")
	public void deleteAnswers(final String questionId) {
		final Content content = contentRepository.findOne(questionId);
		content.resetQuestionState();
		update(content);
		answerRepository.deleteByContentId(content.getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredQuestionIds(final String sessionKey) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionKey);
		return contentRepository.findUnansweredIdsBySessionIdAndUser(session.getId(), user);
	}

	private User getCurrentUser() {
		final User user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		return user;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer getMyAnswer(final String questionId) {
		final Content content = get(questionId);
		if (content == null) {
			throw new NotFoundException();
		}
		return answerRepository.findByQuestionIdUserPiRound(questionId, userService.getCurrentUser(), content.getPiRound());
	}

	@Override
	public void getFreetextAnswerAndMarkRead(final String answerId, final User user) {
		final Answer answer = answerRepository.findOne(answerId);
		if (answer == null) {
			throw new NotFoundException();
		}
		if (answer.isRead()) {
			return;
		}
		final Session session = sessionRepository.findOne(answer.getSessionId());
		if (session.isCreator(user)) {
			answer.setRead(true);
			answerRepository.save(answer);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAnswers(final String questionId, final int piRound, final int offset, final int limit) {
		final Content content = contentRepository.findOne(questionId);
		if (content == null) {
			throw new NotFoundException();
		}
		return "freetext".equals(content.getQuestionType())
				? getFreetextAnswersByQuestionId(questionId, offset, limit)
						: answerRepository.findByContentIdPiRound(content.getId(), piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAnswers(final String questionId, final int offset, final int limit) {
		final Content content = get(questionId);
		if (content == null) {
			throw new NotFoundException();
		}
		if ("freetext".equals(content.getQuestionType())) {
			return getFreetextAnswersByQuestionId(questionId, offset, limit);
		} else {
			return answerRepository.findByContentIdPiRound(content.getId(), content.getPiRound());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAllAnswers(final String questionId, final int offset, final int limit) {
		final Content content = get(questionId);
		if (content == null) {
			throw new NotFoundException();
		}
		if ("freetext".equals(content.getQuestionType())) {
			return getFreetextAnswersByQuestionId(questionId, offset, limit);
		} else {
			return answerRepository.findByContentId(content.getId());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countAnswersByQuestionIdAndRound(final String questionId) {
		final Content content = get(questionId);
		if (content == null) {
			return 0;
		}

		if ("freetext".equals(content.getQuestionType())) {
			return answerRepository.countByContentId(content.getId());
		} else {
			return answerRepository.countByContentIdRound(content.getId(), content.getPiRound());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countAnswersByQuestionIdAndRound(final String questionId, final int piRound) {
		final Content content = get(questionId);
		if (content == null) {
			return 0;
		}

		return answerRepository.countByContentIdRound(content.getId(), piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countTotalAbstentionsByQuestionId(final String questionId) {
		final Content content = get(questionId);
		if (content == null) {
			return 0;
		}

		return answerRepository.countByContentId(questionId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countTotalAnswersByQuestionId(final String questionId) {
		final Content content = get(questionId);
		if (content == null) {
			return 0;
		}

		return answerRepository.countByContentId(content.getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getFreetextAnswersByQuestionId(final String questionId, final int offset, final int limit) {
		final List<Answer> answers = answerRepository.findByContentId(questionId, offset, limit);
		if (answers == null) {
			throw new NotFoundException();
		}
		/* Remove user for privacy concerns */
		for (Answer answer : answers) {
			answer.setUser(null);
		}

		return answers;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getMyAnswersBySessionKey(final String sessionKey) {
		final Session session = getSession(sessionKey);
		// Load contents first because we are only interested in answers of the latest piRound.
		final List<Content> contents = getBySessionKey(sessionKey);
		final Map<String, Content> questionIdToQuestion = new HashMap<>();
		for (final Content content : contents) {
			questionIdToQuestion.put(content.getId(), content);
		}

		/* filter answers by active piRound per question */
		final List<Answer> answers = answerRepository.findByUserSessionId(userService.getCurrentUser(), session.getId());
		final List<Answer> filteredAnswers = new ArrayList<>();
		for (final Answer answer : answers) {
			final Content content = questionIdToQuestion.get(answer.getQuestionId());
			if (content == null) {
				// Content is not present. Most likely it has been locked by the
				// Session's creator. Locked Questions do not appear in this list.
				continue;
			}
			if (0 == answer.getPiRound() && !"freetext".equals(content.getQuestionType())) {
				answer.setPiRound(1);
			}

			// discard all answers that aren't in the same piRound as the content
			if (answer.getPiRound() == content.getPiRound()) {
				filteredAnswers.add(answer);
			}
		}

		return filteredAnswers;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countTotalAnswersBySessionKey(final String sessionKey) {
		return answerRepository.countBySessionKey(sessionKey);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", key = "#contentId")
	public Answer saveAnswer(final String contentId, final Answer answer) {
		final User user = getCurrentUser();
		final Content content = get(contentId);
		if (content == null) {
			throw new NotFoundException();
		}
		final Session session = sessionRepository.findOne(content.getSessionId());

		answer.setUser(user.getUsername());
		answer.setQuestionId(content.getId());
		answer.setSessionId(session.getId());
		answer.setQuestionVariant(content.getQuestionVariant());
		answer.setQuestionValue(content.calculateValue(answer));
		answer.setTimestamp(new Date().getTime());

		if ("freetext".equals(content.getQuestionType())) {
			answer.setPiRound(0);
			imageUtils.generateThumbnailImage(answer);
			if (content.isFixedAnswer() && content.getText() != null) {
				answer.setAnswerTextRaw(answer.getAnswerText());

				if (content.isStrictMode()) {
					content.checkTextStrictOptions(answer);
				}
				answer.setQuestionValue(content.evaluateCorrectAnswerFixedText(answer.getAnswerTextRaw()));
				answer.setSuccessfulFreeTextAnswer(content.isSuccessfulFreeTextAnswer(answer.getAnswerTextRaw()));
			}
		} else {
			answer.setPiRound(content.getPiRound());
		}

		this.answerQueue.offer(new AnswerQueueElement(session, content, answer, user));

		return answer;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", allEntries = true)
	public Answer updateAnswer(final Answer answer) {
		final User user = userService.getCurrentUser();
		final Answer realAnswer = this.getMyAnswer(answer.getQuestionId());
		if (user == null || realAnswer == null || !user.getUsername().equals(realAnswer.getUser())) {
			throw new UnauthorizedException();
		}

		final Content content = get(answer.getQuestionId());
		if ("freetext".equals(content.getQuestionType())) {
			imageUtils.generateThumbnailImage(realAnswer);
			content.checkTextStrictOptions(realAnswer);
		}
		final Session session = sessionRepository.findOne(content.getSessionId());
		answer.setUser(user.getUsername());
		answer.setQuestionId(content.getId());
		answer.setSessionId(session.getId());
		answerRepository.save(realAnswer);
		this.publisher.publishEvent(new NewAnswerEvent(this, session, answer, user, content));

		return answer;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAnswer(final String questionId, final String answerId) {
		final Content content = contentRepository.findOne(questionId);
		if (content == null) {
			throw new NotFoundException();
		}
		final User user = userService.getCurrentUser();
		final Session session = sessionRepository.findOne(content.getSessionId());
		if (user == null || session == null || !session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		answerRepository.delete(answerId);

		this.publisher.publishEvent(new DeleteAnswerEvent(this, session, content));
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("lecturecontentlists")
	public List<Content> getLectureQuestions(final String sessionkey) {
		final Session session = getSession(sessionkey);
		final User user = userService.getCurrentUser();
		if (session.isCreator(user)) {
			return contentRepository.findBySessionIdOnlyLectureVariant(session.getId());
		} else {
			return contentRepository.findBySessionIdOnlyLectureVariantAndActive(session.getId());
		}
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("flashcardcontentlists")
	public List<Content> getFlashcards(final String sessionkey) {
		final Session session = getSession(sessionkey);
		final User user = userService.getCurrentUser();
		if (session.isCreator(user)) {
			return contentRepository.findBySessionIdOnlyFlashcardVariant(session.getId());
		} else {
			return contentRepository.findBySessionIdOnlyFlashcardVariantAndActive(session.getId());
		}
	}

	/* FIXME: caching */
	@Override
	@PreAuthorize("isAuthenticated()")
	//@Cacheable("preparationcontentlists")
	public List<Content> getPreparationQuestions(final String sessionkey) {
		final Session session = getSession(sessionkey);
		final User user = userService.getCurrentUser();
		if (session.isCreator(user)) {
			return contentRepository.findBySessionIdOnlyPreparationVariant(session.getId());
		} else {
			return contentRepository.findBySessionIdOnlyPreparationVariantAndActive(session.getId());
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Content> replaceImageData(final List<Content> contents) {
		for (Content q : contents) {
			if (q.getImage() != null && q.getImage().startsWith("data:image/")) {
				q.setImage("true");
			}
		}

		return contents;
	}

	private Session getSession(final String sessionkey) {
		final Session session = sessionRepository.findByKeyword(sessionkey);
		if (session == null) {
			throw new NotFoundException();
		}
		return session;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countLectureQuestions(final String sessionkey) {
		return contentRepository.countLectureVariantBySessionId(getSession(sessionkey).getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countFlashcards(final String sessionkey) {
		return contentRepository.countFlashcardVariantBySessionId(getSession(sessionkey).getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countPreparationQuestions(final String sessionkey) {
		return contentRepository.countPreparationVariantBySessionId(getSession(sessionkey).getId());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countLectureQuestionAnswers(final String sessionkey) {
		return this.countLectureQuestionAnswersInternal(sessionkey);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countLectureQuestionAnswersInternal(final String sessionkey) {
		return answerRepository.countBySessionIdLectureVariant(getSession(sessionkey).getId());
	}

	@Override
	public Map<String, Object> countAnswersAndAbstentionsInternal(final String questionId) {
		final Content content = get(questionId);
		HashMap<String, Object> map = new HashMap<>();

		if (content == null) {
			return null;
		}

		map.put("_id", questionId);
		map.put("answers", answerRepository.countByContentIdRound(content.getId(), content.getPiRound()));
		map.put("abstentions", answerRepository.countByContentId(questionId));

		return map;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countPreparationQuestionAnswers(final String sessionkey) {
		return this.countPreparationQuestionAnswersInternal(sessionkey);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countPreparationQuestionAnswersInternal(final String sessionkey) {
		return answerRepository.countBySessionIdPreparationVariant(getSession(sessionkey).getId());
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public int countFlashcardsForUserInternal(final String sessionkey) {
		return contentRepository.findBySessionIdOnlyFlashcardVariantAndActive(getSession(sessionkey).getId()).size();
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredLectureQuestionIds(final String sessionkey) {
		final User user = getCurrentUser();
		return this.getUnAnsweredLectureQuestionIds(sessionkey, user);
	}

	@Override
	public List<String> getUnAnsweredLectureQuestionIds(final String sessionkey, final User user) {
		final Session session = getSession(sessionkey);
		return contentRepository.findUnansweredIdsBySessionIdAndUserOnlyLectureVariant(session.getId(), user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredPreparationQuestionIds(final String sessionkey) {
		final User user = getCurrentUser();
		return this.getUnAnsweredPreparationQuestionIds(sessionkey, user);
	}

	@Override
	public List<String> getUnAnsweredPreparationQuestionIds(final String sessionkey, final User user) {
		final Session session = getSession(sessionkey);
		return contentRepository.findUnansweredIdsBySessionIdAndUserOnlyPreparationVariant(session.getId(), user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void publishAll(final String sessionkey, final boolean publish) {
		/* TODO: resolve redundancies */
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		final List<Content> contents = contentRepository.findBySessionId(session.getId());
		publishQuestions(sessionkey, publish, contents);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@Caching(evict = { @CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#sessionId"),
			@CacheEvict(value = "lecturecontentlists", key = "#sessionId"),
			@CacheEvict(value = "preparationcontentlists", key = "#sessionId"),
			@CacheEvict(value = "flashcardcontentlists", key = "#sessionId") })
	public void publishQuestions(final String sessionkey, final boolean publish, List<Content> contents) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		for (final Content content : contents) {
			content.setActive(publish);
		}
		contentRepository.save(contents);
		ArsnovaEvent event;
		if (publish) {
			event = new UnlockQuestionsEvent(this, session, contents);
		} else {
			event = new LockQuestionsEvent(this, session, contents);
		}
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllQuestionsAnswers(final String sessionkey) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}

		final List<Content> contents = contentRepository.findBySessionIdAndVariantAndActive(session.getId());
		resetContentsRoundState(session.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerRepository.deleteAllAnswersForQuestions(contentIds);

		this.publisher.publishEvent(new DeleteAllQuestionsAnswersEvent(this, session));
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllPreparationAnswers(String sessionkey) {
		final Session session = getSession(sessionkey);

		final List<Content> contents = contentRepository.findBySessionIdAndVariantAndActive(session.getId(), "preparation");
		resetContentsRoundState(session.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerRepository.deleteAllAnswersForQuestions(contentIds);

		this.publisher.publishEvent(new DeleteAllPreparationAnswersEvent(this, session));
	}

	/* TODO: Only evict cache entry for the answer's question. This requires some refactoring. */
	@Override
	@PreAuthorize("hasPermission(#sessionkey, 'session', 'owner')")
	@CacheEvict(value = "answerlists", allEntries = true)
	public void deleteAllLectureAnswers(String sessionkey) {
		final Session session = getSession(sessionkey);

		final List<Content> contents = contentRepository.findBySessionIdAndVariantAndActive(session.getId(), "lecture");
		resetContentsRoundState(session.getId(), contents);
		final List<String> contentIds = contents.stream().map(Content::getId).collect(Collectors.toList());
		answerRepository.deleteAllAnswersForQuestions(contentIds);

		this.publisher.publishEvent(new DeleteAllLectureAnswersEvent(this, session));
	}

	@Caching(evict = {
			@CacheEvict(value = "contents", allEntries = true),
			@CacheEvict(value = "contentlists", key = "#sessionId"),
			@CacheEvict(value = "lecturecontentlists", key = "#sessionId"),
			@CacheEvict(value = "preparationcontentlists", key = "#sessionId"),
			@CacheEvict(value = "flashcardcontentlists", key = "#sessionId") })
	private void resetContentsRoundState(final String sessionId, final List<Content> contents) {
		for (final Content q : contents) {
			/* TODO: Check if setting the sessionId is necessary. */
			q.setSessionId(sessionId);
			q.resetQuestionState();
		}
		contentRepository.save(contents);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public String getImage(String questionId, String answerId) {
		final List<Answer> answers = getAnswers(questionId, -1, -1);
		Answer answer = null;

		for (Answer a : answers) {
			if (answerId.equals(a.getId())) {
				answer = a;
				break;
			}
		}

		if (answer == null) {
			throw new NotFoundException();
		}

		return answer.getAnswerImage();
	}

	@Override
	public String getQuestionImage(String questionId) {
		Content content = contentRepository.findOne(questionId);
		String imageData = content.getImage();

		if (imageData == null) {
			imageData = "";
		}

		return imageData;
	}

	@Override
	public String getQuestionFcImage(String questionId) {
		Content content = contentRepository.findOne(questionId);
		String imageData = content.getFcImage();

		if (imageData == null) {
			imageData = "";
		}

		return imageData;
	}
}
