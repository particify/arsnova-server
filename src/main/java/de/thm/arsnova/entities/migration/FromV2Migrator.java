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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts legacy entities from version 2 to current model version.
 *
 * @author Daniel Gerhardt
 */
public class FromV2Migrator {
	static final String V2_TYPE_ABCD = "abcd";
	static final String V2_TYPE_SC = "sc";
	static final String V2_TYPE_MC = "mc";
	static final String V2_TYPE_VOTE = "vote";
	static final String V2_TYPE_SCHOOL = "school";
	static final String V2_TYPE_YESNO = "yesno";
	static final String V2_TYPE_FREETEXT = "freetext";
	static final String V2_TYPE_GRID = "grid";
	private static final Map<String, de.thm.arsnova.entities.Content.Format> formatMapping;

	private boolean ignoreRevision = false;

	static {
		formatMapping = new HashMap<>();
		formatMapping.put(V2_TYPE_ABCD, de.thm.arsnova.entities.Content.Format.CHOICE);
		formatMapping.put(V2_TYPE_SC, de.thm.arsnova.entities.Content.Format.CHOICE);
		formatMapping.put(V2_TYPE_MC, de.thm.arsnova.entities.Content.Format.CHOICE);
		formatMapping.put(V2_TYPE_VOTE, de.thm.arsnova.entities.Content.Format.SCALE);
		formatMapping.put(V2_TYPE_SCHOOL, de.thm.arsnova.entities.Content.Format.SCALE);
		formatMapping.put(V2_TYPE_YESNO, de.thm.arsnova.entities.Content.Format.BINARY);
		formatMapping.put(V2_TYPE_FREETEXT, de.thm.arsnova.entities.Content.Format.TEXT);
		formatMapping.put(V2_TYPE_GRID, de.thm.arsnova.entities.Content.Format.GRID);
	}

	private void copyCommonProperties(final Entity from, final de.thm.arsnova.entities.Entity to) {
		to.setId(from.getId());
		if (!ignoreRevision) {
			to.setRevision(from.getRevision());
		}
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
			Set<UserProfile.RoomHistoryEntry> sessionHistory = loggedIn.getVisitedSessions().stream()
					.map(entry -> new UserProfile.RoomHistoryEntry(entry.getId(), new Date(0)))
					.collect(Collectors.toSet());
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
			settings.setCommentsEnabled(feature.isInterposed() || feature.isInterposedFeedback()
					|| feature.isTwitterWall() || feature.isTotal());
			settings.setQuestionsEnabled(feature.isLecture() || feature.isJitt() || feature.isClicker() || feature.isTotal());
			settings.setSlidesEnabled(feature.isSlides() || feature.isTotal());
			settings.setFlashcardsEnabled(feature.isFlashcardFeature() || feature.isFlashcard() || feature.isTotal());
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
			case V2_TYPE_ABCD:
			case V2_TYPE_SC:
			case V2_TYPE_MC:
			case V2_TYPE_VOTE:
			case V2_TYPE_SCHOOL:
			case V2_TYPE_YESNO:
				ChoiceQuestionContent choiceQuestionContent = new ChoiceQuestionContent();
				to = choiceQuestionContent;
				to.setFormat(formatMapping.get(from.getQuestionType()));
				choiceQuestionContent.setMultiple(V2_TYPE_MC.equals(from.getQuestionType()));
				for (int i = 0; i < from.getPossibleAnswers().size(); i++) {
					de.thm.arsnova.entities.migration.v2.AnswerOption fromOption = from.getPossibleAnswers().get(i);
					ChoiceQuestionContent.AnswerOption toOption = new ChoiceQuestionContent.AnswerOption();
					toOption.setLabel(fromOption.getText());
					toOption.setPoints(fromOption.getValue());
					choiceQuestionContent.getOptions().add(toOption);
					if (fromOption.isCorrect()) {
						choiceQuestionContent.getCorrectOptionIndexes().add(i);
					}
				}

				break;
			case V2_TYPE_FREETEXT:
				to = new de.thm.arsnova.entities.Content();
				to.setFormat(de.thm.arsnova.entities.Content.Format.TEXT);
				break;
			default:
				throw new IllegalArgumentException("Unsupported content format.");
		}
		copyCommonProperties(from, to);
		to.setRoomId(from.getSessionId());
		to.getGroups().add(from.getQuestionVariant());
		to.setSubject(from.getSubject());
		to.setBody(from.getText());
		to.setAbstentionsAllowed(from.isAbstention());
		to.setAbstentionsAllowed(from.isAbstention());
		de.thm.arsnova.entities.Content.State state = to.getState();
		state.setRound(from.getPiRound());
		state.setVisible(from.isActive());
		state.setResponsesVisible(from.isShowStatistic());
		state.setSolutionVisible(from.isShowAnswer());
		state.setResponsesEnabled(!from.isVotingDisabled());

		return to;
	}

	public de.thm.arsnova.entities.Answer migrate(final Answer from, final Content content) {
		switch (content.getQuestionType()) {
			case V2_TYPE_ABCD:
			case V2_TYPE_VOTE:
			case V2_TYPE_SCHOOL:
			case V2_TYPE_YESNO:
				return migrate(from, content.getPossibleAnswers(), false);
			case V2_TYPE_MC:
				return migrate(from, content.getPossibleAnswers(), true);
			case V2_TYPE_FREETEXT:
				return migrate(from);
			default:
				throw new IllegalArgumentException("Unsupported content format.");
		}
	}

	public ChoiceAnswer migrate(final Answer from, final List<AnswerOption> options, final boolean multiple) {
		final ChoiceAnswer to = new ChoiceAnswer();
		copyCommonProperties(from, to);
		to.setContentId(from.getQuestionId());
		to.setRoomId(from.getSessionId());
		to.setRound(from.getPiRound());
		List<Integer> selectedChoiceIndexes = new ArrayList<>();
		to.setSelectedChoiceIndexes(selectedChoiceIndexes);

		if (!from.isAbstention()) {
			if (multiple) {
				List<Boolean> flags = Arrays.stream(from.getAnswerText().split(","))
						.map("1"::equals).collect(Collectors.toList());
				if (flags.size() != options.size()) {
					throw new IndexOutOfBoundsException(
							"Number of answer's choice flags does not match number of content's answer options");
				}
				int i = 0;
				for (boolean flag : flags) {
					if (flag) {
						selectedChoiceIndexes.add(i);
					}
					i++;
				}
			} else {
				int i = 0;
				for (AnswerOption option : options) {
					if (option.getText().equals(from.getAnswerText())) {
						selectedChoiceIndexes.add(i);
						break;
					}
					i++;
				}
			}
		}

		return to;
	}

	public TextAnswer migrate(final Answer from) {
		final TextAnswer to = new TextAnswer();
		copyCommonProperties(from, to);
		to.setContentId(from.getQuestionId());
		to.setRoomId(from.getSessionId());
		to.setRound(from.getPiRound());
		to.setSubject(from.getAnswerSubject());
		to.setBody(from.getAnswerText());

		return to;
	}

	public de.thm.arsnova.entities.Comment migrate(final Comment from, @Nullable final UserProfile creator) {
		if (creator == null && from.getCreator() != null ||
				creator != null && !creator.getLoginId().equals(from.getCreator())) {
			throw new IllegalArgumentException("Username of creator object does not match comment creator.");
		}
		final de.thm.arsnova.entities.Comment to = new de.thm.arsnova.entities.Comment();
		copyCommonProperties(from, to);
		to.setRoomId(from.getSessionId());
		if (creator != null) {
			to.setCreatorId(creator.getId());
		}
		to.setSubject(from.getSubject());
		to.setBody(from.getText());
		to.setTimestamp(new Date(from.getTimestamp()));
		to.setRead(from.isRead());

		return to;
	}


	public de.thm.arsnova.entities.Comment migrate(final Comment from) {
		return migrate(from, null);
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

	public void setIgnoreRevision(final boolean ignoreRevision) {
		this.ignoreRevision = ignoreRevision;
	}
}
