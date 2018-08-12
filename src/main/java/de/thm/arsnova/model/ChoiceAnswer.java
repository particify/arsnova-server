package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.model.serialization.View;
import org.springframework.core.style.ToStringCreator;

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

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("selectedChoiceIndexes", selectedChoiceIndexes);
	}
}
