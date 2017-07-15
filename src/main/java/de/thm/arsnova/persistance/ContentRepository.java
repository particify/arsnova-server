package de.thm.arsnova.persistance;

import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ContentRepository extends CrudRepository<Content, String> {
	List<Content> getQuestions(Object... keys);
	Content getQuestion(String id);
	Content saveQuestion(String sessionId, Content content);
	List<Content> getSkillQuestionsForUsers(String sessionId);
	List<Content> getSkillQuestionsForTeachers(String sessionId);
	int getSkillQuestionCount(String sessionId);
	List<String> getQuestionIds(String sessionId, User user);
	int deleteQuestionWithAnswers(String contentId);
	int[] deleteAllQuestionsWithAnswers(String sessionId);
	List<String> getUnAnsweredQuestionIds(String sessionId, User user);
	Content updateQuestion(Content content);
	List<Content> getLectureQuestionsForUsers(String sessionId);
	List<Content> getLectureQuestionsForTeachers(String sessionId);
	List<Content> getFlashcardsForUsers(String sessionId);
	List<Content> getFlashcardsForTeachers(String sessionId);
	List<Content> getPreparationQuestionsForUsers(String sessionId);
	List<Content> getPreparationQuestionsForTeachers(String sessionId);
	List<Content> getAllSkillQuestions(String sessionId);
	int getLectureQuestionCount(String sessionId);
	int getFlashcardCount(String sessionId);
	int getPreparationQuestionCount(String sessionId);
	void publishQuestions(String sessionId, boolean publish, List<Content> contents);
	List<Content> publishAllQuestions(String sessionId, boolean publish);
	List<String> getQuestionIdsBySubject(String sessionId, String questionVariant, String subject);
	List<Content> getQuestionsByIds(List<String> ids);
	void resetQuestionsRoundState(String sessionId, List<Content> contents);
	void setVotingAdmissions(String sessionId, boolean disableVoting, List<Content> contents);
	List<Content> setVotingAdmissionForAllQuestions(String sessionId, boolean disableVoting);
	int[] deleteAllLectureQuestionsWithAnswers(String sessionId);
	int[] deleteAllFlashcardsWithAnswers(String sessionId);
	int[] deleteAllPreparationQuestionsWithAnswers(String sessionId);
	List<String> getSubjects(String sessionId, String questionVariant);
	List<String> getUnAnsweredLectureQuestionIds(String sessionId, User user);
	List<String> getUnAnsweredPreparationQuestionIds(String sessionId, User user);
}
