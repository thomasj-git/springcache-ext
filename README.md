# 说明
        扩展spring cache，支持到方法级别的缓存过期,spring只提供缓存方案，过期策略需要自己扩展实现（当然前提也是需要缓存中间件支持过期机制）
    提供了guava、memached、redis实现方案，其中memcacached和redis方法级别的缓存过期，guava只能支持全局。

# 使用
## 构建安装到本地仓库
    # 获取代码
    git clone https://github.com/thomasj-git/springcache-ext.git
    cd springcache-ext
    mvn -DskipTests=true package
       
    # 本地安装
    mvn install:install-file  \
    -Dfile=target\springcache-ext-1.0.jar  \
    -DgroupId=com.github.thomasj  \
    -DartifactId=springcache-ext \
    -Dversion=1.0 -Dpackaging=jar
       
    # 安装远程私库
    mvn deploy:deploy-file \
    -DgroupId=com.github.thomasj \
    -DartifactId=springcache-ext \
    -Dversion=1.0 \
    -Dpackaging=jar -Dfile=target\springcache-ext-1.0.jar \
    -Durl=${远程私库地址} \
    -DrepositoryId=${仓库ID}
## maven依赖最新版
    <dependency>
        <groupId>com.github.thomasj</groupId>
        <artifactId>springcache-ext</artifactId>
        <version>1.0</version>
    </dependency>

## 使用
    @Bean
    public CacheManager cacheManager ()
    throws Exception {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        MemcachedCacheFactoryBean factoryBean = new MemcachedCacheFactoryBean();
        factoryBean.setPoolConfig(new PoolConfiguration("192.168.56.201:11201"));
        factoryBean.afterPropertiesSet();
        MemCachedClient client = factoryBean.getObject();
        MemcachedCache cache = new MemcachedCache(client);
        cacheManager.setCaches(ImmutableList.of(cache));
        return cacheManager;
    }
        	
    @Bean
    public KeyGenerator apiKeyGenerator () {
        return new ParamsKeyGenerator();
    }
      
    @RequestMapping (path = "/hello", method = RequestMethod.GET)
    @Cacheable (value = "default", keyGenerator = "apiKeyGenerator", unless = "#result==null")
    @Expiry (unit = TimeUnit.SECONDS, time = 5L)
    public String hello (@RequestParam (name = "name", defaultValue = "World") String name) {
        return "Hello " + name;
    }