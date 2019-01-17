package de.thm.arsnova.persistence.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.AnswerStatistics;
import de.thm.arsnova.persistence.AnswerRepository;
import de.thm.arsnova.persistence.LogEntryRepository;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CouchDbAnswerRepository extends CouchDbCrudRepository<Answer> implements AnswerRepository, ApplicationEventPublisherAware {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbAnswerRepository.class);

	@Autowired
	private LogEntryRepository dbLogger;

	private ApplicationEventPublisher publisher;

	public CouchDbAnswerRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Answer.class, db, "by_id", createIfNotExists);
	}

	@Override
	public void setApplicationEventPublisher(final ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	protected Iterable<Answer> createEntityStubs(final ViewResult viewResult) {
		return super.createEntityStubs(viewResult, Answer::setContentId);
	}

	@Override
	public Iterable<Answer> findStubsByContentId(final String contentId) {
		return createEntityStubs(db.queryView(createQuery("by_contentid").key(contentId)));
	}

	@Override
	public Iterable<Answer> findStubsByContentIds(final List<String> contentIds) {
		return createEntityStubs(db.queryView(createQuery("by_contentid").keys(contentIds)));
	}

	@Override
	public <T extends Answer> T findByContentIdUserIdPiRound(final String contentId, final Class<T> type, final String userId, final int piRound) {
		final List<T> answerList = db.queryView(createQuery("by_contentid_creatorid_round")
				.key(ComplexKey.of(contentId, userId, piRound)), type);
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
				Integer[] indexes = new Integer[jsonIndexes.size()];
				/* Count independently */
				for (int i = 0; i < jsonIndexes.size(); i++) {
					indexes[i] = jsonIndexes.get(i).asInt();
					independentCounts.set(indexes[i], independentCounts.get(indexes[i]) + d.getValueAsInt());
				}
				/* Count option combinations */
				AnswerStatistics.RoundStatistics.Combination combination =
						combinations.getOrDefault(Arrays.asList(indexes),
								new AnswerStatistics.RoundStatistics.Combination(
										Arrays.asList(indexes), d.getValueAsInt()));
				combinations.put(Arrays.asList(indexes), combination);
				roundStats.setCombinatedCounts(combinations.values());
			}
		}
		roundStats.setIndependentCounts(independentCounts);
		/* TODO: Review - might lead easily to IndexOutOfBoundsExceptions - use a Map instead? */
		List<AnswerStatistics.RoundStatistics> roundStatisticsList = new ArrayList(Collections.nCopies(round, null));
		roundStatisticsList.set(round - 1, roundStats);
		stats.setRoundStatistics(roundStatisticsList);

		return stats;
	}

	@Override
	public int countByContentId(final String contentId) {
		final ViewResult result = db.queryView(createQuery("by_contentid_round_body_subject")
				.reduce(true)
				.startKey(ComplexKey.of(contentId))
				.endKey(ComplexKey.of(contentId, ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int countByContentIdRound(final String contentId, final int round) {
		final ViewResult result = db.queryView(createQuery("by_contentid_round_body_subject")
				.reduce(true)
				.startKey(ComplexKey.of(contentId, round))
				.endKey(ComplexKey.of(contentId, round, ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public <T extends Answer> List<T> findByContentId(final String contentId, final Class<T> type, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		final List<T> answers = db.queryView(createQuery("by_contentid_creationtimestamp")
						.skip(qSkip)
						.limit(qLimit)
						.includeDocs(true)
						.startKey(ComplexKey.of(contentId, ComplexKey.emptyObject()))
						.endKey(ComplexKey.of(contentId))
						.descending(true),
				type);

		return answers;
	}

	@Override
	public List<Answer> findByUserIdRoomId(final String userId, final String roomId) {
		return queryView("by_creatorid_roomid", ComplexKey.of(userId, roomId));
	}

	@Override
	public int countByRoomId(final String roomId) {
		final ViewResult result = db.queryView(createQuery("by_roomid")
				.key(roomId)
				.reduce(true));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int countByRoomIdOnlyLectureVariant(final String roomId) {
		return countBySessionIdVariant(roomId, "lecture");
	}

	@Override
	public int countByRoomIdOnlyPreparationVariant(final String roomId) {
		return countBySessionIdVariant(roomId, "preparation");
	}

	private int countBySessionIdVariant(final String sessionId, final String variant) {
		final ViewResult result = db.queryView(createQuery("by_roomid_variant")
				.key(ComplexKey.of(sessionId, variant)));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}
}
