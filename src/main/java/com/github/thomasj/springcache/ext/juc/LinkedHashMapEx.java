package com.github.thomasj.springcache.ext.juc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import lombok.Setter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import lombok.extern.slf4j.Slf4j;

/**
 * @author stantnks@gmail.com
 */
@Slf4j
public class LinkedHashMapEx extends LinkedHashMap<String, Object> implements InitializingBean, DisposableBean {

	@Setter
	private Integer				maxCapacity	= 10000;

	private DelayQueue<LRUKey>	delayQueue	= new DelayQueue<>();

	private Map<String, LRUKey>	lruItems	= Maps.newHashMapWithExpectedSize(1000);

	private ExpiryTask			expiryTask;

	@Override
	public void afterPropertiesSet ()
	throws Exception {
		this.expiryTask = new ExpiryTask(this, delayQueue);
		this.expiryTask.setName("cache-clean");
		this.expiryTask.start();
	}

	@Override
	protected boolean removeEldestEntry (Map.Entry eldest) {
		if (super.size() > maxCapacity) {
			// FIXME 溢出后，延时队列无法清理，必须等待延时达到后启动清理
			log.warn("缓存溢出，移除掉 KEY: {}, 容量: {}", eldest.getKey(), maxCapacity);
			return true;
		}
		return false;
	}

	@Override
	public synchronized Object put (String key, Object value) {
		if (log.isDebugEnabled()) {
			log.debug("添加缓存, 键: {}, 值: {}", key, value);
		}
		return this.put(key, value, 7, TimeUnit.DAYS);
	}

	@Override
	public synchronized Object remove (Object key) {
		if (log.isDebugEnabled()) {
			log.debug("删除缓存, 键: {}", key);
		}
		LRUKey eLRUKey = lruItems.remove(key);
		if (eLRUKey != null) {
			eLRUKey.expiry();
		}
		return super.remove(key);
	}

	public synchronized Object put (String key, Object value, long expiry, TimeUnit unit) {
		if (log.isDebugEnabled()) {
			log.debug("添加缓存, 键: {}, 值: {}, 过期时间: {} ms.", new Object[] {key, value, TimeUnit.MILLISECONDS.convert(expiry, unit)});
		}
		Object result = super.put(key, value);
		LRUKey lruKey = new LRUKey(key, expiry, unit);
		delayQueue.offer(lruKey);
		LRUKey eLRUKey = lruItems.remove(key);
		if (eLRUKey != null) {
			eLRUKey.expiry();
		}
		lruItems.put(key, lruKey);
		return result;
	}

	@Override
	public void destroy ()
	throws Exception {
		this.expiryTask.interrupt();
		delayQueue.clear();
		lruItems.clear();
		super.clear();
	}
}
