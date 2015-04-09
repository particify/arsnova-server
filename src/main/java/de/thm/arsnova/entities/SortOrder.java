/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.	 If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.entities;

import java.util.List;

public class SortOrder {

	private String sessionId;
	private String sortType;
	private String questionVariant;
	private String subject;
	private List<String> sortOrder;

	private String _id;
	private String _rev;
	
	public void setSessionId(final String sessionId) {
		this.sessionId = sessionId;
	}
	
	public String getSessionId() {
		return this.sessionId;	 
	}
	
	public void setSortType(final String sortType) {
		this.sortType = sortType;
	}
	
	public String getSortType() {
		return this.sortType;
	}
	
	public void setQuestionVariant(final String questionVariant) {
		this.questionVariant = questionVariant;
	}
	
	public String getQuestionVariant() {
		return this.questionVariant;
	}
	
	public void setSubject(final String subject) {
		this.subject = subject;
	}
	
	public String getSubject() {
		return this.subject;
	}
	
	public void setSortOrder(final List<String> sortOrder) {
		this.sortOrder = sortOrder;
	}
	
	public List<String> getSortOrder() {
		return this.sortOrder;
	}

	public void set_id(final String id) {
		_id = id;
	}

	public String get_id() {
		return _id;
	}

	public void set_rev(final String rev) {
		_rev = rev;
	}

	public String get_rev() {
		return _rev;
	}

	@Override
	public int hashCode() {
		// auto-generated!
		final int prime = 31;
		int result = 1;
		result = prime * result + ((questionVariant == null) ? 0 : questionVariant.hashCode());
		result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// auto-generated!
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SortOrder other = (SortOrder) obj;
		if (questionVariant == null) {
			if (other.questionVariant != null) {
				return false;
			}
		} else if (!questionVariant.equals(other.questionVariant)) {
			return false;
		}
		if (sessionId == null) {
			if (other.sessionId != null) {
				return false;
			}
		} else if (!sessionId.equals(other.sessionId)) {
			return false;
		}
		if (subject == null) {
			if (other.subject != null) {
				return false;
			}
		} else if (!subject.equals(other.subject)) {
			return false;
		}
		return true;
	}
}
