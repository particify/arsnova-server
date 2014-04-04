package de.thm.arsnova.entities;

public class ServiceDescription {

	private String name;
	private String dialogUrl;
	private boolean allowLecturer = true;

	public ServiceDescription(String name, String dialogUrl) {
		this.name = name;
		this.dialogUrl = dialogUrl;
	}

	public ServiceDescription(String name, String dialogUrl, boolean allowLecturer) {
		this.name = name;
		this.dialogUrl = dialogUrl;
		this.allowLecturer = allowLecturer;
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

	public boolean isAllowLecturer() {
		return allowLecturer;
	}

	public void setAllowLecturer(boolean allowLecturer) {
		this.allowLecturer = allowLecturer;
	}

}
