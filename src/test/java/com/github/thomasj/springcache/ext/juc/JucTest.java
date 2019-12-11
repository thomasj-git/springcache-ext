package com.github.thomasj.springcache.ext.juc;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author stantnks@gmail.com
 */
public class JucTest {

	LinkedHashMapEx cache;

	@Before
	public void setUp ()
	throws Exception {
		cache = new LinkedHashMapEx();
		cache.setMaxCapacity(10);
		cache.afterPropertiesSet();
	}

	@After
	public void setDown () {
		cache.clear();
	}

	@Test
	public void testSimple () {
		cache.put("foo", "bar");
		Assert.assertEquals("bar", cache.get("foo"));
	}

	@Test
	public void testExpiry () {
		System.out.println("添加Key: "+System.currentTimeMillis());
		cache.put("foo1", "bar1", 2, TimeUnit.SECONDS);
		try {
			TimeUnit.MILLISECONDS.sleep(500);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertEquals("bar1", cache.get("foo1"));
		try {
			TimeUnit.SECONDS.sleep(2);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		Assert.assertNotEquals("bar1", cache.get("foo1"));
	}
}
