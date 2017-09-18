/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.entities.migration;

import de.thm.arsnova.entities.ChoiceAnswer;
import de.thm.arsnova.entities.ChoiceQuestionContent;
import de.thm.arsnova.entities.Entity;
import de.thm.arsnova.entities.TextAnswer;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.entities.migration.v2.Answer;
import de.thm.arsnova.entities.migration.v2.AnswerOption;
import de.thm.arsnova.entities.migration.v2.Comment;
import de.thm.arsnova.entities.migration.v2.Content;
import de.thm.arsnova.entities.migration.v2.DbUser;
import de.thm.arsnova.entities.migration.v2.LoggedIn;
import de.thm.arsnova.entities.migration.v2.MotdList;
import de.thm.arsnova.entities.migration.v2.Room;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts legacy entities from version 2 to current model version.
 *
 * @author Daniel Gerhardt
 */
public class FromV2Migrator {
	private void copyCommonProperties(final Entity from, final Entity to) {
		to.setId(from.getId());
		to.setRevision(from.getRevision());
	}

	public UserProfile migrate(final DbUser dbUser, final LoggedIn loggedIn, final MotdList motdList) {
		if (dbUser != null && loggedIn != null && !loggedIn.getUser().equals(dbUser.getUsername())) {
			throw new IllegalArgumentException("Username of loggedIn object does not match.");
		}
		if (dbUser != null && motdList != null && !motdList.getUsername().equals(dbUser.getUsername())) {
			throw new IllegalArgumentException("Username of motdList object does not match.");
		}
		if (loggedIn != null && motdList != null && !loggedIn.getUser().equals(motdList.getUsername())) {
			throw new IllegalArgumentException("Usernames of loggedIn and motdList objects do not match.");
		}
		final UserProfile profile = new UserProfile();
		if (dbUser != null) {
			profile.setLoginId(dbUser.getUsername());
			UserProfile.Account account = profile.new Account();
			profile.setAccount(account);
			account.setPassword(dbUser.getPassword());
			account.setActivationKey(dbUser.getActivationKey());
			account.setPasswordResetKey(dbUser.getPasswordResetKey());
			account.setPasswordResetTime(dbUser.getPasswordResetTime());
		}
		if (loggedIn != null) {
			profile.setLoginId(loggedIn.getUser());
			profile.setLastLogin(loggedIn.getTimestamp());
			List<UserProfile.RoomHistoryEntry> sessionHistory = loggedIn.getVisitedSessions().stream()
					.map(entry -> profile.new RoomHistoryEntry(entry.getId(), 0))
					.collect(Collectors.toList());
			profile.setRoomHistory(sessionHistory);
		}
		if (motdList != null && motdList.getMotdkeys() != null) {
			profile.setAcknowledgedMotds(Arrays.stream(motdList.getMotdkeys().split(",")).collect(Collectors.toSet()));
		}

		return profile;
	}

	public de.thm.arsnova.entities.Room migrate(final Room from, final DbUser owner) {
		if (!owner.getUsername().equals(from.getCreator())) {
			throw new IllegalArgumentException("Username of owner object does not match session creator.");
		}
		final de.thm.arsnova.entities.Room to = new de.thm.arsnova.entities.Room();
		copyCommonProperties(from, to);
		to.setShortId(from.getKeyword());
		to.setOwnerId(owner.getId());
		to.setName(from.getName());
		to.setAbbreviation(from.getShortName());
		to.setClosed(!from.isActive());

		return to;
	}

	public de.thm.arsnova.entities.Content migrate(final Content from) {
		de.thm.arsnova.entities.Content to;
		switch (from.getQuestionType()) {
			case "abcd":
			case "mc":
				ChoiceQuestionContent choiceQuestionContent = new ChoiceQuestionContent();
				to = choiceQuestionContent;
				for (int i = 0; i < from.getPossibleAnswers().size(); i++) {
					de.thm.arsnova.entities.migration.v2.AnswerOption choice = from.getPossibleAnswers().get(i);
					if (choice.isCorrect()) {
						choiceQuestionContent.getCorrectOptionIndexes().add(i);
					}
				}

				break;
			case "text":
				to = new de.thm.arsnova.entities.Content();
				break;
			default:
				throw new IllegalArgumentException("Unsupported content format.");
		}
		copyCommonProperties(from, to);
		to.setRoomId(from.getSessionId());
		to.setSubject(from.getSubject());
		to.setBody(from.getText());
		to.setFormat(from.getQuestionType());
		to.setGroup(from.getQuestionVariant());

		return to;
	}

	public de.thm.arsnova.entities.Answer migrate(final Answer from, final Content content) {
		switch (content.getQuestionType()) {
			case "abcd":
			case "mc":
				return migrate(from, content.getPossibleAnswers());
			case "text":
				return migrate(from);
			default:
				throw new IllegalArgumentException("Unsupported content format.");
		}
	}

	public ChoiceAnswer migrate(final Answer from, final List<AnswerOption> options) {
		final ChoiceAnswer to = new ChoiceAnswer();
		copyCommonProperties(from, to);
		to.setContentId(from.getQuestionId());
		List<Integer> selectedChoiceIndexes = new ArrayList<>();
		to.setSelectedChoiceIndexes(selectedChoiceIndexes);

		for (int i = 0; i < options.size(); i++) {
			AnswerOption choice = options.get(i);
			if (choice.getText().equals(from.getAnswerText())) {
				selectedChoiceIndexes.add(i);
			}
		}

		return to;
	}

	public TextAnswer migrate(final Answer from) {
		final TextAnswer to = new TextAnswer();
		copyCommonProperties(from, to);
		to.setContentId(from.getQuestionId());
		to.setSubject(from.getAnswerSubject());
		to.setBody(from.getAnswerText());

		return to;
	}

	public de.thm.arsnova.entities.Comment migrate(final Comment from, final DbUser creator) {
		if (!creator.getUsername().equals(from.getCreator())) {
			throw new IllegalArgumentException("Username of creator object does not match comment creator.");
		}
		final de.thm.arsnova.entities.Comment to = new de.thm.arsnova.entities.Comment();
		copyCommonProperties(from, to);
		to.setSessionId(from.getSessionId());
		to.setCreatorId(creator.getId());
		to.setSubject(from.getSubject());
		to.setBody(from.getText());
		to.setTimestamp(from.getTimestamp());
		to.setRead(from.isRead());

		return to;
	}
}
