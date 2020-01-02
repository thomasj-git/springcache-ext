package com.github.thomasj.springcache.ext.key;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import com.github.thomasj.springcache.ext.annotation.Expiry;

/**
 * @author stantnks@gmail.com
 */
public class ApiKeyGenerator extends SimpleKeyGenerator {

	@Override
	public Object generate (Object target, Method method, Object ... params) {
		return createKey(true, target, method, params);
	}

	public static Object createKey (boolean hasMethod, Object target, Method method, Object[] params) {
		Expiry expiry = AnnotationUtils.findAnnotation(method, Expiry.class);
		StringBuilder sb = new StringBuilder(200);
		sb.append(target.getClass().getName());
		if (hasMethod) {
			sb.append(method.getName());
		}
		else if (expiry != null && expiry.methodKey() != null) {
			sb.append(expiry.methodKey());
		}
		sb.append("<");
		Object param;
		for (int i = 0; i < params.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			param = params[i];
			if (param == null) {
				sb.append("");
			}
			else if (ClassUtils.isPrimitiveArray(param.getClass()) || ClassUtils.isPrimitiveWrapperArray(param.getClass())) {
				int len = Array.getLength(param);
				sb.append("[");
				for (int i1 = 0; i1 < len; i1++) {
					if (i1 > 0) {
						sb.append(",");
					}
					sb.append(Array.get(param, i1));
				}
				sb.append("]");
			}
			else if (param.getClass().isArray()) {
				Object[] array = (Object[]) param;
				for (int i1 = 0; i1 < array.length; i1++) {
					if (i1 > 0) {
						sb.append(",");
					}
					sb.append(array[i1]);
				}
			}
			else {
				sb.append(param);
			}
		}
		sb.append(">");
		String key = sb.toString();
		if (expiry != null) {
			ExpiryKey expiryKey = new ExpiryKey(key, expiry.unit(), expiry.time());
			return expiryKey;
		}
		return key;
	}
}
