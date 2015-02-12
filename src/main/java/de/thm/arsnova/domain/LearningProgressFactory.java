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
import org.springframework.stereotype.Component;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.events.DeleteAllLectureAnswersEvent;
import de.thm.arsnova.events.DeleteAllPreparationAnswersEvent;
import de.thm.arsnova.events.DeleteAllQuestionsAnswersEvent;
import de.thm.arsnova.events.DeleteAnswerEvent;
import de.thm.arsnova.events.DeleteInterposedQuestionEvent;
import de.thm.arsnova.events.DeleteQuestionEvent;
import de.thm.arsnova.events.NewAnswerEvent;
import de.thm.arsnova.events.NewInterposedQuestionEvent;
import de.thm.arsnova.events.NewQuestionEvent;
import de.thm.arsnova.events.NovaEventVisitor;

@Component
public class LearningProgressFactory implements NovaEventVisitor, ILearningProgressFactory {

	@Autowired
	private IDatabaseDao databaseDao;

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
	public void visit(NewQuestionEvent event) {}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(NewAnswerEvent event) {}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteAnswerEvent event) {}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteQuestionEvent event) {}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteAllQuestionsAnswersEvent event) {}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteAllPreparationAnswersEvent event) {}

	@CacheEvict(value = "learningprogress", key = "#event.Session")
	@Override
	public void visit(DeleteAllLectureAnswersEvent event) {}

}
