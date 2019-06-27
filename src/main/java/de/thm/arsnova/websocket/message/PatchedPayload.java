package de.thm.arsnova.websocket.message;

public class PatchedPayload implements WebSocketPayload {
	String type;

	String id;

	String propertyName;

	boolean propertyValue;

	public PatchedPayload(final String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(final String propertyName) {
		this.propertyName = propertyName;
	}

	public boolean isPropertyValue() {
		return propertyValue;
	}

	public void setPropertyValue(final boolean propertyValue) {
		this.propertyValue = propertyValue;
	}
}
