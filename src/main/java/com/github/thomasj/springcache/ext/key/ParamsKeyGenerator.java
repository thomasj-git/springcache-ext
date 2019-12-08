package com.github.thomasj.springcache.ext.key;

import java.lang.reflect.Method;

import org.springframework.cache.interceptor.SimpleKeyGenerator;

/**
 * @author stantnks@gmail.com
 */
public class ParamsKeyGenerator extends SimpleKeyGenerator {

	@Override
	public Object generate (Object target, Method method, Object ... params) {
		return ApiKeyGenerator.createKey(false, target, method, params);
	}
}
