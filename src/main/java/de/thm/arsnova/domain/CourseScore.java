/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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

import de.thm.arsnova.entities.User;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Calculates the learning progress for users and their courses.
 */
public class CourseScore implements Iterable<QuestionScore> {

	private final Map<String, QuestionScore> scores;

	public CourseScore() {
		this(new HashMap<String, QuestionScore>());
	}

	public CourseScore(Map<String, QuestionScore> theScores) {
		this.scores = theScores;
	}

	public void addQuestion(String questionId, String questionVariant, int piRound, int questionScore) {
		if (questionScore == 0) {
			return;
		}
		scores.put(questionId, new QuestionScore(questionId, questionVariant, piRound, questionScore));
	}

	public void addAnswer(String questionId, int piRound, String username, int userscore) {
		if (!scores.containsKey(questionId)) {
			// Precondition failed, ignore this element.
			// Most likely this is a question that has no learning progress value.
			return;
		}
		if (username == null || username.isEmpty()) {
			// Precondition failed: ignore anonymous users
			return;
		}
		QuestionScore questionScore = scores.get(questionId);
		questionScore.add(piRound, username, userscore);
	}

	public CourseScore filterVariant(String questionVariant) {
		Map<String, QuestionScore> newScores = new HashMap<>();
		for (Entry<String, QuestionScore> entry : this.scores.entrySet()) {
			String questionId = entry.getKey();
			QuestionScore questionScore = entry.getValue();
			if (questionScore.isVariant(questionVariant)) {
				newScores.put(questionId, questionScore);
			}
		}
		return new CourseScore(newScores);
	}

	public int getMaximumScore() {
		int score = 0;
		for (QuestionScore questionScore : this) {
			score += questionScore.getMaximum();
		}
		return score;
	}

	public int getTotalUserScore() {
		int score = 0;
		for (QuestionScore questionScore : this) {
			score += questionScore.getTotalUserScore();
		}
		return score;
	}

	public double getTotalUserScore(User user) {
		int score = 0;
		for (QuestionScore questionScore : this) {
			score += questionScore.getTotalUserScore(user);
		}
		return score;
	}

	public int getTotalUserCount() {
		Set<String> users = new HashSet<>();
		for (QuestionScore questionScore : this) {
			questionScore.collectUsers(users);
		}
		return users.size();
	}

	public int getQuestionCount() {
		return scores.size();
	}

	@Override
	public Iterator<QuestionScore> iterator() {
		return this.scores.values().iterator();
	}
}
