package de.thm.arsnova.entities.transport;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ThumbnailRequest {

	private String questionId;
	private List<String> answerIds;
	
	public String getQuestionId() {
		return questionId;
	}
	
	public List<String> getAnswerIds() {
		return answerIds;
	}
	
	public void setQuestionId(String questionId) {
		this.questionId = questionId;
	}
	
	public void setAnswerIds(List<String> answerIds) {
		this.answerIds = answerIds;
	}
	
}
