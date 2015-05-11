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
package de.thm.arsnova.events;

public interface NovaEventVisitor {

	void visit(NewInterposedQuestionEvent newInterposedQuestionEvent);

	void visit(DeleteInterposedQuestionEvent deleteInterposedQuestionEvent);

	void visit(NewQuestionEvent newQuestionEvent);

	void visit(UnlockQuestionsEvent newQuestionsEvent);

	void visit(LockQuestionEvent lockQuestionEvent);

	void visit(LockQuestionsEvent lockQuestionsEvent);

	void visit(NewAnswerEvent newAnswerEvent);

	void visit(DeleteAnswerEvent deleteAnswerEvent);

	void visit(DeleteQuestionEvent deleteQuestionEvent);

	void visit(DeleteAllQuestionsAnswersEvent deleteAllAnswersEvent);

	void visit(DeleteAllPreparationAnswersEvent deleteAllPreparationAnswersEvent);

	void visit(DeleteAllLectureAnswersEvent deleteAllLectureAnswersEvent);

	void visit(NewFeedbackEvent newFeedbackEvent);

	void visit(DeleteFeedbackForSessionsEvent deleteFeedbackEvent);

	void visit(StatusSessionEvent statusSessionEvent);

	void visit(ChangeLearningProgressEvent changeLearningProgress);

	void visit(PiRoundDelayedStartEvent piRoundDelayedStartEvent);

	void visit(PiRoundEndEvent piRoundEndEvent);

	void visit(PiRoundCancelEvent piRoundCancelEvent);

	void visit(PiRoundResetEvent piRoundResetEvent);

	void visit(NewSessionEvent newSessionEvent);

	void visit(DeleteSessionEvent deleteSessionEvent);

	void visit(LockVotingEvent lockVotingEvent);

}
