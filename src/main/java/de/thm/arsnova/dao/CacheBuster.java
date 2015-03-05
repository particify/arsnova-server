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
package de.thm.arsnova.dao;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

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
import de.thm.arsnova.events.StatusSessionEvent;

/**
 * This class is used to evict caches based on events. The events carry all necessary information to clear the
 * caches, e.g, for a specific session.
 */
@Component
public class CacheBuster implements ICacheBuster, NovaEventVisitor {

	@Override
	public void visit(NewInterposedQuestionEvent event) {}

	@Override
	public void visit(DeleteInterposedQuestionEvent event) {}

	@Override
	public void visit(NewQuestionEvent event) {}

	@CacheEvict(value = "answers", key = "#event.Session")
	@Override
	public void visit(NewAnswerEvent event) {}

	@Override
	public void visit(DeleteAnswerEvent event) {}

	@Override
	public void visit(DeleteQuestionEvent event) {}

	@Override
	public void visit(DeleteAllQuestionsAnswersEvent event) {}

	@Override
	public void visit(DeleteAllPreparationAnswersEvent event) {}

	@Override
	public void visit(DeleteAllLectureAnswersEvent event) {}

	@Override
	public void visit(NewFeedbackEvent event) {}

	@Override
	public void visit(DeleteFeedbackForSessionsEvent event) {}

	@Override
	public void visit(StatusSessionEvent event) {}

	@Override
	public void visit(ChangeLearningProgress changeLearningProgress) {}
}
