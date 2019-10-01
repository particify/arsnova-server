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

import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_ABCD;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_FREETEXT;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_GRID;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_MC;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_SCHOOL;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_VOTE;
import static de.thm.arsnova.model.migration.FromV2Migrator.V2_TYPE_YESNO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.ChoiceQuestionContent;
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
		to.setSubject(from.getSubject());
		to.setText(from.getBody());
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
					to.setQuestionType(V2_TYPE_FREETEXT);
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
			to.setQuestionVariant(from.getGroups().iterator().next());
		}

		return to;
	}

	public Answer migrate(final de.thm.arsnova.model.ChoiceAnswer from,
			final de.thm.arsnova.model.ChoiceQuestionContent content, final Optional<UserProfile> creator) {
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
			if (content.isMultiple()) {
				to.setAnswerText(migrateChoice(from.getSelectedChoiceIndexes(), content.getOptions()));
			} else {
				final int index = from.getSelectedChoiceIndexes().get(0);
				to.setAnswerText(content.getOptions().get(index).getLabel());
			}
		}

		return to;
	}

	public Answer migrate(final de.thm.arsnova.model.ChoiceAnswer from,
			final de.thm.arsnova.model.ChoiceQuestionContent content) {
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
		to.setSubject(from.getSubject());
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
			final de.thm.arsnova.model.ChoiceQuestionContent content, final int round) {
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
		if (content.isMultiple()) {
			/* Map selected choice indexes -> answer count */
			choices = stats.getCombinatedCounts().stream().collect(Collectors.toMap(
					c -> migrateChoice(c.getSelectedChoiceIndexes(), content.getOptions()),
					c -> c.getCount(),
					(u, v) -> {
						throw new IllegalStateException(String.format("Duplicate key %s", u));
					},
					LinkedHashMap::new));
		} else {
			choices = new LinkedHashMap<>();
			int i = 0;
			for (final ChoiceQuestionContent.AnswerOption option : content.getOptions()) {
				choices.put(option.getLabel(), stats.getIndependentCounts().get(i));
				i++;
			}
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

	public String migrateChoice(final List<Integer> selectedChoiceIndexes,
			final List<ChoiceQuestionContent.AnswerOption> options) {
		final List<String> answers = new ArrayList<>();
		for (int i = 0; i < options.size(); i++) {
			answers.add(selectedChoiceIndexes.contains(i) ? "1" : "0");
		}

		return answers.stream().collect(Collectors.joining(","));
	}
}
