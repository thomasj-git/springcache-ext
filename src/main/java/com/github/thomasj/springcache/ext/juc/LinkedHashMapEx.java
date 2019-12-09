package com.github.thomasj.springcache.ext.juc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

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
	private Integer			maxCapacity	= 10000;

	private DelayQueue<LRUKey>	delayQueue	= new DelayQueue<>();

	private ExpiryTask		expiryTask;

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
			log.info("缓存溢出，移除掉 KEY: {}, 容量: {}", eldest.getKey(), maxCapacity);
			return true;
		}
		return false;
	}

	@Override
	public synchronized Object put (String key, Object value) {
		return super.put(key, value);
	}

	@Override
	public synchronized Object remove (Object key) {
		return super.remove(key);
	}

	public synchronized Object put (String key, Object value, long expiry, TimeUnit unit) {
		if (log.isDebugEnabled()) {
			log.debug("添加缓存, 键: {}, 值: {}", key, value);
		}
		Object result = this.put(key, value);
		delayQueue.offer(new LRUKey(key, expiry, unit));
		return result;
	}

	@Override
	public void destroy ()
	throws Exception {
		this.expiryTask.interrupt();
		delayQueue.clear();
		super.clear();
	}
}
