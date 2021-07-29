package de.thm.arsnova.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.ChoiceAnswer;
import de.thm.arsnova.model.ChoiceQuestionContent;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.ScaleChoiceContent;

/**
 * This service generates random data for testing and demonstration purposes.
 *
 * @author Daniel Gerhardt
 */
@Service
@Primary
public class DataGenerationServiceImpl implements DataGenerationService {
	private static final Logger logger = LoggerFactory.getLogger(DataGenerationServiceImpl.class);
	private static final String NIL_UUID = "00000000000000000000000000000000";
	private static final double CORRECT_CHOICE_BIAS = 3;
	private static final double MULTIPLE_CORRECT_CHOICE_BIAS = 5;
	private static final int AVG_ANSWER_COUNT = 25;
	private static final int SORT_PERMUTATION_COUNT = 5;
	private static final double ANSWER_COUNT_RANDOM_FACTOR = 0.2;
	private static final double MIN_RANDOM = 0.1;

	private final ContentService contentService;
	private final AnswerService answerService;
	private final Random random = new Random();

	public DataGenerationServiceImpl(final ContentService contentService, final AnswerService answerService) {
		this.contentService = contentService;
		this.answerService = answerService;
	}

	/**
	 * Generates answers with randomized selected answer options for all
	 * {@link ChoiceQuestionContent}s of the room. The generation algorithm is
	 * biased towards correct options.
	 */
	public void generateRandomChoiceAnswers(final Room room) {
		final List<Content> contents = contentService.getByRoomId(room.getRoomId());
		final List<Answer> answers = new ArrayList<>();
		for (final Content content : contents) {
			answers.addAll(generateRandomAnswersForContent(content, AVG_ANSWER_COUNT));
		}
		answerService.create(answers);
	}

	private List<Answer> generateRandomAnswersForContent(final Content content, final int avgCount) {
		final List<Answer> answers = new ArrayList<>();
		if (content instanceof ChoiceQuestionContent) {
			final int count = avgCount + (int) Math.round(
					avgCount * (Math.random() * 2 * ANSWER_COUNT_RANDOM_FACTOR - ANSWER_COUNT_RANDOM_FACTOR));
			if (content.getFormat() == Content.Format.SORT) {
				final List<List<Integer>> permutations =
						generateSortChoicePermutations((ChoiceQuestionContent) content);
				for (int i = 0; i < count; i++) {
					answers.add(generateRandomizedSortAnswer((ChoiceQuestionContent) content, permutations));
				}
			} else {
				for (int i = 0; i < count; i++) {
					answers.add(generateRandomizedAnswer((ChoiceQuestionContent) content));
				}
			}
		}

		return answers;
	}

	private Answer generateRandomizedAnswer(final ChoiceQuestionContent content) {
		logger.debug("Generating answers for content {}.", content);
		final ChoiceAnswer answer = new ChoiceAnswer(content, NIL_UUID);
		final int optionCount;
		if (content instanceof ScaleChoiceContent) {
			optionCount = ((ScaleChoiceContent) content).getOptionCount();
		} else {
			optionCount = content.getOptions().size();
		}
		final BiasedRandom biasedRandom = new BiasedRandom(
				random,
				optionCount,
				content.isMultiple() ? MULTIPLE_CORRECT_CHOICE_BIAS : CORRECT_CHOICE_BIAS,
				content.getCorrectOptionIndexes());
		answer.setSelectedChoiceIndexes(content.isMultiple()
				? new ArrayList<>(biasedRandom.generateIndexes())
				: List.of(biasedRandom.generateIndex()));
		logger.debug("Generated answer {}.", answer);

		return answer;
	}

	private Answer generateRandomizedSortAnswer(
			final ChoiceQuestionContent content,
			final List<List<Integer>> permutations) {
		logger.debug("Generating answers for content {}.", content);
		final ChoiceAnswer answer = new ChoiceAnswer(content, NIL_UUID);
		final BiasedRandom biasedRandom = new BiasedRandom(
				random,
				permutations.size(),
				CORRECT_CHOICE_BIAS,
				List.of(0));
		answer.setSelectedChoiceIndexes(permutations.get(biasedRandom.generateIndex()));
		logger.debug("Generated answer {}.", answer);

		return answer;
	}

	private List<List<Integer>> generateSortChoicePermutations(final ChoiceQuestionContent content) {
		final List<List<Integer>> permutations = new ArrayList<>();
		permutations.add(new ArrayList<>(content.getCorrectOptionIndexes()));
		for (int i = 0; i < SORT_PERMUTATION_COUNT; i++) {
			final List<Integer> permutation = new ArrayList<>(content.getCorrectOptionIndexes());
			Collections.shuffle(permutation);
			permutations.add(permutation);
		}

		return permutations;
	}

	private static class BiasedRandom {
		private final Random random;
		private final int count;
		private final double[] thresholds;

		private BiasedRandom(final Random random, final int n, final double bias, final List<Integer> biasedIndexes) {
			if (n <= 0) {
				throw new IllegalArgumentException("n must be greater than zero.");
			}
			this.random = random;
			this.count = n;
			this.thresholds = new double[n];
			initThresholds(bias, biasedIndexes);
		}

		private int generateIndex() {
			final double r = random.nextDouble() * thresholds[count - 1];
			return generateIndexForRandomValue(r);
		}

		private Set<Integer> generateIndexes() {
			final SortedSet<Integer> choices = new TreeSet<>();
			for (int i = 0; i < count; i++) {
				final double r = random.nextDouble() * thresholds[count - 1];
				choices.add(generateIndexForRandomValue(r));
			}

			return choices;
		}

		private int generateIndexForRandomValue(final double r) {
			for (int i = 0; i < count; i++) {
				if (r < thresholds[i]) {
					return i;
				}
			}

			return 0;
		}

		private void initThresholds(final double bias, final List<Integer> biasedIndexes) {
			for (int i = 0; i < count; i++) {
				final double localBias = biasedIndexes.contains(i) ? bias : 1;
				final double prev = i == 0 ? 0 : thresholds[i - 1];
				thresholds[i] = prev + (MIN_RANDOM + random.nextDouble() * (1 - MIN_RANDOM)) * localBias;
			}
		}
	}
}
