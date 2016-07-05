package de.thm.arsnova.repositories;

import java.util.List;

import de.thm.arsnova.entities.InterposedQuestion;
import de.thm.arsnova.entities.InterposedReadingCount;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

public class StubInterposedQuestionRepository implements InterposedQuestionRepository {

	public InterposedQuestion interposedQuestion;

	@Override
	public long count() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void delete(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(InterposedQuestion arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Iterable<? extends InterposedQuestion> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAll() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean exists(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterable<InterposedQuestion> findAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterable<InterposedQuestion> findAll(Iterable<String> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InterposedQuestion findOne(String arg0) {
		return this.interposedQuestion;
	}

	@Override
	public <S extends InterposedQuestion> S save(S arg0) {
		return arg0;
	}

	@Override
	public <S extends InterposedQuestion> Iterable<S> save(Iterable<S> arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InterposedReadingCount countReadingBySessionAndCreator(
			Session session, User creator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InterposedReadingCount countReadingBySession(Session session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<InterposedQuestion> findBySession(String sessionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<InterposedQuestion> findBySessionAndCreator(String sessionId,
			String creator) {
		// TODO Auto-generated method stub
		return null;
	}

}
