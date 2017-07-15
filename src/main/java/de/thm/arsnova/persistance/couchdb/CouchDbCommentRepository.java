package de.thm.arsnova.persistance.couchdb;

import com.fasterxml.jackson.databind.JsonNode;
import de.thm.arsnova.entities.Comment;
import de.thm.arsnova.entities.CommentReadingCount;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.persistance.CommentRepository;
import de.thm.arsnova.persistance.LogEntryRepository;
import de.thm.arsnova.persistance.SessionRepository;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class CouchDbCommentRepository extends CouchDbCrudRepository<Comment> implements CommentRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbCommentRepository.class);

	@Autowired
	private LogEntryRepository dbLogger;

	@Autowired
	private SessionRepository sessionRepository;

	public CouchDbCommentRepository(final CouchDbConnector db, final boolean createIfNotExists) {
		super(Comment.class, db, "by_sessionid", createIfNotExists);
	}

	@Override
	public int getInterposedCount(final String sessionKey) {
		final Session s = sessionRepository.getSessionFromKeyword(sessionKey);
		if (s == null) {
			throw new NotFoundException();
		}

		final ViewResult result = db.queryView(createQuery("by_sessionid").key(s.getId()).group(true));
		if (result.isEmpty()) {
			return 0;
		}

		return result.getRows().get(0).getValueAsInt();
	}

	@Override
	public CommentReadingCount getInterposedReadingCount(final String sessionId) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_read")
				.startKey(ComplexKey.of(sessionId))
				.endKey(ComplexKey.of(sessionId, ComplexKey.emptyObject()))
				.group(true));
		return getInterposedReadingCount(result);
	}

	@Override
	public CommentReadingCount getInterposedReadingCount(final String sessionId, final User user) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_creator_read")
				.startKey(ComplexKey.of(sessionId, user.getUsername()))
				.endKey(ComplexKey.of(sessionId, user.getUsername(), ComplexKey.emptyObject()))
				.group(true));
		return getInterposedReadingCount(result);
	}

	private CommentReadingCount getInterposedReadingCount(final ViewResult viewResult) {
		if (viewResult.isEmpty()) {
			return new CommentReadingCount();
		}
		// A complete result looks like this. Note that the second row is optional, and that the first one may be
		// 'unread' or 'read', i.e., results may be switched around or only one result may be present.
		// count = {"rows":[
		// {"key":["cecebabb21b096e592d81f9c1322b877","Guestc9350cf4a3","read"],"value":1},
		// {"key":["cecebabb21b096e592d81f9c1322b877","Guestc9350cf4a3","unread"],"value":1}
		// ]}
		int read = 0, unread = 0;
		boolean isRead = false;
		final ViewResult.Row fst = viewResult.getRows().get(0);
		final ViewResult.Row snd = viewResult.getRows().size() > 1 ? viewResult.getRows().get(1) : null;

		final JsonNode fstkey = fst.getKeyAsNode();
		if (fstkey.size() == 2) {
			isRead = fstkey.get(1).asBoolean();
		} else if (fstkey.size() == 3) {
			isRead = fstkey.get(2).asBoolean();
		}
		if (isRead) {
			read = fst.getValueAsInt();
		} else {
			unread = fst.getValueAsInt();
		}

		if (snd != null) {
			final JsonNode sndkey = snd.getKeyAsNode();
			if (sndkey.size() == 2) {
				isRead = sndkey.get(1).asBoolean();
			} else {
				isRead = sndkey.get(2).asBoolean();
			}
			if (isRead) {
				read = snd.getValueAsInt();
			} else {
				unread = snd.getValueAsInt();
			}
		}
		return new CommentReadingCount(read, unread);
	}

	@Override
	public List<Comment> getInterposedQuestions(final String sessionId, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		final List<Comment> comments = db.queryView(createQuery("by_sessionid_timestamp")
						.skip(qSkip)
						.limit(qLimit)
						.descending(true)
						.startKey(ComplexKey.of(sessionId, ComplexKey.emptyObject()))
						.endKey(ComplexKey.of(sessionId))
						.includeDocs(true),
				Comment.class);
//		for (Comment comment : comments) {
//			comment.setSessionId(session.getKeyword());
//		}

		return comments;
	}

	@Override
	public List<Comment> getInterposedQuestions(final String sessionId, final User user, final int start, final int limit) {
		final int qSkip = start > 0 ? start : -1;
		final int qLimit = limit > 0 ? limit : -1;

		final List<Comment> comments = db.queryView(createQuery("by_sessionid_creator_timestamp")
						.skip(qSkip)
						.limit(qLimit)
						.descending(true)
						.startKey(ComplexKey.of(sessionId, user.getUsername(), ComplexKey.emptyObject()))
						.endKey(ComplexKey.of(sessionId, user.getUsername()))
						.includeDocs(true),
				Comment.class);
//		for (Comment comment : comments) {
//			comment.setSessionId(session.getKeyword());
//		}

		return comments;
	}

	@Override
	public Comment getInterposedQuestion(final String commentId) {
		try {
			final Comment comment = get(commentId);
			/* TODO: Refactor code so the next line can be removed */
			//comment.setSessionId(sessionRepository.getSessionFromKeyword(comment.getSessionId()).getId());
			return comment;
		} catch (final DocumentNotFoundException e) {
			logger.error("Could not load comment {}.", commentId, e);
		}
		return null;
	}

	@Override
	public Comment saveQuestion(final String sessionId, final Comment comment, final User user) {
		/* TODO: This should be done on the service level. */
		comment.setSessionId(sessionId);
		comment.setCreator(user.getUsername());
		comment.setRead(false);
		if (comment.getTimestamp() == 0) {
			comment.setTimestamp(System.currentTimeMillis());
		}
		try {
			db.create(comment);

			return comment;
		} catch (final IllegalArgumentException e) {
			logger.error("Could not save comment {}.", comment, e);
		}

		return null;
	}

	@Override
	public void markInterposedQuestionAsRead(final Comment comment) {
		try {
			comment.setRead(true);
			db.update(comment);
		} catch (final UpdateConflictException e) {
			logger.error("Could not mark comment as read {}.", comment.getId(), e);
		}
	}

	@Override
	public void deleteInterposedQuestion(final Comment comment) {
		try {
			db.delete(comment.getId(), comment.getRevision());
			dbLogger.log("delete", "type", "comment");
		} catch (final UpdateConflictException e) {
			logger.error("Could not delete comment {}.", comment.getId(), e);
		}
	}

	@Override
	public int deleteAllInterposedQuestions(final String sessionId) {
		final ViewResult result = db.queryView(createQuery("by_sessionid").key(sessionId));

		return deleteAllInterposedQuestions(sessionId, result);
	}

	@Override
	public int deleteAllInterposedQuestions(final String sessionId, final User user) {
		final ViewResult result = db.queryView(createQuery("by_sessionid_creator_read")
				.startKey(ComplexKey.of(sessionId, user.getUsername()))
				.endKey(ComplexKey.of(sessionId, user.getUsername(), ComplexKey.emptyObject())));

		return deleteAllInterposedQuestions(sessionId, result);
	}

	private int deleteAllInterposedQuestions(final String sessionId, final ViewResult comments) {
		if (comments.isEmpty()) {
			return 0;
		}
		/* TODO: use bulk delete */
		for (final ViewResult.Row row : comments.getRows()) {
			try {
				db.delete(row.getId(), row.getValueAsNode().get("rev").asText());
			} catch (final UpdateConflictException e) {
				logger.error("Could not delete all comments {}.", sessionId, e);
			}
		}

		/* This does account for failed deletions */
		dbLogger.log("delete", "type", "comment", "commentCount", comments.getSize());

		return comments.getSize();
	}
}
