package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;

import de.thm.arsnova.model.serialization.View;

public class PriorizationChoiceContent extends ChoiceQuestionContent {
	private int assignablePoints = 100;

	public PriorizationChoiceContent() {

	}

	public PriorizationChoiceContent(final PriorizationChoiceContent content) {
		super(content);
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public int getAssignablePoints() {
		return assignablePoints;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAssignablePoints(final int assignablePoints) {
		this.assignablePoints = assignablePoints;
	}
}
