package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.Answer;
import net.particify.arsnova.core.model.AnswerStatistics;
import net.particify.arsnova.core.model.AnswerStatisticsUserSummary;
import net.particify.arsnova.core.model.ChoiceAnswerStatistics;
import net.particify.arsnova.core.model.NumericAnswerStatistics;
import net.particify.arsnova.core.model.PrioritizationAnswerStatistics;
import net.particify.arsnova.core.model.TextAnswer;
import net.particify.arsnova.core.model.TextAnswerStatistics;

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
  public ChoiceAnswerStatistics getChoiceStatistics(final String contentId, final int round) {
    return answerService.getChoiceStatistics(contentId, round);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read')")
  public ChoiceAnswerStatistics getChoiceStatistics(final String contentId) {
    return answerService.getChoiceStatistics(contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read')")
  public TextAnswerStatistics getTextStatistics(final String contentId, final int round) {
    return answerService.getTextStatistics(contentId, round);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read')")
  public TextAnswerStatistics getTextStatistics(final String contentId) {
    return answerService.getTextStatistics(contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read')")
  public NumericAnswerStatistics getNumericStatistics(final String contentId) {
    return answerService.getNumericStatistics(contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read')")
  public NumericAnswerStatistics getNumericStatistics(final String contentId, final int round) {
    return answerService.getNumericStatistics(contentId, round);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read')")
  public PrioritizationAnswerStatistics getPrioritizationStatistics(final String contentId) {
    return answerService.getPrioritizationStatistics(contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read')")
  public AnswerStatistics getAllStatistics(final String contentId) {
    return answerService.getAllStatistics(contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#userId, 'userprofile', 'owner')")
  public AnswerStatisticsUserSummary getStatisticsByUserIdAndContentIds(
      final String userId, final List<String> contentIds) {
    return answerService.getStatisticsByUserIdAndContentIds(userId, contentIds);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read')")
  public List<String> getAnswerIdsByContentIdNotHidden(final String contentId) {
    return answerService.getAnswerIdsByContentIdNotHidden(contentId);
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

  @Override
  @PreAuthorize("hasPermission(#answer, 'moderate')")
  public void hideTextAnswer(final TextAnswer answer, final boolean hidden) {
    answerService.hideTextAnswer(answer, hidden);
  }
}
