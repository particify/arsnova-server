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

package net.particify.arsnova.core.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.event.AfterCreationEvent;
import net.particify.arsnova.core.event.BeforeCreationEvent;
import net.particify.arsnova.core.event.BeforeDeletionEvent;
import net.particify.arsnova.core.event.BulkChangeEvent;
import net.particify.arsnova.core.model.Answer;
import net.particify.arsnova.core.model.AnswerResult;
import net.particify.arsnova.core.model.AnswerStatistics;
import net.particify.arsnova.core.model.AnswerStatisticsSummaryEntry;
import net.particify.arsnova.core.model.AnswerStatisticsUserSummary;
import net.particify.arsnova.core.model.ChoiceAnswerStatistics;
import net.particify.arsnova.core.model.ChoiceQuestionContent;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ContentGroup;
import net.particify.arsnova.core.model.ContentIdRoundResultKey;
import net.particify.arsnova.core.model.Deletion.Initiator;
import net.particify.arsnova.core.model.GridImageContent;
import net.particify.arsnova.core.model.LeaderboardCurrentResult;
import net.particify.arsnova.core.model.LeaderboardEntry;
import net.particify.arsnova.core.model.MultipleTextsAnswer;
import net.particify.arsnova.core.model.NumericAnswer;
import net.particify.arsnova.core.model.NumericAnswerStatistics;
import net.particify.arsnova.core.model.NumericContent;
import net.particify.arsnova.core.model.PrioritizationAnswerStatistics;
import net.particify.arsnova.core.model.PrioritizationChoiceContent;
import net.particify.arsnova.core.model.RoomUserAlias;
import net.particify.arsnova.core.model.ScaleChoiceContent;
import net.particify.arsnova.core.model.ShortAnswer;
import net.particify.arsnova.core.model.ShortAnswerContent;
import net.particify.arsnova.core.model.TextAnswer;
import net.particify.arsnova.core.model.TextAnswerStatistics;
import net.particify.arsnova.core.model.TextAnswerStatistics.TextRoundStatistics;
import net.particify.arsnova.core.model.WordContent;
import net.particify.arsnova.core.persistence.AnswerRepository;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.security.AuthenticationService;
import net.particify.arsnova.core.security.User;
import net.particify.arsnova.core.service.exceptions.AlreadyAnsweredContentException;
import net.particify.arsnova.core.util.StatisticsUtil;
import net.particify.arsnova.core.web.exceptions.NotFoundException;

/**
 * Performs all answer related operations.
 */
@Service
@Primary
public class AnswerServiceImpl extends DefaultEntityServiceImpl<Answer> implements AnswerService {
  private static final String NIL_UUID = "00000000000000000000000000000000";
  private static final Logger logger = LoggerFactory.getLogger(AnswerServiceImpl.class);
  private static final Pattern specialCharPattern = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]");

  private final Queue<AnswerUniqueKey> answerQueue = new ConcurrentLinkedQueue<>();
  private final Map<AnswerUniqueKey, Answer> queuedAnswers = new ConcurrentHashMap<>();
  private ContentService contentService;
  private RoomUserAliasService roomUserAliasService;
  private AnswerRepository answerRepository;
  private AuthenticationService authenticationService;
  private CacheManager cacheManager;

  public AnswerServiceImpl(
      final AnswerRepository repository,
      final DeletionRepository deletionRepository,
      final RoomUserAliasService roomUserAliasService,
      final AuthenticationService authenticationService,
      final CacheManager cacheManager,
      @Qualifier("defaultJsonMessageConverter") final
      MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator) {
    super(Answer.class, repository, deletionRepository, jackson2HttpMessageConverter.getObjectMapper(), validator);
    this.answerRepository = repository;
    this.roomUserAliasService = roomUserAliasService;
    this.authenticationService = authenticationService;
    this.cacheManager = cacheManager;
  }

  @Autowired
  public void setContentService(final ContentService contentService) {
    this.contentService = contentService;
  }

  @Scheduled(fixedDelay = 5000)
  public void flushAnswerQueue() {
    if (answerQueue.isEmpty()) {
      // no need to send an empty bulk request.
      clearContentGroupLeaderboardCache();
      return;
    }

    final Map<AnswerUniqueKey, Answer> answers = new HashMap<>();
    AnswerUniqueKey key;
    while ((key = this.answerQueue.poll()) != null) {
      final Answer answer = queuedAnswers.remove(key);
      // answer could be null in rare cases where a duplicate key is added
      // to the queue by another thread between queue polling and the
      // removal from the map.
      if (answer != null) {
        answers.putIfAbsent(key, answer);
      }
    }
    try {
      for (final Answer e : answers.values()) {
        this.eventPublisher.publishEvent(new BeforeCreationEvent<>(this, e));
      }
      answerRepository.saveAll(new ArrayList<>(answers.values()));
      clearContentGroupLeaderboardCache();
      for (final Answer e : answers.values()) {
        evictLeaderboardCacheEntries(e.getContentId());
        this.eventPublisher.publishEvent(new AfterCreationEvent<>(this, e));
      }
      this.eventPublisher.publishEvent(new BulkChangeEvent<>(this, Answer.class, answers.values()));
    } catch (final DbAccessException e) {
      logger.error("Could not bulk save answers from queue.", e);
    } finally {
      queuedAnswers.keySet().removeAll(answers.keySet());
    }
  }

  @Override
  public void deleteAnswers(final String contentId) {
    final Content content = contentService.get(contentId);
    content.resetState();
    contentService.update(content);
    final Iterable<Answer> answers = answerRepository.findStubsByContentIdAndHidden(content.getId(), false);
    answers.forEach(a -> a.setRoomId(content.getRoomId()));
    delete(answers, Initiator.USER);
    evictLeaderboardCacheEntries(contentId);
  }

  @Override
  public Answer getMyAnswer(final String contentId) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }
    return answerRepository.findByContentIdUserIdPiRound(
        contentId, Answer.class, authenticationService.getCurrentUser().getId(), content.getState().getRound());
  }

  @Override
  public ChoiceAnswerStatistics getChoiceStatistics(final String contentId, final int round) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }
    final ChoiceAnswerStatistics stats;
    final int optionCount;
    if (content instanceof ChoiceQuestionContent) {
      if (content instanceof ScaleChoiceContent) {
        optionCount = ((ScaleChoiceContent) content).getOptionCount();
      } else {
        optionCount = ((ChoiceQuestionContent) content).getOptions().size();
      }
      stats = answerRepository.findStatisticsByContentIdRound(
          content.getId(), round, optionCount);
    } else if (content instanceof GridImageContent) {
      final GridImageContent.Grid grid = ((GridImageContent) content).getGrid();
      optionCount = grid.getColumns() * grid.getRows();
      stats = answerRepository.findStatisticsByContentIdRound(
          content.getId(), round, optionCount);
    } else {
      throw new IllegalStateException(
          "Content expected to be an instance of ChoiceQuestionContent or GridImageContent");
    }
    /* Fill list with zeros to prevent IndexOutOfBoundsExceptions */
    final List<Integer> independentCounts = stats.getRoundStatistics().get(round - 1).getIndependentCounts();
    while (independentCounts.size() < optionCount) {
      independentCounts.add(0);
    }

    return stats;
  }

  @Override
  public ChoiceAnswerStatistics getChoiceStatistics(final String contentId) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }

    return getChoiceStatistics(content.getId(), content.getState().getRound());
  }

  @Override
  public AnswerStatistics getAllStatistics(final String contentId) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }
    final AnswerStatistics stats;
    if (content.getFormat() == Content.Format.WORDCLOUD) {
      final TextAnswerStatistics textStats = getTextStatistics(content.getId(), 1);
      final TextAnswerStatistics textStats2 = getTextStatistics(content.getId(), 2);
      textStats.getRoundStatistics().add(textStats2.getRoundStatistics().get(1));
      stats = textStats;
    } else if (content.getFormat() == Content.Format.SHORT_ANSWER) {
      final TextAnswerStatistics shortAnswerStats = getShortAnswerStatistics(content.getId(), 1);
      final TextAnswerStatistics shortAnswerStats2 = getShortAnswerStatistics(content.getId(), 2);
      shortAnswerStats.getRoundStatistics().add(shortAnswerStats2.getRoundStatistics().get(1));
      stats = shortAnswerStats;
    } else if (content.getFormat() == Content.Format.PRIORITIZATION) {
      stats = getPrioritizationStatistics(content.getId());
    } else if (content.getFormat() == Content.Format.NUMERIC) {
      final NumericAnswerStatistics numericStats = getNumericStatistics(content.getId(), 1);
      final NumericAnswerStatistics numericStats2 = getNumericStatistics(content.getId(), 2);
      numericStats.getRoundStatistics().add(numericStats2.getRoundStatistics().get(1));
      stats = numericStats;
    } else {
      final ChoiceAnswerStatistics choiceStats = getChoiceStatistics(content.getId(), 1);
      final ChoiceAnswerStatistics choiceStats2 = getChoiceStatistics(content.getId(), 2);
      choiceStats.getRoundStatistics().add(choiceStats2.getRoundStatistics().get(1));
      stats = choiceStats;
    }
    return stats;
  }

  public AnswerStatisticsUserSummary getStatisticsByUserIdAndContentIds(
      final String userId, final List<String> contentIds, final boolean hideResult) {
    final List<Content> contents = contentService.get(contentIds);
    final List<String> answerIds = answerRepository.findIdsByAnswerStubs(contents.stream()
        .map(c -> new Answer(c, userId)).collect(Collectors.toList()));
    final List<Answer> answers = get(answerIds);
    // Results for answered contents
    final Map<String, AnswerResult> answerResults = answers.stream()
        .map(a -> hideResult
            ? new AnswerResult(a.getContentId(), 0, 0, 0, a.getDurationMs(), AnswerResult.AnswerResultState.NEUTRAL)
            : contents.get(contentIds.indexOf(a.getContentId())).determineAnswerResult(a))
        .collect(Collectors.toMap(
            AnswerResult::getContentId,
            Function.identity(),
            (r1, r2) -> {
              // The creation of duplicate answers is prevented but duplicates might
              // exist in data from earlier versions (there is no migration to remove
              // duplicates).
              logger.warn("Ignoring duplicate answer for content ID {}.", r1.getContentId());
              return r1;
            }));
    // Add results for unanswered contents
    final List<AnswerResult> answerResultList = contents.stream()
        .map(c -> answerResults.containsKey(c.getId())
            ? answerResults.get(c.getId())
            : new AnswerResult(c.getId(), 0, 0, c.getPoints(), 0, AnswerResult.AnswerResultState.UNANSWERED))
        .collect(Collectors.toList());

    return new AnswerStatisticsUserSummary(
        (int) answerResultList.stream().filter(r -> r.getState() == AnswerResult.AnswerResultState.CORRECT).count(),
        (int) contents.stream().filter(c -> c.isScorable()).count(),
        answerResultList.stream().mapToDouble(r -> r.getAchievedPoints()).sum(),
        contents.stream().mapToInt(c -> c.getPoints()).sum(),
        answerResultList);
  }

  @Override
  public TextAnswerStatistics getTextStatistics(final String contentId) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }

    return getTextStatistics(content.getId(), content.getState().getRound());
  }

  @Override
  public TextAnswerStatistics getTextStatistics(final String contentId, final int round) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }
    final Set<String> bannedKeywords;
    if (content instanceof WordContent wordContent) {
      bannedKeywords = wordContent.getBannedKeywords();
    } else {
      bannedKeywords = Collections.emptySet();
    }
    final List<MultipleTextsAnswer> answers =
        answerRepository.findByContentIdRound(MultipleTextsAnswer.class, contentId, round);
    /* Flatten lists of individual answers to a combined map of texts with
     * count */
    final Map<String, Long> textCounts = answers.stream()
        .flatMap(a -> a.getTexts().stream())
        .collect(Collectors.groupingBy(
            Function.identity(),
            Collectors.counting()));

    return buildTextAnswerStatistics(
      round,
      (int) answers.stream().filter(a -> !a.isAbstention()).count(),
      (int) answers.stream().filter(a -> a.isAbstention()).count(),
      textCounts,
      bannedKeywords,
      Collections.emptySet());
  }

  @Override
  public TextAnswerStatistics getShortAnswerStatistics(final String contentId) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }

    return getShortAnswerStatistics(contentId, content.getState().getRound());
  }

  @Override
  public TextAnswerStatistics getShortAnswerStatistics(final String contentId, final int round) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }
    final Set<String> bannedKeywords;
    if (content instanceof WordContent wordContent) {
      bannedKeywords = wordContent.getBannedKeywords();
    } else {
      bannedKeywords = Collections.emptySet();
    }
    final List<ShortAnswer> answers =
        answerRepository.findByContentIdRound(ShortAnswer.class, contentId, round);
    final Map<String, Long> textCounts = answers.stream()
        .filter(a -> !a.isAbstention())
        .map(a -> a.getText())
        .collect(Collectors.groupingBy(
            Function.identity(),
            Collectors.counting()));
    final Set<String> preferredKeywords = content instanceof ShortAnswerContent shortAnswerContent
        ? shortAnswerContent.getCorrectTerms()
        : Collections.emptySet();

    return buildTextAnswerStatistics(
        round,
        (int) answers.stream().filter(a -> !a.isAbstention()).count(),
        (int) answers.stream().filter(a -> a.isAbstention()).count(),
        textCounts,
        bannedKeywords,
        preferredKeywords);
  }

  private TextAnswerStatistics buildTextAnswerStatistics(
      final int round,
      final int answerCount,
      final int abstentionCount,
      final Map<String, Long> textCounts,
      final Set<String> bannedKeywords,
      final Set<String> preferredKeywords) {
    final TextAnswerStatistics stats = new TextAnswerStatistics();
    final TextRoundStatistics roundStats = new TextRoundStatistics();
    roundStats.setRound(round);
    roundStats.setAbstentionCount(abstentionCount);
    final Map<String, String> normalizedPreferredKeywords = preferredKeywords.stream()
        .collect(Collectors.toMap(
            k -> WordContent.normalizeText(k),
            k -> k,
            (a, b) -> a
        ));
    Map<String, Integer> countsBySimilarity = buildCountsBySimilarityMap(textCounts, bannedKeywords);
    if (!preferredKeywords.isEmpty()) {
      countsBySimilarity = countsBySimilarity.entrySet().stream()
          .collect(Collectors.toMap(
              entry -> normalizedPreferredKeywords.getOrDefault(
                  WordContent.normalizeText(entry.getKey()), entry.getKey()),
              entry -> entry.getValue()
          ));
    }
    roundStats.setTexts(countsBySimilarity.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList()));
    roundStats.setAnswerCount(answerCount);
    roundStats.setIndependentCounts(countsBySimilarity.values().stream().collect(Collectors.toList()));
    stats.setRoundStatistics(new ArrayList(Collections.nCopies(round, null)));
    stats.getRoundStatistics().set(round - 1, roundStats);
    return stats;
  }

  /**
   * Group by text similarity and then choose the most common variant as
   * key and calculate the new count.
   */
  private Map<String, Integer> buildCountsBySimilarityMap(
      final Map<String, Long> textCounts,
      final Set<String> bannedKeywords) {
    return textCounts.entrySet().stream()
        .collect(Collectors.groupingBy(e -> WordContent.normalizeText(e.getKey())))
        .entrySet().stream()
        .filter(entry -> !bannedKeywords.contains(entry.getKey()))
        .collect(Collectors.toMap(
            /* Select most common variant as key */
            entry -> entry.getValue().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(""),
            /* Calculate sum of counts of similar texts */
            entry -> entry.getValue().stream()
                .map(Map.Entry::getValue)
                .reduce(Long::sum)
                .map(Long::intValue)
                .orElse(0)));
  }

  @Override
  public PrioritizationAnswerStatistics getPrioritizationStatistics(final String contentId) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }
    final Integer optionCount = ((PrioritizationChoiceContent) content).getOptions().size();
    final PrioritizationAnswerStatistics stats =
        answerRepository.findByContentIdRoundForPrioritization(contentId, optionCount);
    return stats;
  }

  @Override
  public NumericAnswerStatistics getNumericStatistics(final String contentId, final int round) {
    final NumericContent content = (NumericContent) contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }
    final List<NumericAnswer> answers = answerRepository.findByContentIdRound(NumericAnswer.class, contentId, round);
    final NumericAnswerStatistics stats = new NumericAnswerStatistics();
    stats.setContentId(contentId);
    final NumericAnswerStatistics.NumericRoundStatistics roundStats =
        new NumericAnswerStatistics.NumericRoundStatistics();
    roundStats.setRound(round);
    roundStats.setAbstentionCount((int) answers.stream().filter(a -> a.getSelectedNumber() == null).count());
    final List<Double> numbers = answers.stream()
        .map(NumericAnswer::getSelectedNumber)
        .filter(n -> n != null)
        .toList();
    roundStats.setAnswerCount(numbers.size());
    final Map<Double, Long> numberCounts = numbers.stream()
        .collect(Collectors.groupingBy(
        Function.identity(),
        Collectors.counting()));
    roundStats.setSelectedNumbers(new ArrayList<>(numberCounts.keySet()));
    roundStats.setIndependentCounts(numberCounts.values().stream().map(Long::intValue).collect(Collectors.toList()));
    roundStats.setMinimum(StatisticsUtil.findMinimum(numbers));
    roundStats.setMaximum(StatisticsUtil.findMaximum(numbers));
    roundStats.setMean(StatisticsUtil.calculateMean(numbers));
    roundStats.setMedian(StatisticsUtil.calculateMedian(numbers));
    roundStats.setStandardDeviation(StatisticsUtil.calculateStandardDeviation(numbers));
    roundStats.setVariance(StatisticsUtil.calculateVariance(numbers));
    if (content.getCorrectNumber() != null) {
      roundStats.setCorrectAnswerFraction(
          calculateNumericCorrectFraction(numbers, content.getCorrectNumber(), content.getTolerance()));
    }
    final List<NumericAnswerStatistics.NumericRoundStatistics> roundStatisticsList =
        new ArrayList<>(Collections.nCopies(round, null));
    roundStatisticsList.set(round - 1, roundStats);
    stats.setRoundStatistics(roundStatisticsList);
    return stats;
  }

  @Override
  public NumericAnswerStatistics getNumericStatistics(final String contentId) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }
    return getNumericStatistics(content.getId(), content.getState().getRound());
  }

  private static double calculateNumericCorrectFraction(
      final List<Double> numbers, final double correctNumber, final double tolerance) {
    if (numbers.isEmpty()) {
      return 0;
    }
    final double correctCount = numbers.stream()
        .filter(number -> isNumericAnswerCorrect(number, correctNumber, tolerance))
        .count();
    final double totalCount = numbers.size();

    return correctCount / totalCount;
  }

  private static boolean isNumericAnswerCorrect(
      final Double selectedNumber, final double correctNumber, final double tolerance) {
    return selectedNumber >= correctNumber - tolerance && selectedNumber <= correctNumber + tolerance;
  }

  @Override
  public List<String> getAnswerIdsByContentIdNotHidden(final String contentId) {
    return answerRepository.findIdsByContentIdAndHidden(contentId, true);
  }

  @Override
  public List<String> getAnswerIdsByCreatorIdRoomId(final String creatorId, final String roomId) {
    return answerRepository.findIdsByCreatorIdRoomId(creatorId, roomId);
  }

  @Override
  public List<String> getAnswerIdsByCreatorIdContentIdsRound(
      final String creatorId, final List<String> contentIds, final int round) {
    if (round > 0) {
      return Stream.concat(
          /* TODO:
           *   Currently round 0 is always added because of text answers.
           *   It might be better to always use round 1 for text or allow multiple rounds.
           *   This would require refactoring in other parts of the application. */
          answerRepository.findIdsByCreatorIdContentIdsRound(creatorId, contentIds, 0).stream(),
          answerRepository.findIdsByCreatorIdContentIdsRound(creatorId, contentIds, round).stream()
      ).collect(Collectors.toList());
    } else {
      final List<Answer> answerStubs = contentService.get(contentIds).stream()
          .map(content -> new Answer(content, creatorId)).collect(Collectors.toList());
      return answerRepository.findIdsByAnswerStubs(answerStubs);
    }
  }

  @Override
  public Answer getAnswerByContentIdAndUserIdAndCurrentRound(final String contentId, final String userId) {
    final Content content = contentService.get(contentId);
    if (content == null) {
      throw new NotFoundException();
    }

    final int piRound = content.getState().getRound();

    return answerRepository.findByContentIdUserIdPiRound(contentId, Answer.class, userId, piRound);
  }

  @Override
  public Answer create(final Answer answer) {
    prepareCreate(answer);
    final AnswerUniqueKey key = new AnswerUniqueKey(answer.getCreatorId(), answer.getContentId());
    if (answerQueue.add(key)) {
      queuedAnswers.put(key, answer);
    }
    finalizeCreate(answer);

    return answer;
  }

  @Override
  protected void prepareCreate(final Answer answer) {
    final User user = authenticationService.getCurrentUser();
    final Content content = contentService.get(answer.getContentId());
    if (content == null) {
      throw new NotFoundException();
    }

    if (!NIL_UUID.equals(answer.getCreatorId())) {
      final Answer maybeExistingAnswer = answerRepository.findByContentIdUserIdPiRound(
          content.getId(),
          Answer.class,
          user.getId(),
          content.getState().getRound());

      if (maybeExistingAnswer != null) {
        throw new AlreadyAnsweredContentException();
      }
    }

    answer.setCreationTimestamp(new Date());
    if (answer.getCreatorId() == null) {
      answer.setCreatorId(user.getId());
    }
    answer.setRoomId(content.getRoomId());

    if (content.isScorable() && !answer.isAbstention()) {
      final double achievedPoints = content.calculateAchievedPoints(answer);
      if (content.getDuration() == 0 || content.getState().getAnsweringEndTime() == null) {
        answer.setPoints((int) Math.round(achievedPoints));
      } else {
        answer.setPoints((int) Math.round(content.calculateCompetitivePoints(
            answer.getCreationTimestamp().toInstant(), achievedPoints)));
        final int timeLeft = (int) Instant.now().until(
            content.getState().getAnsweringEndTime().toInstant(), ChronoUnit.MILLIS);
        answer.setDurationMs(content.getDuration() * 1000 - timeLeft);
      }
    }
    answer.setResult(content.determineAnswerResult(answer).getState());

    if (content.getFormat() == Content.Format.TEXT) {
      answer.setRound(0);
    } else {
      if (content.getFormat() == Content.Format.WORDCLOUD) {
        final MultipleTextsAnswer multipleTextsAnswer = (MultipleTextsAnswer) answer;
        final Set<String> sanitizedAnswers = new HashSet<>();
        // Remove blank and similar texts
        multipleTextsAnswer.setTexts(
            multipleTextsAnswer.getTexts().stream()
                .filter(t -> !t.isBlank())
                .filter(t -> sanitizedAnswers.add(
                    specialCharPattern.matcher(t.toLowerCase()).replaceAll("")))
                .collect(Collectors.toList()));
      } else if (content instanceof NumericContent numericContent) {
        final Double selectedNumber = ((NumericAnswer) answer).getSelectedNumber();
        if (selectedNumber != null
            && (selectedNumber < numericContent.getMinNumber() || selectedNumber > numericContent.getMaxNumber())) {
          throw new IllegalArgumentException("Selected number must be in content range.");
        }
      }
      answer.setRound(content.getState().getRound());
    }
    validate(answer);
  }

  @Override
  protected void prepareUpdate(final Answer answer) {
    final Content content = contentService.get(answer.getContentId());
    answer.setContentId(content.getId());
    answer.setRoomId(content.getRoomId());
    validate(answer);
  }

  @EventListener
  public void handleContentDeletion(final BeforeDeletionEvent<? extends Content> event) {
    final Iterable<Answer> answers = answerRepository.findStubsByContentIdAndHidden(event.getEntity().getId(), false);
    answers.forEach(a -> a.setRoomId(event.getEntity().getRoomId()));
    delete(answers, Initiator.CASCADE);
  }

  @Override
  public void hideTextAnswer(final TextAnswer answer, final boolean hidden) {
    answer.setHidden(hidden);
    update(answer);
  }

  @Override
  public Collection<LeaderboardEntry> buildAliasedLeaderboard(
      final ContentGroup contentGroup,
      final String currentContentId,
      final Locale locale) {
    final Map<String, RoomUserAlias> aliasMappings =
        roomUserAliasService.getUserAliasMappingsByRoomId(contentGroup.getRoomId(), locale);
    final Map<String, LeaderboardCurrentResult> currentResults =
        currentContentId != null ? buildContentLeaderboard(currentContentId) : new HashMap<>();
    return buildContentGroupLeaderboard(contentGroup).entrySet().stream()
        .map(es -> new LeaderboardEntry(
            aliasMappings.get(es.getKey()), es.getValue(), currentResults.get(es.getKey())))
        .collect(Collectors.toList());
  }

  @Cacheable(cacheNames = "leaderboard-contentgroup")
  private Map<String, Integer> buildContentGroupLeaderboard(final ContentGroup contentGroup) {
    final Map<String, Integer> leaderboard = new HashMap<>();
    final List<Content> contents = contentService.get(contentGroup.getContentIds());
    for (final Content content : contents) {
      final Map<String, Integer> contentScores =
          answerRepository.findUserScoreByContentIdRound(content.getId(), content.getState().getRound());
      for (final Map.Entry<String, Integer> entry : contentScores.entrySet()) {
        if (entry.getKey().equals(NIL_UUID)) {
          continue;
        }
        final int score = leaderboard.getOrDefault(entry.getKey(), 0);
        leaderboard.put(entry.getKey(), score + entry.getValue());
      }
    }
    return leaderboard;
  }

  @Cacheable(cacheNames = "leaderboard-content")
  private Map<String, LeaderboardCurrentResult> buildContentLeaderboard(final String contentId) {
    final Content content = contentService.get(contentId);
    final List<Answer> answers = answerRepository.findByContentIdRound(
        Answer.class, contentId, content.getState().getRound());
    return answers.stream()
      .filter(a -> !a.getCreatorId().equals(NIL_UUID))
      .collect(Collectors.toMap(
          a -> a.getCreatorId(),
          a -> new LeaderboardCurrentResult(
              a.getPoints(),
              a.getDurationMs(),
             content.determineAnswerResult(a).getState() == AnswerResult.AnswerResultState.CORRECT)));
  }

  public List<AnswerStatisticsSummaryEntry> calculateStatsByContentIds(
      final String roomId,
      final List<String> contentIds) {
    final Map<ContentIdRoundResultKey, Integer> counts =
        answerRepository.countByRoomIdGroupByContentIdRoundResult(roomId);
    final List<AnswerStatisticsSummaryEntry> results = counts.entrySet().stream()
        .filter(entry -> contentIds.contains(entry.getKey().contentId()))
        .map(entry -> {
          final var k = entry.getKey();
          return new AnswerStatisticsSummaryEntry(
              k.contentId(),
              k.round(),
              k.result(),
              entry.getValue());
        })
        .collect(Collectors.toList());
    if (results.stream().anyMatch(e -> e.result() == AnswerResult.AnswerResultState.UNKNOWN)) {
      throw new NotFoundException("Statistics are not available for this content group.");
    }
    return results;
  }

  private void evictLeaderboardCacheEntries(final String contentId) {
    cacheManager.getCache("leaderboard-content").evict(contentId);
  }

  public void clearContentGroupLeaderboardCache() {
    cacheManager.getCache("leaderboard-contentgroup").clear();
  }

  private record AnswerUniqueKey(String userId, String contentId) { }
}
