package com.github.thomasj.springcache.ext.juc;

import java.util.concurrent.*;

import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;

import com.github.thomasj.springcache.ext.key.ExpiryKey;

import lombok.extern.slf4j.Slf4j;

/**
 * @author stantnks@gmail.com
 */
@Slf4j
public class SimpleLRUCache extends AbstractValueAdaptingCache {

	private final static int						AVAILABLE_PROCESSORS	= Runtime.getRuntime().availableProcessors();

	private ThreadPoolExecutor						pool					= new ThreadPoolExecutor(AVAILABLE_PROCESSORS / 2,
		AVAILABLE_PROCESSORS, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10000),
		new ThreadPoolExecutor.AbortPolicy());

	private ConcurrentHashMap<Object, FutureTask>	readVisibility			= new ConcurrentHashMap<>();

	private LinkedHashMapEx							nativeCache;

	private String									name;

	public SimpleLRUCache(LinkedHashMapEx nativeCache){
		this("default", nativeCache, true);
	}

	public SimpleLRUCache(LinkedHashMapEx nativeCache, boolean allowNullValues){
		this("default", nativeCache, allowNullValues);
	}

	public SimpleLRUCache(String name, LinkedHashMapEx nativeCache, boolean allowNullValues){
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
			return this.nativeCache.get(eKey.getKey());
		}
		else {
			return this.nativeCache.get(key);
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
	public <T> T get (Object key, Callable<T> valueLoader) {
		if (log.isDebugEnabled()) {
			log.debug("查询, 键: {}", key);
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			String k = eKey.getKey().toString();
			T v = (T) this.nativeCache.get(k);
			if (v != null) {
				return v;
			}
			FutureTask future = new FutureTask( () -> {
				Object obj = valueLoader.call();
				nativeCache.put(k, obj, eKey.getExpiry(), eKey.getUnit());
				return obj;
			});
			FutureTask readFuture = readVisibility.putIfAbsent(k, future);
			// first
			if (readFuture == null) {
				pool.submit(future);
				readFuture = future;
			}
			else {
				readFuture = readVisibility.get(k);
			}
			try {
				return (T) readFuture.get();
			}
			catch (Exception e) {
				throw new ValueRetrievalException(eKey.getKey(), valueLoader, e.getCause());
			}
			finally {
				readVisibility.remove(k);
			}
		}
		else {
			String k = key.toString();
			T v = (T) this.nativeCache.get(k);
			if (v != null) {
				return v;
			}
			FutureTask future = new FutureTask( () -> {
				Object obj = valueLoader.call();
				nativeCache.put(k, obj);
				return obj;
			});
			FutureTask readFuture = readVisibility.putIfAbsent(k, future);
			if (readFuture == null) {
				pool.submit(future);
				readFuture = future;
			}
			else {
				readFuture = readVisibility.get(k);
			}
			try {
				return (T) readFuture.get();
			}
			catch (Exception e) {
				throw new ValueRetrievalException(key, valueLoader, e.getCause());
			}
			finally {
				readVisibility.remove(k);
			}
		}
	}

	@Override
	public void put (Object key, @Nullable Object value) {
		if (log.isDebugEnabled()) {
			log.debug("添加缓存, 键: {}, 值: {}", key, value);
		}
		if (!this.isAllowNullValues() && value == null) {
			throw new NullPointerException(String.format("LRUKey: [%s], Value is null, isAllowNullValues [no]", key));
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			this.nativeCache.put(eKey.getKey().toString(), value, eKey.getExpiry(), eKey.getUnit());
		}
		else {
			this.nativeCache.put(key.toString(), value);
		}
	}

	@Nullable
	@Override
	public ValueWrapper putIfAbsent (Object key, @Nullable Object value) {
		if (log.isDebugEnabled()) {
			log.debug("添加缓存(不存在添加), 键: {}, 值: {}", key, value);
		}
		if (!this.isAllowNullValues() && value == null) {
			throw new NullPointerException(String.format("LRUKey: [%s], Value is null, isAllowNullValues [no]", key));
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			String k = eKey.getKey().toString();
			Object v = this.nativeCache.get(k);
			if (v == null) {
				this.nativeCache.put(k, value, eKey.getExpiry(), eKey.getUnit());
				return null;
			}
			else {
				return new SimpleValueWrapper(v);
			}
		}
		else {
			String k = key.toString();
			Object v = this.nativeCache.get(k);
			if (v == null) {
				this.nativeCache.put(k, value);
				return null;
			}
			else {
				return new SimpleValueWrapper(v);
			}
		}
	}

	@Override
	public void evict (Object key) {
		if (log.isDebugEnabled()) {
			log.debug("删除, 键: {}", key);
		}
		this.nativeCache.remove(key);
	}

	@Override
	public void clear () {
		log.warn("清空缓存");
		this.nativeCache.clear();
	}
}
