package com.github.thomasj.springcache.ext.memcached;

import java.util.Arrays;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import com.danga.MemCached.MemCachedClient;
import com.danga.MemCached.SockIOPool;
import com.github.thomasj.springcache.ext.memcached.config.ClientConfiguration;
import com.github.thomasj.springcache.ext.memcached.config.PoolConfiguration;
import com.schooner.MemCached.TransCoder;

import lombok.Setter;

/**
 * @author stantnks@gmail.com
 */
public class MemcachedCacheFactoryBean implements FactoryBean, InitializingBean {

	@Setter
	private ClientConfiguration	clientConfig;

	@Setter
	private PoolConfiguration	poolConfig;

	private SockIOPool			sip;

	@Setter
	private boolean				useBinary	= true;

	@Setter
	private TransCoder			transCoder;

	private MemCachedClient		target;

	@Override
	public void afterPropertiesSet ()
	throws Exception {
		if (poolConfig == null) {
			throw new NullPointerException();
		}
		// pool configuration
		String pn = Arrays.toString(poolConfig.getServers());
		sip = SockIOPool.getInstance(pn);
		sip.setAliveCheck(poolConfig.isAliveCheck());
		sip.setFailback(poolConfig.isFailback());
		sip.setFailover(poolConfig.isFailover());
		sip.setInitConn(poolConfig.getInitConn());
		sip.setMaintSleep(poolConfig.getMaintSleep());
		sip.setMaxConn(poolConfig.getMaxConn());
		sip.setMaxIdle(poolConfig.getMaxIdle());
		sip.setMinConn(poolConfig.getMinConn());
		sip.setNagle(poolConfig.isNagle());
		sip.setServers(poolConfig.getServers());
		sip.setSocketConnectTO(poolConfig.getSocketConnectTO());
		sip.setSocketTO(poolConfig.getSocketTO());
		sip.setWeights(poolConfig.getWeights());
		sip.initialize();
		target = new MemCachedClient(pn, useBinary);
		if (clientConfig != null) {
			target.setPrimitiveAsString(clientConfig.isPrimitiveAsString());
			target.setDefaultEncoding(clientConfig.getDefaultEncoding());
		}
		if (transCoder != null) {
			target.setTransCoder(transCoder);
		}
	}

	@Nullable
	@Override
	public MemCachedClient getObject ()
	throws Exception {
		return this.target;
	}

	@Nullable
	@Override
	public Class<?> getObjectType () {
		return MemCachedClient.class;
	}

	@Override
	public boolean isSingleton () {
		return true;
	}
}
