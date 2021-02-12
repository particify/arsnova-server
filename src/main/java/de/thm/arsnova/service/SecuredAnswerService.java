package de.thm.arsnova.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.AnswerStatistics;

@Service
public class SecuredAnswerService extends AbstractSecuredEntityServiceImpl<Answer>
		implements AnswerService, SecuredService {
	private final AnswerService answerService;

	SecuredAnswerService(final AnswerService answerService) {
		super(Answer.class, answerService);
		this.answerService = answerService;
	}

	@Override
	@PreAuthorize("isAuthenticated")
	public Answer getMyAnswer(final String contentId) {
		return answerService.getMyAnswer(contentId);
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'read')")
	public AnswerStatistics getStatistics(final String contentId, final int round) {
		return answerService.getStatistics(contentId, round);
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'read')")
	public AnswerStatistics getStatistics(final String contentId) {
		return answerService.getStatistics(contentId);
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'read')")
	public AnswerStatistics getAllStatistics(final String contentId) {
		return answerService.getAllStatistics(contentId);
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'read')")
	public List<String> getAnswerIdsByContentId(final String contentId) {
		return answerService.getAnswerIdsByContentId(contentId);
	}

	@Override
	@PreAuthorize("hasPermission(#creatorId, 'userprofile', 'owner')")
	public List<String> getAnswerIdsByCreatorIdRoomId(final String creatorId, final String roomId) {
		return answerService.getAnswerIdsByCreatorIdRoomId(creatorId, roomId);
	}

	@Override
	@PreAuthorize("hasPermission(#creatorId, 'userprofile', 'owner')")
	public List<String> getAnswerIdsByCreatorIdContentIdsRound(
			final String creatorId,
			final List<String> contentIds, final int round) {
		return answerService.getAnswerIdsByCreatorIdContentIdsRound(creatorId, contentIds, round);
	}

	@Override
	@PreAuthorize("hasPermission(#userId, 'userprofile', 'owner')")
	public Answer getAnswerByContentIdAndUserIdAndCurrentRound(
			final String contentId,
			final String userId) {
		return answerService.getAnswerByContentIdAndUserIdAndCurrentRound(contentId, userId);
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'owner')")
	public void deleteAnswers(final String contentId) {
		answerService.deleteAnswers(contentId);
	}

	@Override
	@PreAuthorize("hasPermission(#answer, 'create')")
	public Answer create(final Answer answer) {
		return answerService.create(answer);
	}
}
