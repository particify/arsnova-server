package de.thm.arsnova.service;

public interface TimerService {
	void startNewRound(final String contentId);
	void startNewRoundDelayed(final String contentId, final int time);
	void cancelRoundChange(final String contentId);
	void cancelDelayedRoundChange(final String contentId);
	void resetRoundState(final String contentId);
}
