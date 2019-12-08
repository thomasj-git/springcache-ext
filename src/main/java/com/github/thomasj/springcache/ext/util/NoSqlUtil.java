package com.github.thomasj.springcache.ext.util;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author stantnks@gmail.com
 * 
 */
public class NoSqlUtil {

	public static boolean strEmpty (String str) {
		return StringUtils.isBlank(str) || "null".equalsIgnoreCase(str.trim());
	}

	public static boolean objEmpty (Object object) {
		return strEmpty(String.valueOf(object));
	}
}
