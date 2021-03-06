package com.github.thomasj.springcache.ext.juc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author stantnks@gmail.com
 */
@Slf4j
@Data
public class LRUKey implements Delayed {

	private long				initMills;

	private volatile long		expiry;

	private TimeUnit			eUnit;

	private String				key;

	private volatile boolean	isTake	= false;

	public LRUKey(String key, long expiry, TimeUnit eUnit){
		this.initMills = System.currentTimeMillis();
		this.key = key;
		this.expiry = expiry;
		this.eUnit = eUnit;
	}

	@Override
	public long getDelay (TimeUnit unit) {
		long delay = unit.convert(TimeUnit.MILLISECONDS.convert(expiry, eUnit) - now(), TimeUnit.MILLISECONDS);
		if (delay <= 0) {
			isTake = true;
		}
		return delay;
	}

	@Override
	public int compareTo (Delayed other) {
		// compare zero ONLY if same object
		if (other == this) {
			return 0;
		}
		long d = (getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS));
		return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
	}

	private long now () {
		return System.currentTimeMillis() - initMills;
	}

	public long expiryMills () {
		return TimeUnit.MILLISECONDS.convert(expiry, eUnit);
	}

	public void expiry () {
		if (!isTake) {
			log.info("把KEY: {} 设置为立即过期, 原始过期时间: {}.", key, expiryMills());
			this.expiry = 0L;
		}
	}
}
