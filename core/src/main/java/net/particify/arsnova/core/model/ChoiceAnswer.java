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

package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.style.ToStringCreator;

import net.particify.arsnova.core.model.serialization.View;

public class ChoiceAnswer extends Answer {
  private List<@NotNull @PositiveOrZero Integer> selectedChoiceIndexes = new ArrayList<>();

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
  public boolean isAbstention() {
    return selectedChoiceIndexes.isEmpty();
  }

  @Override
  protected ToStringCreator buildToString() {
    return super.buildToString()
        .append("selectedChoiceIndexes", selectedChoiceIndexes);
  }
}
