package de.thm.arsnova.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.thm.arsnova.dao.IDatabaseDao;

@Component
public class LearningProgressFactory {

	@Autowired
	private IDatabaseDao databaseDao;

	public LearningProgress createFromType(String progressType) {
		if (progressType.equals("questions")) {
			return new QuestionBasedLearningProgress(databaseDao);
		} else {
			return new PointBasedLearningProgress(databaseDao);
		}
	}

}
