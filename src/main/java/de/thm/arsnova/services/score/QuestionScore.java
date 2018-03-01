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

import de.thm.arsnova.entities.migration.v2.ClientAuthentication;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Calculates score for a specific question.
 */
public class QuestionScore implements Iterable<UserScore> {

	/* FIXME: what is questionId used for? */
	private String questionId;

	private String questionVariant;

	private int piRound;

	private int maximumScore;

	private List<UserScore> userScores = new ArrayList<>();

	public QuestionScore(String questionId, String questionVariant, int piRound, int maximumScore) {
		this.questionId = questionId;
		this.questionVariant = questionVariant;
		this.piRound = piRound;
		this.maximumScore = maximumScore;
	}

	public int getMaximum() {
		return this.maximumScore;
	}

	@Override
	public Iterator<UserScore> iterator() {
		return this.userScores.iterator();
	}

	public boolean hasScores() {
		return !this.userScores.isEmpty();
	}

	public void add(int piRound, String username, int userscore) {
		if (this.piRound == piRound) {
			userScores.add(new UserScore(username, userscore));
		}
	}

	public int getTotalUserScore() {
		int totalScore = 0;
		for (UserScore score : userScores) {
			totalScore += score.getScore();
		}
		return totalScore;
	}

	public int getTotalUserScore(ClientAuthentication user) {
		int totalScore = 0;
		for (UserScore score : userScores) {
			if (score.isUser(user)) {
				totalScore += score.getScore();
			}
		}
		return totalScore;
	}

	public int getUserCount() {
		return userScores.size();
	}

	public void collectUsers(Set<String> users) {
		for (UserScore score : userScores) {
			users.add(score.getUsername());
		}
	}

	public boolean isVariant(String questionVariant) {
		return this.questionVariant.equals(questionVariant);
	}
}
