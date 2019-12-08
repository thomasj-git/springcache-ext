package com.github.thomasj.springcache.ext.memcached;

import java.util.Date;
import java.util.concurrent.*;

import com.github.thomasj.springcache.ext.key.ExpiryKey;
import com.github.thomasj.springcache.ext.util.NoSqlUtil;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;

import com.danga.MemCached.MemCachedClient;

import lombok.extern.slf4j.Slf4j;

/**
 * @author stantnks@gmail.com
 */
@Slf4j
public class MemcachedCache extends AbstractValueAdaptingCache {

	private final static int						AVAILABLE_PROCESSORS	= Runtime.getRuntime().availableProcessors();

	private ThreadPoolExecutor						pool					= new ThreadPoolExecutor(AVAILABLE_PROCESSORS / 2,
																				AVAILABLE_PROCESSORS, 60, TimeUnit.SECONDS,
																				new ArrayBlockingQueue<Runnable>(10000),
																				new ThreadPoolExecutor.AbortPolicy());

	private ConcurrentHashMap<Object, FutureTask>	readVisibility			= new ConcurrentHashMap<>();

	private String									name;

	private MemCachedClient							nativeCache;

	private Long									expiryMills;

	public MemcachedCache(MemCachedClient nativeCache){
		this("default", nativeCache, Long.MAX_VALUE, true);
	}

	public MemcachedCache(MemCachedClient nativeCache, Long expiryMills){
		this("default", nativeCache, expiryMills, true);
	}

	public MemcachedCache(MemCachedClient nativeCache, Long expiryMills, boolean allowNullValues){
		this("default", nativeCache, expiryMills, allowNullValues);
	}

	public MemcachedCache(String name, MemCachedClient nativeCache, Long expiryMills, boolean allowNullValues){
		super(allowNullValues);
		this.name = name;
		this.nativeCache = nativeCache;
		this.expiryMills = expiryMills;
	}

	@Nullable
	@Override
	protected Object lookup (Object key) {
		if (log.isDebugEnabled()) {
			log.debug("lookup, 键: {}", key);
		}
		if (NoSqlUtil.objEmpty(key)) {
			return null;
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			if (NoSqlUtil.objEmpty(eKey.getKey())) {
				return null;
			}
			return this.nativeCache.get(eKey.getKey().toString());
		}
		else {
			return this.nativeCache.get(key.toString());
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
		if (NoSqlUtil.objEmpty(key)) {
			return null;
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			if (NoSqlUtil.objEmpty(eKey.getKey())) {
				return null;
			}
			String k = eKey.getKey().toString();
			T v = (T) this.nativeCache.get(k);
			if (v != null) {
				return v;
			}
			FutureTask future = new FutureTask( () -> {
				Object obj = valueLoader.call();
				Date expiryDate = new Date(System.currentTimeMillis()
					+ TimeUnit.MILLISECONDS.convert(eKey.getExpiry(), eKey.getUnit()));
				nativeCache.set(k, obj, expiryDate);
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
				nativeCache.set(k, obj, new Date(System.currentTimeMillis() + expiryMills));
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
		if (NoSqlUtil.objEmpty(key)) {
			return;
		}
		if (!this.isAllowNullValues() && value == null) {
			throw new NullPointerException(String.format("Key: [%s], Value is null, isAllowNullValues [no]", key));
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			if (NoSqlUtil.objEmpty(eKey.getKey())) {
				return;
			}
			this.nativeCache.set(eKey.getKey().toString(),
				value,
				new Date(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(eKey.getExpiry(), eKey.getUnit())));
		}
		else {
			if (expiryMills == Long.MAX_VALUE) {
				this.nativeCache.set(key.toString(), value);
			}
			else {
				this.nativeCache.set(key.toString(), value, new Date(System.currentTimeMillis() + expiryMills));
			}
		}
	}

	@Nullable
	@Override
	public ValueWrapper putIfAbsent (Object key, @Nullable Object value) {
		if (log.isDebugEnabled()) {
			log.debug("添加缓存(不存在添加), 键: {}, 值: {}", key, value);
		}
		if (NoSqlUtil.objEmpty(key)) {
			return null;
		}
		if (!this.isAllowNullValues() && value == null) {
			throw new NullPointerException(String.format("Key: [%s], Value is null, isAllowNullValues [no]", key));
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			if (NoSqlUtil.objEmpty(eKey.getKey())) {
				return null;
			}
			String k = eKey.getKey().toString();
			Object v = this.nativeCache.get(k);
			if (v == null) {
				this.nativeCache.set(k,
					value,
					new Date(System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(eKey.getExpiry(), eKey.getUnit())));
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
				if (expiryMills == Long.MAX_VALUE) {
					this.nativeCache.set(k, value);
				}
				else {
					this.nativeCache.set(k, value, new Date(System.currentTimeMillis() + expiryMills));
				}
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
		if (NoSqlUtil.objEmpty(key)) {
			return;
		}
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			if (NoSqlUtil.objEmpty(eKey.getKey())) {
				return;
			}
			this.nativeCache.delete(eKey.getKey().toString());
		}
		else {
			this.nativeCache.delete(key.toString());
		}
	}

	@Override
	public void clear () {
		log.warn("清空缓存");
		this.nativeCache.flushAll();
	}
}
