package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import de.thm.arsnova.model.serialization.View;

public class MultipleTextsAnswer extends Answer {
	private @NotNull List<@NotBlank String> texts = new ArrayList<>();

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
