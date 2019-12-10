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
   	    protected Cache buildCache(){
            MemcachedCacheFactoryBean factoryBean = new MemcachedCacheFactoryBean();
            factoryBean.setPoolConfig(new PoolConfiguration("192.168.56.201:11201"));
            factoryBean.afterPropertiesSet();
            MemCachedClient client = factoryBean.getObject();
            MemcachedCache cache = new MemcachedCache(client);
            return cache;
   	    }
   };
}
  
// 使用redis作为缓存
@Bean
public CacheManager cacheManager ()
throws Exception {
    return new CacheConfig(){
       	    protected Cache buildCache(){
                RedisFactoryBean factoryBean = new RedisFactoryBean();
                factoryBean.setRedisUri("redis://192.168.56.201:6379/1");
                factoryBean.setUseBinary(true);
                factoryBean.afterPropertiesSet();
                StatefulRedisConnection nativeRedis = factoryBean.getObject();
                RedisCache cache=new RedisCache(nativeRedis);
                return cache;
       	    }
       };
}
  
// 缓存key的创建策略（方法全限定名+参数）
@Bean
public KeyGenerator apiKeyGenerator () {
    return new ParamsKeyGenerator();
}
  
// 方法上申明缓存策略（使用@Cacheable配合@Expiry）
@RequestMapping (path = "/hello", method = RequestMethod.GET)
@Cacheable (value = "default", keyGenerator = "apiKeyGenerator", unless = "#result==null")
@Expiry (unit = TimeUnit.SECONDS, time = 5L)
public String hello (@RequestParam (name = "name", defaultValue = "World") String name) {
    return "Hello " + name;
}
  
//开启springcache功能
@EnableCaching
```