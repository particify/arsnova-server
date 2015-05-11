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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.events.ChangeLearningProgressEvent;
import de.thm.arsnova.events.DeleteAllLectureAnswersEvent;
import de.thm.arsnova.events.DeleteAllPreparationAnswersEvent;
import de.thm.arsnova.events.DeleteAllQuestionsAnswersEvent;
import de.thm.arsnova.events.DeleteAllQuestionsEvent;
import de.thm.arsnova.events.DeleteAnswerEvent;
import de.thm.arsnova.events.DeleteFeedbackForSessionsEvent;
import de.thm.arsnova.events.DeleteInterposedQuestionEvent;
import de.thm.arsnova.events.DeleteQuestionEvent;
import de.thm.arsnova.events.DeleteSessionEvent;
import de.thm.arsnova.events.LockQuestionEvent;
import de.thm.arsnova.events.LockQuestionsEvent;
import de.thm.arsnova.events.LockVotingEvent;
import de.thm.arsnova.events.NewAnswerEvent;
import de.thm.arsnova.events.NewFeedbackEvent;
import de.thm.arsnova.events.NewInterposedQuestionEvent;
import de.thm.arsnova.events.NewQuestionEvent;
import de.thm.arsnova.events.UnlockQuestionEvent;
import de.thm.arsnova.events.UnlockQuestionsEvent;
import de.thm.arsnova.events.NewSessionEvent;
import de.thm.arsnova.events.NovaEvent;
import de.thm.arsnova.events.NovaEventVisitor;
import de.thm.arsnova.events.PiRoundCancelEvent;
import de.thm.arsnova.events.PiRoundDelayedStartEvent;
import de.thm.arsnova.events.PiRoundEndEvent;
import de.thm.arsnova.events.PiRoundResetEvent;
import de.thm.arsnova.events.StatusSessionEvent;

/**
 * This class provides a number of methods for counting things like questions. The numbers are updated
 * using ARSnova's internal event system.
 */
@Service
public class EventedCountService implements CountService, NovaEventVisitor, ApplicationListener<NovaEvent> {

	@Autowired
	private IDatabaseDao databaseDao;

	private Map<Session, Integer> lectureQuestionCount = new ConcurrentHashMap<>();

	private Map<Session, Integer> preparationQuestionCount = new ConcurrentHashMap<>();

	private Map<Session, Integer> flashcardCount = new ConcurrentHashMap<>();

	@Override
	public int lectureQuestionCount(final Session session) {
		if (!lectureQuestionCount.containsKey(session)) {
			int count = databaseDao.getLectureQuestionCount(session);
			lectureQuestionCount.put(session, count);
		}
		return lectureQuestionCount.get(session);
	}

	@Override
	public int preparationQuestionCount(final Session session) {
		if (!preparationQuestionCount.containsKey(session)) {
			int count = databaseDao.getPreparationQuestionCount(session);
			preparationQuestionCount.put(session, count);
		}
		return preparationQuestionCount.get(session);
	}

	@Override
	public int flashcardCount(final Session session) {
		if (!flashcardCount.containsKey(session)) {
			int count = databaseDao.getFlashcardCount(session);
			flashcardCount.put(session, count);
		}
		return flashcardCount.get(session);
	}

	@Override
	public void visit(NewInterposedQuestionEvent event) {
		// TODO: Future use
	}

	@Override
	public void visit(DeleteInterposedQuestionEvent event) {
		// TODO: Future use
	}

	@Override
	public void visit(NewQuestionEvent event) {
		final Question q = event.getQuestion();
		final Session s = event.getSession();
		if (q.getQuestionVariant().equals("lecture")) {
			if (lectureQuestionCount.containsKey(s)) {
				int count = lectureQuestionCount.get(s);
				lectureQuestionCount.put(s, count+1);
			}
		} else if (q.getQuestionVariant().equals("preparation")) {
			if (preparationQuestionCount.containsKey(s)) {
				int count = preparationQuestionCount.get(s);
				preparationQuestionCount.put(s, count+1);
			}
		} else if (q.getQuestionVariant().equals("flashcard")) {
			if (flashcardCount.containsKey(s)) {
				int count = flashcardCount.get(s);
				flashcardCount.put(s, count+1);
			}
		}
	}

	@Override
	public void visit(DeleteQuestionEvent event) {
		final Question q = event.getQuestion();
		final Session s = event.getSession();
		if (q.getQuestionVariant().equals("lecture")) {
			if (lectureQuestionCount.containsKey(s)) {
				int count = lectureQuestionCount.get(s);
				lectureQuestionCount.put(s, count-1);
			}
		} else if (q.getQuestionVariant().equals("preparation")) {
			if (preparationQuestionCount.containsKey(s)) {
				int count = preparationQuestionCount.get(s);
				preparationQuestionCount.put(s, count-1);
			}
		} else if (q.getQuestionVariant().equals("flashcard")) {
			if (flashcardCount.containsKey(s)) {
				int count = flashcardCount.get(s);
				flashcardCount.put(s, count-1);
			}
		}
	}

	@Override
	public void visit(DeleteAllQuestionsEvent event) {
		final Session s = event.getSession();
		lectureQuestionCount.put(s, 0);
		preparationQuestionCount.put(s, 0);
		flashcardCount.put(s, 0);
	}

	@Override
	public void visit(UnlockQuestionEvent event) {
		// TODO Use for role-specific counting
	}

	@Override
	public void visit(UnlockQuestionsEvent event) {
		// TODO Use for role-specific counting
	}

	@Override
	public void visit(LockQuestionEvent event) {
		// TODO Use for role-specific counting
	}

	@Override
	public void visit(LockQuestionsEvent event) {
		// TODO Use for role-specific counting
	}

	@Override
	public void visit(NewAnswerEvent event) {
		// TODO: Future use
	}

	@Override
	public void visit(DeleteAnswerEvent event) {
		// TODO: Future use
	}

	@Override
	public void visit(DeleteAllQuestionsAnswersEvent event) {
		// TODO: Future use
	}

	@Override
	public void visit(DeleteAllPreparationAnswersEvent event) {
		// TODO: Future use
	}

	@Override
	public void visit(DeleteAllLectureAnswersEvent event) {
		// TODO: Future use
	}

	@Override
	public void visit(DeleteSessionEvent event) {
		final Session s = event.getSession();
		lectureQuestionCount.remove(s);
		preparationQuestionCount.remove(s);
		flashcardCount.remove(s);
	}

	// Unused events

	@Override
	public void visit(NewFeedbackEvent event) {}

	@Override
	public void visit(DeleteFeedbackForSessionsEvent event) {}

	@Override
	public void visit(StatusSessionEvent event) {}

	@Override
	public void visit(ChangeLearningProgressEvent event) {}

	@Override
	public void visit(PiRoundDelayedStartEvent event) {}

	@Override
	public void visit(PiRoundEndEvent event) {}

	@Override
	public void visit(PiRoundCancelEvent event) {}

	@Override
	public void visit(PiRoundResetEvent event) {}

	@Override
	public void visit(NewSessionEvent event) {}

	@Override
	public void visit(LockVotingEvent event) {}

	@Override
	public void onApplicationEvent(NovaEvent event) {
		event.accept(this);
	}

}
