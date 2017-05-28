package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;

import java.util.List;

public interface ContentRepository {
	List<Content> getQuestions(Object... keys);
	Content getQuestion(String id);
	Content saveQuestion(Session session, Content content);
	List<Content> getSkillQuestionsForUsers(Session session);
	List<Content> getSkillQuestionsForTeachers(Session session);
	int getSkillQuestionCount(Session session);
	List<String> getQuestionIds(Session session, User user);
	int deleteQuestionWithAnswers(Content content);
	int[] deleteAllQuestionsWithAnswers(Session session);
	List<String> getUnAnsweredQuestionIds(Session session, User user);
	Content updateQuestion(Content content);
	List<Content> getLectureQuestionsForUsers(Session session);
	List<Content> getLectureQuestionsForTeachers(Session session);
	List<Content> getFlashcardsForUsers(Session session);
	List<Content> getFlashcardsForTeachers(Session session);
	List<Content> getPreparationQuestionsForUsers(Session session);
	List<Content> getPreparationQuestionsForTeachers(Session session);
	List<Content> getAllSkillQuestions(Session session);
	int getLectureQuestionCount(Session session);
	int getFlashcardCount(Session session);
	int getPreparationQuestionCount(Session session);
	void publishQuestions(Session session, boolean publish, List<Content> contents);
	List<Content> publishAllQuestions(Session session, boolean publish);
	List<String> getQuestionIdsBySubject(Session session, String questionVariant, String subject);
	List<Content> getQuestionsByIds(List<String> ids, Session session);
	void resetQuestionsRoundState(Session session, List<Content> contents);
	void setVotingAdmissions(Session session, boolean disableVoting, List<Content> contents);
	List<Content> setVotingAdmissionForAllQuestions(Session session, boolean disableVoting);
	int[] deleteAllLectureQuestionsWithAnswers(Session session);
	int[] deleteAllFlashcardsWithAnswers(Session session);
	int[] deleteAllPreparationQuestionsWithAnswers(Session session);
	List<String> getSubjects(Session session, String questionVariant);
	List<String> getUnAnsweredLectureQuestionIds(Session session, User user);
	List<String> getUnAnsweredPreparationQuestionIds(Session session, User user);
}
