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

/**
 * The learning progress answer score of a particular user.
 */
public class UserScore {

	private String username;

	private int score;

	public UserScore(String username, int score) {
		this.username = username;
		this.score = score;
	}

	public boolean hasScore(int score) {
		return this.score == score;
	}

	public int getScore() {
		return score;
	}

	public boolean isUser(ClientAuthentication user) {
		return user.getUsername().equals(username);
	}

	public String getUsername() {
		return username;
	}
}
