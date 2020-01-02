package com.github.thomasj.springcache.ext.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author stantnks@gmail.com
 */
@Target ({ElementType.METHOD})
@Retention (RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Expiry {

	TimeUnit unit() default TimeUnit.MILLISECONDS;

	long time() default 300000L;

	String methodKey() default "";
}
