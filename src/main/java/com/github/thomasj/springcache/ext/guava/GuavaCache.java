package com.github.thomasj.springcache.ext.guava;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.github.thomasj.springcache.ext.key.ExpiryKey;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;

import com.google.common.cache.Cache;

import lombok.extern.slf4j.Slf4j;

/**
 * @author stantnks@gmail.com
 */
@Slf4j
public class GuavaCache extends AbstractValueAdaptingCache {

	private String					name;

	private Cache<Object, Object> nativeCache;

	public GuavaCache(Cache<Object, Object> nativeCache){
		this("default", nativeCache, true);
	}

	public GuavaCache(Cache<Object, Object> nativeCache, boolean allowNullValues){
		this("default", nativeCache, allowNullValues);
	}

	public GuavaCache(String name, Cache<Object, Object> nativeCache, boolean allowNullValues){
		super(allowNullValues);
		this.name = name;
		this.nativeCache = nativeCache;
	}

	@Nullable
	@Override
	protected Object lookup (Object key) {
		if (log.isDebugEnabled()) {
			log.debug("lookup, 键: {}", key);
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			return this.nativeCache.getIfPresent(eKey.getKey());
		}
		else {
			return this.nativeCache.getIfPresent(key);
		}
	}

	@Override
	public String getName () {
		return this.name;
	}

	@Override
	public Object getNativeCache () {
		return this.nativeCache;
	}

	@Nullable
	@Override
	public <T>T get (Object key, Callable<T> valueLoader) {
		if (log.isDebugEnabled()) {
			log.debug("查询, 键: {}", key);
		}
		try {
			if (key instanceof ExpiryKey) {
				ExpiryKey eKey = (ExpiryKey) key;
				return (T) this.nativeCache.get(eKey.getKey(), valueLoader);
			}
			else {
				return (T) this.nativeCache.get(key, valueLoader);
			}
		}
		catch (ExecutionException e) {
			throw new ValueRetrievalException(key, valueLoader, e.getCause());
		}
	}

	@Override
	public void put (Object key, @Nullable Object value) {
		if (log.isDebugEnabled()) {
			log.debug("添加缓存, 键: {}, 值: {}", key, value);
		}
		if (key instanceof ExpiryKey) {
			throw new UnsupportedOperationException("Guava cache not support @Expiry on method.");
		}
		else {
			if (!this.isAllowNullValues() && value == null) {
				throw new NullPointerException(String.format("LRUKey: [%s], Value is null, isAllowNullValues [no]", key));
			}
			this.nativeCache.put(key, value);
		}
	}

	@Nullable
	@Override
	public ValueWrapper putIfAbsent (Object key, @Nullable Object value) {
		if (log.isDebugEnabled()) {
			log.debug("添加缓存(不存在添加), 键: {}, 值: {}", key, value);
		}
		Object v;
		if (key instanceof ExpiryKey) {
			throw new UnsupportedOperationException("Guava cache not support @Expiry on method.");
		}
		else {
			v = this.nativeCache.getIfPresent(key);
		}
		if (v == null) {
			if (!this.isAllowNullValues() && value == null) {
				throw new NullPointerException(String.format("LRUKey: [%s], Value is null, isAllowNullValues [no]", key));
			}
			this.nativeCache.put(key, value);
			return null;
		}
		else {
			return new SimpleValueWrapper(v);
		}
	}

	@Override
	public void evict (Object key) {
		if (log.isDebugEnabled()) {
			log.debug("删除, 键: {}", key);
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			this.nativeCache.invalidate(eKey.getKey());
		}
		else {
			this.nativeCache.invalidate(key);
		}
	}

	@Override
	public void clear () {
		log.warn("清空缓存");
		this.nativeCache.cleanUp();
	}
}
