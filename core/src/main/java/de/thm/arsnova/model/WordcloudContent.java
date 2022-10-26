package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import de.thm.arsnova.model.serialization.View;

public class WordcloudContent extends Content {
	@Min(1)
	@Max(10)
	private int maxAnswers = 1;

	@JsonView({View.Persistence.class, View.Public.class})
	public int getMaxAnswers() {
		return maxAnswers;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setMaxAnswers(final int maxAnswers) {
		this.maxAnswers = maxAnswers;
	}
}
