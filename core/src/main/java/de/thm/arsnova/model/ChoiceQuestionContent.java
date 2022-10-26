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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotBlank;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class ChoiceQuestionContent extends Content {
	public static class AnswerOption {
		public AnswerOption() {

		}

		public AnswerOption(final String label) {
			this.label = label;
		}

		@NotBlank
		private String label;

		private String renderedLabel;

		@JsonView({View.Persistence.class, View.Public.class})
		public String getLabel() {
			return label;
		}

		@JsonView({View.Persistence.class, View.Public.class})
		public void setLabel(final String label) {
			this.label = label;
		}

		@JsonView(View.Public.class)
		public String getRenderedLabel() {
			return renderedLabel;
		}

		public void setRenderedLabel(final String renderedLabel) {
			this.renderedLabel = renderedLabel;
		}

		@Override
		public int hashCode() {
			return Objects.hash(label);
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

			return Objects.equals(label, that.label);
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("label", label)
					.toString();
		}

	}

	private List<AnswerOption> options = new ArrayList<>();
	private List<Integer> correctOptionIndexes = new ArrayList<>();
	private boolean multiple;

	public ChoiceQuestionContent() {

	}

	public ChoiceQuestionContent(final ChoiceQuestionContent content) {
		super(content);
		this.options = content.options;
		this.correctOptionIndexes = content.correctOptionIndexes;
		this.multiple = content.multiple;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public List<AnswerOption> getOptions() {
		return options;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setOptions(final List<AnswerOption> options) {
		this.options = options;
	}

	@JsonView({View.Persistence.class, View.Extended.class})
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
