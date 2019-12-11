package com.github.thomasj.springcache.ext.juc;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author stantnks@gmail.com
 */
@AllArgsConstructor
@Slf4j
public class ExpiryTask extends Thread {

	private LinkedHashMapEx		linkedHashMapEx;

	private DelayQueue<LRUKey>	delayQueue;

	@Override
	public void run () {
		log.info("延时队列检出任务启动.");
		LRUKey key;
		while (true) {
			try {
				key = delayQueue.poll(100, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				log.info("延时队列检出任务中断");
				break;
			}
			if (key == null) {
				continue;
			}
			log.info("缓存KEY: {} 已经过期, 从延时队列移除.", key.getKey());
			linkedHashMapEx.remove(key.getKey());
		}
	}
}
