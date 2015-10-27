package de.thm.arsnova.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

@Conditional(CacheConfig.ConcurrentMapConfigCondition.class)
public class ConcurrentMapCacheConfig implements CacheConfig{

	@Override
	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}

}
