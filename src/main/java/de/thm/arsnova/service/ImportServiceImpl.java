package de.thm.arsnova.service;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.model.migration.FromV2Migrator;
import de.thm.arsnova.model.transport.ImportExportContainer;
import de.thm.arsnova.security.User;

/**
 * Handles data imports into the system.
 */
@Service
public class ImportServiceImpl implements ImportService {
	private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);

	private RoomService roomService;

	private ContentService contentService;

	private ContentGroupService contentGroupService;

	private AnswerService answerService;

	private MotdService motdService;

	private UserService userService;

	private FromV2Migrator fromV2Migrator;

	private static final String V2_PREPARATION_QUESTION_CONTENT_GROUP_NAME = "preparation";
	private static final String V2_LECTURE_QUESTION_CONTENT_GROUP_NAME = "lecture";

	public ImportServiceImpl(
			final RoomService roomService,
			final ContentService contentService,
			final ContentGroupService contentGroupService,
			final AnswerService answerService,
			final MotdService motdService,
			final UserService userService,
			final FromV2Migrator fromV2Migrator
	) {
		this.roomService = roomService;
		this.contentService = contentService;
		this.contentGroupService = contentGroupService;
		this.answerService = answerService;
		this.motdService = motdService;
		this.userService = userService;
		this.fromV2Migrator = fromV2Migrator;
	}

	@Override
	@Secured({"ROLE_USER", "RUN_AS_SYSTEM"})
	public Room importFromV2(final ImportExportContainer container) {
		final User user = userService.getCurrentUser();
		logger.debug("Starting import for: {}, triggered by: {}", container.getSession().getName(), user);
		final Room room = new Room();

		final ContentGroup preparationContentGroup = new ContentGroup();
		preparationContentGroup.setName(V2_PREPARATION_QUESTION_CONTENT_GROUP_NAME);
		final Set<String> preparationContentGroupIds = preparationContentGroup.getContentIds();

		final ContentGroup lectureContentGroup = new ContentGroup();
		lectureContentGroup.setName(V2_LECTURE_QUESTION_CONTENT_GROUP_NAME);
		final Set<String> lectureContentGroupIds = lectureContentGroup.getContentIds();

		final ImportExportContainer.ImportExportRoom toImport = container.getSession();

		room.setOwnerId(user.getId());
		room.setName(toImport.getName());
		room.setAbbreviation(toImport.getShortName());

		logger.trace("Import room: {}", room);

		final Room savedRoom = roomService.create(room);

		for (final ImportExportContainer.ImportExportContent importExportContent : container.getQuestions()) {
			final Content newContent = fromV2Migrator.migrate(importExportContent);
			logger.trace("Import content: {}", newContent);
			newContent.setRoomId(savedRoom.getId());
			final Content.State contentStage = newContent.getState();
			contentStage.setResponsesEnabled(true);
			final Content savedContent = contentService.create(newContent);

			if (importExportContent.getQuestionVariant().equals("preparation")) {
				preparationContentGroupIds.add(savedContent.getId());
			} else if (importExportContent.getQuestionVariant().equals("lecture")) {
				lectureContentGroupIds.add(savedContent.getId());
			}

			for (final de.thm.arsnova.model.migration.v2.Answer v2Answer : importExportContent.getAnswers()) {
				logger.trace("Answer model v2 to import: {}", v2Answer);
				final UserProfile newAnonUser = userService.createAnonymizedGuestUser();
				if (v2Answer.getAnswerText() == null || v2Answer.getAnswerText().isEmpty()) {
					v2Answer.setAbstention(true);
				}
				final Answer newAnswer = fromV2Migrator.migrate(v2Answer, savedContent);
				newAnswer.setRoomId(savedRoom.getId());
				newAnswer.setContentId(savedContent.getId());
				newAnswer.setCreatorId(newAnonUser.getId());
				logger.trace("Answer model v3 to import: {}", newAnswer);
				answerService.create(newAnswer);
			}
		}

		if (!preparationContentGroupIds.isEmpty()) {
			preparationContentGroup.setRoomId(savedRoom.getId());
			logger.trace("New content group resulting from import: {}", preparationContentGroup);
			contentGroupService.create(preparationContentGroup);
		}
		if (!lectureContentGroupIds.isEmpty()) {
			lectureContentGroup.setRoomId(savedRoom.getId());
			logger.trace("New content group resulting from import: {}", lectureContentGroup);
			contentGroupService.create(lectureContentGroup);
		}

		logger.debug("Import finished for: {}", container.getSession().getName());
		return savedRoom;
	}
}
