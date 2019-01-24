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
package de.thm.arsnova.model.transport;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.model.serialization.View;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * The calculated score along with meta-data.
 */
@ApiModel(value = "session/{shortId}/learningprogress", description = "the score API")
public class ScoreStatistics {

	private int courseProgress;

	private int myProgress;

	private int numQuestions;

	private int numerator;

	private int denominator;

	private int numUsers;

	@ApiModelProperty(required = true, value = "used to display course score")
	@JsonView(View.Public.class)
	public int getCourseProgress() {
		return courseProgress;
	}

	public void setCourseProgress(int courseProgress) {
		this.courseProgress = courseProgress;
	}

	@ApiModelProperty(required = true, value = "used to display my score")
	@JsonView(View.Public.class)
	public int getMyProgress() {
		return myProgress;
	}

	public void setMyProgress(int myProgress) {
		this.myProgress = myProgress;
	}

	@ApiModelProperty(required = true, value = "used to display questions number")
	@JsonView(View.Public.class)
	public int getNumQuestions() {
		return numQuestions;
	}

	public void setNumQuestions(int numQuestions) {
		this.numQuestions = numQuestions;
	}

	@JsonView(View.Public.class)
	public int getNumerator() {
		return numerator;
	}

	public void setNumerator(int numerator) {
		this.numerator = numerator;
	}

	@JsonView(View.Public.class)
	public int getDenominator() {
		return denominator;
	}

	public void setDenominator(int denominator) {
		this.denominator = denominator;
	}

	@ApiModelProperty(required = true, value = "used to display user number")
	@JsonView(View.Public.class)
	public int getNumUsers() {
		return numUsers;
	}

	public void setNumUsers(int numUsers) {
		this.numUsers = numUsers;
	}

	@Override
	public int hashCode() {
		// auto generated!
		final int prime = 31;
		int result = 1;
		result = prime * result + courseProgress;
		result = prime * result + myProgress;
		result = prime * result + numQuestions;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// auto generated!
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ScoreStatistics other = (ScoreStatistics) obj;
		if (courseProgress != other.courseProgress) {
			return false;
		}
		if (myProgress != other.myProgress) {
			return false;
		}
		if (numQuestions != other.numQuestions) {
			return false;
		}
		return true;
	}
}
