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
package de.thm.arsnova.cache;

import de.thm.arsnova.events.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

/**
 * This class is used to evict caches based on events. The events carry all necessary information to clear the
 * caches, e.g, for a specific session.
 */
@Component
public class CacheBusterImpl implements CacheBuster, ArsnovaEventVisitor {

	@CacheEvict(value = "statistics", allEntries = true)
	@Override
	public void visit(NewCommentEvent event) { }

	@CacheEvict(value = "statistics", allEntries = true)
	@Override
	public void visit(DeleteCommentEvent event) { }

	@Override
	public void visit(NewQuestionEvent event) { }

	@Override
	public void visit(UnlockQuestionEvent event) { }

	@Override
	public void visit(UnlockQuestionsEvent newQuestionsEvent) { }

	@Override
	public void visit(LockQuestionEvent lockQuestionEvent) { }

	@Override
	public void visit(LockQuestionsEvent lockQuestionsEvent) { }

	@CacheEvict(value = "answerlists", key = "#event.content.id")
	@Override
	public void visit(NewAnswerEvent event) { }

	@Override
	public void visit(DeleteAnswerEvent event) { }

	@Override
	public void visit(DeleteQuestionEvent event) { }

	@Override
	public void visit(DeleteAllQuestionsEvent event) { }

	@Override
	public void visit(DeleteAllQuestionsAnswersEvent event) { }

	@Override
	public void visit(DeleteAllPreparationAnswersEvent event) { }

	@Override
	public void visit(DeleteAllLectureAnswersEvent event) { }

	@Override
	public void visit(NewFeedbackEvent event) { }

	@Override
	public void visit(DeleteFeedbackForSessionsEvent event) { }

	@Override
	public void visit(StatusSessionEvent event) { }

	@CacheEvict(value = "statistics", allEntries = true)
	@Override
	public void visit(ChangeScoreEvent changeLearningProgress) { }

	@Override
	public void visit(PiRoundDelayedStartEvent piRoundDelayedStartEvent) { }

	@Override
	public void visit(PiRoundEndEvent piRoundEndEvent) { }

	@Override
	public void visit(PiRoundCancelEvent piRoundCancelEvent) { }

	@Override
	public void visit(PiRoundResetEvent piRoundResetEvent) { }

	@CacheEvict(value = "statistics", allEntries = true)
	@Override
	public void visit(NewSessionEvent newSessionEvent) { }

	@CacheEvict(value = "statistics", allEntries = true)
	@Override
	public void visit(DeleteSessionEvent deleteSessionEvent) { }

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
