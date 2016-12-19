package de.thm.arsnova.entities;

import java.util.Map;

public class LogEntry {
	public enum LogLevel {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		FATAL
	}

	private String id;
	private String rev;
	private long timestamp;
	private String event;
	private int level;
	private Map<String, Object> payload;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	/* CouchDB deserialization */
	public void set_id(String id) {
		this.id = id;
	}

	public String getRev() {
		return rev;
	}

	public void setRev(String rev) {
		this.rev = rev;
	}

	/* CouchDB deserialization */
	public void set_rev(String rev) {
		this.rev = rev;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public void setLevel(LogLevel level) {
		this.level = level.ordinal();
	}

	public Map<String, Object> getPayload() {
		return payload;
	}

	public void setPayload(Map<String, Object> payload) {
		this.payload = payload;
	}
}
