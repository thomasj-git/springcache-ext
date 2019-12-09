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
		cache.put("foo1", "bar1", 1, TimeUnit.SECONDS);
		try {
			TimeUnit.SECONDS.sleep(2);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(cache.get("foo1"));
		Assert.assertNotEquals("bar1", cache.get("foo1"));
	}
}
