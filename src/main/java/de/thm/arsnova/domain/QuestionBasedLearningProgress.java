/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2021 The ARSnova Team and Contributors
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
package de.thm.arsnova.domain;

import de.thm.arsnova.dao.IDatabaseDao;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.entities.transport.LearningProgressValues;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calculates learning progress based on correctness of an answer.
 */
public class QuestionBasedLearningProgress implements LearningProgress {

	private final IDatabaseDao databaseDao;

	private String questionVariant = "lecture";

	public QuestionBasedLearningProgress(IDatabaseDao dao) {
		this.databaseDao = dao;
	}

	@Override
	public LearningProgressValues getCourseProgress(Session session) {
		List<Question> eligibleQuestions = getQuestionsForVariant(session, questionVariant);
		return createCourseProgress(eligibleQuestions);
	}

	@Override
	public LearningProgressValues getMyProgress(Session session, User user) {
		List<Question> eligibleQuestions = getQuestionsForVariant(session, questionVariant);
		return createMyProgress(eligibleQuestions, user);
	}

	public void setQuestionVariant(String questionVariant) {
		this.questionVariant = questionVariant;
	}

	private LearningProgressValues createMyProgress(List<Question> questions, User user) {
		int numMyCorrectAnswers = 0;
		int numAllCorrectAnswers = 0;
		List<Answer> allAnswers = new ArrayList<>();
		for (Question q : new ArrayList<>(questions)) {
			List<Answer> answers = databaseDao.getAnswerTextAndUser(q, q.getPiRound());
			if (!q.isActive() && answers.size() == 0) {
				// Ignore this question: It's not active and does not have any answers, so it's irrelevant.
				questions.remove(q);
			}
			allAnswers.addAll(answers);
			List<Answer> myAnswers = answers.stream().filter(a -> a.getUser().equals(user.getUsername())).collect(Collectors.toList());
			numMyCorrectAnswers += countCorrectAnswers(q, myAnswers);
			numAllCorrectAnswers += countCorrectAnswers(q, answers);
		}
		int numUsers = countUsers(allAnswers);
		LearningProgressValues lpv = new LearningProgressValues();
		lpv.setNumerator(numMyCorrectAnswers);
		lpv.setDenominator(questions.size());
		lpv.setNumQuestions(questions.size());
		lpv.setNumUsers(numUsers);
		lpv.setMyProgress(myPercentage(numMyCorrectAnswers, questions.size()));
		if (numUsers != 0 && questions.size() != 0) {
			lpv.setCourseProgress((int)(100 * (numAllCorrectAnswers / ((double)(numUsers * questions.size())))));
		} else {
			lpv.setCourseProgress(0);
		}
		return lpv;
	}

	private List<Question> getQuestionsForVariant(Session session, String questionVariant) {
		List<Question> eligibleQuestions;
		if (questionVariant == null || questionVariant.isEmpty()) {
			List<Question> questions = databaseDao.getLectureQuestionsForTeachers(session);
			questions.addAll(databaseDao.getPreparationQuestionsForTeachers(session));
			eligibleQuestions = filterIrrelevantQuestions(questions);
		} else if (questionVariant.equals("lecture")) {
			eligibleQuestions = filterIrrelevantQuestions(databaseDao.getLectureQuestionsForTeachers(session));
		} else if (questionVariant.equals("preparation")) {
			eligibleQuestions = filterIrrelevantQuestions(databaseDao.getPreparationQuestionsForTeachers(session));
		} else {
			throw new RuntimeException("Unknown question variant: " + questionVariant);
		}
		return eligibleQuestions;
	}

	private LearningProgressValues createCourseProgress(List<Question> questions) {
		int numCorrectAnswers = 0;
		List<Answer> allAnswers = new ArrayList<>();
		for (Question q : new ArrayList<>(questions)) {
			List<Answer> answers = databaseDao.getAnswerTextAndUser(q, q.getPiRound());
			if (!q.isActive() && answers.size() == 0) {
				// Ignore this question: It's not active and does not have any answers, so it's irrelevant.
				questions.remove(q);
			}
			allAnswers.addAll(answers);
			numCorrectAnswers += countCorrectAnswers(q, answers);
		}
		int courseProgress = 0;
		int numUsers = countUsers(allAnswers);
		if (numUsers != 0 && questions.size() != 0) {
			courseProgress = (int)(100 * (numCorrectAnswers / ((double)(numUsers * questions.size()))));
		}
		int numerator = 0;
		if (numUsers != 0) {
			numerator = numCorrectAnswers / numUsers;
		}
		final int denominator = questions.size();
		LearningProgressValues lpv = new LearningProgressValues();
		lpv.setNumerator(numerator);
		lpv.setDenominator(denominator);
		lpv.setNumQuestions(questions.size());
		lpv.setNumUsers(numUsers);
		lpv.setCourseProgress(courseProgress);
		return lpv;
	}

	private int countUsers(List<Answer> answers) {
		Set<String> usernames = new HashSet<>();
		for (Answer a : answers) {
			usernames.add(a.getUser());
		}
		return usernames.size();
	}

	private int countCorrectAnswers(Question q, List<Answer> answers) {
		return (int) answers.stream().filter(a -> a.isCorrect(q)).count();
	}

	private List<Question> filterIrrelevantQuestions(List<Question> questions) {
		return questions.stream()
				.filter(q -> !q.isNoCorrect())
				.filter(q -> q.getPossibleAnswers() != null && !q.getPossibleAnswers().isEmpty())
				.collect(Collectors.toList());
	}

	private int myPercentage(int numQuestionsCorrect, int questionCount) {
		final double myLearningProgress = numQuestionsCorrect / (double) questionCount;
		return (int) Math.min(100, Math.round(myLearningProgress * 100));
	}


}
