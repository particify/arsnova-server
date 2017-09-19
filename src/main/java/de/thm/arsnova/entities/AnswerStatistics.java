package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

import java.util.List;

public class AnswerStatistics {
	public class RoundStatistics {
		public class Combination {
			private int[] selectedChoiceIndexes;
			private int count;
		}

		private int round;
		private int[] independentCounts;
		private List<Combination> combinatedCounts;
		private int abstentionCount;

		@JsonView(View.Public.class)
		public int getRound() {
			return round;
		}

		public void setRound(int round) {
			this.round = round;
		}

		@JsonView(View.Public.class)
		public int[] getIndependentCounts() {
			return independentCounts;
		}

		public void setIndependentCounts(final int[] independentCounts) {
			this.independentCounts = independentCounts;
		}

		@JsonView(View.Public.class)
		public List<Combination> getCombinatedCounts() {
			return combinatedCounts;
		}

		public void setCombinatedCounts(List<Combination> combinatedCounts) {
			this.combinatedCounts = combinatedCounts;
		}

		@JsonView(View.Public.class)
		public int getAbstentionCount() {
			return abstentionCount;
		}

		public void setAbstentionCount(int abstentionCount) {
			this.abstentionCount = abstentionCount;
		}
	}

	public class RoundTransition {
		private int roundA;
		private int roundB;
		private int[] selectedChoiceIndexesA;
		private int[] selectedChoiceIndexesB;
		private int count;
	}

	private String contentId;
	private List<RoundStatistics> roundStatistics;
	private List<RoundTransition> roundTransitions;

	@JsonView(View.Public.class)
	public String getContentId() {
		return contentId;
	}

	public void setContentId(final String contentId) {
		this.contentId = contentId;
	}

	@JsonView(View.Public.class)
	public List<RoundStatistics> getRoundStatistics() {
		return roundStatistics;
	}

	public void setRoundStatistics(List<RoundStatistics> roundStatistics) {
		this.roundStatistics = roundStatistics;
	}

	@JsonView(View.Public.class)
	public List<RoundTransition> getRoundTransitions() {
		return roundTransitions;
	}

	public void setRoundTransitions(List<RoundTransition> roundTransitions) {
		this.roundTransitions = roundTransitions;
	}
}
