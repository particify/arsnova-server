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

	public void setDatabaseDao(IDatabaseDao databaseDao) {
		this.databaseDao = databaseDao;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getSkillQuestions(String sessionkey) {
		return databaseDao.getSkillQuestions(userService.getCurrentUser(), getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getSkillQuestionCount(String sessionkey) {
		Session session = this.databaseDao.getSessionFromKeyword(sessionkey);
		return databaseDao.getSkillQuestionCount(session);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#question.getSessionKeyword(), 'session', 'owner')")
	public Question saveQuestion(Question question) {
		Session session = this.databaseDao.getSessionFromKeyword(question.getSessionKeyword());
		question.setSessionId(session.get_id());

		if ("freetext".equals(question.getQuestionType())) {
			question.setPiRound(0);
		} else if (question.getPiRound() < 1 || question.getPiRound() > 2) {
			question.setPiRound(1);
		}

		// convert imageurl to base64 if neccessary
		if ("grid".equals(question.getQuestionType())) {
			org.slf4j.Logger logger = LoggerFactory.getLogger(QuestionService.class);
			if (question.getImage().startsWith("http")) {
				String base64ImageString = ImageUtils.encodeImageToString(question.getImage());
				if (base64ImageString == null) {
					throw new BadRequestException();
				}
				question.setImage(base64ImageString);
			}

			// base64 adds offset to filesize, formular taken from: http://en.wikipedia.org/wiki/Base64#MIME
			int fileSize =  (int)((question.getImage().length()-814)/1.37);
			if ( fileSize > this.uploadFileSizeByte ) {
				LOGGER.error("Could not save file. File is too large with "+ fileSize + " Byte.");
				throw new BadRequestException();
			}
		}

		Question result = this.databaseDao.saveQuestion(session, question);
		socketIoServer.reportLecturerQuestionAvailable(result.getSessionKeyword(), result.get_id());

		return result;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public boolean saveQuestion(InterposedQuestion question) {
		Session session = this.databaseDao.getSessionFromKeyword(question.getSessionId());
		InterposedQuestion result = this.databaseDao.saveQuestion(session, question);

		if (null != result) {
			socketIoServer.reportAudienceQuestionAvailable(result.getSessionId(), result.get_id());

			return true;
		}

		return false;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Question getQuestion(String id) {
		Question result = databaseDao.getQuestion(id);
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
	@PreAuthorize("isAuthenticated()")
	public void deleteQuestion(String questionId) {
		Question question = databaseDao.getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}

		User user = userService.getCurrentUser();
		Session session = databaseDao.getSession(question.getSessionKeyword());
		if (user == null || session == null || ! session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.deleteQuestionWithAnswers(question);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllQuestions(String sessionKeyword) {
		Session session = getSessionWithAuthCheck(sessionKeyword);
		databaseDao.deleteAllQuestionsWithAnswers(session);
	}

	private Session getSessionWithAuthCheck(String sessionKeyword) {
		User user = userService.getCurrentUser();
		Session session = databaseDao.getSession(sessionKeyword);
		if (user == null || session == null || ! session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		return session;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteInterposedQuestion(String questionId) {
		InterposedQuestion question = databaseDao.getInterposedQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		User user = userService.getCurrentUser();
		Session session = databaseDao.getSessionFromKeyword(question.getSessionId());
		if (user == null || session == null || ! session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.deleteInterposedQuestion(question);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllInterposedQuestions(String sessionKeyword) {
		User user = userService.getCurrentUser();
		Session session = databaseDao.getSessionFromKeyword(sessionKeyword);
		if (user == null || session == null || ! session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.deleteAllInterposedQuestions(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAnswers(String questionId) {
		Question question = databaseDao.getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}

		User user = userService.getCurrentUser();
		Session session = databaseDao.getSession(question.getSessionKeyword());
		if (user == null || session == null || ! session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.deleteAnswers(question);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredQuestionIds(String sessionKey) {
		User user = getCurrentUser();
		Session session = getSession(sessionKey);
		return databaseDao.getUnAnsweredQuestionIds(session, user);
	}

	private User getCurrentUser() {
		User user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		return user;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer getMyAnswer(String questionId) {
		Question question = getQuestion(questionId);

		return databaseDao.getMyAnswer(questionId, question.getPiRound());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAnswers(String questionId, int piRound) {
		Question question = databaseDao.getQuestion(questionId);

		return "freetext".equals(question.getQuestionType())
				? getFreetextAnswers(questionId)
						: databaseDao.getAnswers(questionId, piRound);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getAnswers(String questionId) {
		Question question = getQuestion(questionId);

		return getAnswers(questionId, question.getPiRound());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getAnswerCount(String questionId) {
		Question question = getQuestion(questionId);

		return databaseDao.getAnswerCount(question, question.getPiRound());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getFreetextAnswers(String questionId) {
		List<Answer> answers = databaseDao.getFreetextAnswers(questionId);
		if (answers == null) {
			throw new NotFoundException();
		}
		return answers;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Answer> getMyAnswers(String sessionKey) {
		List<Question> questions = getSkillQuestions(sessionKey);
		Map<String, Question> questionIdToQuestion = new HashMap<String, Question>();
		for (Question question : questions) {
			questionIdToQuestion.put(question.get_id(), question);
		}

		/* filter answers by active piRound per question */
		List<Answer> answers = databaseDao.getMyAnswers(sessionKey);
		List<Answer> filteredAnswers = new ArrayList<Answer>();
		for (Answer answer : answers) {
			Question question = questionIdToQuestion.get(answer.getQuestionId());
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
	public int getTotalAnswerCount(String sessionKey) {
		return databaseDao.getTotalAnswerCount(sessionKey);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getInterposedCount(String sessionKey) {
		return databaseDao.getInterposedCount(sessionKey);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public InterposedReadingCount getInterposedReadingCount(String sessionKey) {
		Session session = this.databaseDao.getSessionFromKeyword(sessionKey);
		if (session == null) {
			throw new NotFoundException();
		}
		return databaseDao.getInterposedReadingCount(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<InterposedQuestion> getInterposedQuestions(String sessionKey) {
		return databaseDao.getInterposedQuestions(sessionKey);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public InterposedQuestion readInterposedQuestion(String questionId) {
		InterposedQuestion question = databaseDao.getInterposedQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		Session session = this.databaseDao.getSessionFromKeyword(question.getSessionId());

		User user = this.userService.getCurrentUser();
		if (session.isCreator(user)) {
			this.databaseDao.markInterposedQuestionAsRead(question);
		}
		return question;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Question update(Question question) {
		Question oldQuestion = databaseDao.getQuestion(question.get_id());
		if (null == oldQuestion) {
			throw new NotFoundException();
		}

		User user = userService.getCurrentUser();
		Session session = databaseDao.getSession(question.getSessionKeyword());
		if (user == null || session == null || !session.isCreator(user)) {
			throw new UnauthorizedException();
		}

		if ("freetext".equals(question.getQuestionType())) {
			question.setPiRound(0);
		} else if (question.getPiRound() < 1 || question.getPiRound() > 2) {
			question.setPiRound(oldQuestion.getPiRound() > 0 ? oldQuestion.getPiRound() : 1);
		}

		return this.databaseDao.updateQuestion(question);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer saveAnswer(Answer answer) {
		User user = getCurrentUser();
		Question question = this.getQuestion(answer.getQuestionId());
		if (question == null) {
			throw new NotFoundException();
		}

		if ("freetext".equals(question.getQuestionType())) {
			answer.setPiRound(0);
		} else {
			answer.setPiRound(question.getPiRound());
		}

		Answer result = this.databaseDao.saveAnswer(answer, user);
		socketIoServer.reportAnswersToLecturerQuestionAvailable(question.getSessionKeyword(), question.get_id());

		return result;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Answer updateAnswer(Answer answer) {
		User user = userService.getCurrentUser();
		if (user == null || !user.getUsername().equals(answer.getUser())) {
			throw new UnauthorizedException();
		}

		Question question = this.getQuestion(answer.getQuestionId());
		Answer result = this.databaseDao.updateAnswer(answer);
		socketIoServer.reportAnswersToLecturerQuestionAvailable(question.getSessionKeyword(), question.get_id());

		return result;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAnswer(String questionId, String answerId) {
		Question question = this.databaseDao.getQuestion(questionId);
		if (question == null) {
			throw new NotFoundException();
		}
		User user = userService.getCurrentUser();
		Session session = this.databaseDao.getSessionFromKeyword(question.getSessionKeyword());
		if (user == null || session == null || !session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		this.databaseDao.deleteAnswer(answerId);

		socketIoServer.reportAnswersToLecturerQuestionAvailable(question.getSessionKeyword(), question.get_id());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getLectureQuestions(String sessionkey) {
		return databaseDao.getLectureQuestions(userService.getCurrentUser(), getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getFlashcards(String sessionkey) {
		return databaseDao.getFlashcards(userService.getCurrentUser(), getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Question> getPreparationQuestions(String sessionkey) {
		return databaseDao.getPreparationQuestions(userService.getCurrentUser(), getSession(sessionkey));
	}

	private Session getSession(String sessionkey) {
		Session session = this.databaseDao.getSessionFromKeyword(sessionkey);
		if (session == null) {
			throw new NotFoundException();
		}
		return session;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getLectureQuestionCount(String sessionkey) {
		return databaseDao.getLectureQuestionCount(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getFlashcardCount(String sessionkey) {
		return databaseDao.getFlashcardCount(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getPreparationQuestionCount(String sessionkey) {
		return databaseDao.getPreparationQuestionCount(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countLectureQuestionAnswers(String sessionkey) {
		return databaseDao.countLectureQuestionAnswers(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int countPreparationQuestionAnswers(String sessionkey) {
		return databaseDao.countPreparationQuestionAnswers(getSession(sessionkey));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteLectureQuestions(String sessionkey) {
		Session session = getSessionWithAuthCheck(sessionkey);
		databaseDao.deleteAllLectureQuestionsWithAnswers(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteFlashcards(String sessionkey) {
		Session session = getSessionWithAuthCheck(sessionkey);
		databaseDao.deleteAllFlashcardsWithAnswers(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deletePreparationQuestions(String sessionkey) {
		Session session = getSessionWithAuthCheck(sessionkey);
		databaseDao.deleteAllPreparationQuestionsWithAnswers(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredLectureQuestionIds(String sessionkey) {
		User user = getCurrentUser();
		Session session = getSession(sessionkey);
		return databaseDao.getUnAnsweredLectureQuestionIds(session, user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<String> getUnAnsweredPreparationQuestionIds(String sessionkey) {
		User user = getCurrentUser();
		Session session = getSession(sessionkey);
		return databaseDao.getUnAnsweredPreparationQuestionIds(session, user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void publishAll(String sessionkey, boolean publish) {
		User user = getCurrentUser();
		Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.publishAllQuestions(session, publish);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public void deleteAllQuestionsAnswers(String sessionkey) {
		User user = getCurrentUser();
		Session session = getSession(sessionkey);
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		databaseDao.deleteAllQuestionsAnswers(session);
	}
}
