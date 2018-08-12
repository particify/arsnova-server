package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import de.thm.arsnova.model.serialization.View;
import org.springframework.core.style.ToStringCreator;

import java.util.Objects;

public class Attachment extends Entity {
	private String mediaType;
	private long size;
	private String originalSourceUrl;
	private String storageLocation;

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getId() {
		return id;
	}

	@Override
	@JsonView(View.Persistence.class)
	public void setId(final String id) {
		this.id = id;
	}

	@Override
	@JsonView({View.Persistence.class, View.Public.class})
	public String getRevision() {
		return rev;
	}

	@Override
	@JsonView(View.Public.class)
	public void setRevision(final String rev) {
		this.rev = rev;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getMediaType() {
		return mediaType;
	}

	@JsonView(View.Persistence.class)
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	@JsonView(View.Persistence.class)
	public long getSize() {
		return size;
	}

	@JsonView(View.Persistence.class)
	public void setSize(long size) {
		this.size = size;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public String getOriginalSourceUrl() {
		return originalSourceUrl;
	}

	@JsonView({View.Persistence.class, View.Public.class})
	public void setOriginalSourceUrl(String originalSourceUrl) {
		this.originalSourceUrl = originalSourceUrl;
	}

	@JsonView(View.Persistence.class)
	public String getStorageLocation() {
		return storageLocation;
	}

	@JsonView(View.Persistence.class)
	public void setStorageLocation(String storageLocation) {
		this.storageLocation = storageLocation;
	}

	/**
	 * {@inheritDoc}
	 *
	 * All fields of <tt>Attachment</tt> are included in equality checks.
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!super.equals(o)) {
			return false;
		}
		final Attachment that = (Attachment) o;

		return size == that.size &&
				Objects.equals(mediaType, that.mediaType) &&
				Objects.equals(originalSourceUrl, that.originalSourceUrl) &&
				Objects.equals(storageLocation, that.storageLocation);
	}

	@Override
	protected ToStringCreator buildToString() {
		return super.buildToString()
				.append("mediaType", mediaType)
				.append("size", size)
				.append("originalSourceUrl", originalSourceUrl)
				.append("storageLocation", storageLocation);
	}
}
