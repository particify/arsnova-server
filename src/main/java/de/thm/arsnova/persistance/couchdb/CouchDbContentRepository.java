package de.thm.arsnova.persistance.couchdb;

import de.thm.arsnova.entities.migration.v2.Content;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.persistance.ContentRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import org.ektorp.BulkDeleteDocument;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentOperationResult;
import org.ektorp.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CouchDbContentRepository extends CouchDbCrudRepository<Content> implements ContentRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbContentRepository.class);

	@Autowired
	private LogEntryRepository dbLogger;

	public CouchDbContentRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Content.class, db, "by_sessionid", createIfNotExists);
	}

	@Override
	public List<Content> findBySessionIdForUsers(final String sessionId) {
		final List<Content> contents = new ArrayList<>();
		final List<Content> questions1 = findBySessionIdAndVariantAndActive(sessionId, "lecture", true);
		final List<Content> questions2 = findBySessionIdAndVariantAndActive(sessionId, "preparation", true);
		final List<Content> questions3 = findBySessionIdAndVariantAndActive(sessionId, "flashcard", true);
		contents.addAll(questions1);
		contents.addAll(questions2);
		contents.addAll(questions3);

		return contents;
	}

	@Override
	public List<Content> findBySessionIdForSpeaker(final String sessionId) {
		return findBySessionIdAndVariantAndActive(new Object[] {sessionId}, sessionId);
	}

	@Override
	public int countBySessionId(final String sessionId) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(sessionId))
				.endKey(ComplexKey.of(sessionId, ComplexKey.emptyObject())));

		return result.getSize();
	}

	@Override
	public List<String> findIdsBySessionId(final String sessionId) {
		return collectQuestionIds(db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(sessionId))
				.endKey(ComplexKey.of(sessionId, ComplexKey.emptyObject()))));
	}

	@Override
	public List<String> findIdsBySessionIdAndVariant(final String sessionId, final String variant) {
		return collectQuestionIds(db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(sessionId, variant))
				.endKey(ComplexKey.of(sessionId, variant, ComplexKey.emptyObject()))));
	}

	@Override
	public int deleteBySessionId(final String sessionId) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(sessionId))
				.endKey(ComplexKey.of(sessionId, ComplexKey.emptyObject()))
				.reduce(false));

		final List<BulkDeleteDocument> deleteDocs = new ArrayList<>();
		for (final ViewResult.Row a : result.getRows()) {
			final BulkDeleteDocument d = new BulkDeleteDocument(a.getId(), a.getValueAsNode().get("_rev").asText());
			deleteDocs.add(d);
		}
		List<DocumentOperationResult> errors = db.executeBulk(deleteDocs);

		return deleteDocs.size() - errors.size();
	}

	@Override
	public List<String> findUnansweredIdsBySessionIdAndUser(final String sessionId, final User user) {
		final ViewResult result = db.queryView(createQuery("questionid_by_user_sessionid_variant")
				.designDocId("_design/Answer")
				.startKey(ComplexKey.of(user.getUsername(), sessionId))
				.endKey(ComplexKey.of(user.getUsername(), sessionId, ComplexKey.emptyObject())));
		final List<String> answeredIds = new ArrayList<>();
		for (final ViewResult.Row row : result.getRows()) {
			answeredIds.add(row.getId());
		}
		return collectUnansweredQuestionIds(findIdsBySessionId(sessionId), answeredIds);
	}

	@Override
	public List<String> findUnansweredIdsBySessionIdAndUserOnlyLectureVariant(final String sessionId, final User user) {
		final ViewResult result = db.queryView(createQuery("questionid_piround_by_user_sessionid_variant")
				.designDocId("_design/Answer")
				.key(ComplexKey.of(user.getUsername(), sessionId, "lecture")));
		final Map<String, Integer> answeredQuestions = new HashMap<>();
		for (final ViewResult.Row row : result.getRows()) {
			answeredQuestions.put(row.getId(), row.getKeyAsNode().get(2).asInt());
		}

		return collectUnansweredQuestionIdsByPiRound(findBySessionIdOnlyLectureVariantAndActive(sessionId), answeredQuestions);
	}

	@Override
	public List<String> findUnansweredIdsBySessionIdAndUserOnlyPreparationVariant(final String sessionId, final User user) {
		final ViewResult result = db.queryView(createQuery("questionid_piround_by_user_sessionid_variant")
				.designDocId("_design/Answer")
				.key(ComplexKey.of(user.getUsername(), sessionId, "preparation")));
		final Map<String, Integer> answeredQuestions = new HashMap<>();
		for (final ViewResult.Row row : result.getRows()) {
			answeredQuestions.put(row.getId(), row.getKeyAsNode().get(2).asInt());
		}

		return collectUnansweredQuestionIdsByPiRound(findBySessionIdOnlyPreparationVariantAndActive(sessionId), answeredQuestions);
	}

	@Override
	public List<Content> findBySessionIdOnlyLectureVariantAndActive(final String sessionId) {
		return findBySessionIdAndVariantAndActive(sessionId, "lecture", true);
	}

	@Override
	public List<Content> findBySessionIdOnlyLectureVariant(final String sessionId) {
		return findBySessionIdAndVariantAndActive(sessionId, "lecture");
	}

	@Override
	public List<Content> findBySessionIdOnlyFlashcardVariantAndActive(final String sessionId) {
		return findBySessionIdAndVariantAndActive(sessionId, "flashcard", true);
	}

	@Override
	public List<Content> findBySessionIdOnlyFlashcardVariant(final String sessionId) {
		return findBySessionIdAndVariantAndActive(sessionId, "flashcard");
	}

	@Override
	public List<Content> findBySessionIdOnlyPreparationVariantAndActive(final String sessionId) {
		return findBySessionIdAndVariantAndActive(sessionId, "preparation", true);
	}

	@Override
	public List<Content> findBySessionIdOnlyPreparationVariant(final String sessionId) {
		return findBySessionIdAndVariantAndActive(sessionId, "preparation");
	}

	@Override
	public List<Content> findBySessionId(final String sessionId) {
		return findBySessionIdAndVariantAndActive(sessionId);
	}

	@Override
	public List<Content> findBySessionIdAndVariantAndActive(final Object... keys) {
		final Object[] endKeys = Arrays.copyOf(keys, keys.length + 1);
		endKeys[keys.length] = ComplexKey.emptyObject();
		final List<Content> contents = db.queryView(createQuery("by_sessionid_variant_active")
						.includeDocs(true)
						.reduce(false)
						.startKey(ComplexKey.of(keys))
						.endKey(ComplexKey.of(endKeys)),
				Content.class);
		for (final Content content : contents) {
			content.updateRoundManagementState();
			//content.setSessionKeyword(session.getKeyword());
		}

		return contents;
	}

	@Override
	public int countLectureVariantBySessionId(final String sessionId) {
		/* TODO: reduce code duplication */
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(sessionId, "lecture"))
				.endKey(ComplexKey.of(sessionId, "lecture", ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int countFlashcardVariantBySessionId(final String sessionId) {
		/* TODO: reduce code duplication */
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(sessionId, "flashcard"))
				.endKey(ComplexKey.of(sessionId, "flashcard", ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int countPreparationVariantBySessionId(final String sessionId) {
		/* TODO: reduce code duplication */
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(sessionId, "preparation"))
				.endKey(ComplexKey.of(sessionId, "preparation", ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	private List<String> collectUnansweredQuestionIds(
			final List<String> contentIds,
			final List<String> answeredContentIds
	) {
		final List<String> unanswered = new ArrayList<>();
		for (final String contentId : contentIds) {
			if (!answeredContentIds.contains(contentId)) {
				unanswered.add(contentId);
			}
		}
		return unanswered;
	}

	private List<String> collectUnansweredQuestionIdsByPiRound(
			final List<Content> contents,
			final Map<String, Integer> answeredQuestions
	) {
		final List<String> unanswered = new ArrayList<>();

		for (final Content content : contents) {
			if (!"slide".equals(content.getQuestionType()) && (!answeredQuestions.containsKey(content.getId())
					|| (answeredQuestions.containsKey(content.getId()) && answeredQuestions.get(content.getId()) != content.getPiRound()))) {
				unanswered.add(content.getId());
			}
		}

		return unanswered;
	}

	private List<String> collectQuestionIds(final ViewResult viewResult) {
		final List<String> ids = new ArrayList<>();
		for (final ViewResult.Row row : viewResult.getRows()) {
			ids.add(row.getId());
		}
		return ids;
	}

	/* TODO: remove if this method is no longer used */
	@Override
	public List<String> findIdsBySessionIdAndVariantAndSubject(final String sessionId, final String questionVariant, final String subject) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(sessionId, questionVariant, 1, subject))
				.endKey(ComplexKey.of(sessionId, questionVariant, 1, subject, ComplexKey.emptyObject())));

		final List<String> qids = new ArrayList<>();

		for (final ViewResult.Row row : result.getRows()) {
			final String s = row.getId();
			qids.add(s);
		}

		return qids;
	}

	@Override
	public List<String> findSubjectsBySessionIdAndVariant(final String sessionId, final String questionVariant) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_variant_active")
				.startKey(ComplexKey.of(sessionId, questionVariant))
				.endKey(ComplexKey.of(sessionId, questionVariant, ComplexKey.emptyObject())));

		final Set<String> uniqueSubjects = new HashSet<>();

		for (final ViewResult.Row row : result.getRows()) {
			uniqueSubjects.add(row.getKeyAsNode().get(3).asText());
		}

		return new ArrayList<>(uniqueSubjects);
	}
}
