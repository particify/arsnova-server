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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.services.score;

import de.thm.arsnova.events.*;
import de.thm.arsnova.persistance.SessionStatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

/**
 * Creates a score calculator implementation.
 *
 * This class additionally clears all score caches and reports this via event system.
 */
@Component
public class ScoreCalculatorFactoryImpl implements ArsnovaEventVisitor, ScoreCalculatorFactory, ApplicationEventPublisherAware {

	@Autowired
	private SessionStatisticsRepository sessionStatisticsRepository;

	private ApplicationEventPublisher publisher;

	@Override
	public ScoreCalculator create(String type, String questionVariant) {
		VariantScoreCalculator scoreCalculator;
		if ("questions".equals(type)) {
			scoreCalculator = new QuestionBasedScoreCalculator(sessionStatisticsRepository);
		} else {
			scoreCalculator = new ScoreBasedScoreCalculator(sessionStatisticsRepository);
		}
		scoreCalculator.setQuestionVariant(questionVariant);
		return scoreCalculator;
	}

	@Override
	public void visit(NewCommentEvent event) { }

	@Override
	public void visit(DeleteCommentEvent deleteCommentEvent) { }

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(NewQuestionEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(UnlockQuestionEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(UnlockQuestionsEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(LockQuestionEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(LockQuestionsEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(NewAnswerEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(DeleteAnswerEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(DeleteQuestionEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(DeleteAllQuestionsEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(DeleteAllQuestionsAnswersEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(DeleteAllPreparationAnswersEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(DeleteAllLectureAnswersEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@CacheEvict(value = "score", key = "#event.Session")
	@Override
	public void visit(PiRoundResetEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getSession()));
	}

	@Override
	public void visit(NewFeedbackEvent newFeedbackEvent) { }

	@Override
	public void visit(DeleteFeedbackForSessionsEvent deleteFeedbackEvent) { }

	@Override
	public void visit(StatusSessionEvent statusSessionEvent) { }

	@Override
	public void visit(ChangeScoreEvent changeLearningProgress) { }

	@Override
	public void visit(PiRoundDelayedStartEvent piRoundDelayedStartEvent) { }

	@Override
	public void visit(PiRoundEndEvent piRoundEndEvent) { }

	@Override
	public void visit(PiRoundCancelEvent piRoundCancelEvent) { }

	@Override
	public void visit(NewSessionEvent event) { }

	@Override
	public void visit(DeleteSessionEvent event) { }

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void visit(LockVoteEvent lockVoteEvent) { }

	@Override
	public void visit(LockVotesEvent lockVotesEvent) { }

	@Override
	public void visit(UnlockVoteEvent unlockVoteEvent) { }

	@Override
	public void visit(UnlockVotesEvent unlockVotesEvent) { }

	@Override
	public void visit(FeatureChangeEvent featureChangeEvent) { }

	@Override
	public void visit(LockFeedbackEvent lockFeedbackEvent) { }

	@Override
	public void visit(FlipFlashcardsEvent flipFlashcardsEvent) { }

}
