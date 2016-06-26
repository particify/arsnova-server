package de.thm.arsnova.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.google.code.ssm.CacheFactory;
import com.google.code.ssm.config.DefaultAddressProvider;
import com.google.code.ssm.providers.CacheConfiguration;
import com.google.code.ssm.providers.xmemcached.MemcacheClientFactoryImpl;
import com.google.code.ssm.spring.SSMCache;
import com.google.code.ssm.spring.SSMCacheManager;

import de.thm.arsnova.config.properties.CacheConfigProperties;

@Configuration
@EnableCaching
@PropertySource(
		value = {"classpath:arsnova.properties.example", "file:/etc/arsnova/arsnova.properties"},
		ignoreResourceNotFound = true
		)
public class CacheConfig {

	@Autowired
	private CacheConfigProperties cacheConfigProperties;

	@Bean
	@ConditionalOnProperty(name = "cache.type", havingValue = "concurrent-map-cache", matchIfMissing = true)
	public CacheManager concurrentMapCacheManager() {
		return new ConcurrentMapCacheManager();
	}

	@Bean
	@ConditionalOnProperty(name = "cache.type", havingValue = "memcache")
	public CacheManager cacheManager() throws Exception {
		final SSMCacheManager cm = new SSMCacheManager();
		final List<SSMCache> caches = new ArrayList<>();
		caches.add(new SSMCache(defaultMemcache().getObject(), cacheConfigProperties.getExpiration()));
		cm.setCaches(caches);
		return cm;
	}

	@Bean
	@ConditionalOnProperty(name = "cache.type", havingValue = "memcache")
	public CacheFactory defaultMemcache() {
		final CacheFactory cf = new CacheFactory();
		cf.setCacheClientFactory(new MemcacheClientFactoryImpl());
		cf.setCacheName("default");
		cf.setAddressProvider(
				new DefaultAddressProvider(
						cacheConfigProperties.getHost()
						+ ":"
						+ cacheConfigProperties.getPort()
						)
				);
		final CacheConfiguration config = new CacheConfiguration();
		config.setConsistentHashing(true);
		cf.setConfiguration(config);
		return cf;
	}

}
