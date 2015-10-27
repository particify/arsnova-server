package de.thm.arsnova.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
@EnableCaching
public interface CacheConfig {

	public static class ConcurrentMapConfigCondition implements Condition {

		@Override
		public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
			return context.getEnvironment().getProperty("cache.type") == null
					|| "ConcurrentMap".equals(context.getEnvironment().getProperty("cache.type"));
		}

	}

	public static class MemcacheConfigCondition implements Condition {

		@Override
		public boolean matches(final ConditionContext context, final AnnotatedTypeMetadata metadata) {
			return "Memcache".equals(context.getEnvironment().getProperty("cache.type"));
		}

	}

	public CacheManager cacheManager() throws Exception;
}
