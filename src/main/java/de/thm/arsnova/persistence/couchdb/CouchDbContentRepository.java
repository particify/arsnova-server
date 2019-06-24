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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.persistence.ContentRepository;
import de.thm.arsnova.persistence.LogEntryRepository;

public class CouchDbContentRepository extends CouchDbCrudRepository<Content> implements ContentRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbContentRepository.class);

	@Autowired
	private LogEntryRepository dbLogger;

	public CouchDbContentRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Content.class, db, "by_id", createIfNotExists);
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
				.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))
				.reduce(false)));
	}

	@Override
	public Iterable<Content> findStubsByRoomId(final String roomId) {
		return createEntityStubs(db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId))
				.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))
				.reduce(false)));
	}

	@Override
	public Iterable<Content> findStubsByRoomIdAndVariant(final String roomId, final String variant) {
		return createEntityStubs(db.queryView(createQuery("by_roomid_group_locked")
				.startKey(ComplexKey.of(roomId, variant))
				.endKey(ComplexKey.of(roomId, variant, ComplexKey.emptyObject()))
				.reduce(false)));
	}

	protected Iterable<Content> createEntityStubs(final ViewResult viewResult) {
		return super.createEntityStubs(viewResult, Content::setRoomId);
	}

	@Override
	public List<String> findUnansweredIdsByRoomIdAndUser(final String roomId, final String userId) {
		final ViewResult result = db.queryView(createQuery("contentid_by_creatorid_roomid_variant")
				.designDocId("_design/Answer")
				.startKey(ComplexKey.of(userId, roomId))
				.endKey(ComplexKey.of(userId, roomId, ComplexKey.emptyObject())));
		final List<String> answeredIds = new ArrayList<>();
		for (final ViewResult.Row row : result.getRows()) {
			answeredIds.add(row.getId());
		}
		return collectUnansweredQuestionIds(findIdsByRoomId(roomId), answeredIds);
	}

	@Override
	public List<String> findUnansweredIdsByRoomIdAndUserOnlyLectureVariant(final String roomId, final String userId) {
		final ViewResult result = db.queryView(createQuery("contentid_round_by_creatorid_roomid_variant")
				.designDocId("_design/Answer")
				.key(ComplexKey.of(userId, roomId, "lecture")));
		final Map<String, Integer> answeredQuestions = new HashMap<>();
		for (final ViewResult.Row row : result.getRows()) {
			answeredQuestions.put(row.getId(), row.getKeyAsNode().get(2).asInt());
		}

		return collectUnansweredQuestionIdsByPiRound(findByRoomIdOnlyLectureVariantAndActive(roomId), answeredQuestions);
	}

	@Override
	public List<String> findUnansweredIdsByRoomIdAndUserOnlyPreparationVariant(final String roomId, final String userId) {
		final ViewResult result = db.queryView(createQuery("contentid_round_by_creatorid_roomid_variant")
				.designDocId("_design/Answer")
				.key(ComplexKey.of(userId, roomId, "preparation")));
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
			final List<String> answeredContentIds) {
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
			final Map<String, Integer> answeredQuestions) {
		final List<String> unanswered = new ArrayList<>();

		for (final Content content : contents) {
			// TODO: Set correct format for slides, which currently aren't implemented
			if (Content.Format.TEXT != content.getFormat() && (!answeredQuestions.containsKey(content.getId())
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
