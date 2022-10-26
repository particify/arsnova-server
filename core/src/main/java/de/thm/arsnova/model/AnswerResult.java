package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;

import de.thm.arsnova.model.serialization.View;

@JsonView(View.Public.class)
public class AnswerResult {
	private String contentId;
	private double achievedPoints;
	private int maxPoints;
	private AnswerResultState state;

	public AnswerResult(
			final String contentId,
			final double achievedPoints,
			final int maxPoints,
			final AnswerResultState state) {
		this.contentId = contentId;
		this.achievedPoints = achievedPoints;
		this.maxPoints = maxPoints;
		this.state = state;
	}

	public String getContentId() {
		return contentId;
	}

	public double getAchievedPoints() {
		return achievedPoints;
	}

	public int getMaxPoints() {
		return maxPoints;
	}

	public AnswerResultState getState() {
		return state;
	}

	public void setState(final AnswerResultState state) {
		this.state = state;
	}

	public enum AnswerResultState {
		UNANSWERED,
		ABSTAINED,
		CORRECT,
		WRONG,
		NEUTRAL
	}
}
