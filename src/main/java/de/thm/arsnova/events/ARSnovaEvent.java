package de.thm.arsnova.events;

import org.springframework.context.ApplicationEvent;

public class ARSnovaEvent extends ApplicationEvent {
	
	private static final long serialVersionUID = 1L;
	
	private String sessionKey;
	private String eventName;
	private Object data;
	
	public ARSnovaEvent(Object source, String sKey, String eName, Object d) {
		super(source);
		this.data = d;
		this.eventName = eName;
		this.sessionKey = sKey;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public String getEventName() {
		return eventName;
	}

	public Object getData() {
		return data;
	}
}
