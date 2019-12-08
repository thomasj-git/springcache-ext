package com.github.thomasj.springcache.ext.memcached.config;

import java.util.Arrays;

/**
 * 
 * @author stantnks@gmail.com
 * 
 */
public class PoolConfiguration {
	// cache server list
	private String[]	servers;
	// each server weights, must be same
	private Integer[]	weights;
	// init connection
	private int			initConn		= 20;
	// min connection
	private int			minConn			= 10;
	// max connection
	private int			maxConn			= 100;
	// keep connection with pool max timeout
	private long		maxIdle			= 24 * 60 * 60 * 1000;
	// monitor thread sleep time
	private long		maintSleep		= 0;
	// nagle arithmetic
	private boolean		nagle			= false;
	// read wait timeout, 这个值设置超过1s没有意思，最大就1s
	private int			socketTO		= 1000;
	// connect wait timeout
	private int			socketConnectTO	= 100;
	// every communicat wether check cache alive
	private boolean		aliveCheck		= false;
	// wether still useful after cache server
	// restart
	private boolean		failback		= true;
	// lookup available other server after cur
	// server down
	private boolean		failover		= true;

	public PoolConfiguration ( String[] servers ) {
		this.servers = servers;
		defaultWeights();
	}
	public PoolConfiguration ( String serverStrings ) {
		this(serverStrings.split("[,;]"));
	}
	// set default servers weight
	private void defaultWeights() {
		int size = this.servers.length;
		Integer[] ws = new Integer[size];
		for (int i = 0;i < size;i++)
			ws[i] = 1;
		this.weights = ws;
	}
	/**
	 * get server list
	 * 
	 * @return
	 */
	public String[] getServers() {
		return servers;
	}
	/**
	 * set server list
	 * 
	 * @param servers
	 */
	public void setServers( String[] servers ) {
		this.servers = servers;
	}
	/**
	 * get server weight
	 * 
	 * @return
	 */
	public Integer[] getWeights() {
		return weights;
	}
	/**
	 * set server weight
	 * 
	 * @param weights
	 */
	public void setWeights( Integer[] weights ) {
		this.weights = weights;
	}
	/**
	 * get init connection number
	 * 
	 * @return
	 */
	public int getInitConn() {
		return initConn;
	}
	/**
	 * set init connection number
	 * 
	 * @param initConn
	 */
	public void setInitConn( int initConn ) {
		this.initConn = initConn;
	}
	/**
	 * get min connection number
	 * 
	 * @return
	 */
	public int getMinConn() {
		return minConn;
	}
	/**
	 * set min connection number
	 * 
	 * @param minConn
	 */
	public void setMinConn( int minConn ) {
		this.minConn = minConn;
	}
	/**
	 * get max connection number
	 * 
	 * @return
	 */
	public int getMaxConn() {
		return maxConn;
	}
	/**
	 * set max connection number
	 * 
	 * @param maxConn
	 */
	public void setMaxConn( int maxConn ) {
		this.maxConn = maxConn;
	}
	/**
	 * get keep connection with pool max timeout
	 * 
	 * @return
	 */
	public long getMaxIdle() {
		return maxIdle;
	}
	/**
	 * set keep connection with pool max timeout
	 * 
	 * @param maxIdle
	 */
	public void setMaxIdle( long maxIdle ) {
		this.maxIdle = maxIdle;
	}
	/**
	 * get monitor thread sleep time
	 * 
	 * @return
	 */
	public long getMaintSleep() {
		return maintSleep;
	}
	/**
	 * set monitor thread sleep time
	 * 
	 * @param maintSleep
	 */
	public void setMaintSleep( long maintSleep ) {
		this.maintSleep = maintSleep;
	}
	/**
	 * wether nagle arithmetic
	 * 
	 * @return
	 */
	public boolean isNagle() {
		return nagle;
	}
	/**
	 * set nagle arithmetic
	 * 
	 * @param nagle
	 */
	public void setNagle( boolean nagle ) {
		this.nagle = nagle;
	}
	/**
	 * get read wait timeout
	 * 
	 * @return
	 */
	public int getSocketTO() {
		return socketTO;
	}
	/**
	 * set read wait timeout
	 * 
	 * @param socketTO
	 */
	public void setSocketTO( int socketTO ) {
		this.socketTO = socketTO;
	}
	/**
	 * get connect wait timeout
	 * 
	 * @return
	 */
	public int getSocketConnectTO() {
		return socketConnectTO;
	}
	/**
	 * set connect wait timeout
	 * 
	 * @param socketConnectTO
	 */
	public void setSocketConnectTO( int socketConnectTO ) {
		this.socketConnectTO = socketConnectTO;
	}
	/**
	 * every communicat wether check cache alive
	 * 
	 * @return
	 */
	public boolean isAliveCheck() {
		return aliveCheck;
	}
	/**
	 * set every communicat wether check cache
	 * alive
	 * 
	 * @param aliveCheck
	 */
	public void setAliveCheck( boolean aliveCheck ) {
		this.aliveCheck = aliveCheck;
	}
	/**
	 * wether still useful after cache server
	 * restart
	 * 
	 * @return
	 */
	public boolean isFailback() {
		return failback;
	}
	/**
	 * set wether still useful after cache server
	 * restart
	 * 
	 * @param failback
	 */
	public void setFailback( boolean failback ) {
		this.failback = failback;
	}
	/**
	 * wether lookup available other server after
	 * cur server down
	 * 
	 * @return
	 */
	public boolean isFailover() {
		return failover;
	}
	/**
	 * set wether lookup available other server
	 * after cur server down
	 * 
	 * @param failover
	 */
	public void setFailover( boolean failover ) {
		this.failover = failover;
	}
	@Override
	public String toString() {
		return String.format(
			"PoolConfiguration [servers=%s, weights=%s, initConn=%s, minConn=%s, maxConn=%s, maxIdle=%s, maintSleep=%s, nagle=%s, socketTO=%s, socketConnectTO=%s, aliveCheck=%s, failback=%s, failover=%s]",
			Arrays.toString(servers), Arrays.toString(weights), initConn,
			minConn, maxConn, maxIdle, maintSleep, nagle, socketTO,
			socketConnectTO, aliveCheck, failback, failover);
	}
	public static void main( String[] args ) {
		String s = "x,y;z";
		System.out.println(Arrays.toString(s.split("[,;]")));
	}
}
