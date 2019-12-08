package com.github.thomasj.springcache.ext.guava;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author stantnks@gmail.com
 */
@Slf4j
public class GuavaCacheFactoryBean implements FactoryBean, InitializingBean {

	@Setter
	private long					evictTimeMills		= 300000L;

	@Setter
	private int						initialCapacity		= 100;

	@Setter
	private int						maximumSize			= 20000;

	@Setter
	private int						concurrencyLevel	= Runtime.getRuntime().availableProcessors();

	private Cache<Object, Object> target;

	@Override
	public void afterPropertiesSet ()
	throws Exception {
		this.target = CacheBuilder
			.newBuilder()
			.removalListener(notification -> log.info("清理KEY: {}", notification.getKey()))
			.expireAfterWrite(evictTimeMills, TimeUnit.MILLISECONDS)
			.initialCapacity(initialCapacity)
			.maximumSize(maximumSize)
			.concurrencyLevel(concurrencyLevel)
			.build();
	}

	@Nullable
	@Override
	public Cache getObject ()
	throws Exception {
		return this.target;
	}

	@Nullable
	@Override
	public Class<?> getObjectType () {
		return Cache.class;
	}

	@Override
	public boolean isSingleton () {
		return true;
	}
}
