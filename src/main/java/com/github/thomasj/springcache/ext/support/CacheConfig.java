package com.github.thomasj.springcache.ext.support;

import com.github.thomasj.springcache.ext.juc.LinkedHashMapEx;
import com.github.thomasj.springcache.ext.juc.SimpleLRUCache;
import com.google.common.collect.ImmutableList;
import lombok.Data;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.lang.Nullable;

/**
 * @author stantnks@gmail.com
 */
@Data
public class CacheConfig implements FactoryBean<CacheManager> {

	@Value ("${spring.cache.spi.juc.maxCapacity:10000}")
	private Integer linkedHashMapExMaxCapacity;

	protected Cache buildCache ()
	throws Exception {
		LinkedHashMapEx nativeCache = new LinkedHashMapEx();
		nativeCache.setMaxCapacity(linkedHashMapExMaxCapacity);
		nativeCache.afterPropertiesSet();
		SimpleLRUCache cache = new SimpleLRUCache(nativeCache);
		return cache;
	}

	@Nullable
	@Override
	public CacheManager getObject ()
	throws Exception {
		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(ImmutableList.of(buildCache()));
		cacheManager.afterPropertiesSet();
		return cacheManager;
	}

	@Nullable
	@Override
	public Class<?> getObjectType () {
		return CacheManager.class;
	}

	@Override
	public boolean isSingleton () {
		return true;
	}
}
