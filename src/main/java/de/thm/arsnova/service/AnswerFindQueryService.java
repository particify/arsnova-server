package de.thm.arsnova.service;

import java.util.HashSet;
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
		}

		return ids;
	}
}
