package de.thm.arsnova.websocket.message;

public class WebSocketMessage<P extends WebSocketPayload> {
	private String type;

	private P payload;

	public WebSocketMessage(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public P getPayload() {
		return payload;
	}

	public void setPayload(P payload) {
		this.payload = payload;
	}
}
