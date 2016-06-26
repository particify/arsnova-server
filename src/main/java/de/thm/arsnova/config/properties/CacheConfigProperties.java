package de.thm.arsnova.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/** Cache configuration properties
 * @author Paul-Christian Volkmer
 */
@Data
@Component
@ConfigurationProperties("cache")
public class CacheConfigProperties {

	/** The cache to be used */
	private CacheType type = CacheType.CONCURRENT_MAP_CACHE;

	/** The hostname of the cache server */
	private String host;

	/** The port of the cache server */
	private int port;

	private int expiration = 1800;

}
