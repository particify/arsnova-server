/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.ImageUtils;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SortOrder;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.events.DeleteAllLectureAnswersEvent;
import de.thm.arsnova.events.DeleteAllPreparationAnswersEvent;
import de.thm.arsnova.events.DeleteAllQuestionsAnswersEvent;
import de.thm.arsnova.events.DeleteAllQuestionsEvent;
import de.thm.arsnova.events.DeleteAnswerEvent;
import de.thm.arsnova.events.DeleteInterposedQuestionEvent;
import de.thm.arsnova.events.DeleteQuestionEvent;
import de.thm.arsnova.events.LockQuestionEvent;
import de.thm.arsnova.events.LockQuestionsEvent;
import de.thm.arsnova.events.LockVoteEvent;
import de.thm.arsnova.events.LockVotesEvent;
import de.thm.arsnova.events.NewAnswerEvent;
import de.thm.arsnova.events.NewInterposedQuestionEvent;
import de.thm.arsnova.events.NewQuestionEvent;
import de.thm.arsnova.events.UnlockQuestionEvent;
import de.thm.arsnova.events.UnlockQuestionsEvent;
import de.thm.arsnova.events.NovaEvent;
import de.thm.arsnova.events.PiRoundCancelEvent;
import de.thm.arsnova.events.PiRoundDelayedStartEvent;
import de.thm.arsnova.events.PiRoundEndEvent;
import de.thm.arsnova.events.PiRoundResetEvent;
import de.thm.arsnova.events.UnlockVoteEvent;
import de.thm.arsnova.events.UnlockVotesEvent;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;

/**
 * Performs all question, interposed question, and answer related operations.
 */
@Service
public class QuestionService implements IQuestionService, ApplicationEventPublisherAware {

	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private IUserService userService;

	@Autowired
	private ImageUtils imageUtils;

	@Value("${upload.filesize_b}")
	private int uploadFileSizeByte;

	private ApplicationEventPublisher publisher;

	public static final Logger LOGGER = LoggerFactory.getLogger(QuestionService.class);

	private HashMap<String, Timer> timerList = new HashMap<String, Timer>();

	public void setDatabaseDao(final IDatabaseDao databaseDao) {
		this.databaseDao = databaseDao;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getSkillQuestions(final String sessionkey, final int offset, final int limit) {
		final Session session = getSession(sessionkey);
		final User user = userService.getCurrentUser();
		if (session.isCreator(user)) {
			return databaseDao.getSkillQuestionsForTeachers(session, offset, limit);
		} else {
			return databaseDao.getSkillQuestionsForUsers(session, offset, limit);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getSkillQuestionCount(final String sessionkey) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		return databaseDao.getSkillQuestionCount(session);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#question.getSessionKeyword(), 'session', 'owner')")
	public Question saveQuestion(final Question question) {
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());
		question.setSessionId(session.get_id());
		question.setTimestamp(System.currentTimeMillis() / 1000L);

		if ("freetext".equals(question.getQuestionType())) {
			question.setPiRound(0);
		} else if (question.getPiRound() < 1 || question.getPiRound() > 2) {
			question.setPiRound(1);
		}

		// convert imageurl to base64 if neccessary
		if ("grid".equals(question.getQuestionType())) {
			if (question.getImage().startsWith("http")) {
				final String base64ImageString = imageUtils.encodeImageToString(question.getImage());
				if (base64ImageString == null) {
					throw new BadRequestException();
				}
				question.setImage(base64ImageString);
			}

			// base64 adds offset to filesize, formula taken from: http://en.wikipedia.org/wiki/Base64#MIME
			final int fileSize = (int) ((question.getImage().length() - 814) / 1.37);
			if (fileSize > uploadFileSizeByte) {
				LOGGER.error("Could not save file. File is too large with " + fileSize + " Byte.");
				throw new BadRequestException();
			}
		}

		final Question result = databaseDao.saveQuestion(session, question);

		SortOrder subjectSortOrder = databaseDao.getSortOrder(session.get_id(), question.getQuestionVariant(), "");
		if (subjectSortOrder != null) {
			SortOrder questionSortOrder = databaseDao.getSortOrder(session.get_id(), question.getQuestionVariant(), question.getSubject());
			if (questionSortOrder == null) {
				List<String> s = new ArrayList<String>();
				s.add(question.get_id());
				SortOrder newQSortOrder = new SortOrder();
				newQSortOrder.setSessionId(question.getSessionId());
				newQSortOrder.setSubject(question.getSubject());
				newQSortOrder.setSortType(subjectSortOrder.getSortType());
				newQSortOrder.setQuestionVariant(subjectSortOrder.getQuestionVariant());
				newQSortOrder.setSortOrder(s);
				databaseDao.createOrUpdateSortOrder(newQSortOrder);
				addToSortOrder(subjectSortOrder, question.getSubject());
			}
			else {
				addToSortOrder(questionSortOrder, question.get_id());
			}
		}
		else {
			createSortOrder(session, question.getQuestionVariant(), "");
		}

		final NewQuestionEvent event = new NewQuestionEvent(this, session, result);
		this.publisher.publishEvent(event);

		return result;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public boolean saveQuestion(final InterposedQuestion question) {
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionId());
		final InterposedQuestion result = databaseDao.saveQuestion(session, question, userService.getCurrentUser());

		if (null != result) {
			final NewInterposedQuestionEvent event = new NewInterposedQuestionEvent(this, session, result);
			this.publisher.publishEvent(event);
			return true;
		}
		return false;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Question getQuestion(final String id) {
		final Question result = databaseDao.getQuestion(id);
		if (result == null) {
			return null;
		}
		if (!"freetext".equals(result.getQuestionType()) && 0 == result.getPiRound()) {
			/* needed for legacy questions whose piRound property has not been set */
			result.setPiRound(1);
		}

		return result;
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#questionId, 'question', 'owner')")
	public void deleteQuestion(final String questionId) {
		final Question question = databaseDao.getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}

		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());
		if (session == null) {
			throw new UnauthorizedException();
		}
		deleteQuestionFromSortOrder(question);
		databaseDao.deleteQuestionWithAnswers(question);

		final DeleteQuestionEvent event = new DeleteQuestionEvent(this, session, question);
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionKeyword, 'session', 'owner')")
	public void deleteAllQuestions(final String sessionKeyword) {
		final Session session = getSessionWithAuthCheck(sessionKeyword);
		databaseDao.deleteAllQuestionsWithAnswers(session);

		final DeleteAllQuestionsEvent event = new DeleteAllQuestionsEvent(this, session);
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#questionId, 'question', 'owner')")
	public void startNewPiRound(final String questionId, User user) {
		final Question question = databaseDao.getQuestion(questionId);
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());

		if(null == user) {
			user = userService.getCurrentUser();
		}

		cancelDelayedPiRoundChange(questionId);

		question.setPiRoundEndTime(0);
		question.setVotingDisabled(true);
		question.updateRoundManagementState();
		update(question, user);

		this.publisher.publishEvent(new PiRoundEndEvent(this, session, question));
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#questionId, 'question', 'owner')")
	public void startNewPiRoundDelayed(final String questionId, final int time) {
		final IQuestionService questionService = this;
		final User user = userService.getCurrentUser();
		final Question question = databaseDao.getQuestion(questionId);
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());

		final Date date = new Date();
		final Timer timer = new Timer();
		final Date endDate = new Date(date.getTime() + (time * 1000));
		question.updateRoundStartVariables(date, endDate);
		update(question);

		this.publisher.publishEvent(new PiRoundDelayedStartEvent(this, session, question));
		timerList.put(questionId, timer);

		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				questionService.startNewPiRound(questionId, user);
			}
		}, endDate);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#questionId, 'question', 'owner')")
	public void cancelPiRoundChange(final String questionId) {
		final Question question = databaseDao.getQuestion(questionId);
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());

		cancelDelayedPiRoundChange(questionId);
		question.resetRoundManagementState();

		if(question.getPiRound() == 1) {
			question.setPiRoundFinished(false);
		} else {
			question.setPiRound(1);
			question.setPiRoundFinished(true);
		}

		update(question);
		this.publisher.publishEvent(new PiRoundCancelEvent(this, session, question));
	}

	@Override
	public void cancelDelayedPiRoundChange(final String questionId) {
		Timer timer = timerList.get(questionId);

		if(null != timer) {
			timer.cancel();
			timerList.remove(questionId);
			timer.purge();
		}
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#questionId, 'question', 'owner')")
	public void resetPiRoundState(final String questionId) {
		final Question question = databaseDao.getQuestion(questionId);
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());
		cancelDelayedPiRoundChange(questionId);

		question.setPiRound(1);
		question.resetRoundManagementState();
		databaseDao.deleteAnswers(question);
		update(question);
		this.publisher.publishEvent(new PiRoundResetEvent(this, session, question));
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#questionId, 'question', 'owner')")
	public void setVotingAdmission(final String questionId, final boolean disableVoting) {
		final Question question = databaseDao.getQuestion(questionId);
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());
		question.setVotingDisabled(disableVoting);

		if (disableVoting == false && !question.isActive()) {
			question.setActive(true);
			update(question);
		} else {
			databaseDao.updateQuestion(question);
		}
		NovaEvent event;
		if (disableVoting) {
			event = new LockVoteEvent(this, session, question);
		} else {
			event = new UnlockVoteEvent(this, session, question);
		}
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void setVotingAdmissions(final String sessionkey, final boolean disableVoting, List<Question> questions) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.setVotingAdmissions(session, disableVoting, questions);
		NovaEvent event;
		if (disableVoting) {
			event = new LockVotesEvent(this, session, questions);
		} else {
			event = new UnlockVotesEvent(this, session, questions);
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
		final List<Question> questions = databaseDao.setVotingAdmissionForAllQuestions(session, disableVoting);
		NovaEvent event;
		if (disableVoting) {
			event = new LockVotesEvent(this, session, questions);
		} else {
			event = new UnlockVotesEvent(this, session, questions);
		}
		this.publisher.publishEvent(event);
	}

	private Session getSessionWithAuthCheck(final String sessionKeyword) {
		final User user = userService.getCurrentUser();
		final Session session = databaseDao.getSessionFromKeyword(sessionKeyword);
		if (user == null || session == null || !session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		return session;
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#questionId, 'interposedquestion', 'owner')")
	public void deleteInterposedQuestion(final String questionId) {
		final InterposedQuestion question = databaseDao.getInterposedQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		databaseDao.deleteInterposedQuestion(question);

		final Session session = databaseDao.getSessionFromKeyword(question.getSessionId());
		final DeleteInterposedQuestionEvent event = new DeleteInterposedQuestionEvent(this, session, question);
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllInterposedQuestions(final String sessionKeyword) {
		final Session session = databaseDao.getSessionFromKeyword(sessionKeyword);
		if (session == null) {
			throw new UnauthorizedException();
		}
		final User user = getCurrentUser();
		if (session.isCreator(user)) {
			databaseDao.deleteAllInterposedQuestions(session);
		} else {
			databaseDao.deleteAllInterposedQuestions(session, user);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#questionId, 'question', 'owner')")
	public void deleteAnswers(final String questionId) {
		final Question question = databaseDao.getQuestion(questionId);
		question.resetQuestionState();
		databaseDao.updateQuestion(question);
		databaseDao.deleteAnswers(question);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredQuestionIds(final String sessionKey) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionKey);
		return databaseDao.getUnAnsweredQuestionIds(session, user);
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
		final Question question = getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		return databaseDao.getMyAnswer(userService.getCurrentUser(), questionId, question.getPiRound());
	}

	@Override
	public void readFreetextAnswer(final String answerId, final User user) {
		final Answer answer = databaseDao.getObjectFromId(answerId, Answer.class);
		if (answer == null) {
			throw new NotFoundException();
		}
		if (answer.isRead()) {
			return;
		}
		final Session session = databaseDao.getSessionFromId(answer.getSessionId());
		if (session.isCreator(user)) {
			answer.setRead(true);
			databaseDao.updateAnswer(answer);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAnswers(final String questionId, final int piRound) {
		final Question question = databaseDao.getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		return "freetext".equals(question.getQuestionType())
				? getFreetextAnswers(questionId, -1, -1)
						: databaseDao.getAnswers(question, piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAnswers(final String questionId) {
		final Question question = getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		if ("freetext".equals(question.getQuestionType())) {
			return getFreetextAnswers(questionId, -1, -1);
		} else {
			return databaseDao.getAnswers(question);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAllAnswers(final String questionId) {
		final Question question = getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		if ("freetext".equals(question.getQuestionType())) {
			return getFreetextAnswers(questionId, -1, -1);
		} else {
			return databaseDao.getAllAnswers(question);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getAnswerCount(final String questionId) {
		final Question question = getQuestion(questionId);
		if (question == null) {
			return 0;
		}

		return databaseDao.getAnswerCount(question, question.getPiRound());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getAnswerCount(final String questionId, final int piRound) {
		final Question question = getQuestion(questionId);
		if (question == null) {
			return 0;
		}

		return databaseDao.getAnswerCount(question, piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getAbstentionAnswerCount(final String questionId) {
		final Question question = getQuestion(questionId);
		if (question == null) {
			return 0;
		}

		return databaseDao.getAbstentionAnswerCount(questionId);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getTotalAnswerCountByQuestion(final String questionId) {
		final Question question = getQuestion(questionId);
		if (question == null) {
			return 0;
		}

		return databaseDao.getTotalAnswerCountByQuestion(question);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getFreetextAnswers(final String questionId, final int offset, final int limit) {
		final List<Answer> answers = databaseDao.getFreetextAnswers(questionId, offset, limit);
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
	public List<Answer> getMyAnswers(final String sessionKey) {
		final Session session = getSession(sessionKey);
		// Load questions first because we are only interested in answers of the latest piRound.
		final List<Question> questions = databaseDao.getSkillQuestionsForUsers(session, -1, -1);
		final Map<String, Question> questionIdToQuestion = new HashMap<String, Question>();
		for (final Question question : questions) {
			questionIdToQuestion.put(question.get_id(), question);
		}

		/* filter answers by active piRound per question */
		final List<Answer> answers = databaseDao.getMyAnswers(userService.getCurrentUser(), session);
		final List<Answer> filteredAnswers = new ArrayList<Answer>();
		for (final Answer answer : answers) {
			final Question question = questionIdToQuestion.get(answer.getQuestionId());
			if (question == null) {
				// Question is not present. Most likely it has been locked by the
				// Session's creator. Locked Questions do not appear in this list.
				continue;
			}
			if (0 == answer.getPiRound() && !"freetext".equals(question.getQuestionType())) {
				answer.setPiRound(1);
			}

			// discard all answers that aren't in the same piRound as the question
			if (answer.getPiRound() == question.getPiRound()) {
				filteredAnswers.add(answer);
			}
		}

		return filteredAnswers;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getTotalAnswerCount(final String sessionKey) {
		return databaseDao.getTotalAnswerCount(sessionKey);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getInterposedCount(final String sessionKey) {
		return databaseDao.getInterposedCount(sessionKey);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public InterposedReadingCount getInterposedReadingCount(final String sessionKey, String username) {
		final Session session = databaseDao.getSessionFromKeyword(sessionKey);
		if (session == null) {
			throw new NotFoundException();
		}
		if (username == null) {
			return databaseDao.getInterposedReadingCount(session);
		} else {
			User currentUser = userService.getCurrentUser();
			if (!currentUser.getUsername().equals(username)) {
				throw new ForbiddenException();
			}

			return databaseDao.getInterposedReadingCount(session, currentUser);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<InterposedQuestion> getInterposedQuestions(final String sessionKey, final int offset, final int limit) {
		final Session session = this.getSession(sessionKey);
		final User user = getCurrentUser();
		if (session.isCreator(user)) {
			return databaseDao.getInterposedQuestions(session, offset, limit);
		} else {
			return databaseDao.getInterposedQuestions(session, user, offset, limit);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public InterposedQuestion readInterposedQuestion(final String questionId) {
		final User user = userService.getCurrentUser();
		return this.readInterposedQuestionInternal(questionId, user);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public InterposedQuestion readInterposedQuestionInternal(final String questionId, User user) {
		final InterposedQuestion question = databaseDao.getInterposedQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionId());
		if (!question.isCreator(user) && !session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		if (session.isCreator(user)) {
			databaseDao.markInterposedQuestionAsRead(question);
		}
		return question;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Question update(final Question question) {
		final User user = userService.getCurrentUser();
		return update(question, user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Question update(final Question question, User user) {
		final Question oldQuestion = databaseDao.getQuestion(question.get_id());
		if (null == oldQuestion) {
			throw new NotFoundException();
		}

		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());
		if (user == null || session == null || !session.isCreator(user)) {
			throw new UnauthorizedException();
		}

		if ("freetext".equals(question.getQuestionType())) {
			question.setPiRound(0);
		} else if (question.getPiRound() < 1 || question.getPiRound() > 2) {
			question.setPiRound(oldQuestion.getPiRound() > 0 ? oldQuestion.getPiRound() : 1);
		}

		final Question result = databaseDao.updateQuestion(question);

		if (!oldQuestion.isActive() && question.isActive()) {
			final UnlockQuestionEvent event = new UnlockQuestionEvent(this, session, result);
			this.publisher.publishEvent(event);
		} else if (oldQuestion.isActive() && !question.isActive()) {
			final LockQuestionEvent event = new LockQuestionEvent(this, session, result);
			this.publisher.publishEvent(event);
		}

		if (!oldQuestion.getSubject().equals(result.getSubject())) {
			// Subject changed, question moved to another sort order document
			deleteQuestionFromSortOrder(oldQuestion);
		}
		SortOrder subjectSortOrder = databaseDao.getSortOrder(session.get_id(), result.getQuestionVariant(), "");
		if (subjectSortOrder != null) {
			SortOrder questionSortOrder = databaseDao.getSortOrder(session.get_id(), result.getQuestionVariant(), result.getSubject());
			if (questionSortOrder == null) {
				List<String> order = new ArrayList<String>();
				order.add(result.get_id());
				SortOrder newQSortOrder = new SortOrder();
				newQSortOrder.setSessionId(result.getSessionId());
				newQSortOrder.setSubject(result.getSubject());
				newQSortOrder.setSortType(subjectSortOrder.getSortType());
				newQSortOrder.setQuestionVariant(subjectSortOrder.getQuestionVariant());
				newQSortOrder.setSortOrder(order);
				databaseDao.createOrUpdateSortOrder(newQSortOrder);
				addToSortOrder(subjectSortOrder, result.getSubject());
			} else {
				addToSortOrder(questionSortOrder, result.get_id());
			}
		} else {
			createSortOrder(session, result.getQuestionVariant(), "");
		}
		return result;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer saveAnswer(final String questionId, final de.thm.arsnova.entities.transport.Answer answer) {
		final User user = getCurrentUser();
		final Question question = getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}

		Answer theAnswer = answer.generateAnswerEntity(user, question);
		if ("freetext".equals(question.getQuestionType())) {
			imageUtils.generateThumbnailImage(theAnswer);
		}

		return databaseDao.saveAnswer(theAnswer, user, question, getSession(question.getSessionKeyword()));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer updateAnswer(final Answer answer) {
		final User user = userService.getCurrentUser();
		final Answer realAnswer = this.getMyAnswer(answer.getQuestionId());
		if (user == null || realAnswer == null || !user.getUsername().equals(realAnswer.getUser())) {
			throw new UnauthorizedException();
		}

		final Question question = getQuestion(answer.getQuestionId());
		if ("freetext".equals(question.getQuestionType())) {
			imageUtils.generateThumbnailImage(realAnswer);
		}
		final Answer result = databaseDao.updateAnswer(realAnswer);
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());
		this.publisher.publishEvent(new NewAnswerEvent(this, session, result, user, question));

		return result;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAnswer(final String questionId, final String answerId) {
		final Question question = databaseDao.getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		final User user = userService.getCurrentUser();
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionKeyword());
		if (user == null || session == null || !session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.deleteAnswer(answerId);

		this.publisher.publishEvent(new DeleteAnswerEvent(this, session, question));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getLectureQuestions(final String sessionkey, final int offset, final int limit) {
		final Session session = getSession(sessionkey);
		SortOrder subjectSortOrder = databaseDao.getSortOrder(session.get_id(), "lecture", "");
		if (subjectSortOrder == null) {
			subjectSortOrder = createSortOrder(session, "lecture", "");
		}
		if (subjectSortOrder == null) {
			return null;
		}
		final User user = userService.getCurrentUser();
		if (session.isCreator(user)) {
			return databaseDao.getLectureQuestionsForTeachers(session, offset, limit);
		} else {
			return databaseDao.getLectureQuestionsForUsers(session, offset, limit);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getFlashcards(final String sessionkey, final int offset, final int limit) {
		final Session session = getSession(sessionkey);
		final User user = userService.getCurrentUser();
		if (session.isCreator(user)) {
			return databaseDao.getFlashcardsForTeachers(session, offset, limit);
		} else {
			return databaseDao.getFlashcardsForUsers(session, offset, limit);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getPreparationQuestions(final String sessionkey, final int offset, final int limit) {
		final Session session = getSession(sessionkey);
		SortOrder subjectSortOrder = databaseDao.getSortOrder(session.get_id(), "preparation", "");
		if (subjectSortOrder == null) {
			subjectSortOrder = createSortOrder(session, "preparation", "");
		}
		if (subjectSortOrder == null) {
			return null;
		}
		final User user = userService.getCurrentUser();
		if (session.isCreator(user)) {
			return databaseDao.getPreparationQuestionsForTeachers(session, offset, limit);
		} else {
			return databaseDao.getPreparationQuestionsForUsers(session, offset, limit);
		}
	}

	private Session getSession(final String sessionkey) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		if (session == null) {
			throw new NotFoundException();
		}
		return session;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getLectureQuestionCount(final String sessionkey) {
		return databaseDao.getLectureQuestionCount(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getFlashcardCount(final String sessionkey) {
		return databaseDao.getFlashcardCount(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getPreparationQuestionCount(final String sessionkey) {
		return databaseDao.getPreparationQuestionCount(getSession(sessionkey));
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
		return databaseDao.countLectureQuestionAnswers(getSession(sessionkey));
	}

	@Override
	public Map<String, Object> getAnswerAndAbstentionCountInternal(final String questionId) {
		final Question question = getQuestion(questionId);
		HashMap<String, Object> map = new HashMap<String, Object>();

		if (question == null) {
			return null;
		}

		map.put("_id", questionId);
		map.put("answers", databaseDao.getAnswerCount(question, question.getPiRound()));
		map.put("abstentions", databaseDao.getAbstentionAnswerCount(questionId));

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
		return databaseDao.countPreparationQuestionAnswers(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteLectureQuestions(final String sessionkey) {
		final Session session = getSessionWithAuthCheck(sessionkey);
		this.deleteSortOrder(session, "lecture", "");
		databaseDao.deleteAllLectureQuestionsWithAnswers(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteFlashcards(final String sessionkey) {
		final Session session = getSessionWithAuthCheck(sessionkey);
		databaseDao.deleteAllFlashcardsWithAnswers(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deletePreparationQuestions(final String sessionkey) {
		final Session session = getSessionWithAuthCheck(sessionkey);
		this.deleteSortOrder(session, "preparation", "");
		databaseDao.deleteAllPreparationQuestionsWithAnswers(session);
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
		return databaseDao.getUnAnsweredLectureQuestionIds(session, user);
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
		return databaseDao.getUnAnsweredPreparationQuestionIds(session, user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void publishAll(final String sessionkey, final boolean publish) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		final List<Question> questions = databaseDao.publishAllQuestions(session, publish);
		NovaEvent event;
		if (publish) {
			event = new UnlockQuestionsEvent(this, session, questions);
		} else {
			event = new LockQuestionsEvent(this, session, questions);
		}
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void publishQuestions(final String sessionkey, final boolean publish, List<Question> questions) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.publishQuestions(session, publish, questions);
		NovaEvent event;
		if (publish) {
			event = new UnlockQuestionsEvent(this, session, questions);
		} else {
			event = new LockQuestionsEvent(this, session, questions);
		}
		this.publisher.publishEvent(event);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllQuestionsAnswers(final String sessionkey) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.deleteAllQuestionsAnswers(session);

		this.publisher.publishEvent(new DeleteAllQuestionsAnswersEvent(this, session));
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionkey, 'session', 'owner')")
	public void deleteAllPreparationAnswers(String sessionkey) {
		final Session session = getSession(sessionkey);
		databaseDao.deleteAllPreparationAnswers(session);

		this.publisher.publishEvent(new DeleteAllPreparationAnswersEvent(this, session));
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionkey, 'session', 'owner')")
	public void deleteAllLectureAnswers(String sessionkey) {
		final Session session = getSession(sessionkey);
		databaseDao.deleteAllLectureAnswers(session);

		this.publisher.publishEvent(new DeleteAllLectureAnswersEvent(this, session));
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public String getImage(String questionId, String answerId) {
		final List<Answer> answers = getAnswers(questionId);
		Answer answer = null;

		for (Answer a : answers) {
			if (answerId.equals(a.get_id())) {
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
	public String getSubjectSortType(String sessionkey, String isPreparation) {
		Session session = databaseDao.getSessionFromKeyword(sessionkey);
		String questionVariant = "lecture";
		if ("true".equals(isPreparation)) {
			questionVariant = "preparation";
		}
		SortOrder sortOrder = databaseDao.getSortOrder(session.get_id(), questionVariant, "");
		return sortOrder.getSortType();
	}

	@Override
	public SortOrder setSort(String sessionkey, String subject, String sortType, String isPreparation, String[] sortOrderList) {
		Session session = databaseDao.getSessionFromKeyword(sessionkey);
		String questionVariant = "preparation";
		if ("false".equals(isPreparation)) {
			questionVariant = "lecture";
		}
		SortOrder existing = databaseDao.getSortOrder(session.get_id(), questionVariant, subject);
		SortOrder sortOrder = new SortOrder();
		if (existing != null) {
			sortOrder.set_id(existing.get_id());
			sortOrder.set_rev(existing.get_rev());
		}
		sortOrder.setSessionId(session.get_id());
		sortOrder.setSubject(subject);
		sortOrder.setSortType(sortType);
		sortOrder.setQuestionVariant(questionVariant);
		sortOrder.setSortOrder(Arrays.asList(sortOrderList));
		return databaseDao.createOrUpdateSortOrder(sortOrder);
	}

	@Override
	public String getQuestionSortType(String sessionkey, boolean isPreparation, String subject) {
		Session session = databaseDao.getSessionFromKeyword(sessionkey);
		String questionVariant = "lecture";
		if (isPreparation) {
			questionVariant = "preparation";
		}
		SortOrder sortOrder = databaseDao.getSortOrder(session.get_id(), questionVariant, subject);
		if (sortOrder == null) {
			return null;
		}
		return sortOrder.getSortType();
	}

	public SortOrder addToSortOrder(SortOrder sortOrder, String toBeAdded) {
		List<String> tmpList = sortOrder.getSortOrder();
		tmpList.add(toBeAdded);
		sortOrder.setSortOrder(tmpList);
		if("alphabet".equals(sortOrder.getSortType())) {
			sortOrder = alphabeticalSort(sortOrder);
		}
		return databaseDao.createOrUpdateSortOrder(sortOrder);
	}

	public void deleteQuestionFromSortOrder(Question question){
		SortOrder sortOrder = databaseDao.getSortOrder(question.getSessionId(), question.getQuestionVariant(), question.getSubject());
		if (sortOrder != null) {
			List<String> tempSortOrder = sortOrder.getSortOrder();
			tempSortOrder.remove(question.get_id());
			sortOrder.setSortOrder(tempSortOrder);
			if (sortOrder.getSortOrder().isEmpty()) {
				databaseDao.deleteSortOrder(sortOrder);
				SortOrder subjectSortOrder = databaseDao.getSortOrder(sortOrder.getSessionId(), sortOrder.getQuestionVariant(), "");
				List<String> tempSubSort = subjectSortOrder.getSortOrder();
				tempSubSort.remove(question.getSubject());
				subjectSortOrder.setSortOrder(tempSubSort);
				if (subjectSortOrder.getSortOrder().isEmpty()) {
					databaseDao.deleteSortOrder(subjectSortOrder);
				}
				else {
					databaseDao.createOrUpdateSortOrder(subjectSortOrder);
				}
			}
			else {
				databaseDao.createOrUpdateSortOrder(sortOrder);
			}
		}
	}

	public void deleteSortOrder(Session session, String questionVariant, String subject) {
		SortOrder sortOrder = databaseDao.getSortOrder(session.get_id(), questionVariant, subject);
		if (sortOrder == null) {
			return;
		}
		if ("".equals(subject)) {
			List<String> subs = sortOrder.getSortOrder();
			for (String sub : subs) {
				deleteSortOrder(session, questionVariant, sub);
			}
		}
		databaseDao.deleteSortOrder(sortOrder);
	}

	private List<Question> getQuestionsBySortOrder(SortOrder subjectSortOrder, final Session session, final User user) {
		if (subjectSortOrder.getSortOrder().isEmpty()) {
			return null;
		}
		List<String> questionIds = new ArrayList<String>();
		List<String> subjects = subjectSortOrder.getSortOrder();
		for (String sub : subjects) {
			SortOrder questionSortOrder = databaseDao.getSortOrder(subjectSortOrder.getSessionId(), subjectSortOrder.getQuestionVariant(), sub);
			if (questionSortOrder == null) {
				continue;
			}
			questionIds.addAll(questionSortOrder.getSortOrder());
		}
		List<Question> questions = databaseDao.getQuestionsByIds(questionIds, session);

		if (!session.isCreator(user)) {
			List<Question> tempquestions = new ArrayList<Question>();
			for (Question q : questions) {
				if (q.isActive()) {
					tempquestions.add(q);
				}
			}
			return tempquestions;
		}

		return questions;
	}

	public SortOrder createSortOrder(Session session, String questionVariant, String subject) {
		if ("".equals(subject)) {
			SortOrder subjectSortOrder = new SortOrder();
			subjectSortOrder.setSortOrder(databaseDao.getSubjects(session, questionVariant));
			if (subjectSortOrder.getSortOrder() == null) {
				return null;
			}
			subjectSortOrder.setSubject("");
			subjectSortOrder.setSortType("alphabet");
			subjectSortOrder.setQuestionVariant(questionVariant);
			subjectSortOrder.setSessionId(session.get_id());
			alphabeticalSort(subjectSortOrder);
			List<String> subjects = subjectSortOrder.getSortOrder();
			for (String sub : subjects) {
				createSortOrder(session, questionVariant, sub);
			}
			return databaseDao.createOrUpdateSortOrder(subjectSortOrder);
		}
		else {
			SortOrder sortOrder = new SortOrder();
			sortOrder.setSessionId(session.get_id());
			sortOrder.setSubject(subject);
			sortOrder.setQuestionVariant(questionVariant);
			sortOrder.setSortType("alphabet");
			sortOrder.setSortOrder(databaseDao.getQuestionIdsBySubject(session, questionVariant, subject));
			alphabeticalSort(sortOrder);
			return databaseDao.createOrUpdateSortOrder(sortOrder);
		}
	}

	public SortOrder alphabeticalSort(SortOrder sortOrder){
		if (sortOrder.getSortOrder() == null) {
			return null;
		}
		if (sortOrder.getSortOrder().isEmpty()) {
			return null;
		}
		if ("".equals(sortOrder.getSubject())) {
			List<String> subjects = sortOrder.getSortOrder();
			Collections.sort(subjects);
			sortOrder.setSortOrder(subjects);
			return sortOrder;
		}
		else {
			Hashtable<String, String> hash = new Hashtable<>();
			for (String qid : sortOrder.getSortOrder()) {
				Question question = getQuestion(qid);
				hash.put(question.getText(), qid);
			}
			List<String> sortList = new ArrayList<>();
			List<String> keys = new ArrayList<>(hash.keySet());
			Collections.sort(keys);
			for (String textKey : keys) {
				sortList.add(hash.get(textKey));
			}
			sortOrder.setSortOrder(sortList);
			return sortOrder;
		}
	}
}
