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

package de.thm.arsnova.persistence.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.model.MultipleTextsAnswer;
import de.thm.arsnova.persistence.AnswerRepository;

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
	public AnswerStatistics findByContentIdRound(final String contentId, final int round, final int optionCount) {
		final ViewResult result = db.queryView(createQuery("by_contentid_round_selectedchoiceindexes")
						.group(true)
						.startKey(ComplexKey.of(contentId, round))
						.endKey(ComplexKey.of(contentId, round, ComplexKey.emptyObject())));
		final AnswerStatistics stats = new AnswerStatistics();
		stats.setContentId(contentId);
		final AnswerStatistics.RoundStatistics roundStats = new AnswerStatistics.RoundStatistics();
		roundStats.setRound(round);
		roundStats.setAbstentionCount(0);
		final List<Integer> independentCounts = new ArrayList(Collections.nCopies(optionCount, 0));
		final Map<List<Integer>, AnswerStatistics.RoundStatistics.Combination> combinations = new HashMap();
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
				final AnswerStatistics.RoundStatistics.Combination combination =
						combinations.getOrDefault(Arrays.asList(indexes),
								new AnswerStatistics.RoundStatistics.Combination(
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
		final List<AnswerStatistics.RoundStatistics> roundStatisticsList = new ArrayList(Collections.nCopies(round, null));
		roundStatisticsList.set(round - 1, roundStats);
		stats.setRoundStatistics(roundStatisticsList);

		return stats;
	}

	@Override
	public List<MultipleTextsAnswer> findByContentIdRoundForText(final String contentId, final int round) {
		return db.queryView(createQuery("by_contentid_round_selectedchoiceindexes")
				.reduce(false)
				.includeDocs(true)
				.startKey(ComplexKey.of(contentId, round))
				.endKey(ComplexKey.of(contentId, round, ComplexKey.emptyObject())), MultipleTextsAnswer.class);
	}
}
