package de.thm.arsnova.config.properties;

public enum CacheType {

	CONCURRENT_MAP_CACHE("concurrent-map-cache"),
	MEMCACHE("memcache");

	private String text;

	private CacheType(final String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}
}
