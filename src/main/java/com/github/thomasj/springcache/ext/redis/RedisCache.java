package com.github.thomasj.springcache.ext.redis;

import java.util.concurrent.*;

import com.github.thomasj.springcache.ext.key.ExpiryKey;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.lang.Nullable;
import org.springframework.util.SerializationUtils;


import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;

/**
 * @author stantnks@gmail.com
 */
@Slf4j
public class RedisCache extends AbstractValueAdaptingCache {

	private final static int						AVAILABLE_PROCESSORS	= Runtime.getRuntime().availableProcessors();

	private ThreadPoolExecutor						pool					= new ThreadPoolExecutor(AVAILABLE_PROCESSORS / 2,
																				AVAILABLE_PROCESSORS, 60, TimeUnit.SECONDS,
																				new ArrayBlockingQueue<Runnable>(10000),
																				new ThreadPoolExecutor.AbortPolicy());

	private ConcurrentHashMap<Object, FutureTask>	readVisibility			= new ConcurrentHashMap<>();

	private StatefulRedisConnection nativeRedis;

	private String									name;

	private Long									expiryMills;

	public RedisCache(StatefulRedisConnection nativeRedis){
		this("default", nativeRedis, Long.MAX_VALUE, true);
	}

	public RedisCache(StatefulRedisConnection nativeRedis, Long expiryMills){
		this("default", nativeRedis, expiryMills, true);
	}

	public RedisCache(StatefulRedisConnection nativeRedis, Long expiryMills, boolean allowNullValues){
		this("default", nativeRedis, expiryMills, allowNullValues);
	}

	public RedisCache(String name, StatefulRedisConnection nativeRedis, Long expiryMills, boolean allowNullValues){
		super(allowNullValues);
		this.name = name;
		this.nativeRedis = nativeRedis;
		this.expiryMills = expiryMills;
	}

	@Nullable
	@Override
	protected Object lookup (Object key) {
		if (log.isDebugEnabled()) {
			log.debug("lookup, 键: {}", key);
		}
		RedisCommands cmd = this.nativeRedis.sync();
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			byte[] bytes = (byte[]) cmd.get(SerializationUtils.serialize(eKey.getKey()));
			if (bytes == null || bytes.length == 0) {
				return null;
			}
			else {
				return SerializationUtils.deserialize(bytes);
			}
		}
		else {
			byte[] bytes = (byte[]) cmd.get(SerializationUtils.serialize(key));
			if (bytes == null || bytes.length == 0) {
				return null;
			}
			else {
				return SerializationUtils.deserialize(bytes);
			}
		}
	}

	@Override
	public String getName () {
		return this.name;
	}

	@Override
	public Object getNativeCache () {
		return this.nativeRedis;
	}

	@Nullable
	@Override
	public <T>T get (Object key, Callable<T> valueLoader) {
		if (log.isDebugEnabled()) {
			log.debug("查询, 键: {}", key);
		}
		RedisCommands cmd = this.nativeRedis.sync();
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			if (eKey.getKey() == null) {
				return null;
			}
			String k = eKey.getKey().toString();
			byte[] bytes = (byte[]) cmd.get(SerializationUtils.serialize(k));
			if (bytes != null && bytes.length > 0) {
				T v = (T) SerializationUtils.deserialize(bytes);
				return v;
			}
			FutureTask future = new FutureTask( () -> {
				Object obj = valueLoader.call();
				byte[] bKey = SerializationUtils.serialize(k);
				long secs = TimeUnit.SECONDS.convert(eKey.getExpiry(), eKey.getUnit());
				byte[] bVal = SerializationUtils.serialize(obj);
				cmd.setex(bKey, secs, bVal);
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
				throw new ValueRetrievalException(eKey.getKey(), valueLoader, e.getCause());
			}
			finally {
				readVisibility.remove(k);
			}
		}
		else {
			if (key == null) {
				return null;
			}
			byte[] bytes = (byte[]) cmd.get(SerializationUtils.serialize(key));
			if (bytes != null && bytes.length > 0) {
				return (T) SerializationUtils.deserialize(bytes);
			}
			FutureTask future = new FutureTask( () -> {
				Object obj = valueLoader.call();
				byte[] bKey = SerializationUtils.serialize(key);
				long secs = expiryMills / 1000L;
				byte[] bVal = SerializationUtils.serialize(obj);
				cmd.setex(bKey, secs, bVal);
				return obj;
			});
			FutureTask readFuture = readVisibility.putIfAbsent(key, future);
			if (readFuture == null) {
				pool.submit(future);
				readFuture = future;
			}
			else {
				readFuture = readVisibility.get(key);
			}
			try {
				return (T) readFuture.get();
			}
			catch (Exception e) {
				throw new ValueRetrievalException(key, valueLoader, e.getCause());
			}
			finally {
				readVisibility.remove(key);
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
		RedisCommands cmd = this.nativeRedis.sync();
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			cmd.setex(SerializationUtils.serialize(eKey.getKey()),
				TimeUnit.SECONDS.convert(eKey.getExpiry(), eKey.getUnit()),
				SerializationUtils.serialize(value));
		}
		else {
			if (expiryMills == Long.MAX_VALUE) {
				cmd.set(SerializationUtils.serialize(key), SerializationUtils.serialize(value));
			}
			else {
				cmd.setex(SerializationUtils.serialize(key), expiryMills / 1000L, SerializationUtils.serialize(value));
			}
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
		RedisCommands cmd = this.nativeRedis.sync();
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			byte[] bytes = (byte[]) cmd.get(SerializationUtils.serialize(eKey.getKey()));
			if (bytes == null || bytes.length == 0) {
				cmd.setex(SerializationUtils.serialize(eKey.getKey()),
					TimeUnit.SECONDS.convert(eKey.getExpiry(), eKey.getUnit()),
					SerializationUtils.serialize(value));
				return null;
			}
			else {
				return new SimpleValueWrapper(SerializationUtils.deserialize(bytes));
			}
		}
		else {
			byte[] bytes = (byte[]) cmd.get(SerializationUtils.serialize(key));
			if (bytes == null || bytes.length == 0) {
				if (expiryMills == Long.MAX_VALUE) {
					cmd.set(SerializationUtils.serialize(key), SerializationUtils.serialize(value));
				}
				else {
					cmd.setex(SerializationUtils.serialize(key), expiryMills / 1000L, SerializationUtils.serialize(value));
				}
				return null;
			}
			else {
				return new SimpleValueWrapper(SerializationUtils.deserialize(bytes));
			}
		}
	}

	@Override
	public void evict (Object key) {
		if (log.isDebugEnabled()) {
			log.debug("删除, 键: {}", key);
		}
		RedisCommands cmd = this.nativeRedis.sync();
		if (key instanceof ExpiryKey) {
			ExpiryKey eKey = (ExpiryKey) key;
			cmd.del(SerializationUtils.serialize(eKey.getKey()));
		}
		else {
			cmd.del(SerializationUtils.serialize(key));
		}
	}

	@Override
	public void clear () {
		RedisAsyncCommands cmd = this.nativeRedis.async();
		RedisFuture future = cmd.flushall();
		future.thenAccept( (o) -> {
			log.warn("清空redis. {}", o);
		});
	}
}
