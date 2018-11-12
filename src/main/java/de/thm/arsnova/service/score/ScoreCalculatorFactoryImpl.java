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
package de.thm.arsnova.service.score;

import de.thm.arsnova.event.*;
import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.persistence.SessionStatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates a score calculator implementation.
 *
 * This class additionally clears all score caches and reports this via event system.
 */
@Component
public class ScoreCalculatorFactoryImpl implements ScoreCalculatorFactory, ApplicationEventPublisherAware {

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

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleAfterContentCreation(AfterCreationEvent<Content> event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getEntity().getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleUnlockQuestion(UnlockQuestionEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleUnlockQuestions(UnlockQuestionsEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleLockQuestion(LockQuestionEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleLockQuestions(LockQuestionsEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleNewAnswer(AfterCreationEvent<Answer> event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getEntity().getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleDeleteAnswer(AfterDeletionEvent<Answer> event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getEntity().getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleDeleteQuestion(AfterDeletionEvent<Content> event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getEntity().getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleDeleteAllQuestions(DeleteAllQuestionsEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleDeleteAllQuestionsAnswers(DeleteAllQuestionsAnswersEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleDeleteAllPreparationAnswers(DeleteAllPreparationAnswersEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handleDeleteAllLectureAnswers(DeleteAllLectureAnswersEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.Room")
	@EventListener
	public void handlePiRoundReset(PiRoundResetEvent event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getRoomId()));
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

}
