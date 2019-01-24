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
package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.model.serialization.View;
import org.springframework.core.style.ToStringCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChoiceQuestionContent extends Content {
	public static class AnswerOption {
		private String label;
		private int points;

		@JsonView({View.Persistence.class, View.Public.class})
		public String getLabel() {
			return label;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setLabel(String label) {
			this.label = label;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public int getPoints() {
			return points;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setPoints(int points) {
			this.points = points;
		}

		@Override
		public int hashCode() {
			return Objects.hash(label, points);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (!super.equals(o)) {
				return false;
			}
			final AnswerOption that = (AnswerOption) o;

			return points == that.points &&
					Objects.equals(label, that.label);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("label", label)
					.append("points", points)
					.toString();
		}

	}

	private List<AnswerOption> options = new ArrayList<>();
	private List<Integer> correctOptionIndexes = new ArrayList<>();
	private boolean multiple;

	@JsonView({View.Persistence.class, View.Public.class})
	public List<AnswerOption> getOptions() {
		return options;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setOptions(final List<AnswerOption> options) {
		this.options = options;
	}

	/* TODO: A new JsonView is needed here. */
	@JsonView(View.Persistence.class)
	public List<Integer> getCorrectOptionIndexes() {
		return correctOptionIndexes;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setCorrectOptionIndexes(final List<Integer> correctOptionIndexes) {
		this.correctOptionIndexes = correctOptionIndexes;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isMultiple() {
		return multiple;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setMultiple(final boolean multiple) {
		this.multiple = multiple;
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("options", options)
				.append("correctOptionIndexes", correctOptionIndexes)
				.append("multiple", multiple);
	}
}
