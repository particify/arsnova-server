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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.ImageUtils;
import de.thm.arsnova.connector.client.ConnectorClient;
import de.thm.arsnova.connector.model.Course;
import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.domain.ILearningProgressFactory;
import de.thm.arsnova.domain.LearningProgress;
import de.thm.arsnova.entities.LearningProgressOptions;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.SessionFeature;
import de.thm.arsnova.entities.SessionInfo;
import de.thm.arsnova.entities.SortOrder;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.ImportExportSession;
import de.thm.arsnova.entities.transport.LearningProgressValues;
import de.thm.arsnova.events.DeleteSessionEvent;
import de.thm.arsnova.events.NewSessionEvent;
import de.thm.arsnova.events.StatusSessionEvent;
import de.thm.arsnova.exceptions.BadRequestException;
import de.thm.arsnova.exceptions.ForbiddenException;
import de.thm.arsnova.exceptions.NotFoundException;
import de.thm.arsnova.exceptions.PayloadTooLargeException;
import de.thm.arsnova.exceptions.UnauthorizedException;

/**
 * Performs all session related operations.
 */
@Service
public class SessionService implements ISessionService, ApplicationEventPublisherAware {

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
	private ILearningProgressFactory learningProgressFactory;

	@Autowired(required = false)
	private ConnectorClient connectorClient;

	@Autowired
	private ImageUtils imageUtils;

	@Value("${pp.logofilesize_b}")
	private int uploadFileSizeByte;

	private ApplicationEventPublisher publisher;

	public static final Logger LOGGER = LoggerFactory.getLogger(SessionService.class);

	public void setDatabaseDao(final IDatabaseDao newDatabaseDao) {
		databaseDao = newDatabaseDao;
	}

	@Override
	public Session joinSession(final String keyword, final UUID socketId) {
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
	public Session getSession(final String keyword) {
		final User user = userService.getCurrentUser();
		return this.getSessionInternal(keyword, user);
	}

	/*
	 * The "internal" suffix means it is called by internal services that have no authentication!
	 * TODO: Find a better way of doing this...
	 */
	@Override
	public Session getSessionInternal(final String keyword, final User user) {
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
	public List<Session> getMySessions(final int offset, final int limit) {
		return databaseDao.getMySessions(userService.getCurrentUser(), offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<SessionInfo> getPublicPoolSessionsInfo() {
		return databaseDao.getPublicPoolSessionsInfo();
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<SessionInfo> getMyPublicPoolSessionsInfo() {
		return databaseDao.getMyPublicPoolSessionsInfo(userService.getCurrentUser());
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<SessionInfo> getMySessionsInfo(final int offset, final int limit) {
		final User user = userService.getCurrentUser();
		return databaseDao.getMySessionsInfo(user, offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<Session> getMyVisitedSessions(final int offset, final int limit) {
		return databaseDao.getMyVisitedSessions(userService.getCurrentUser(), offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public List<SessionInfo> getMyVisitedSessionsInfo(final int offset, final int limit) {
		return databaseDao.getMyVisitedSessionsInfo(userService.getCurrentUser(), offset, limit);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public Session saveSession(final Session session) {
		if (connectorClient != null && session.getCourseId() != null) {
			if (!connectorClient.getMembership(
					userService.getCurrentUser().getUsername(), session.getCourseId()).isMember()
					) {
				throw new ForbiddenException();
			}
		}
		handleLogo(session);

		// set some default values
		LearningProgressOptions lpo = new LearningProgressOptions();
		lpo.setType("questions");
		session.setLearningProgressOptions(lpo);

		SessionFeature sf = new SessionFeature();
		sf.setFeedback(true);
		sf.setInterposed(true);
		sf.setJitt(true);
		sf.setLearningProgress(true);
		sf.setPi(true);
		session.setFeatures(sf);

		final Session result = databaseDao.saveSession(userService.getCurrentUser(), session);
		this.publisher.publishEvent(new NewSessionEvent(this, result));
		return result;
	}

	@Override
	public boolean sessionKeyAvailable(final String keyword) {
		return databaseDao.sessionKeyAvailable(keyword);
	}

	@Override
	public String generateKeyword() {
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
		this.publisher.publishEvent(new StatusSessionEvent(this, session));
		return databaseDao.updateSession(session);
	}

	@Override
	@PreAuthorize("isAuthenticated() and hasPermission(#session, 'owner')")
	public Session updateSession(final String sessionkey, final Session session) {
		final Session existingSession = databaseDao.getSessionFromKeyword(sessionkey);

		existingSession.setActive(session.isActive());
		existingSession.setShortName(session.getShortName());
		existingSession.setPpAuthorName(session.getPpAuthorName());
		existingSession.setPpAuthorMail(session.getPpAuthorMail());
		existingSession.setShortName(session.getShortName());
		existingSession.setPpAuthorName(session.getPpAuthorName());
		existingSession.setPpFaculty(session.getPpFaculty());
		existingSession.setName(session.getName());
		existingSession.setPpUniversity(session.getPpUniversity());
		existingSession.setPpDescription(session.getPpDescription());
		existingSession.setPpLevel(session.getPpLevel());
		existingSession.setPpLicense(session.getPpLicense());
		existingSession.setPpSubject(session.getPpSubject());
		
		handleLogo(session);
		existingSession.setPpLogo(session.getPpLogo());

		return databaseDao.updateSession(existingSession);
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

		List<String> prepSubjects = databaseDao.getSubjects(session, "preparation");
		if (prepSubjects == null) {
			prepSubjects = new ArrayList<String>();
		} else {
			prepSubjects.add("");
		}

		List<String> lectureSubjects = databaseDao.getSubjects(session, "lecture");
		if (lectureSubjects == null) {
			lectureSubjects = new ArrayList<String>();
		} else {
			lectureSubjects.add("");
		}

		for (String subject : prepSubjects) {
			SortOrder sortOrder = databaseDao.getSortOrder(session.get_id(), "preparation", subject);
			databaseDao.deleteSortOrder(sortOrder);
		}
		for (String subject : lectureSubjects) {
			SortOrder sortOrder = databaseDao.getSortOrder(session.get_id(), "lecture", subject);
			databaseDao.deleteSortOrder(sortOrder);
		}
		databaseDao.deleteAllQuestionsWithAnswers(session);
		databaseDao.deleteSession(session);

		this.publisher.publishEvent(new DeleteSessionEvent(this, session));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public LearningProgressValues getLearningProgress(final String sessionkey, final String progressType, final String questionVariant) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		LearningProgress learningProgress = learningProgressFactory.create(progressType, questionVariant);
		return learningProgress.getCourseProgress(session);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public LearningProgressValues getMyLearningProgress(final String sessionkey, final String progressType, final String questionVariant) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		final User user = userService.getCurrentUser();
		LearningProgress learningProgress = learningProgressFactory.create(progressType, questionVariant);
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

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public SessionFeature getSessionFeatures(String sessionkey) {
		return databaseDao.getSessionFromKeyword(sessionkey).getFeatures();
	}

	@Override
	public SessionFeature changeSessionFeatures(String sessionkey, SessionFeature features) {
		final Session session = databaseDao.getSessionFromKeyword(sessionkey);
		final User user = userService.getCurrentUser();
		if (!session.isCreator(user)) {
			throw new UnauthorizedException();
		}
		session.setFeatures(features);
		return databaseDao.updateSession(session).getFeatures();
	}
	
	/**
	 * 
	 * @param session 
	 * @throws PayloadTooLargeException
	 * @throws BadRequestException
	 */
	private void handleLogo(Session session) {
		if (session.getPpLogo() != null) {
			if (session.getPpLogo().startsWith("http")) {
				final String base64ImageString = imageUtils.encodeImageToString(session.getPpLogo());
				if (base64ImageString == null) {
					throw new BadRequestException();
				}
				session.setPpLogo(base64ImageString);
			}

			// base64 adds offset to filesize, formula taken from: http://en.wikipedia.org/wiki/Base64#MIME
			final int fileSize = (int) ((session.getPpLogo().length() - 814) / 1.37);
			if (fileSize > uploadFileSizeByte) {
				LOGGER.error("Could not save file. File is too large with " + fileSize + " Byte.");
				throw new PayloadTooLargeException();
			}
		}
	}
}
