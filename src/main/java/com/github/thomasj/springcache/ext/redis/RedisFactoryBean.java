package com.github.thomasj.springcache.ext.redis;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import lombok.Setter;

/**
 * @author stantnks@gmail.com
 */
public class RedisFactoryBean implements FactoryBean, InitializingBean {

	/**
	 * <code>
	 * redis://[password@]host[:port][/databaseNumber]
	 * rediss://[password@]host[:port][/databaseNumber]
	 * </code>
	 */
	@Setter
	private String					redisUri	= "redis://localhost:6379/1";

	@Setter
	private String					clientName;

	@Setter
	private boolean					useBinary	= false;

	@Setter
	private RedisCodec				codec;

	private RedisClient				redis;

	private StatefulRedisConnection	target;

	@Nullable
	@Override
	public StatefulRedisConnection getObject ()
	throws Exception {
		return this.target;
	}

	@Nullable
	@Override
	public Class<?> getObjectType () {
		return StatefulRedisConnection.class;
	}

	@Override
	public void afterPropertiesSet ()
	throws Exception {
		RedisURI uri = RedisURI.create(redisUri);
		if (clientName != null) {
			uri.setClientName(clientName);
		}
		this.redis = RedisClient.create(uri);
		if (this.codec != null) {
			this.target = this.redis.connect(this.codec);
		}
		else {
			if (this.useBinary) {
				this.target = this.redis.connect(new ByteArrayCodec());
			}
			else {
				this.target = this.redis.connect();
			}
		}
		this.target.setAutoFlushCommands(true);
	}
}
