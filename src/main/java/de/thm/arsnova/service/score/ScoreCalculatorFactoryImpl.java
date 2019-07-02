/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.thm.arsnova.event.AfterCreationEvent;
import de.thm.arsnova.event.AfterDeletionEvent;
import de.thm.arsnova.event.ChangeScoreEvent;
import de.thm.arsnova.event.StateChangeEvent;
import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.persistence.SessionStatisticsRepository;

/**
 * Creates a score calculator implementation.
 *
 * <p>
 * This class additionally clears all score caches and reports this via event system.
 * </p>
 */
@Component
public class ScoreCalculatorFactoryImpl implements ScoreCalculatorFactory, ApplicationEventPublisherAware {

	@Autowired
	private SessionStatisticsRepository sessionStatisticsRepository;

	private ApplicationEventPublisher publisher;

	@Override
	public ScoreCalculator create(final String type, final String questionVariant) {
		final VariantScoreCalculator scoreCalculator;
		if ("questions".equals(type)) {
			scoreCalculator = new QuestionBasedScoreCalculator(sessionStatisticsRepository);
		} else {
			scoreCalculator = new ScoreBasedScoreCalculator(sessionStatisticsRepository);
		}
		scoreCalculator.setQuestionVariant(questionVariant);
		return scoreCalculator;
	}

	@CacheEvict(value = "score", key = "#event.entity.roomId", condition = "#event.entity.roomId != null")
	@EventListener
	public void handleAfterContentCreation(final AfterCreationEvent<Content> event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getEntity().getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.entity.roomId", condition = "#event.entity.roomId != null")
	@EventListener(condition = "#event.stateName == 'state'")
	public void handleContentStateChange(final StateChangeEvent<Content, Content.State> event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getEntity().getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.entity.roomId", condition = "#event.entity.roomId != null")
	@EventListener
	public void handleNewAnswer(final AfterCreationEvent<Answer> event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getEntity().getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.entity.roomId", condition = "#event.entity.roomId != null")
	@EventListener
	public void handleDeleteAnswer(final AfterDeletionEvent<Answer> event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getEntity().getRoomId()));
	}

	@CacheEvict(value = "score", key = "#event.entity.roomId", condition = "#event.entity.roomId != null")
	@EventListener
	public void handleDeleteQuestion(final AfterDeletionEvent<Content> event) {
		this.publisher.publishEvent(new ChangeScoreEvent(this, event.getEntity().getRoomId()));
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

}
