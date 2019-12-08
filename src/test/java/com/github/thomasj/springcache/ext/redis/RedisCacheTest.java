package com.github.thomasj.springcache.ext.redis;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
public class RedisCacheTest {

	@Before
	public void setUp ()
	throws Exception {
	}

	@After
	public void setDown () {
	}

	@Test
	public void testSimple ()
	throws Exception {
		RedisFactoryBean factoryBean = new RedisFactoryBean();
		factoryBean.setRedisUri("redis://192.168.56.201:6379/1");
		factoryBean.afterPropertiesSet();
		StatefulRedisConnection redis = (StatefulRedisConnection) factoryBean.getObject();
		RedisCommands cmd = redis.sync();
		cmd.set("foo", "bar");
		Assert.assertEquals("bar", cmd.get("foo"));
		redis.close();
	}

	@Test
	public void testAsync ()
	throws Exception {
		RedisFactoryBean factoryBean = new RedisFactoryBean();
		factoryBean.setRedisUri("redis://192.168.56.201:6379/1");
		factoryBean.afterPropertiesSet();
		StatefulRedisConnection redis = (StatefulRedisConnection) factoryBean.getObject();
		RedisAsyncCommands cmd = redis.async();
		RedisFuture future = cmd.set("foo", "bar");
		future.await(5, TimeUnit.SECONDS);
		future = cmd.get("foo");
		Assert.assertEquals("bar", future.get());
		log.info("testAsync end.");
		redis.close();
	}

	@Test
	public void testExpiry ()
	throws Exception {
		RedisFactoryBean factoryBean = new RedisFactoryBean();
		factoryBean.setRedisUri("redis://192.168.56.201:6379/1");
		factoryBean.afterPropertiesSet();
		StatefulRedisConnection redis = (StatefulRedisConnection) factoryBean.getObject();
		RedisCommands cmd = redis.sync();
		cmd.setex("foo", 1, "bar");
		TimeUnit.MILLISECONDS.sleep(1500);
		Assert.assertNotEquals("bar", cmd.get("foo"));
		redis.close();
	}

	@Test
	public void testByte ()
	throws Exception {
		RedisFactoryBean factoryBean = new RedisFactoryBean();
		factoryBean.setRedisUri("redis://192.168.56.201:6379/1");
		factoryBean.setUseBinary(true);
		factoryBean.afterPropertiesSet();
		StatefulRedisConnection redis = (StatefulRedisConnection) factoryBean.getObject();
		RedisCommands cmd = redis.sync();
		cmd.set(SerializationUtils.serialize("foo"), SerializationUtils.serialize("bar"));
		byte[] bytes = (byte[]) cmd.get(SerializationUtils.serialize("foo"));
		Assert.assertEquals("bar", SerializationUtils.deserialize(bytes));
		redis.close();
	}

	@Test
	public void testByteExpiry ()
	throws Exception {
		RedisFactoryBean factoryBean = new RedisFactoryBean();
		factoryBean.setRedisUri("redis://192.168.56.201:6379/1");
		factoryBean.setUseBinary(true);
		factoryBean.afterPropertiesSet();
		StatefulRedisConnection redis = (StatefulRedisConnection) factoryBean.getObject();
		RedisCommands cmd = redis.sync();
		cmd.setex(SerializationUtils.serialize("foo"), 1, SerializationUtils.serialize("bar"));
		TimeUnit.MILLISECONDS.sleep(1500);
		byte[] bytes = (byte[]) cmd.get(SerializationUtils.serialize("foo"));
		Assert.assertNotEquals("bar", SerializationUtils.deserialize(bytes));
		redis.close();
	}
}
