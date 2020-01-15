# 说明
        扩展spring cache，支持到方法级别的缓存过期,spring只提供缓存方案，过期策略需要自己扩展实现（当然前提也是需要缓存中间件支持过期机制）
    提供了guava、juc、mmc、redis实现方案，其中juc、mmc、redis方法级别的缓存过期，guava只能支持全局。

# 使用
## 获取安装
```Bash
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
```
## maven依赖最新版
```xml
<dependency>
    <groupId>com.github.thomasj</groupId>
    <artifactId>springcache-ext</artifactId>
    <version>1.0</version>
</dependency>
```

## 使用
```Java
// 以下CacheManager只需申明一种即可
  
// 基于LinkedHashMap和DelayQueue实现的缓存，无需依赖缓存中间件(缺点是只能支持进程级别)
@Bean
public CacheManager cacheManager ()
throws Exception {
    return new CacheConfig();
}
   
// 使用memcached作为缓存
@Bean
public CacheManager cacheManager ()
throws Exception {
   return new CacheConfig(){
   	    protected Collection<Cache> buildCaches(){
            MemcachedCacheFactoryBean factoryBean = new MemcachedCacheFactoryBean();
            factoryBean.setPoolConfig(new PoolConfiguration("192.168.56.201:11201"));
            factoryBean.afterPropertiesSet();
            MemCachedClient client = factoryBean.getObject();
            MemcachedCache cache = new MemcachedCache(client);
            return ImmutableList.of(cache);
   	    }
   };
}
  
// 使用redis作为缓存
@Bean
public CacheManager cacheManager ()
throws Exception {
    return new CacheConfig(){
       	    protected Collection<Cache> buildCaches(){
                RedisFactoryBean factoryBean = new RedisFactoryBean();
                factoryBean.setRedisUri("redis://192.168.56.201:6379/1");
                factoryBean.setUseBinary(true);
                factoryBean.afterPropertiesSet();
                StatefulRedisConnection nativeRedis = factoryBean.getObject();
                RedisCache cache=new RedisCache(nativeRedis);
                return ImmutableList.of(cache);
       	    }
       };
}
  
// 缓存key的创建策略（类全限定名+@Expiry.methodKey）
@Bean
public KeyGenerator apiKeyGenerator () {
    return new ParamsKeyGenerator();
}
  
// 接口返回会被缓存30秒
@Version
@RequestMapping (path = "/hello/{v}", method = RequestMethod.GET)
@ResponseBody
@Cacheable (value = "default", keyGenerator = "apiKeyGenerator")
@Expiry (time=30, unit=TimeUnit.SECONDS, methodKey = "hello")
public String hello (@ApiParam (required = true, defaultValue = "v0.1.0") @PathVariable String v) {
    String uuid = UUID.randomUUID().toString();
    log.info("创建随机串: {}", uuid);
    return uuid;
}
  
// 手工立即清除上面接口的缓存
@Version
@RequestMapping (path = "/hello/{v}", method = RequestMethod.DELETE)
@ResponseBody
@CacheEvict (value = "default", keyGenerator = "apiKeyGenerator")
@Expiry (methodKey = "hello")
public String evictHello (@ApiParam (required = true, defaultValue = "v0.1.0") @PathVariable String v) {
    log.info("调用缓存清除");
    return "删除缓存成功";
}
  
//开启springcache功能
@EnableCaching
```