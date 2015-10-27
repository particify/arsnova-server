package de.thm.arsnova.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import com.google.code.ssm.CacheFactory;
import com.google.code.ssm.config.DefaultAddressProvider;
import com.google.code.ssm.providers.CacheConfiguration;
import com.google.code.ssm.providers.xmemcached.MemcacheClientFactoryImpl;
import com.google.code.ssm.spring.SSMCache;
import com.google.code.ssm.spring.SSMCacheManager;

@Conditional(CacheConfig.MemcacheConfigCondition.class)
public class MemcacheConfig implements CacheConfig{

	@Override
	@Bean
	public CacheManager cacheManager() throws Exception {
		final SSMCacheManager cm = new SSMCacheManager();
		final List<SSMCache> caches = new ArrayList<>();
		caches.add(new SSMCache(defaultCache().getObject(), 1800));
		cm.setCaches(caches);
		return cm;
	}

	@Bean
	public CacheFactory defaultCache() {
		final CacheFactory cf = new CacheFactory();
		cf.setCacheClientFactory(new MemcacheClientFactoryImpl());
		cf.setCacheName("default");
		cf.setAddressProvider(new DefaultAddressProvider("127.0.0.1:11211"));
		final CacheConfiguration config = new CacheConfiguration();
		config.setConsistentHashing(true);
		cf.setConfiguration(config);
		return cf;
	}

}
