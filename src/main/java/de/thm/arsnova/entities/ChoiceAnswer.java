package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.migration.v2.AnswerOption;
import de.thm.arsnova.entities.serialization.View;

import java.util.List;

public class ChoiceAnswer extends Answer {
	private List<Integer> selectedChoiceIndexes;

	@JsonView({View.Persistence.class, View.Public.class})
	public List<Integer> getSelectedChoiceIndexes() {
		return selectedChoiceIndexes;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setSelectedChoiceIndexes(final List<Integer> selectedChoiceIndexes) {
		this.selectedChoiceIndexes = selectedChoiceIndexes;
	}
}
