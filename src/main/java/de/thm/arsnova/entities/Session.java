package de.thm.arsnova.entities;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.entities.serialization.View;

public class Session implements Entity {
	private String id;
	private String rev;
	private String shortId;
	private String ownerId;
	private String name;
	private String abbreviation;
	private boolean closed;
	private SessionStatistics statistics;

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setId(final String id) {
		this.id = id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getShortId() {
		return shortId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setShortId(final String shortId) {
		this.shortId = shortId;
	}

	@JsonView(View.Persistence.class)
	public String getOwnerId() {
		return ownerId;
	}

	@JsonView(View.Persistence.class)
	public void setOwnerId(final String ownerId) {
		this.ownerId = ownerId;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getName() {
		return name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setName(final String name) {
		this.name = name;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getAbbreviation() {
		return abbreviation;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setAbbreviation(final String abbreviation) {
		this.abbreviation = abbreviation;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public boolean isClosed() {
		return closed;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setClosed(final boolean closed) {
		this.closed = closed;
	}

	@JsonView(View.Public.class)
	public SessionStatistics getStatistics() {
		return statistics;
	}

	public void setStatistics(final SessionStatistics statistics) {
		this.statistics = statistics;
	}
}
