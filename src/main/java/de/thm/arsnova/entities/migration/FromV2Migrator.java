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
import de.thm.arsnova.entities.TextAnswer;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.entities.migration.v2.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts legacy entities from version 2 to current model version.
 *
 * @author Daniel Gerhardt
 */
public class FromV2Migrator {
	private void copyCommonProperties(final Entity from, final de.thm.arsnova.entities.Entity to) {
		to.setId(from.getId());
		//to.setRevision(from.getRevision());
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
			copyCommonProperties(dbUser, profile);
			profile.setLoginId(dbUser.getUsername());
			profile.setAuthProvider(UserProfile.AuthProvider.ARSNOVA);
			profile.setCreationTimestamp(new Date(dbUser.getCreation()));
			profile.setUpdateTimestamp(new Date());
			UserProfile.Account account = new UserProfile.Account();
			profile.setAccount(account);
			account.setPassword(dbUser.getPassword());
			account.setActivationKey(dbUser.getActivationKey());
			account.setPasswordResetKey(dbUser.getPasswordResetKey());
			account.setPasswordResetTime(new Date(dbUser.getPasswordResetTime()));
		}
		if (loggedIn != null) {
			if (dbUser == null) {
				copyCommonProperties(loggedIn, profile);
				profile.setLoginId(loggedIn.getUser());
				profile.setAuthProvider(detectAuthProvider(profile.getLoginId()));
				profile.setCreationTimestamp(new Date());
			}
			profile.setLastLoginTimestamp(new Date(loggedIn.getTimestamp()));
			List<UserProfile.RoomHistoryEntry> sessionHistory = loggedIn.getVisitedSessions().stream()
					.map(entry -> new UserProfile.RoomHistoryEntry(entry.getId(), new Date(0)))
					.collect(Collectors.toList());
			profile.setRoomHistory(sessionHistory);
		}
		if (motdList != null && motdList.getMotdkeys() != null) {
			profile.setAcknowledgedMotds(migrate(motdList));
		}

		return profile;
	}

	public Set<String> migrate(final MotdList motdList) {
		return Arrays.stream(motdList.getMotdkeys().split(",")).collect(Collectors.toSet());
	}

	public de.thm.arsnova.entities.Room migrate(final Room from, final Optional<UserProfile> owner) {
		if (!owner.isPresent() && from.getCreator() != null ||
				owner.isPresent() && !owner.get().getLoginId().equals(from.getCreator())) {
			throw new IllegalArgumentException("Username of owner object does not match session creator.");
		}
		final de.thm.arsnova.entities.Room to = new de.thm.arsnova.entities.Room();
		copyCommonProperties(from, to);
		to.setCreationTimestamp(new Date(from.getCreationTime()));
		to.setUpdateTimestamp(new Date());
		to.setShortId(from.getKeyword());
		if (owner.isPresent()) {
			to.setOwnerId(owner.get().getId());
		}
		to.setName(from.getName());
		to.setAbbreviation(from.getShortName());
		to.setDescription(from.getPpDescription());
		to.setClosed(!from.isActive());
		if (from.hasAuthorDetails()) {
			final de.thm.arsnova.entities.Room.Author author = new de.thm.arsnova.entities.Room.Author();
			to.setAuthor(author);
			author.setName(from.getPpAuthorName());
			author.setMail(from.getPpAuthorMail());
			author.setOrganizationName(from.getPpUniversity());
			author.setOrganizationUnit(from.getPpFaculty());
			author.setOrganizationLogo(from.getPpLogo());
		}
		if ("public_pool".equals(from.getSessionType())) {
			final de.thm.arsnova.entities.Room.PoolProperties poolProperties = new de.thm.arsnova.entities.Room.PoolProperties();
			to.setPoolProperties(poolProperties);
			poolProperties.setLevel(from.getPpLevel());
			poolProperties.setCategory(from.getPpSubject());
			poolProperties.setLicense(from.getPpLicense());
		}
		to.setSettings(migrate(from.getFeatures()));

		return to;
	}

	public de.thm.arsnova.entities.Room migrate(final Room from) {
		return migrate(from, Optional.empty());
	}

	public de.thm.arsnova.entities.Room.Settings migrate(final RoomFeature feature) {
		de.thm.arsnova.entities.Room.Settings settings = new de.thm.arsnova.entities.Room.Settings();
		if (feature != null) {
			settings.setCommentsEnabled(feature.isInterposed() || feature.isInterposedFeedback() || feature.isTotal());
			settings.setQuestionsEnabled(feature.isLecture() || feature.isJitt() || feature.isClicker() || feature.isTotal());
			settings.setSlidesEnabled(feature.isSlides() || feature.isTotal());
			settings.setFlashcardsEnabled(feature.isFlashcard() || feature.isFlashcardFeature() || feature.isTotal());
			settings.setQuickSurveyEnabled(feature.isLiveClicker());
			settings.setQuickFeedbackEnabled(feature.isFeedback() || feature.isLiveFeedback() || feature.isTotal());
			settings.setMultipleRoundsEnabled(feature.isPi() || feature.isClicker() || feature.isTotal());
			settings.setTimerEnabled(feature.isPi() || feature.isClicker() || feature.isTotal());
			settings.setScoreEnabled(feature.isLearningProgress() || feature.isTotal());
		}

		return settings;
	}

	public de.thm.arsnova.entities.Content migrate(final Content from) {
		de.thm.arsnova.entities.Content to;
		switch (from.getQuestionType()) {
			case "abcd":
			case "mc":
				ChoiceQuestionContent choiceQuestionContent = new ChoiceQuestionContent();
				to = choiceQuestionContent;
				to.setFormat(de.thm.arsnova.entities.Content.Format.CHOICE);
				choiceQuestionContent.setMultiple("mc".equals(from.getQuestionType()));
				for (int i = 0; i < from.getPossibleAnswers().size(); i++) {
					de.thm.arsnova.entities.migration.v2.AnswerOption choice = from.getPossibleAnswers().get(i);
					if (choice.isCorrect()) {
						choiceQuestionContent.getCorrectOptionIndexes().add(i);
					}
				}

				break;
			case "text":
				to = new de.thm.arsnova.entities.Content();
				to.setFormat(de.thm.arsnova.entities.Content.Format.TEXT);
				break;
			default:
				throw new IllegalArgumentException("Unsupported content format.");
		}
		copyCommonProperties(from, to);
		to.setRoomId(from.getSessionId());
		to.setSubject(from.getSubject());
		to.setBody(from.getText());
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

	public de.thm.arsnova.entities.Comment migrate(final Comment from, final UserProfile creator) {
		if (!creator.getLoginId().equals(from.getCreator())) {
			throw new IllegalArgumentException("Username of creator object does not match comment creator.");
		}
		final de.thm.arsnova.entities.Comment to = new de.thm.arsnova.entities.Comment();
		copyCommonProperties(from, to);
		to.setRoomId(from.getSessionId());
		to.setCreatorId(creator.getId());
		to.setSubject(from.getSubject());
		to.setBody(from.getText());
		to.setTimestamp(new Date(from.getTimestamp()));
		to.setRead(from.isRead());

		return to;
	}

	public de.thm.arsnova.entities.Motd migrate(final Motd from) {
		final de.thm.arsnova.entities.Motd to = new de.thm.arsnova.entities.Motd();
		copyCommonProperties(from, to);
		to.setCreationTimestamp(from.getStartdate());
		to.setUpdateTimestamp(new Date());
		to.setStartDate(from.getStartdate());
		to.setEndDate(from.getEnddate());
		switch (from.getAudience()) {
			case "all":
				to.setAudience(de.thm.arsnova.entities.Motd.Audience.ALL);
				break;
			case "tutors":
				to.setAudience(de.thm.arsnova.entities.Motd.Audience.AUTHORS);
				break;
			case "students":
				to.setAudience(de.thm.arsnova.entities.Motd.Audience.PARTICIPANTS);
				break;
			case "session":
				to.setAudience(de.thm.arsnova.entities.Motd.Audience.ROOM);
				break;
		}
		to.setTitle(from.getTitle());
		to.setBody(from.getText());
		to.setRoomId(from.getSessionId());

		return to;
	}

	private UserProfile.AuthProvider detectAuthProvider(final String loginId) {
		if (loginId.length() == 15 && loginId.startsWith("Guest")) {
			return UserProfile.AuthProvider.ARSNOVA_GUEST;
		}
		if (loginId.startsWith("https://www.facebook.com/") || loginId.startsWith("http://www.facebook.com/")) {
			return UserProfile.AuthProvider.FACEBOOK;
		}
		if (loginId.contains("@")) {
			return UserProfile.AuthProvider.GOOGLE;
		}

		return UserProfile.AuthProvider.UNKNOWN;
	}
}
