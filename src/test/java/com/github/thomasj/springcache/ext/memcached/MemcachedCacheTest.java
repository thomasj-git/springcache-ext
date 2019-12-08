package com.github.thomasj.springcache.ext.memcached;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.github.thomasj.springcache.ext.memcached.config.PoolConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.danga.MemCached.MemCachedClient;

/**
 * @author stantnks@gmail.com
 */
public class MemcachedCacheTest {

	MemCachedClient client;

	@Before
	public void setUp ()
	throws Exception {
		MemcachedCacheFactoryBean factoryBean = new MemcachedCacheFactoryBean();
		factoryBean.setPoolConfig(new PoolConfiguration("192.168.56.201:11201"));
		factoryBean.afterPropertiesSet();
		client = (MemCachedClient) factoryBean.getObject();
	}

	@After
	public void setDown () {
		client.flushAll();
	}

	@Test
	public void testSimple () {
		client.set("foo", "bar");
		Assert.assertEquals("bar", client.get("foo"));
	}

	@Test
	public void testExpiry () {
		client.set("foo1", "bar1", new Date(System.currentTimeMillis() + 1000));
		try {
			TimeUnit.MILLISECONDS.sleep(5000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertNotEquals("bar1", client.get("foo1"));
	}
}
