package com.github.thomasj.springcache.ext.guava;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.cache.Cache;

/**
 * @author stantnks@gmail.com
 */
public class GuavaCacheTest {

	Cache cache;

	@Before
	public void setUp ()
	throws Exception {
		GuavaCacheFactoryBean factoryBean = new GuavaCacheFactoryBean();
		factoryBean.setEvictTimeMills(1000);
		factoryBean.afterPropertiesSet();
		cache = factoryBean.getObject();
	}

	@After
	public void setDown () {
		cache.cleanUp();
	}

	@Test
	public void testSimple () {
		cache.put("foo", "bar");
		Assert.assertEquals("bar", cache.getIfPresent("foo"));
	}

	@Test
	public void testExpiry () {
		cache.put("foo1", "bar1");
		try {
			TimeUnit.MILLISECONDS.sleep(2500);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertNotEquals("bar1", cache.getIfPresent("foo1"));
	}
}
