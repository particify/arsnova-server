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
		final ViewResult result = db.queryView(createQuery("by_roomid_locked")
				.startKey(ComplexKey.of(roomId))
				.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))
				.reduce(true));

		return result.isEmpty() ? 0 : result.getRows().get(0).getValueAsInt();
	}

	@Override
	public List<String> findIdsByRoomId(final String roomId) {
		return collectQuestionIds(db.queryView(createQuery("by_roomid_locked")
				.startKey(ComplexKey.of(roomId))
				.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))
				.reduce(false)));
	}

	@Override
	public Iterable<Content> findStubsByIds(final Set<String> ids) {
		return createEntityStubs(db.queryView(createQuery("by_id")
				.keys(ids)
				.reduce(false)));
	}

	@Override
	public Iterable<Content> findStubsByRoomId(final String roomId) {
		return createEntityStubs(db.queryView(createQuery("by_roomid_locked")
				.startKey(ComplexKey.of(roomId))
				.endKey(ComplexKey.of(roomId, ComplexKey.emptyObject()))
				.reduce(false)));
	}

	protected Iterable<Content> createEntityStubs(final ViewResult viewResult) {
		return super.createEntityStubs(viewResult, Content::setRoomId);
	}

	@Override
	public List<String> findUnansweredIdsByRoomIdAndUser(final String roomId, final String userId) {
		final ViewResult result = db.queryView(createQuery("contentid_by_creatorid_roomid")
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
	public List<Content> findByRoomIdOnlyFlashcardVariantAndActive(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId, "flashcard", true);
	}

	@Override
	public List<Content> findByRoomId(final String roomId) {
		return findByRoomIdAndVariantAndActive(roomId);
	}

	@Override
	public List<Content> findByRoomIdAndVariantAndActive(final Object... keys) {
		final Object[] endKeys = Arrays.copyOf(keys, keys.length + 1);
		endKeys[keys.length] = ComplexKey.emptyObject();

		return db.queryView(createQuery("by_roomid_locked")
						.includeDocs(true)
						.reduce(false)
						.startKey(ComplexKey.of(keys))
						.endKey(ComplexKey.of(endKeys)),
				Content.class);
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
					|| (answeredQuestions.containsKey(content.getId())
					&& answeredQuestions.get(content.getId()) != content.getState().getRound()))) {
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
}
