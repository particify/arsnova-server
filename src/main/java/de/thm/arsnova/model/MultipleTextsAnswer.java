package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import javax.validation.constraints.NotEmpty;

import de.thm.arsnova.model.serialization.View;

public class MultipleTextsAnswer extends Answer {
	private @NotEmpty List<String> texts;

	public MultipleTextsAnswer() {

	}

	public MultipleTextsAnswer(final Content content, final String creatorId) {
		super(content, creatorId);
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public List<String> getTexts() {
		return texts;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setTexts(final List<String> texts) {
		this.texts = texts;
	}

	@Override
	public boolean isAbstention() {
		return texts.isEmpty();
	}
}
