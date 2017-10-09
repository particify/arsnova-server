package de.thm.arsnova.services;

import de.thm.arsnova.entities.UserAuthentication;

public interface TimerService {
	void startNewPiRound(final String contentId, UserAuthentication user);
	void startNewPiRoundDelayed(final String contentId, final int time);
	void cancelPiRoundChange(final String contentId);
	void cancelDelayedPiRoundChange(final String contentId);
	void resetPiRoundState(final String contentId);
}
