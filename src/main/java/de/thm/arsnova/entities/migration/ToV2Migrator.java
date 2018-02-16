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

import de.thm.arsnova.entities.AnswerStatistics;
import de.thm.arsnova.entities.ChoiceQuestionContent;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.entities.migration.v2.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.thm.arsnova.entities.migration.FromV2Migrator.*;

/**
 * Converts entities from current model version to legacy version 2.
 *
 * @author Daniel Gerhardt
 */
public class ToV2Migrator {

	private void copyCommonProperties(final de.thm.arsnova.entities.Entity from, final Entity to) {
		to.setId(from.getId());
		to.setRevision(from.getRevision());
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

	public Room migrate(final de.thm.arsnova.entities.Room from, final Optional<UserProfile> owner) {
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

		return to;
	}

	public Room migrate(final de.thm.arsnova.entities.Room from) {
		return migrate(from, Optional.empty());
	}

	public RoomFeature migrate(final de.thm.arsnova.entities.Room.Settings settings) {
		RoomFeature feature = new RoomFeature();
		feature.setInterposed(settings.isCommentsEnabled());
		feature.setLecture(settings.isQuestionsEnabled());
		feature.setJitt(settings.isQuestionsEnabled());
		feature.setSlides(settings.isSlidesEnabled());
		feature.setFlashcard(settings.isFlashcardsEnabled());
		feature.setFeedback(settings.isQuickSurveyEnabled());
		feature.setPi(settings.isMultipleRoundsEnabled() || settings.isTimerEnabled());
		feature.setLearningProgress(settings.isScoreEnabled());

		int count = 0;
		/* Single-feature use cases can be migrated */
		if (settings.isCommentsEnabled()) {
			feature.setTwitterWall(true);
			count++;
		}
		if (settings.isFlashcardsEnabled()) {
			feature.setFlashcardFeature(true);
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
			feature.setTwitterWall(false);
			feature.setFlashcardFeature(false);
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

	public Content migrate(final de.thm.arsnova.entities.Content from) {
		final Content to = new Content();
		copyCommonProperties(from, to);
		to.setSessionId(from.getRoomId());
		to.setSubject(from.getSubject());
		to.setText(from.getBody());
		to.setQuestionVariant(from.getGroup());
		to.setAbstention(from.isAbstentionsAllowed());

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
					to.setQuestionType(V2_TYPE_VOTE);
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
				AnswerOption option = new AnswerOption();
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
		de.thm.arsnova.entities.Content.State state = from.getState();
		to.setPiRound(state.getRound());
		to.setActive(state.isVisible());
		to.setShowStatistic(state.isResponsesVisible());
		to.setShowAnswer(state.isSolutionVisible());
		to.setVotingDisabled(!state.isResponsesEnabled());

		return to;
	}

	public Answer migrate(final de.thm.arsnova.entities.ChoiceAnswer from,
			final de.thm.arsnova.entities.ChoiceQuestionContent content, final Optional<UserProfile> creator) {
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
				int index = from.getSelectedChoiceIndexes().get(0);
				to.setAnswerText(content.getOptions().get(index).getLabel());
			}
		}

		return to;
	}

	public String migrateChoice(final List<Integer> selectedChoiceIndexes,
			final List<ChoiceQuestionContent.AnswerOption> options) {
		List<String> answers = new ArrayList<>();
		for (int i = 0; i < options.size(); i++) {
			answers.add(selectedChoiceIndexes.contains(i) ? "1" : "0");
		}

		return answers.stream().collect(Collectors.joining(","));
	}

	public Answer migrate(final de.thm.arsnova.entities.ChoiceAnswer from,
			final de.thm.arsnova.entities.ChoiceQuestionContent content) {
		return migrate(from, content, Optional.empty());
	}

	public Answer migrate(final de.thm.arsnova.entities.TextAnswer from,
			final Optional<de.thm.arsnova.entities.Content> content, final Optional<UserProfile> creator) {
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

	public Answer migrate(final de.thm.arsnova.entities.TextAnswer from) {
		return migrate(from, Optional.empty(), Optional.empty());
	}

	public Comment migrate(final de.thm.arsnova.entities.Comment from, final Optional<UserProfile> creator) {
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

	public Comment migrate(final de.thm.arsnova.entities.Comment from) {
		return migrate(from, Optional.empty());
	}

	public Motd migrate(final de.thm.arsnova.entities.Motd from) {
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
		}
		to.setTitle(from.getTitle());
		to.setText(from.getBody());
		to.setSessionId(from.getRoomId());

		return to;
	}

	public List<Answer> migrate(final AnswerStatistics from,
			final de.thm.arsnova.entities.ChoiceQuestionContent content, int round) {
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

		Map<String, Integer> choices;
		if (content.isMultiple()) {
			/* Map selected choice indexes -> answer count */
			choices = stats.getCombinatedCounts().stream().collect(Collectors.toMap(
					c -> migrateChoice(c.getSelectedChoiceIndexes(), content.getOptions()),
					c -> c.getCount(),
					(u, v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); },
					LinkedHashMap::new));
		} else {
			choices = new LinkedHashMap<>();
			int i = 0;
			for (ChoiceQuestionContent.AnswerOption option : content.getOptions()) {
				choices.put(option.getLabel(), stats.getIndependentCounts().get(i));
				i++;
			}
		}

		for (Map.Entry<String, Integer> choice : choices.entrySet()) {
			Answer answer = new Answer();
			answer.setQuestionId(content.getId());
			answer.setPiRound(round);
			answer.setAnswerCount(choice.getValue());
			answer.setAbstentionCount(stats.getAbstentionCount());
			answer.setAnswerText(choice.getKey());
			to.add(answer);
		}

		return to;
	}
}
