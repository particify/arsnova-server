package de.thm.arsnova.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Answer;
import de.thm.arsnova.model.FindQuery;

@Service
public class AnswerFindQueryService implements FindQueryService<Answer> {
  private AnswerService answerService;

  public AnswerFindQueryService(final AnswerService answerService) {
    this.answerService = answerService;
  }

  @Override
  public Set<String> resolveQuery(final FindQuery<Answer> findQuery) {
    final Set<String> ids = new HashSet<>();
    final Answer properties = findQuery.getProperties();

    if ((properties.getContentId() != null) && (properties.getCreatorId() != null)) {
      final Answer answer = answerService.getAnswerByContentIdAndUserIdAndCurrentRound(
          properties.getContentId(),
          properties.getCreatorId());
      if (answer != null) {
        ids.add(answer.getId());
      }
    } else if (properties.getContentId() != null) {
      ids.addAll(answerService.getAnswerIdsByContentIdNotHidden(properties.getContentId()));
    } else if (properties.getCreatorId() != null) {
      if (findQuery.getExternalFilters().get("contentIds") instanceof List) {
        ids.addAll(answerService.getAnswerIdsByCreatorIdContentIdsRound(
            properties.getCreatorId(),
            (List) findQuery.getExternalFilters().get("contentIds"),
            properties.getRound()));
      } else if (properties.getRoomId() != null) {
        ids.addAll(answerService.getAnswerIdsByCreatorIdRoomId(
            properties.getCreatorId(),
            properties.getRoomId()));
      }
    }

    return ids;
  }
}
