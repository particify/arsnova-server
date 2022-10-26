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
import java.util.List;
import javax.validation.constraints.PositiveOrZero;
import org.springframework.core.style.ToStringCreator;

import de.thm.arsnova.model.serialization.View;

public class ChoiceAnswer extends Answer {
	private List<@PositiveOrZero Integer> selectedChoiceIndexes;

	public ChoiceAnswer() {

	}

	public ChoiceAnswer(final ChoiceQuestionContent content, final String creatorId) {
		super(content, creatorId);
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public List<Integer> getSelectedChoiceIndexes() {
		return selectedChoiceIndexes;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSelectedChoiceIndexes(final List<Integer> selectedChoiceIndexes) {
		this.selectedChoiceIndexes = selectedChoiceIndexes;
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("selectedChoiceIndexes", selectedChoiceIndexes);
	}
}
