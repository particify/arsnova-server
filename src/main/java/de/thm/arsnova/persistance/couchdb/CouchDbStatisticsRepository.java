package de.thm.arsnova.persistance.couchdb;

import de.thm.arsnova.entities.Statistics;
import de.thm.arsnova.persistance.StatisticsRepository;
import org.ektorp.CouchDbConnector;
import org.ektorp.DbAccessException;
import org.ektorp.ViewResult;
import org.ektorp.support.CouchDbRepositorySupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashSet;
import java.util.Set;

public class CouchDbStatisticsRepository extends CouchDbRepositorySupport implements StatisticsRepository {
	private static final Logger logger = LoggerFactory.getLogger(CouchDbStatisticsRepository.class);

	public CouchDbStatisticsRepository(CouchDbConnector db, boolean createIfNotExists) {
		super(Object.class, db, "statistics", createIfNotExists);
	}

	@Cacheable("statistics")
	@Override
	public Statistics getStatistics() {
		final Statistics stats = new Statistics();
		try {
			final ViewResult statsResult = db.queryView(createQuery("statistics").group(true));
			final ViewResult creatorResult = db.queryView(createQuery("unique_session_creators").group(true));
			final ViewResult studentUserResult = db.queryView(createQuery("active_student_users").group(true));

			if (!statsResult.isEmpty()) {
				for (ViewResult.Row row: statsResult.getRows()) {
					final int value = row.getValueAsInt();
					switch (row.getKey()) {
						case "openSessions":
							stats.setOpenSessions(stats.getOpenSessions() + value);
							break;
						case "closedSessions":
							stats.setClosedSessions(stats.getClosedSessions() + value);
							break;
						case "deletedSessions":
						/* Deleted sessions are not exposed separately for now. */
							stats.setClosedSessions(stats.getClosedSessions() + value);
							break;
						case "answers":
							stats.setAnswers(stats.getAnswers() + value);
							break;
						case "lectureQuestions":
							stats.setLectureQuestions(stats.getLectureQuestions() + value);
							break;
						case "preparationQuestions":
							stats.setPreparationQuestions(stats.getPreparationQuestions() + value);
							break;
						case "interposedQuestions":
							stats.setInterposedQuestions(stats.getInterposedQuestions() + value);
							break;
						case "conceptQuestions":
							stats.setConceptQuestions(stats.getConceptQuestions() + value);
							break;
						case "flashcards":
							stats.setFlashcards(stats.getFlashcards() + value);
							break;
					}
				}
			}
			if (!creatorResult.isEmpty()) {
				Set<String> creators = new HashSet<>();
				for (ViewResult.Row row: statsResult.getRows()) {
					creators.add(row.getKey());
				}
				stats.setCreators(creators.size());
			}
			if (!studentUserResult.isEmpty()) {
				Set<String> students = new HashSet<>();
				for (ViewResult.Row row: statsResult.getRows()) {
					students.add(row.getKey());
				}
				stats.setActiveStudents(students.size());
			}
			return stats;
		} catch (final DbAccessException e) {
			logger.error("Could not retrieve statistics.", e);
		}

		return stats;
	}
}
