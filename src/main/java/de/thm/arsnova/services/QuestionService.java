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

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fourspaces.couchdb.Document;

import de.thm.arsnova.annotation.Authenticated;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NoContentException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.UnauthorizedException;

@Service
public class QuestionService implements IQuestionService {

	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private IUserService userService;

	public void setDatabaseDao(IDatabaseDao databaseDao) {
		this.databaseDao = databaseDao;
	}

	@Override
	@Authenticated
	public List<Question> getSkillQuestions(String sessionkey) {
		List<Question> result = databaseDao.getSkillQuestions(sessionkey);
		if (result == null || result.size() == 0) {
			throw new NoContentException();
		}
		return result;
	}

	@Override
	@Authenticated
	public int getSkillQuestionCount(String sessionkey) {
		return databaseDao.getSkillQuestionCount(sessionkey);
	}

	@Override
	@Authenticated
	public Question saveQuestion(Question question) {
		Session session = this.databaseDao.getSessionFromKeyword(question.getSession());		
		return this.databaseDao.saveQuestion(session, question);
	}

	@Override
	@Authenticated
	public boolean saveQuestion(InterposedQuestion question) {
		Session session = this.databaseDao.getSessionFromKeyword(question.getSessionId());
		return this.databaseDao.saveQuestion(session, question);
	}

	@Override
	@Authenticated
	public Question getQuestion(String id, String sessionKey) {
		return databaseDao.getQuestion(id, sessionKey);
	}

	@Override
	@Authenticated
	public List<String> getQuestionIds(String sessionKey) {
		User user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		Session session = databaseDao.getSessionFromKeyword(sessionKey);
		if (session == null) {
			throw new NotFoundException();
		}
		return databaseDao.getQuestionIds(session, user);
	}

	@Override
	@Authenticated
	public void deleteQuestion(String sessionKey, String questionId) {
		databaseDao.deleteQuestion(sessionKey, questionId);
	}

	@Override
	@Authenticated
	public List<String> getUnAnsweredQuestions(String sessionKey) {
		User user = userService.getCurrentUser();
		if (user == null) {
			throw new UnauthorizedException();
		}
		Session session = databaseDao.getSessionFromKeyword(sessionKey);
		if (session == null) {
			throw new NotFoundException();
		}
		return databaseDao.getUnAnsweredQuestions(session, user);
	}

	@Override
	@Authenticated
	public Answer getMyAnswer(String sessionKey, String questionId) {
		return databaseDao.getMyAnswer(sessionKey, questionId);
	}

	@Override
	@Authenticated
	public List<Answer> getAnswers(String sessionKey, String questionId) {
		return databaseDao.getAnswers(sessionKey, questionId);
	}

	@Override
	@Authenticated
	public int getAnswerCount(String sessionKey, String questionId) {
		return databaseDao.getAnswerCount(sessionKey, questionId);
	}

	@Override
	@Authenticated
	public List<Answer> getFreetextAnswers(String sessionKey, String questionId) {
		return databaseDao.getFreetextAnswers(sessionKey, questionId);
	}

	@Override
	@Authenticated
	public List<Answer> getMytAnswers(String sessionKey) {
		return databaseDao.getMyAnswers(sessionKey);
	}

	@Override
	@Authenticated
	public int getTotalAnswerCount(String sessionKey) {
		return databaseDao.getTotalAnswerCount(sessionKey);
	}

	@Override
	@Authenticated
	public int getInterposedCount(String sessionKey) {
		return databaseDao.getInterposedCount(sessionKey);
	}

	@Override
	@Authenticated
	public InterposedReadingCount getInterposedReadingCount(String sessionKey) {
		Session session = this.databaseDao.getSessionFromKeyword(sessionKey);
		return databaseDao.getInterposedReadingCount(session);
	}

	@Override
	@Authenticated
	public List<InterposedQuestion> getInterposedQuestions(String sessionKey) {
		return databaseDao.getInterposedQuestions(sessionKey);
	}

	@Override
	@Authenticated
	public InterposedQuestion readInterposedQuestion(String sessionKey, String questionId) {
		try {
			InterposedQuestion question = databaseDao.getInterposedQuestion(questionId);
			Session session = this.databaseDao.getSessionFromKeyword(sessionKey);
			if (session == null || !session.getKeyword().equals(question.getSessionId())) {
				throw new NotFoundException();
			}

			User user = this.userService.getCurrentUser();
			if (session.isCreator(user)) {
				this.databaseDao.markInterposedQuestionAsRead(question);
			}
			return question;
		} catch (IOException e) {
			throw new NotFoundException();
		}
	}
}
