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
package de.thm.arsnova.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.events.ChangeLearningProgress;
import de.thm.arsnova.events.DeleteAllLectureAnswersEvent;
import de.thm.arsnova.events.DeleteAllPreparationAnswersEvent;
import de.thm.arsnova.events.DeleteAllQuestionsAnswersEvent;
import de.thm.arsnova.events.DeleteAnswerEvent;
import de.thm.arsnova.events.DeleteFeedbackForSessionsEvent;
import de.thm.arsnova.events.DeleteInterposedQuestionEvent;
import de.thm.arsnova.events.DeleteQuestionEvent;
import de.thm.arsnova.events.NewAnswerEvent;
import de.thm.arsnova.events.NewFeedbackEvent;
import de.thm.arsnova.events.NewInterposedQuestionEvent;
import de.thm.arsnova.events.NewQuestionEvent;
import de.thm.arsnova.events.NovaEventVisitor;
import de.thm.arsnova.events.PiRoundDelayedStartEvent;
import de.thm.arsnova.events.PiRoundEndEvent;
import de.thm.arsnova.events.StatusSessionEvent;

@Component
public class LearningProgressFactory implements NovaEventVisitor, ILearningProgressFactory, ApplicationEventPublisherAware {

	@Autowired
	private IDatabaseDao databaseDao;

	private ApplicationEventPublisher publisher;

	@Override
	public LearningProgress createFromType(String progressType) {
		if (progressType.equals("questions")) {
			return new QuestionBasedLearningProgress(databaseDao);
		} else {
			return new PointBasedLearningProgress(databaseDao);
		}
	}

	@Override
	public void visit(NewInterposedQuestionEvent event) {}

	@Override
	public void visit(DeleteInterposedQuestionEvent deleteInterposedQuestionEvent) {}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(NewQuestionEvent event) {
		this.publisher.publishEvent(new ChangeLearningProgress(this, event.getSession()));
	}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(NewAnswerEvent event) {
		this.publisher.publishEvent(new ChangeLearningProgress(this, event.getSession()));
	}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteAnswerEvent event) {
		this.publisher.publishEvent(new ChangeLearningProgress(this, event.getSession()));
	}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteQuestionEvent event) {
		this.publisher.publishEvent(new ChangeLearningProgress(this, event.getSession()));
	}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteAllQuestionsAnswersEvent event) {
		this.publisher.publishEvent(new ChangeLearningProgress(this, event.getSession()));
	}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteAllPreparationAnswersEvent event) {
		this.publisher.publishEvent(new ChangeLearningProgress(this, event.getSession()));
	}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteAllLectureAnswersEvent event) {
		this.publisher.publishEvent(new ChangeLearningProgress(this, event.getSession()));
	}

	@Override
	public void visit(NewFeedbackEvent newFeedbackEvent) {}

	@Override
	public void visit(DeleteFeedbackForSessionsEvent deleteFeedbackEvent) {}

	@Override
	public void visit(StatusSessionEvent statusSessionEvent) {}

	@Override
	public void visit(ChangeLearningProgress changeLearningProgress) {}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void visit(PiRoundDelayedStartEvent piRoundDelayedStartEvent) {}

	@Override
	public void visit(PiRoundEndEvent piRoundEndEvent) {}

}
