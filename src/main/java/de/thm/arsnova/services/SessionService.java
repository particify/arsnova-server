/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
package de.thm.arsnova.services;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.thm.arsnova.ImageUtils;
import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.domain.ILearningProgressFactory;
import de.thm.arsnova.domain.LearningProgress;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.RequestEntityTooLargeException;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Service
public class SessionService implements ISessionService {

	public static class SessionNameComparator implements Comparator<Session>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Session session1, final Session session2) {
			return session1.getName().compareToIgnoreCase(session2.getName());
		}
	}

	public static class SessionInfoNameComparator implements Comparator<SessionInfo>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final SessionInfo session1, final SessionInfo session2) {
			return session1.getName().compareToIgnoreCase(session2.getName());
		}
	}

	public static class SessionShortNameComparator implements Comparator<Session>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final Session session1, final Session session2) {
			return session1.getShortName().compareToIgnoreCase(session2.getShortName());
		}
	}

	public static class SessionInfoShortNameComparator implements Comparator<SessionInfo>, Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public int compare(final SessionInfo session1, final SessionInfo session2) {
			return session1.getShortName().compareToIgnoreCase(session2.getShortName());
		}
	}

	@Autowired
	private IDatabaseDao databaseDao;

	@Autowired
	private IUserService userService;

	@Autowired
	private ARSnovaSocketIOServer socketIoServer;

	@Autowired
	private ILearningProgressFactory learningProgressFactory;

	@Autowired(required = false)
	private ConnectorClient connectorClient;

	@Value("${pp.logofilesize_b}")
	private int uploadFileSizeByte;

	public static final Logger LOGGER = LoggerFactory.getLogger(SessionService.class);

	public void setDatabaseDao(final IDatabaseDao newDatabaseDao) {
		databaseDao = newDatabaseDao;
	}

	@Override
	public final Session joinSession(final String keyword, final UUID socketId) {
		/* Socket.IO solution */

		Session session = null != keyword ? databaseDao.getSessionFromKeyword(keyword) : null;

		if (null == session) {
			userService.removeUserFromSessionBySocketId(socketId);
			return null;
		}
		final User user = userService.getUser2SocketId(socketId);

		userService.addUserToSessionBySocketId(socketId, keyword);

		if (session.getCreator().equals(user.getUsername())) {
			databaseDao.updateSessionOwnerActivity(session);
		}
		databaseDao.registerAsOnlineUser(user, session);

		if (connectorClient != null && session.isCourseSession()) {
			final String courseid = session.getCourseId();
			if (!connectorClient.getMembership(user.getUsername(), courseid).isMember()) {
				throw new ForbiddenException();
			}
		}

		return session;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public final Session getSession(final String keyword) {
		final User user = userService.getCurrentUser();
		return this.getSessionInternal(keyword, user);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public final Session getSessionInternal(final String keyword, final User user) {
		final Session session = databaseDao.getSessionFromKeyword(keyword);
		if (session == null) {
			throw new NotFoundException();
		}
		if (!session.isActive()) {
			if (user.hasRole(UserSessionService.Role.STUDENT)) {
				throw new ForbiddenException();
			} else if (user.hasRole(UserSessionService.Role.SPEAKER) && !session.isCreator(user)) {
				throw new ForbiddenException();
			}
		}
		if (connectorClient != null && session.isCourseSession()) {
			final String courseid = session.getCourseId();
			if (!connectorClient.getMembership(userService.getCurrentUser().getUsername(), courseid).isMember()) {
				throw new ForbiddenException();
			}
		}
		return session;
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public final List<Session> getMySessions() {
		return databaseDao.getMySessions(userService.getCurrentUser());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public final List<SessionInfo> getPublicPoolSessionsInfo() {
		return databaseDao.getPublicPoolSessionsInfo();
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public final List<SessionInfo> getMyPublicPoolSessionsInfo() {
		return databaseDao.getMyPublicPoolSessionsInfo(userService.getCurrentUser());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public final List<SessionInfo> getMySessionsInfo() {
		final User user = userService.getCurrentUser();
		return databaseDao.getMySessionsInfo(user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public final List<Session> getMyVisitedSessions() {
		return databaseDao.getMyVisitedSessions(userService.getCurrentUser());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public final List<SessionInfo> getMyVisitedSessionsInfo() {
		return databaseDao.getMyVisitedSessionsInfo(userService.getCurrentUser());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public final Session saveSession(final Session session) {
		if (connectorClient != null && session.getCourseId() != null) {
			if (!connectorClient.getMembership(
					userService.getCurrentUser().getUsername(), session.getCourseId()).isMember()
					) {
				throw new ForbiddenException();
			}
		}
		if (session.getPpLogo() != null) {
			if (session.getPpLogo().startsWith("http")) {
				final String base64ImageString = ImageUtils.encodeImageToString(session.getPpLogo());
				if (base64ImageString == null) {
					throw new BadRequestException();
				}
				session.setPpLogo(base64ImageString);
			}
			// base64 adds offset to filesize, formula taken from: http://en.wikipedia.org/wiki/Base64#MIME
			final int fileSize = (int) ((session.getPpLogo().length() - 814) / 1.37);
			if (fileSize > uploadFileSizeByte) {
				LOGGER.error("Could not save file. File is too large with " + fileSize + " Byte.");
				throw new RequestEntityTooLargeException();
			}
		}

		return databaseDao.saveSession(userService.getCurrentUser(), session);
	}

	@Override
	public final boolean sessionKeyAvailable(final String keyword) {
		return databaseDao.sessionKeyAvailable(keyword);
	}

	@Override
	public final String generateKeyword() {
		final int low = 10000000;
		final int high = 100000000;
		final String keyword = String
				.valueOf((int) (Math.random() * (high - low) + low));

		if (sessionKeyAvailable(keyword)) {
			return keyword;
		}
		return generateKeyword();
	}

	@Override
	public int countSessions(final List<Course> courses) {
		final List<Session> sessions = databaseDao.getCourseSessions(courses);
		if (sessions == null) {
			return 0;
		}
		return sessions.size();
	}

	@Override
	public int activeUsers(final String sessionkey) {
		return userService.getUsersInSession(sessionkey).size();
	}

	@Override
	public Session setActive(final String sessionkey, final Boolean lock) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		final User user = userService.getCurrentUser();
		if (!session.isCreator(user)) {
			throw new ForbiddenException();
		}
		session.setActive(lock);
		socketIoServer.reportSessionStatus(sessionkey, lock);
		return databaseDao.updateSession(session);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#session, 'owner')")
	public Session updateSession(final String sessionkey, final Session session) {
		return databaseDao.updateSession(session);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public Session updateSessionInternal(final Session session, final User user) {
		if (session.isCreator(user)) {
			return databaseDao.updateSession(session);
		}
		return null;
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#sessionkey, 'session', 'owner')")
	public void deleteSession(final String sessionkey) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		for (final Question q : databaseDao.getSkillQuestions(userService.getCurrentUser(), session)) {
			databaseDao.deleteQuestionWithAnswers(q);
		}
		databaseDao.deleteSession(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public int getLearningProgress(final String sessionkey, final String progressType) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		LearningProgress learningProgress = learningProgressFactory.createFromType(progressType);
		return learningProgress.getCourseProgress(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public SimpleEntry<Integer, Integer> getMyLearningProgress(final String sessionkey, final String progressType) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		final User user = userService.getCurrentUser();
		LearningProgress learningProgress = learningProgressFactory.createFromType(progressType);
		return learningProgress.getMyProgress(session, user);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public SessionInfo importSession(ImportExportSession importSession) {
		final User user = userService.getCurrentUser();
		final SessionInfo info = databaseDao.importSession(user, importSession);
		if (info == null) {
			throw new RuntimeException("Error while importing the session.");
		}
		return info;
	}
}
