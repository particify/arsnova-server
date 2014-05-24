package de.thm.arsnova.events;

import org.springframework.context.ApplicationEvent;

import de.thm.arsnova.entities.User;

public class ARSnovaEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	private String sessionKey;
	private User user;
	private final String eventName;
	private final Object data;
	private final Destination destination;

	public enum Destination {
		USER,
		SESSION
	};

	public ARSnovaEvent(Object source, String sKey, String eName, Object d) {
		super(source);
		this.data = d;
		this.eventName = eName;
		this.sessionKey = sKey;
		this.destination = Destination.SESSION;
	}

	public ARSnovaEvent(Object source, User recipient, String eName, Object d) {
		super(source);
		this.data = d;
		this.eventName = eName;
		this.user = recipient;
		this.destination = Destination.USER;
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

	public User getRecipient() {
		return user;
	}

	public Destination getDestinationType() {
		return destination;
	}
}
