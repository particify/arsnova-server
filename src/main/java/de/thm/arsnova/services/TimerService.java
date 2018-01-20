package de.thm.arsnova.services;

import de.thm.arsnova.entities.UserAuthentication;

public interface TimerService {
	void startNewRound(final String contentId, UserAuthentication user);
	void startNewRoundDelayed(final String contentId, final int time);
	void cancelRoundChange(final String contentId);
	void cancelDelayedRoundChange(final String contentId);
	void resetRoundState(final String contentId);
}
