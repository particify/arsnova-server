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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.thm.arsnova.entities.User;

public class QuestionScore implements Iterable<UserScore> {

	private String questionId;

	private String questionVariant;

	private int maximumScore;

	private List<UserScore> userScores = new ArrayList<UserScore>();

	public QuestionScore(String questionId, String questionVariant, int maximumScore) {
		this.questionId = questionId;
		this.questionVariant = questionVariant;
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
		return this.userScores.size() > 0;
	}

	public void add(String username, int userscore) {
		userScores.add(new UserScore(username, userscore));
	}

	public int getTotalUserScore() {
		int totalScore = 0;
		for (UserScore score : userScores) {
			totalScore += score.getScore();
		}
		return totalScore;
	}

	public int getTotalUserScore(User user) {
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
