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

package net.particify.arsnova.core.persistence.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;

import net.particify.arsnova.core.model.Answer;
import net.particify.arsnova.core.model.AnswerResult;
import net.particify.arsnova.core.model.ChoiceAnswerStatistics;
import net.particify.arsnova.core.model.ChoiceAnswerStatistics.ChoiceRoundStatistics;
import net.particify.arsnova.core.model.ContentIdRoundResultKey;
import net.particify.arsnova.core.model.PrioritizationAnswer;
import net.particify.arsnova.core.model.PrioritizationAnswerStatistics;
import net.particify.arsnova.core.model.PrioritizationAnswerStatistics.PrioritizationRoundStatistics;
import net.particify.arsnova.core.persistence.AnswerRepository;

public class CouchDbAnswerRepository extends CouchDbCrudRepository<Answer>
    implements AnswerRepository {
  public CouchDbAnswerRepository(final CouchDbConnector db, final boolean createIfNotExists) {
    super(Answer.class, db, "by_id", createIfNotExists);
  }

  protected Iterable<Answer> createEntityStubs(final ViewResult viewResult) {
    return super.createEntityStubs(viewResult, Answer::setContentId);
  }

  @Override
  public Iterable<Answer> findStubsByContentIdAndHidden(final String contentId, final boolean excludeHidden) {
    return createEntityStubs(queryByContentIdAndHidden(contentId, excludeHidden));
  }

  @Override
  public List<String> findIdsByContentIdAndHidden(final String contentId, final boolean excludeHidden) {
    final ViewResult result = queryByContentIdAndHidden(contentId, excludeHidden);

    return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
  }

  private ViewResult queryByContentIdAndHidden(final String contentId, final boolean excludeHidden) {
    ViewQuery query = createQuery("by_contentid_hidden").reduce(false);
    query = excludeHidden
        ? query.key(ComplexKey.of(contentId, false))
        : query.startKey(ComplexKey.of(contentId)).endKey(ComplexKey.of(contentId, ComplexKey.emptyObject()));

    return db.queryView(query);
  }

  @Override
  public List<String> findIdsByCreatorIdRoomId(final String creatorId, final String roomId) {
    final ViewResult result = db.queryView(createQuery("by_creatorid_roomid")
        .reduce(false)
        .key(ComplexKey.of(creatorId, roomId)));

    return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
  }

  @Override
  public List<String> findIdsByCreatorIdContentIdsRound(
      final String creatorId, final List<String> contentIds, final int round) {
    final ViewResult result =  db.queryView(
        createQuery("by_contentid_creatorid_round")
            .keys(contentIds.stream()
                .map(contentId -> ComplexKey.of(contentId, creatorId, round))
                .collect(Collectors.toList())));

    return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
  }

  @Override
  public List<String> findIdsByAnswerStubs(final List<Answer> answerStubs) {
    final ViewResult result = db.queryView(
        createQuery("by_contentid_creatorid_round")
            .keys(answerStubs.stream()
                .map(a -> ComplexKey.of(a.getContentId(), a.getCreatorId(), a.getRound()))
                .collect(Collectors.toList())));

    return result.getRows().stream().map(ViewResult.Row::getId).collect(Collectors.toList());
  }

  @Override
  public <T extends Answer> T findByContentIdUserIdPiRound(
      final String contentId, final Class<T> type, final String userId, final int piRound) {
    final List<T> answerList = db.queryView(createQuery("by_contentid_creatorid_round")
        .key(ComplexKey.of(contentId, userId, piRound)).includeDocs(true), type);
    return answerList.isEmpty() ? null : answerList.get(0);
  }

  @Override
  public ChoiceAnswerStatistics findStatisticsByContentIdRound(
      final String contentId, final int round, final int optionCount) {
    final ViewResult result = db.queryView(createQuery("by_contentid_round_selectedchoiceindexes")
            .group(true)
            .startKey(ComplexKey.of(contentId, round))
            .endKey(ComplexKey.of(contentId, round, ComplexKey.emptyObject())));
    final ChoiceAnswerStatistics stats = new ChoiceAnswerStatistics();
    stats.setContentId(contentId);
    final ChoiceAnswerStatistics.ChoiceRoundStatistics roundStats = new ChoiceAnswerStatistics.ChoiceRoundStatistics();
    roundStats.setRound(round);
    roundStats.setAbstentionCount(0);
    final List<Integer> independentCounts = new ArrayList(Collections.nCopies(optionCount, 0));
    final Map<List<Integer>, ChoiceAnswerStatistics.ChoiceRoundStatistics.Combination> combinations = new HashMap();
    for (final ViewResult.Row d : result) {
      if (d.getKeyAsNode().get(2).size() == 0) {
        /* Abstentions */
        roundStats.setAbstentionCount(d.getValueAsInt());
      } else {
        /* Answers:
         * Extract selected indexes from key[2] and count from value */
        final JsonNode jsonIndexes = d.getKeyAsNode().get(2);
        final Integer[] indexes = new Integer[jsonIndexes.size()];
        /* Count independently */
        for (int i = 0; i < jsonIndexes.size(); i++) {
          indexes[i] = jsonIndexes.get(i).asInt();
          independentCounts.set(indexes[i], independentCounts.get(indexes[i]) + d.getValueAsInt());
        }
        /* Count option combinations */
        final ChoiceAnswerStatistics.ChoiceRoundStatistics.Combination combination =
            combinations.getOrDefault(Arrays.asList(indexes),
                new ChoiceAnswerStatistics.ChoiceRoundStatistics.Combination(
                    Arrays.asList(indexes), d.getValueAsInt()));
        combinations.put(Arrays.asList(indexes), combination);
        roundStats.setCombinatedCounts(combinations.values());
        roundStats.setAnswerCount(combinations.values().stream()
            .map(c -> c.getCount())
            .reduce(0, Integer::sum));
      }
    }
    roundStats.setIndependentCounts(independentCounts);
    /* TODO: Review - might lead easily to IndexOutOfBoundsExceptions - use a Map instead? */
    final List<ChoiceRoundStatistics> roundStatisticsList = new ArrayList(Collections.nCopies(round, null));
    roundStatisticsList.set(round - 1, roundStats);
    stats.setRoundStatistics(roundStatisticsList);

    return stats;
  }

  @Override
  public PrioritizationAnswerStatistics findByContentIdRoundForPrioritization(
      final String contentId, final int optionCount) {
    final List<PrioritizationAnswer> answers = db.queryView(createQuery("by_contentid_hidden")
            .reduce(false)
            .includeDocs(true)
            .startKey(ComplexKey.of(contentId, false))
            .endKey(ComplexKey.of(contentId, false, ComplexKey.emptyObject())), PrioritizationAnswer.class);
    final PrioritizationAnswerStatistics stats = new PrioritizationAnswerStatistics();
    stats.setContentId(contentId);
    final PrioritizationRoundStatistics roundStats = new PrioritizationRoundStatistics();
    roundStats.setRound(1);
    roundStats.setAbstentionCount(0);
    final List<Integer> assignedPoints = new ArrayList<>(Collections.nCopies(optionCount, 0));
    for (final PrioritizationAnswer answer : answers) {
      final List<Integer> points = answer.getAssignedPoints();
      if (points.size() == 0) {
        roundStats.setAbstentionCount(roundStats.getAbstentionCount() + 1);
      } else {
        for (int i = 0; i < points.size(); i++) {
          assignedPoints.set(i, assignedPoints.get(i) + points.get(i));
        }
      }
    }
    roundStats.setAnswerCount(answers.size() - roundStats.getAbstentionCount());
    roundStats.setAssignedPoints(assignedPoints);
    final List<PrioritizationRoundStatistics> roundStatisticsList = new ArrayList<>(Collections.nCopies(1, null));
    roundStatisticsList.set(0, roundStats);
    stats.setRoundStatistics(roundStatisticsList);

    return stats;
  }

  @Override
  public <T extends Answer> List<T> findByContentIdRound(
      final Class<T> clazz, final String contentId, final int round) {
    return db.queryView(createQuery("by_contentid_round_selectedchoiceindexes")
        .reduce(false)
        .includeDocs(true)
        .startKey(ComplexKey.of(contentId, round))
        .endKey(ComplexKey.of(contentId, round, ComplexKey.emptyObject())), clazz);
  }

  @Override
  public Map<String, Integer> findUserScoreByContentIdRound(final String contentId, final int round) {
    final ViewResult result = db.queryView(createQuery("points_by_contentid_round_creatorid")
        .reduce(true)
        .startKey(ComplexKey.of(contentId, round))
        .endKey(ComplexKey.of(contentId, round, ComplexKey.emptyObject()))
        .group(true));
    return StreamSupport.stream(result.spliterator(), false).collect(Collectors.toMap(
        r -> r.getKeyAsNode().get(2).asText(),
        r -> r.getValueAsInt()
    ));
  }

  @Override
  public Map<ContentIdRoundResultKey, Integer> countByRoomIdGroupByContentIdRoundResult(
      final String roomId) {
    final ViewResult result = db.queryView(createQuery("by_roomid_contentid_round_result")
        .reduce(true)
        .startKey(ComplexKey.of(roomId))
        .endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))
        .group(true));
    return StreamSupport.stream(result.spliterator(), false).collect(Collectors.toMap(
        r -> {
          final var node = r.getKeyAsNode();
          final var contentId = node.get(1).asText();
          final var round = node.get(2).asInt();
          final var resultState = node.hasNonNull(3)
              ? AnswerResult.AnswerResultState.valueOf(node.get(3).asText())
              : AnswerResult.AnswerResultState.UNKNOWN;
          return new ContentIdRoundResultKey(contentId, round, resultState);
        },
        r -> r.getValueAsInt()
    ));
  }
}
