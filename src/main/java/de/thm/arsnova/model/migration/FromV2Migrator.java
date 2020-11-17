/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
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

package de.thm.arsnova.model.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

import de.thm.arsnova.model.ChoiceAnswer;
import de.thm.arsnova.model.ChoiceQuestionContent;
import de.thm.arsnova.model.GridImageContent;
import de.thm.arsnova.model.TextAnswer;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.model.migration.v2.Answer;
import de.thm.arsnova.model.migration.v2.Comment;
import de.thm.arsnova.model.migration.v2.Content;
import de.thm.arsnova.model.migration.v2.DbUser;
import de.thm.arsnova.model.migration.v2.Entity;
import de.thm.arsnova.model.migration.v2.LoggedIn;
import de.thm.arsnova.model.migration.v2.Motd;
import de.thm.arsnova.model.migration.v2.MotdList;
import de.thm.arsnova.model.migration.v2.Room;
import de.thm.arsnova.model.migration.v2.RoomFeature;

/**
 * Converts legacy entities from version 2 to current model version.
 *
 * @author Daniel Gerhardt
 */
public class FromV2Migrator {
	static final String V2 = "v2";
	static final String V2_TYPE_ABCD = "abcd";
	static final String V2_TYPE_SC = "sc";
	static final String V2_TYPE_MC = "mc";
	static final String V2_TYPE_VOTE = "vote";
	static final String V2_TYPE_SCHOOL = "school";
	static final String V2_TYPE_YESNO = "yesno";
	static final String V2_TYPE_FREETEXT = "freetext";
	static final String V2_TYPE_SLIDE = "slide";
	static final String V2_TYPE_FLASHCARD = "flashcard";
	static final String V2_TYPE_GRID = "grid";
	static final String V2_GRID_DEFAULT_TYPE = "image";
	static final String V2_GRID_TYPE = "gridType";
	static final String V2_GRID_IMAGE_ABSOLUTE_X = "gridImageAbsoluteX";
	static final String V2_GRID_IMAGE_ABSOLUTE_Y = "gridImageAbsoluteY";
	static final String V2_GRID_MODERATION_DOT_LIMIT = "gridModerationDotLimit";
	static final int V2_GRID_CONTAINER_SIZE = 400;
	static final int V2_GRID_FIELD_COUNT = 16;
	static final double V2_GRID_SCALE_FACTOR = 1.05;
	private static final Map<String, de.thm.arsnova.model.Content.Format> formatMapping;
	private static final Pattern prefixedLabelPattern = Pattern.compile("^[A-Z]: .+");
	private static final Pattern labelPattern = Pattern.compile("^([A-Z]: )?(.+)");

	private boolean ignoreRevision = false;
	private UserProfile.AuthProvider authProviderFallback;
	private Map<String, String> contentGroupNames;

	static {
		formatMapping = new HashMap<>();
		formatMapping.put(V2_TYPE_ABCD, de.thm.arsnova.model.Content.Format.CHOICE);
		formatMapping.put(V2_TYPE_SC, de.thm.arsnova.model.Content.Format.CHOICE);
		formatMapping.put(V2_TYPE_MC, de.thm.arsnova.model.Content.Format.CHOICE);
		formatMapping.put(V2_TYPE_VOTE, de.thm.arsnova.model.Content.Format.SCALE);
		formatMapping.put(V2_TYPE_SCHOOL, de.thm.arsnova.model.Content.Format.SCALE);
		formatMapping.put(V2_TYPE_YESNO, de.thm.arsnova.model.Content.Format.BINARY);
		formatMapping.put(V2_TYPE_FREETEXT, de.thm.arsnova.model.Content.Format.TEXT);
		formatMapping.put(V2_TYPE_SLIDE, de.thm.arsnova.model.Content.Format.SLIDE);
		formatMapping.put(V2_TYPE_FLASHCARD, de.thm.arsnova.model.Content.Format.SLIDE);
		formatMapping.put(V2_TYPE_GRID, de.thm.arsnova.model.Content.Format.GRID);
	}

	public FromV2Migrator(
			final UserProfile.AuthProvider authProviderFallback,
			final Map<String, String> contentGroupNames) {
		this.authProviderFallback = authProviderFallback;
		this.contentGroupNames = contentGroupNames;
	}

	private void copyCommonProperties(final Entity from, final de.thm.arsnova.model.Entity to) {
		to.setId(from.getId());
		if (!ignoreRevision) {
			to.setRevision(from.getRevision());
		}
	}

	private Date migrateDate(final long unixDate) {
		/* Set new timestamp if existing one is too long in the past (format inconsistency). */
		if (unixDate == 0) {
			return new Date();
		} else if (unixDate < 1000000000000L) {
			return new Date(unixDate * 1000);
		} else {
			return new Date(unixDate);
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
		profile.setUpdateTimestamp(new Date());
		if (dbUser != null) {
			copyCommonProperties(dbUser, profile);
			profile.setLoginId(dbUser.getUsername());
			profile.setAuthProvider(UserProfile.AuthProvider.ARSNOVA);
			profile.setCreationTimestamp(migrateDate(dbUser.getCreation()));
			profile.setUpdateTimestamp(new Date());
			final UserProfile.Account account = new UserProfile.Account();
			profile.setAccount(account);
			account.setPassword(dbUser.getPassword());
			account.setActivationKey(dbUser.getActivationKey());
			account.setPasswordResetKey(dbUser.getPasswordResetKey());
			account.setPasswordResetTime(new Date(dbUser.getPasswordResetTime()));
		}
		if (loggedIn != null) {
			if (dbUser == null) {
				copyCommonProperties(loggedIn, profile);
				updateProfileFromLoginId(profile, loggedIn.getUser());
				profile.setCreationTimestamp(new Date());
			}
			profile.setLastLoginTimestamp(new Date(loggedIn.getTimestamp()));
			final Set<UserProfile.RoomHistoryEntry> sessionHistory = loggedIn.getVisitedSessions().stream()
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

	public de.thm.arsnova.model.Room migrate(
			final Room from,
			final Optional<UserProfile> owner,
			final boolean overrideOwner) {
		if (!owner.isPresent() && from.getCreator() != null
				|| owner.isPresent() && !overrideOwner && !owner.get().getLoginId().equals(from.getCreator())) {
			throw new IllegalArgumentException("Username of owner object does not match session creator.");
		}
		final de.thm.arsnova.model.Room to = new de.thm.arsnova.model.Room();
		copyCommonProperties(from, to);
		to.setCreationTimestamp(migrateDate(from.getCreationTime()));
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
			final de.thm.arsnova.model.Room.Author author = new de.thm.arsnova.model.Room.Author();
			to.setAuthor(author);
			author.setName(from.getPpAuthorName());
			author.setMail(from.getPpAuthorMail());
			author.setOrganizationName(from.getPpUniversity());
			author.setOrganizationUnit(from.getPpFaculty());
			author.setOrganizationLogo(from.getPpLogo());
		}
		if ("public_pool".equals(from.getSessionType())) {
			final de.thm.arsnova.model.Room.PoolProperties poolProperties = new de.thm.arsnova.model.Room.PoolProperties();
			to.setPoolProperties(poolProperties);
			poolProperties.setLevel(from.getPpLevel());
			poolProperties.setCategory(from.getPpSubject());
			poolProperties.setLicense(from.getPpLicense());
		}
		to.setSettings(migrate(from.getFeatures()));

		return to;
	}

	public de.thm.arsnova.model.Room migrate(final Room from) {
		return migrate(from, Optional.empty(), false);
	}

	public de.thm.arsnova.model.Room.Settings migrate(final RoomFeature feature) {
		final de.thm.arsnova.model.Room.Settings settings = new de.thm.arsnova.model.Room.Settings();
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

	public de.thm.arsnova.model.Content migrate(final Content from) {
		final de.thm.arsnova.model.Content to;
		final Map<String, Map<String, Object>> extensions;
		final Map<String, Object> v2;
		switch (from.getQuestionType()) {
			case V2_TYPE_ABCD:
			case V2_TYPE_SC:
			case V2_TYPE_MC:
			case V2_TYPE_VOTE:
			case V2_TYPE_SCHOOL:
			case V2_TYPE_YESNO:
				final ChoiceQuestionContent choiceQuestionContent = new ChoiceQuestionContent();
				to = choiceQuestionContent;
				to.setFormat(formatMapping.get(from.getQuestionType()));
				choiceQuestionContent.setMultiple(V2_TYPE_MC.equals(from.getQuestionType()));
				final boolean prefixedLabels = from.getPossibleAnswers().stream()
						.allMatch(a -> prefixedLabelPattern.matcher(a.getText()).matches());
				for (int i = 0; i < from.getPossibleAnswers().size(); i++) {
					final de.thm.arsnova.model.migration.v2.AnswerOption fromOption = from.getPossibleAnswers().get(i);
					final ChoiceQuestionContent.AnswerOption toOption = new ChoiceQuestionContent.AnswerOption();
					toOption.setLabel(prefixedLabels ? fromOption.getText().substring(3) : fromOption.getText());
					toOption.setPoints(fromOption.getValue());
					choiceQuestionContent.getOptions().add(toOption);
					if (fromOption.isCorrect()) {
						choiceQuestionContent.getCorrectOptionIndexes().add(i);
					}
				}

				break;
			case V2_TYPE_FREETEXT:
				to = new de.thm.arsnova.model.Content();
				to.setFormat(de.thm.arsnova.model.Content.Format.TEXT);

				break;
			case V2_TYPE_SLIDE:
				to = new de.thm.arsnova.model.Content();
				to.setFormat(de.thm.arsnova.model.Content.Format.TEXT);
				extensions = new HashMap<>();
				to.setExtensions(extensions);
				v2 = new HashMap<>();
				extensions.put("v2", v2);
				v2.put("format", V2_TYPE_SLIDE);

				break;
			case V2_TYPE_FLASHCARD:
				to = new de.thm.arsnova.model.Content();
				to.setFormat(de.thm.arsnova.model.Content.Format.SLIDE);
				extensions = new HashMap<>();
				to.setExtensions(extensions);
				v2 = new HashMap<>();
				extensions.put("v2", v2);
				v2.put("format", V2_TYPE_FLASHCARD);
				if (!from.getPossibleAnswers().isEmpty()) {
					to.setAdditionalText(from.getPossibleAnswers().get(0).getText());
					to.setAdditionalTextTitle("Back");
				}

				break;
			case V2_TYPE_GRID:
				final GridImageContent gridImageContent = new GridImageContent();
				to = gridImageContent;
				to.setFormat(de.thm.arsnova.model.Content.Format.GRID);
				final GridImageContent.Grid grid = gridImageContent.getGrid();
				grid.setColumns(from.getGridSizeX());
				grid.setRows(from.getGridSizeY());
				grid.setNormalizedX(1.0 * from.getGridOffsetX() / V2_GRID_CONTAINER_SIZE);
				grid.setNormalizedY(1.0 * from.getGridOffsetY() / V2_GRID_CONTAINER_SIZE);
				/* v3 normalized field size = v2 scale factor ^ v2 zoom level / v2 grid size */
				grid.setNormalizedFieldSize(Math.pow(Double.valueOf(from.getGridScaleFactor()), from.getGridZoomLvl())
						/ from.getGridSize());
				grid.setVisible(!from.getGridIsHidden());
				final GridImageContent.Image image = gridImageContent.getImage();
				image.setUrl(from.getImage());
				image.setRotation(from.getImgRotation() * 90 % 360);
				image.setScaleFactor(Math.pow(Double.valueOf(from.getScaleFactor()), from.getZoomLvl()));
				gridImageContent.setCorrectOptionIndexes(from.getPossibleAnswers().stream()
						.filter(o -> o.isCorrect())
						.map(o -> {
							try {
								final String[] coords = (o.getText() != null ? o.getText() : "").split(";");
								return coords.length == 2
										? Integer.valueOf(coords[0]) + Integer.valueOf(coords[1]) * from.getGridSizeX()
										: -1;
							} catch (final NumberFormatException e) {
								return -1;
							}
						})
						.filter(i -> i >= 0 && i < grid.getColumns() * grid.getRows())
						.collect(Collectors.toList()));
				extensions = new HashMap<>();
				to.setExtensions(extensions);
				v2 = new HashMap<>();
				extensions.put(V2, v2);
				v2.put(V2_GRID_TYPE, from.getGridType());
				/* It is not possible to migrate legacy image offsets to normalized values. */
				if (from.getOffsetX() != 0) {
					v2.put(V2_GRID_IMAGE_ABSOLUTE_X, from.getOffsetX());
				}
				if (from.getOffsetY() != 0) {
					v2.put(V2_GRID_IMAGE_ABSOLUTE_Y, from.getOffsetY());
				}
				if (from.getNumberOfDots() != 0) {
					v2.put(V2_GRID_MODERATION_DOT_LIMIT, from.getNumberOfDots());
				}

				break;
			default:
				throw new IllegalArgumentException("Unsupported content format.");
		}
		copyCommonProperties(from, to);
		to.setCreationTimestamp(migrateDate(from.getTimestamp()));
		to.setUpdateTimestamp(new Date());
		to.setRoomId(from.getSessionId());
		to.getGroups().add(migrateGroupName(from.getQuestionVariant()));
		to.setSubject(from.getSubject());
		to.setBody(from.getText());
		to.setAbstentionsAllowed(from.isAbstention());
		to.setAbstentionsAllowed(from.isAbstention());
		if (from.getSolution() != null && !from.getSolution().isEmpty()) {
			to.setAdditionalText(from.getSolution());
			to.setAdditionalTextTitle("Solution");
		} else if (from.getHint() != null && !from.getHint().isEmpty()) {
			to.setAdditionalText(from.getHint());
			to.setAdditionalTextTitle("Hint");
		}
		final de.thm.arsnova.model.Content.State state = to.getState();
		state.setRound(from.getPiRound());
		state.setVisible(from.isActive());
		state.setResponsesVisible(from.isShowStatistic());
		state.setAdditionalTextVisible(from.isShowAnswer());
		state.setResponsesEnabled(!from.isVotingDisabled());

		return to;
	}

	public de.thm.arsnova.model.Answer migrate(final Answer from, final de.thm.arsnova.model.Content content) {
		final de.thm.arsnova.model.Answer answer;
		if (content instanceof ChoiceQuestionContent || content instanceof GridImageContent) {
			answer = migrateChoice(from, content);
		} else {
			answer = migrate(from);
		}
		answer.setCreationTimestamp(migrateDate(from.getTimestamp()));
		answer.setUpdateTimestamp(new Date());
		answer.setFormat(content.getFormat());

		return answer;
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

	public de.thm.arsnova.model.Comment migrate(final Comment from, @Nullable final UserProfile creator) {
		if (creator == null && from.getCreator() != null
				|| creator != null && !creator.getLoginId().equals(from.getCreator())) {
			throw new IllegalArgumentException("Username of creator object does not match comment creator.");
		}
		final de.thm.arsnova.model.Comment to = new de.thm.arsnova.model.Comment();
		copyCommonProperties(from, to);
		to.setCreationTimestamp(migrateDate(from.getTimestamp()));
		to.setUpdateTimestamp(new Date());
		to.setRoomId(from.getSessionId());
		if (creator != null) {
			to.setCreatorId(creator.getId());
		}
		if (!from.getSubject().isBlank()) {
			to.setBody(from.getSubject() + ": " + from.getText());
		} else {
			to.setBody(from.getText());
		}
		to.setTimestamp(to.getCreationTimestamp());
		to.setRead(from.isRead());

		return to;
	}


	public de.thm.arsnova.model.Comment migrate(final Comment from) {
		return migrate(from, null);
	}

	public de.thm.arsnova.model.Motd migrate(final Motd from) {
		final de.thm.arsnova.model.Motd to = new de.thm.arsnova.model.Motd();
		copyCommonProperties(from, to);
		to.setCreationTimestamp(from.getStartdate());
		to.setUpdateTimestamp(new Date());
		to.setStartDate(from.getStartdate());
		to.setEndDate(from.getEnddate());
		switch (from.getAudience()) {
			case "all":
				to.setAudience(de.thm.arsnova.model.Motd.Audience.ALL);
				break;
			case "tutors":
				to.setAudience(de.thm.arsnova.model.Motd.Audience.AUTHORS);
				break;
			case "students":
				to.setAudience(de.thm.arsnova.model.Motd.Audience.PARTICIPANTS);
				break;
			case "session":
				to.setAudience(de.thm.arsnova.model.Motd.Audience.ROOM);
				break;
			default:
				/* TODO: Add log message. */
				break;
		}
		to.setTitle(from.getTitle());
		to.setBody(from.getText());
		to.setRoomId(from.getSessionId());

		return to;
	}

	private ChoiceAnswer migrateChoice(final Answer from, final de.thm.arsnova.model.Content content) {
		final ChoiceAnswer to = new ChoiceAnswer();
		copyCommonProperties(from, to);
		to.setContentId(from.getQuestionId());
		to.setRoomId(from.getSessionId());
		to.setRound(from.getPiRound());

		if (from.isAbstention()) {
			return to;
		}

		if (content instanceof ChoiceQuestionContent) {
			final List<Integer> selectedChoiceIndexes = new ArrayList<>();
			to.setSelectedChoiceIndexes(selectedChoiceIndexes);
			final ChoiceQuestionContent choiceQuestionContent = (ChoiceQuestionContent) content;
			if (choiceQuestionContent.isMultiple()) {
				final List<Boolean> flags = Arrays.stream(from.getAnswerText().split(","))
						.map("1"::equals).collect(Collectors.toList());
				if (flags.size() != choiceQuestionContent.getOptions().size()) {
					throw new IndexOutOfBoundsException(
							"Number of answer's choice flags does not match number of content's answer options");
				}
				int i = 0;
				for (final boolean flag : flags) {
					if (flag) {
						selectedChoiceIndexes.add(i);
					}
					i++;
				}
			} else {
				int i = 0;
				for (final ChoiceQuestionContent.AnswerOption option : choiceQuestionContent.getOptions()) {
					final Matcher labelMatcher = labelPattern.matcher(from.getAnswerText());
					if (labelMatcher.matches() && option.getLabel().equals(labelMatcher.group(2))) {
						selectedChoiceIndexes.add(i);
						break;
					}
					i++;
				}
			}
		} else if (content instanceof GridImageContent) {
			final GridImageContent gridImageContent = (GridImageContent) content;
			to.setSelectedChoiceIndexes(migrateChoice(from.getAnswerText(), gridImageContent.getGrid()));
		} else {
			throw new IllegalArgumentException(
					"Content expected to be an instance of ChoiceQuestionContent or GridImageContent");
		}

		return to;
	}

	private List<Integer> migrateChoice(final String choice, final GridImageContent.Grid grid) {
		return Arrays.stream(choice.split(","))
			.map(c -> {
				try {
					final String[] coords = c.split(";");
					return coords.length == 2
							? Integer.valueOf(coords[0])
							+ Integer.valueOf(coords[1]) * grid.getColumns()
							: -1;
				} catch (final NumberFormatException e) {
					return -1;
				}
			})
			.filter(i -> i >= 0
				&& i < grid.getColumns() * grid.getRows())
			.collect(Collectors.toList());
	}

	private String migrateGroupName(final String variantName) {
		return contentGroupNames.getOrDefault(variantName, variantName);
	}

	private void updateProfileFromLoginId(final UserProfile profile, final String loginId) {
		if (loginId.length() == 15 && loginId.startsWith("Guest")) {
			profile.setAuthProvider(UserProfile.AuthProvider.ARSNOVA_GUEST);
			profile.setLoginId(loginId);
		} else if (loginId.startsWith("$")) {
			profile.setAuthProvider(UserProfile.AuthProvider.ANONYMIZED);
			/* Remove redundant prefix and shorten ID to 10 chars */
			profile.setLoginId(loginId.substring(loginId.lastIndexOf("$") + 1).substring(0, 10));
		} else if (loginId.startsWith("oidc:")) {
			profile.setAuthProvider(UserProfile.AuthProvider.OIDC);
			profile.setLoginId(loginId.substring(5));
		} else if (loginId.startsWith("saml:")) {
			profile.setAuthProvider(UserProfile.AuthProvider.SAML);
			profile.setLoginId(loginId.substring(5));
		} else if (loginId.startsWith("https://www.facebook.com/") || loginId.startsWith("http://www.facebook.com/")) {
			profile.setAuthProvider(UserProfile.AuthProvider.FACEBOOK);
			/* Extract ID from URL */
			profile.setLoginId(loginId.substring(loginId.indexOf("/", 23) + 1, loginId.length() - 1));
		} else if (loginId.contains("@")) {
			profile.setAuthProvider(UserProfile.AuthProvider.GOOGLE);
			profile.setLoginId(loginId);
		} else {
			profile.setAuthProvider(authProviderFallback);
			profile.setLoginId(loginId);
		}
	}

	public void setIgnoreRevision(final boolean ignoreRevision) {
		this.ignoreRevision = ignoreRevision;
	}
}
