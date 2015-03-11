package de.thm.arsnova.entities.transport;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ThumbnailResponse {

	private String questionId;
	private List<ThumbnailResponseEntry> entries;
	
	public String getQuestionId() {
		return questionId;
	}
	
	public void setQuestionId(String questionId) {
		this.questionId = questionId;
	}
	
	public List<ThumbnailResponseEntry> getEntries() {
		return entries;
	}
	
	@JsonIgnore
	public void addThumbnailEntry(String answerId, String answerThumbnailImage) {
		if (entries == null) {
			entries = new ArrayList<>();
		}
		entries.add(new ThumbnailResponseEntry(answerId, answerThumbnailImage));
	}

	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	static class ThumbnailResponseEntry {
		
		private String answerId;
		private String answerThumbnailImage;
		
		public ThumbnailResponseEntry(String answerId, String answerThumbnailImage) {
			this.answerId = answerId;
			this.answerThumbnailImage = answerThumbnailImage;
		}
		
		public String getAnswerId() {
			return answerId;
		}
		
		public String getAnswerThumbnailImage() {
			return answerThumbnailImage;
		}
		
		public void setAnswerId(String answerId) {
			this.answerId = answerId;
		}
		
		public void setAnswerThumbnailImage(String answerThumbnailImage) {
			this.answerThumbnailImage = answerThumbnailImage;
		}
		
	}
}
