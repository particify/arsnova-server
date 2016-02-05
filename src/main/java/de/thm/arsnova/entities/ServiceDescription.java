/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2016 The ARSnova Team
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
package de.thm.arsnova.entities;

/**
 * A login service description. For example, this class is used to display the login buttons in ARSnova mobile.
 */
public class ServiceDescription {
	private String id;
	private String name;
	private String dialogUrl;
	private String image;
	private int order = 0;
	private boolean allowLecturer = true;

	public ServiceDescription(String id, String name, String dialogUrl) {
		this.id = id;
		this.name = name;
		this.dialogUrl = dialogUrl;
	}

	public ServiceDescription(String id, String name, String dialogUrl, String image) {
		this.id = id;
		this.name = name;
		this.dialogUrl = dialogUrl;
		if (!"".equals(image)) {
			this.image = image;
		}
	}

	public ServiceDescription(String id, String name, String dialogUrl, boolean allowLecturer) {
		this.id = id;
		this.name = name;
		this.dialogUrl = dialogUrl;
		this.allowLecturer = allowLecturer;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDialogUrl() {
		return dialogUrl;
	}

	public void setDialogUrl(String dialogUrl) {
		this.dialogUrl = dialogUrl;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public boolean isAllowLecturer() {
		return allowLecturer;
	}

	public void setAllowLecturer(boolean allowLecturer) {
		this.allowLecturer = allowLecturer;
	}
}
