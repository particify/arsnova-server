package de.thm.arsnova.repositories;

import java.util.List;

import org.springframework.data.couchbase.core.view.Query;
import org.springframework.data.couchbase.repository.CouchbaseRepository;

import de.thm.arsnova.entities.InterposedQuestion;

/**
 * Handles all database operations on interposed questions.
 */
public interface InterposedQuestionRepository extends CouchbaseRepository<InterposedQuestion, String>,
		InterposedQuestionRepositoryCustom {

	@Query("$SELECT_ENTITY$ WHERE sessionId = $1 AND $FILTER_TYPE$")
	public List<InterposedQuestion> findBySession(String sessionId);

	@Query("$SELECT_ENTITY$ WHERE sessionId = $1 AND creator = $2 AND $FILTER_TYPE$")
	public List<InterposedQuestion> findBySessionAndCreator(String sessionId, String creator);

}
