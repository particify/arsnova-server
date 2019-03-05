package de.thm.arsnova.websocket.message;

public class PatchedPayload implements WebSocketPayload {
	String type;

	String id;

	String propertyName;

	boolean propertyValue;

	public PatchedPayload(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public boolean isPropertyValue() {
		return propertyValue;
	}

	public void setPropertyValue(boolean propertyValue) {
		this.propertyValue = propertyValue;
	}
}
