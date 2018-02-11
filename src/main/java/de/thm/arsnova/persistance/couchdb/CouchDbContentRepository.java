package de.thm.arsnova.persistance.couchdb;

import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.UserAuthentication;
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
		super(Content.class, db, "by_roomid", createIfNotExists);
	}

	@Override
	public List<Content> findByRoomIdForUsers(final String roomId) {
		final List<Content> contents = new ArrayList<>();
		final List<Content> questions1 = findByRoomIdAndVariantAndActive(roomId, "lecture", true);
		final List<Content> questions2 = findByRoomIdAndVariantAndActive(roomId, "preparation", true);
		final List<Content> questions3 = findByRoomIdAndVariantAndActive(roomId, "flashcard", true);
		contents.addAll(questions1);
		contents.addAll(questions2);
		contents.addAll(questions3);

		return contents;
	}

	@Override
	public List<Content> findByRoomIdForSpeaker(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId);
	}

	@Override
	public int countByRoomId(final String roomId) {
		final ViewResult result = db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId))
				.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))
				.reduce(true));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public List<String> findIdsByRoomId(final String roomId) {
		return collectQuestionIds(db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId))
				.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))));
	}

	@Override
	public List<String> findIdsByRoomIdAndVariant(final String roomId, final String variant) {
		return collectQuestionIds(db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId, variant))
				.endKey(ComplexKey.of(roomId, variant, ComplexKey.emptyObject()))));
	}

	@Override
	public int deleteByRoomId(final String roomId) {
		final ViewResult result = db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId))
				.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))
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
	public List<String> findUnansweredIdsByRoomIdAndUser(final String roomId, final UserAuthentication user) {
		final ViewResult result = db.queryView(createQuery("contentid_by_creatorid_roomid_variant")
				.designDocId("_design/Answer")
				.startKey(ComplexKey.of(user.getId(), roomId))
				.endKey(ComplexKey.of(user.getUsername(), roomId, ComplexKey.emptyObject())));
		final List<String> answeredIds = new ArrayList<>();
		for (final ViewResult.Row row : result.getRows()) {
			answeredIds.add(row.getId());
		}
		return collectUnansweredQuestionIds(findIdsByRoomId(roomId), answeredIds);
	}

	@Override
	public List<String> findUnansweredIdsByRoomIdAndUserOnlyLectureVariant(final String roomId, final UserAuthentication user) {
		final ViewResult result = db.queryView(createQuery("contentid_round_by_creatorid_roomid_variant")
				.designDocId("_design/Answer")
				.key(ComplexKey.of(user.getId(), roomId, "lecture")));
		final Map<String, Integer> answeredQuestions = new HashMap<>();
		for (final ViewResult.Row row : result.getRows()) {
			answeredQuestions.put(row.getId(), row.getKeyAsNode().get(2).asInt());
		}

		return collectUnansweredQuestionIdsByPiRound(findByRoomIdOnlyLectureVariantAndActive(roomId), answeredQuestions);
	}

	@Override
	public List<String> findUnansweredIdsByRoomIdAndUserOnlyPreparationVariant(final String roomId, final UserAuthentication user) {
		final ViewResult result = db.queryView(createQuery("contentid_round_by_creatorid_roomid_variant")
				.designDocId("_design/Answer")
				.key(ComplexKey.of(user.getId(), roomId, "preparation")));
		final Map<String, Integer> answeredQuestions = new HashMap<>();
		for (final ViewResult.Row row : result.getRows()) {
			answeredQuestions.put(row.getId(), row.getKeyAsNode().get(2).asInt());
		}

		return collectUnansweredQuestionIdsByPiRound(findByRoomIdOnlyPreparationVariantAndActive(roomId), answeredQuestions);
	}

	@Override
	public List<Content> findByRoomIdOnlyLectureVariantAndActive(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId, "lecture", true);
	}

	@Override
	public List<Content> findByRoomIdOnlyLectureVariant(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId, "lecture");
	}

	@Override
	public List<Content> findByRoomIdOnlyFlashcardVariantAndActive(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId, "flashcard", true);
	}

	@Override
	public List<Content> findByRoomIdOnlyFlashcardVariant(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId, "flashcard");
	}

	@Override
	public List<Content> findByRoomIdOnlyPreparationVariantAndActive(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId, "preparation", true);
	}

	@Override
	public List<Content> findByRoomIdOnlyPreparationVariant(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId, "preparation");
	}

	@Override
	public List<Content> findByRoomId(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId);
	}

	@Override
	public List<Content> findByRoomIdAndVariantAndActive(final Object... keys) {
		final Object[] endKeys = Arrays.copyOf(keys, keys.length + 1);
		endKeys[keys.length] = ComplexKey.emptyObject();

		return db.queryView(createQuery("by_roomid_group_locked")
						.includeDocs(true)
						.reduce(false)
						.startKey(ComplexKey.of(keys))
						.endKey(ComplexKey.of(endKeys)),
				Content.class);
	}

	@Override
	public int countLectureVariantByRoomId(final String roomId) {
		/* TODO: reduce code duplication */
		final ViewResult result = db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId, "lecture"))
				.endKey(ComplexKey.of(roomId, "lecture", ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int countFlashcardVariantRoomId(final String roomId) {
		/* TODO: reduce code duplication */
		final ViewResult result = db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId, "flashcard"))
				.endKey(ComplexKey.of(roomId, "flashcard", ComplexKey.emptyObject())));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public int countPreparationVariantByRoomId(final String roomId) {
		/* TODO: reduce code duplication */
		final ViewResult result = db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId, "preparation"))
				.endKey(ComplexKey.of(roomId, "preparation", ComplexKey.emptyObject())));

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
			if (!"slide".equals(content.getFormat()) && (!answeredQuestions.containsKey(content.getId())
					|| (answeredQuestions.containsKey(content.getId()) && answeredQuestions.get(content.getId()) != content.getState().getRound()))) {
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
	public List<String> findIdsByRoomIdAndVariantAndSubject(final String roomId, final String questionVariant, final String subject) {
		final ViewResult result = db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId, questionVariant, false, subject))
				.endKey(ComplexKey.of(roomId, questionVariant, false, subject, ComplexKey.emptyObject())));

		final List<String> qids = new ArrayList<>();

		for (final ViewResult.Row row : result.getRows()) {
			final String s = row.getId();
			qids.add(s);
		}

		return qids;
	}

	@Override
	public List<String> findSubjectsByRoomIdAndVariant(final String roomId, final String questionVariant) {
		final ViewResult result = db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId, questionVariant))
				.endKey(ComplexKey.of(roomId, questionVariant, ComplexKey.emptyObject())));

		final Set<String> uniqueSubjects = new HashSet<>();

		for (final ViewResult.Row row : result.getRows()) {
			uniqueSubjects.add(row.getKeyAsNode().get(3).asText());
		}

		return new ArrayList<>(uniqueSubjects);
	}
}
