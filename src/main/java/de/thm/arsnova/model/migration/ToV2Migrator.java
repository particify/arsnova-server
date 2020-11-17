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

import static de.thm.arsnova.model.migration.FromV2Migrator.V2;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_GRID_CONTAINER_SIZE;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_GRID_DEFAULT_TYPE;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_GRID_FIELD_COUNT;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_GRID_IMAGE_ABSOLUTE_X;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_GRID_IMAGE_ABSOLUTE_Y;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_GRID_MODERATION_DOT_LIMIT;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_GRID_SCALE_FACTOR;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_GRID_TYPE;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_ABCD;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_FLASHCARD;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_FREETEXT;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_GRID;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_MC;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_SCHOOL;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_SLIDE;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_VOTE;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_YESNO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.ChoiceQuestionContent;
import de.thm.arsnova.model.GridImageContent;
import de.thm.arsnova.model.RoomStatistics;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.model.migration.v2.Answer;
import de.thm.arsnova.model.migration.v2.AnswerOption;
import de.thm.arsnova.model.migration.v2.Comment;
import de.thm.arsnova.model.migration.v2.Content;
import de.thm.arsnova.model.migration.v2.Entity;
import de.thm.arsnova.model.migration.v2.LoggedIn;
import de.thm.arsnova.model.migration.v2.Motd;
import de.thm.arsnova.model.migration.v2.MotdList;
import de.thm.arsnova.model.migration.v2.Room;
import de.thm.arsnova.model.migration.v2.RoomFeature;
import de.thm.arsnova.model.migration.v2.RoomInfo;
import de.thm.arsnova.model.migration.v2.VisitedRoom;

/**
 * Converts entities from current model version to legacy version 2.
 *
 * @author Daniel Gerhardt
 */
public class ToV2Migrator {
	private Map<String, String> contentGroupNames;

	public ToV2Migrator(final Map<String, String> contentGroupNames) {
		this.contentGroupNames = contentGroupNames.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
	}

	private void copyCommonProperties(final de.thm.arsnova.model.Entity from, final Entity to) {
		to.setId(from.getId());
		to.setRevision(from.getRevision());
	}

	public Room migrate(final de.thm.arsnova.model.Room from, final Optional<UserProfile> owner) {
		final Room to = new Room();
		copyCommonProperties(from, to);
		to.setKeyword(from.getShortId());
		if (owner.isPresent()) {
			to.setCreator(owner.get().getLoginId());
		}
		to.setName(from.getName());
		to.setShortName(from.getAbbreviation());
		to.setActive(!from.isClosed());
		if (from.getAuthor() != null) {
			to.setPpAuthorName(from.getAuthor().getName());
			to.setPpAuthorMail(from.getAuthor().getMail());
			to.setPpUniversity(from.getAuthor().getOrganizationName());
			to.setPpFaculty(from.getAuthor().getOrganizationUnit());
			to.setPpLogo(from.getAuthor().getOrganizationLogo());
		}
		to.setFeatures(migrate(from.getSettings()));

		return to;
	}

	public Room migrate(final de.thm.arsnova.model.Room from) {
		return migrate(from, Optional.empty());
	}

	public RoomFeature migrate(final de.thm.arsnova.model.Room.Settings settings) {
		final RoomFeature feature = new RoomFeature();

		/* Features */
		feature.setInterposed(settings.isCommentsEnabled());
		feature.setLecture(settings.isQuestionsEnabled());
		feature.setJitt(settings.isQuestionsEnabled());
		feature.setSlides(settings.isSlidesEnabled());
		feature.setFlashcardFeature(settings.isFlashcardsEnabled());
		feature.setFeedback(settings.isQuickFeedbackEnabled());
		feature.setPi(settings.isMultipleRoundsEnabled() || settings.isTimerEnabled());
		feature.setLearningProgress(settings.isScoreEnabled());

		/* Use cases */
		int count = 0;
		/* Single-feature use cases can be migrated */
		if (settings.isCommentsEnabled()) {
			feature.setInterposedFeedback(true);
			count++;
		}
		if (settings.isFlashcardsEnabled()) {
			feature.setFlashcard(true);
			count++;
		}
		if (settings.isQuickFeedbackEnabled()) {
			feature.setLiveFeedback(true);
			count++;
		}
		if (settings.isQuickSurveyEnabled()) {
			feature.setLiveClicker(true);
			count++;
		}
		/* For the following features an exact migration is not possible, so custom is set */
		if (settings.isQuestionsEnabled()) {
			feature.setCustom(true);
			count++;
		}
		if (settings.isSlidesEnabled()) {
			feature.setCustom(true);
			count++;
		}
		if (settings.isMultipleRoundsEnabled() || settings.isTimerEnabled()) {
			feature.setCustom(true);
			count++;
		}
		if (settings.isScoreEnabled()) {
			feature.setCustom(true);
			count++;
		}

		if (count != 1) {
			/* Reset single-feature use-cases since multiple features were detected */
			feature.setInterposedFeedback(false);
			feature.setFlashcard(false);
			feature.setLiveFeedback(false);
			feature.setLiveClicker(false);

			if (count == 7) {
				feature.setCustom(false);
				feature.setTotal(true);
			} else {
				feature.setCustom(true);
			}
		}

		return feature;
	}

	public Content migrate(final de.thm.arsnova.model.Content from) {
		final Content to = new Content();
		copyCommonProperties(from, to);
		to.setSessionId(from.getRoomId());
		if (from.getSubject().equals("") && from.getBody().contains("\n\n")) {
			final String[] subjectAndBody = from.getBody().split("\n\n", 2);
			to.setSubject(subjectAndBody[0]);
			to.setText(subjectAndBody[1]);
		} else {
			to.setSubject(from.getSubject());
			to.setText(from.getBody());
		}
		to.setAbstention(from.isAbstentionsAllowed());
		if (!from.getState().isAdditionalTextVisible() || "Solution".equals(from.getAdditionalTextTitle())) {
			to.setSolution(from.getAdditionalText());
		} else if (from.getAdditionalText() != null) {
			to.setHint(from.getAdditionalText());
		}

		if (from instanceof ChoiceQuestionContent) {
			final ChoiceQuestionContent fromChoiceQuestionContent = (ChoiceQuestionContent) from;
			switch (from.getFormat()) {
				case CHOICE:
					to.setQuestionType(fromChoiceQuestionContent.isMultiple() ? V2_TYPE_MC : V2_TYPE_ABCD);
					break;
				case BINARY:
					to.setQuestionType(V2_TYPE_YESNO);
					break;
				case SCALE:
					final int optionCount = fromChoiceQuestionContent.getOptions().size();
					/* The number of options for vote/school format is hard-coded by the legacy client */
					if (optionCount == 5) {
						to.setQuestionType(V2_TYPE_VOTE);
					} else if (optionCount == 6) {
						to.setQuestionType(V2_TYPE_SCHOOL);
					} else {
						to.setQuestionType(V2_TYPE_ABCD);
					}
					break;
				case GRID:
					to.setQuestionType(V2_TYPE_GRID);
					break;
				default:
					throw new IllegalArgumentException("Unsupported content format.");
			}
			final List<AnswerOption> toOptions = new ArrayList<>();
			to.setPossibleAnswers(toOptions);
			for (int i = 0; i < fromChoiceQuestionContent.getOptions().size(); i++) {
				final AnswerOption option = new AnswerOption();
				option.setText(fromChoiceQuestionContent.getOptions().get(i).getLabel());
				option.setValue(fromChoiceQuestionContent.getOptions().get(i).getPoints());
				option.setCorrect(fromChoiceQuestionContent.getCorrectOptionIndexes().contains(i));
				toOptions.add(option);
			}
		} else {
			switch (from.getFormat()) {
				case NUMBER:
					to.setQuestionType(V2_TYPE_FREETEXT);
					break;
				case TEXT:
					final String legacyType = from.getExtensions() != null
							? (String) from.getExtensions()
								.getOrDefault("v2", Collections.emptyMap()).getOrDefault("format", "")
							: "";
					switch (legacyType) {
						case V2_TYPE_SLIDE:
							to.setQuestionType(V2_TYPE_SLIDE);
							break;
						case V2_TYPE_FLASHCARD:
							to.setQuestionType(V2_TYPE_FLASHCARD);
							final AnswerOption back = new AnswerOption();
							back.setText(from.getAdditionalText());
							back.setCorrect(true);
							to.setPossibleAnswers(Collections.singletonList(back));

							break;
						default:
							to.setQuestionType(V2_TYPE_FREETEXT);
							break;
					}
					break;
				case GRID:
					final GridImageContent fromGridImageContent = (GridImageContent) from;
					final GridImageContent.Grid grid = fromGridImageContent.getGrid();
					final GridImageContent.Image image = fromGridImageContent.getImage();
					to.setQuestionType(V2_TYPE_GRID);
					to.setGridSizeX(grid.getColumns());
					to.setGridSizeY(grid.getRows());
					to.setGridOffsetX((int) (Math.round(grid.getNormalizedX() * V2_GRID_CONTAINER_SIZE)));
					to.setGridOffsetY((int) (Math.round(grid.getNormalizedY() * V2_GRID_CONTAINER_SIZE)));
					/* v3 normalized field size = v2 scale factor ^ v2 zoom level / v2 grid size */
					to.setGridSize(V2_GRID_FIELD_COUNT);
					to.setGridScaleFactor(String.valueOf(V2_GRID_SCALE_FACTOR));
					to.setGridZoomLvl((int) Math.round(
							Math.log(grid.getNormalizedFieldSize() * V2_GRID_FIELD_COUNT)
								/ Math.log(V2_GRID_SCALE_FACTOR)));
					to.setGridIsHidden(!grid.isVisible());
					to.setImage(image.getUrl());
					to.setImgRotation(image.getRotation() / 90 % 4);
					to.setScaleFactor(String.valueOf(V2_GRID_SCALE_FACTOR));
					to.setZoomLvl((int) Math.round(
							Math.log(image.getScaleFactor()) / Math.log(V2_GRID_SCALE_FACTOR)));
					to.setPossibleAnswers(
							fromGridImageContent.getCorrectOptionIndexes().stream()
									.map(i -> {
										final int x = i % fromGridImageContent.getGrid().getColumns();
										final int y = i / fromGridImageContent.getGrid().getColumns();
										final AnswerOption answerOption = new AnswerOption();
										answerOption.setText(x + ";" + y);
										answerOption.setCorrect(true);

										return answerOption;
									})
									.collect(Collectors.toList()));
					if (fromGridImageContent.getExtensions() != null) {
						final Map<String, Object> v2 = fromGridImageContent.getExtensions()
								.getOrDefault(V2, Collections.emptyMap());
						to.setGridType((String) v2.getOrDefault(V2_GRID_TYPE, V2_GRID_DEFAULT_TYPE));
						to.setOffsetX((int) v2.getOrDefault(V2_GRID_IMAGE_ABSOLUTE_X, 0));
						to.setOffsetY((int) v2.getOrDefault(V2_GRID_IMAGE_ABSOLUTE_Y, 0));
						to.setNumberOfDots((int) v2.getOrDefault(V2_GRID_MODERATION_DOT_LIMIT, 0));
					} else {
						to.setGridType(V2_GRID_DEFAULT_TYPE);
					}

					break;
				default:
					throw new IllegalArgumentException("Unsupported content format.");
			}
		}
		final de.thm.arsnova.model.Content.State state = from.getState();
		to.setPiRound(state.getRound());
		to.setActive(state.isVisible());
		to.setShowStatistic(state.isResponsesVisible());
		to.setShowAnswer(state.isAdditionalTextVisible());
		to.setVotingDisabled(!state.isResponsesEnabled());
		if (from.getGroups().size() == 1) {
			to.setQuestionVariant(migrateGroupName(from.getGroups().iterator().next()));
		}

		return to;
	}

	public Answer migrate(final de.thm.arsnova.model.ChoiceAnswer from,
			final de.thm.arsnova.model.Content content, final Optional<UserProfile> creator) {
		final Answer to = new Answer();
		copyCommonProperties(from, to);
		to.setQuestionId(from.getContentId());
		to.setSessionId(from.getRoomId());
		to.setPiRound(from.getRound());
		if (creator.isPresent()) {
			to.setUser(creator.get().getLoginId());
		}
		if (from.getSelectedChoiceIndexes().isEmpty()) {
			to.setAbstention(true);
		} else {
			if (content instanceof ChoiceQuestionContent) {
				final ChoiceQuestionContent choiceQuestionContent = (ChoiceQuestionContent) content;
				if (choiceQuestionContent.isMultiple()) {
					to.setAnswerText(migrateChoice(from.getSelectedChoiceIndexes(),
							choiceQuestionContent.getOptions()));
				} else {
					final int index = from.getSelectedChoiceIndexes().get(0);
					to.setAnswerText(choiceQuestionContent.getOptions().get(index).getLabel());
				}
			} else if (content instanceof GridImageContent) {
				final GridImageContent gridImageContent = (GridImageContent) content;
				to.setAnswerText(migrateChoice(from.getSelectedChoiceIndexes(), gridImageContent.getGrid()));
			} else {
				throw new IllegalArgumentException(
						"Content expected to be an instance of ChoiceQuestionContent or GridImageContent");
			}
		}

		return to;
	}

	public Answer migrate(final de.thm.arsnova.model.ChoiceAnswer from,
			final de.thm.arsnova.model.Content content) {
		return migrate(from, content, Optional.empty());
	}

	public Answer migrate(final de.thm.arsnova.model.TextAnswer from,
			final Optional<de.thm.arsnova.model.Content> content, final Optional<UserProfile> creator) {
		final Answer to = new Answer();
		copyCommonProperties(from, to);
		to.setQuestionId(from.getContentId());
		to.setSessionId(from.getRoomId());
		to.setPiRound(from.getRound());
		if (creator.isPresent()) {
			to.setUser(creator.get().getLoginId());
		}

		to.setAnswerSubject(from.getSubject());
		to.setAnswerText(from.getBody());

		return to;
	}

	public Answer migrate(final de.thm.arsnova.model.TextAnswer from) {
		return migrate(from, Optional.empty(), Optional.empty());
	}

	public Comment migrate(final de.thm.arsnova.model.Comment from, final Optional<UserProfile> creator) {
		final Comment to = new Comment();
		copyCommonProperties(from, to);
		to.setSessionId(from.getRoomId());
		if (creator.isPresent()) {
			to.setCreator(creator.get().getLoginId());
		}
		to.setSubject(from.getBody().substring(0, 20) + ((from.getBody().length() > 20) ? "…" : ""));
		to.setText(from.getBody());
		to.setTimestamp(from.getTimestamp().getTime());
		to.setRead(from.isRead());

		return to;
	}

	public Comment migrate(final de.thm.arsnova.model.Comment from) {
		return migrate(from, Optional.empty());
	}

	public Motd migrate(final de.thm.arsnova.model.Motd from) {
		final Motd to = new Motd();
		copyCommonProperties(from, to);
		to.setMotdkey(from.getId());
		to.setStartdate(from.getCreationTimestamp());
		to.setStartdate(from.getStartDate());
		to.setEnddate(from.getEndDate());
		switch (from.getAudience()) {
			case ALL:
				to.setAudience("all");
				break;
			case AUTHORS:
				to.setAudience("tutors");
				break;
			case PARTICIPANTS:
				to.setAudience("students");
				break;
			case ROOM:
				to.setAudience("session");
				break;
			default:
				break;
		}
		to.setTitle(from.getTitle());
		to.setText(from.getBody());
		to.setSessionId(from.getRoomId());

		return to;
	}

	public List<Answer> migrate(final AnswerStatistics from,
			final de.thm.arsnova.model.Content content, final int round) {
		if (round < 1 || round > content.getState().getRound()) {
			throw new IllegalArgumentException("Invalid value for round");
		}
		final List<Answer> to  = new ArrayList<>();
		final AnswerStatistics.RoundStatistics stats = from.getRoundStatistics().get(round - 1);

		if (content.isAbstentionsAllowed()) {
			final Answer abstention = new Answer();
			abstention.setQuestionId(content.getId());
			abstention.setPiRound(round);
			abstention.setAnswerCount(stats.getAbstentionCount());
			abstention.setAbstentionCount(stats.getAbstentionCount());
			to.add(abstention);
		}

		final Map<String, Integer> choices;
		if (content instanceof ChoiceQuestionContent) {
			final ChoiceQuestionContent choiceQuestionContent = (ChoiceQuestionContent) content;
			if (choiceQuestionContent.isMultiple()) {
				/* Map selected choice indexes -> answer count */
				choices = stats.getCombinatedCounts().stream().collect(Collectors.toMap(
						c -> migrateChoice(c.getSelectedChoiceIndexes(), choiceQuestionContent.getOptions()),
						c -> c.getCount(),
						(u, v) -> {
							throw new IllegalStateException(String.format("Duplicate key %s", u));
						},
						LinkedHashMap::new));
			} else {
				choices = new LinkedHashMap<>();
				int i = 0;
				for (final ChoiceQuestionContent.AnswerOption option : choiceQuestionContent.getOptions()) {
					choices.put(option.getLabel(), stats.getIndependentCounts().get(i));
					i++;
				}
			}
		} else {
			final GridImageContent gridImageContent = (GridImageContent) content;
			choices = stats.getCombinatedCounts().stream().collect(Collectors.toMap(
					c -> migrateChoice(c.getSelectedChoiceIndexes(), gridImageContent.getGrid()),
					c -> c.getCount(),
					(u, v) -> {
						throw new IllegalStateException(String.format("Duplicate key %s", u));
					},
					LinkedHashMap::new));
		}

		for (final Map.Entry<String, Integer> choice : choices.entrySet()) {
			final Answer answer = new Answer();
			answer.setQuestionId(content.getId());
			answer.setPiRound(round);
			answer.setAnswerCount(choice.getValue());
			answer.setAbstentionCount(stats.getAbstentionCount());
			answer.setAnswerText(choice.getKey());
			to.add(answer);
		}

		return to;
	}

	public LoggedIn migrateLoggedIn(final UserProfile from) {
		final LoggedIn to = new LoggedIn();
		copyCommonProperties(from, to);
		to.setUser(from.getLoginId());
		to.setTimestamp(from.getLastLoginTimestamp().getTime());
		to.setVisitedSessions(from.getRoomHistory().stream()
				.map(entry -> new VisitedRoom())
				.collect(Collectors.toList()));

		return to;
	}

	public MotdList migrateMotdList(final UserProfile from) {
		final MotdList to = new MotdList();
		copyCommonProperties(from, to);
		to.setUsername(from.getLoginId());
		to.setMotdkeys(String.join(",", from.getAcknowledgedMotds()));

		return to;
	}

	public RoomInfo migrateStats(final de.thm.arsnova.model.Room from) {
		final RoomInfo to = new RoomInfo(migrate(from));
		final RoomStatistics stats = from.getStatistics();
		to.setNumQuestions(stats.getContentCount());
		to.setNumUnanswered(stats.getUnansweredContentCount());
		to.setNumAnswers(stats.getAnswerCount());
		to.setNumInterposed(stats.getCommentCount());
		to.setNumUnredInterposed(stats.getUnreadCommentCount());

		return to;
	}

	private String migrateChoice(final List<Integer> selectedChoiceIndexes,
			final List<ChoiceQuestionContent.AnswerOption> options) {
		final List<String> answers = new ArrayList<>();
		for (int i = 0; i < options.size(); i++) {
			answers.add(selectedChoiceIndexes.contains(i) ? "1" : "0");
		}

		return answers.stream().collect(Collectors.joining(","));
	}

	private String migrateChoice(final List<Integer> selectedChoiceIndexes, final GridImageContent.Grid grid) {
		return selectedChoiceIndexes.stream()
				.map(i -> {
					final int x = i % grid.getColumns();
					final int y = i / grid.getColumns();
					return x + ";" + y;
				})
				.collect(Collectors.joining(","));
	}

	private String migrateGroupName(final String groupName) {
		return contentGroupNames.getOrDefault(groupName, groupName);
	}
}
