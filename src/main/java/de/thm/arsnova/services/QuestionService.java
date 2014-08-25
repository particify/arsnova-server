/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.thm.arsnova.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.ImageUtils;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Service
public class QuestionService implements IQuestionService {

	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private IUserService userService;

	@Autowired
	private ARSnovaSocketIOServer socketIoServer;

	@Value("${upload.filesize_b}")
	private int uploadFileSizeByte;

	public static final Logger LOGGER = LoggerFactory.getLogger(QuestionService.class);

	public void setDatabaseDao(final IDatabaseDao databaseDao) {
		this.databaseDao = databaseDao;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getSkillQuestions(final String sessionkey) {
		return databaseDao.getSkillQuestions(userService.getCurrentUser(), getSession(sessionkey));
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

		if ("freetext".equals(question.getQuestionType())) {
			question.setPiRound(0);
		} else if (question.getPiRound() < 1 || question.getPiRound() > 2) {
			question.setPiRound(1);
		}

		// convert imageurl to base64 if neccessary
		if ("grid".equals(question.getQuestionType())) {
			if (question.getImage().startsWith("http")) {
				final String base64ImageString = ImageUtils.encodeImageToString(question.getImage());
				if (base64ImageString == null) {
					throw new BadRequestException();
				}
				question.setImage(base64ImageString);
			}

			// base64 adds offset to filesize, formular taken from: http://en.wikipedia.org/wiki/Base64#MIME
			final int fileSize =  (int)((question.getImage().length()-814)/1.37);
			if ( fileSize > uploadFileSizeByte ) {
				LOGGER.error("Could not save file. File is too large with "+ fileSize + " Byte.");
				throw new BadRequestException();
			}
		}

		final Question result = databaseDao.saveQuestion(session, question);
		socketIoServer.reportLecturerQuestionAvailable(result.getSessionKeyword(), result.get_id());

		return result;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public boolean saveQuestion(final InterposedQuestion question) {
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionId());
		final InterposedQuestion result = databaseDao.saveQuestion(session, question, userService.getCurrentUser());

		if (null != result) {
			socketIoServer.reportAudienceQuestionAvailable(result.getSessionId(), result.get_id());

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

		final Session session = databaseDao.getSession(question.getSessionKeyword());
		if (session == null) {
			throw new UnauthorizedException();
		}
		databaseDao.deleteQuestionWithAnswers(question);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionKeyword, 'session', 'owner')")
	public void deleteAllQuestions(final String sessionKeyword) {
		final Session session = getSessionWithAuthCheck(sessionKeyword);
		databaseDao.deleteAllQuestionsWithAnswers(session);
	}

	private Session getSessionWithAuthCheck(final String sessionKeyword) {
		final User user = userService.getCurrentUser();
		final Session session = databaseDao.getSession(sessionKeyword);
		if (user == null || session == null || ! session.isCreator(user)) {
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
	@PreAuthorize("isAuthenticated()")
	public void deleteAnswers(final String questionId) {
		final Question question = databaseDao.getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}

		final User user = userService.getCurrentUser();
		final Session session = databaseDao.getSession(question.getSessionKeyword());
		if (user == null || session == null || ! session.isCreator(user)) {
			throw new UnauthorizedException();
		}
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

		return databaseDao.getMyAnswer(userService.getCurrentUser(), questionId, question.getPiRound());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAnswers(final String questionId, final int piRound) {
		final Question question = databaseDao.getQuestion(questionId);

		return "freetext".equals(question.getQuestionType())
				? getFreetextAnswers(questionId)
						: databaseDao.getAnswers(questionId, piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAnswers(final String questionId) {
		final Question question = getQuestion(questionId);

		return getAnswers(questionId, question.getPiRound());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getAnswerCount(final String questionId) {
		final Question question = getQuestion(questionId);

		return databaseDao.getAnswerCount(question, question.getPiRound());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getFreetextAnswers(final String questionId) {
		final List<Answer> answers = databaseDao.getFreetextAnswers(questionId);
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
		final List<Question> questions = getSkillQuestions(sessionKey);
		final Map<String, Question> questionIdToQuestion = new HashMap<String, Question>();
		for (final Question question : questions) {
			questionIdToQuestion.put(question.get_id(), question);
		}

		/* filter answers by active piRound per question */
		final List<Answer> answers = databaseDao.getMyAnswers(userService.getCurrentUser(), sessionKey);
		final List<Answer> filteredAnswers = new ArrayList<Answer>();
		for (final Answer answer : answers) {
			final Question question = questionIdToQuestion.get(answer.getQuestionId());
			if (0 == answer.getPiRound() && !"freetext".equals(question.getQuestionType())) {
				answer.setPiRound(1);
			}
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
	public InterposedReadingCount getInterposedReadingCount(final String sessionKey) {
		final Session session = databaseDao.getSessionFromKeyword(sessionKey);
		final User user = getCurrentUser();
		if (session == null) {
			throw new NotFoundException();
		}
		if (session.isCreator(user)) {
			return databaseDao.getInterposedReadingCount(session);
		} else {
			return databaseDao.getInterposedReadingCount(session, user);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<InterposedQuestion> getInterposedQuestions(final String sessionKey) {
		final Session session = this.getSession(sessionKey);
		final User user = getCurrentUser();
		if (session.isCreator(user)) {
			return databaseDao.getInterposedQuestions(session);
		} else {
			return databaseDao.getInterposedQuestions(session, user);
		}
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public InterposedQuestion readInterposedQuestion(final String questionId) {
		final InterposedQuestion question = databaseDao.getInterposedQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		final Session session = databaseDao.getSessionFromKeyword(question.getSessionId());

		final User user = userService.getCurrentUser();
		if (session.isCreator(user)) {
			databaseDao.markInterposedQuestionAsRead(question);
		}
		return question;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Question update(final Question question) {
		final Question oldQuestion = databaseDao.getQuestion(question.get_id());
		if (null == oldQuestion) {
			throw new NotFoundException();
		}

		final User user = userService.getCurrentUser();
		final Session session = databaseDao.getSession(question.getSessionKeyword());
		if (user == null || session == null || !session.isCreator(user)) {
			throw new UnauthorizedException();
		}

		if ("freetext".equals(question.getQuestionType())) {
			question.setPiRound(0);
		} else if (question.getPiRound() < 1 || question.getPiRound() > 2) {
			question.setPiRound(oldQuestion.getPiRound() > 0 ? oldQuestion.getPiRound() : 1);
		}

		return databaseDao.updateQuestion(question);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer saveAnswer(final Answer answer) {
		final User user = getCurrentUser();
		final Question question = getQuestion(answer.getQuestionId());
		if (question == null) {
			throw new NotFoundException();
		}

		if ("freetext".equals(question.getQuestionType())) {
			answer.setPiRound(0);
		} else {
			answer.setPiRound(question.getPiRound());
		}

		final Answer result = databaseDao.saveAnswer(answer, user);
		socketIoServer.reportAnswersToLecturerQuestionAvailable(question.getSessionKeyword(), question.get_id());

		return result;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer updateAnswer(final Answer answer) {
		final User user = userService.getCurrentUser();
		if (user == null || !user.getUsername().equals(answer.getUser())) {
			throw new UnauthorizedException();
		}

		final Question question = getQuestion(answer.getQuestionId());
		final Answer result = databaseDao.updateAnswer(answer);
		socketIoServer.reportAnswersToLecturerQuestionAvailable(question.getSessionKeyword(), question.get_id());

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

		socketIoServer.reportAnswersToLecturerQuestionAvailable(question.getSessionKeyword(), question.get_id());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getLectureQuestions(final String sessionkey) {
		return databaseDao.getLectureQuestions(userService.getCurrentUser(), getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getFlashcards(final String sessionkey) {
		return databaseDao.getFlashcards(userService.getCurrentUser(), getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getPreparationQuestions(final String sessionkey) {
		return databaseDao.getPreparationQuestions(userService.getCurrentUser(), getSession(sessionkey));
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
		return databaseDao.countLectureQuestionAnswers(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countPreparationQuestionAnswers(final String sessionkey) {
		return databaseDao.countPreparationQuestionAnswers(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteLectureQuestions(final String sessionkey) {
		final Session session = getSessionWithAuthCheck(sessionkey);
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
		databaseDao.deleteAllPreparationQuestionsWithAnswers(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredLectureQuestionIds(final String sessionkey) {
		final User user = getCurrentUser();
		final Session session = getSession(sessionkey);
		return databaseDao.getUnAnsweredLectureQuestionIds(session, user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredPreparationQuestionIds(final String sessionkey) {
		final User user = getCurrentUser();
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
		databaseDao.publishAllQuestions(session, publish);
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
	}
}
