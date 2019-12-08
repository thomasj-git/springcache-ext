package com.github.thomasj.springcache.ext.key;

import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author stantnks@gmail.com
 */
@AllArgsConstructor
@Data
public class ExpiryKey {

	private Object		key;

	private TimeUnit	unit;

	private long		expiry;

	@Override
	public String toString () {
		return "<key:" + key + ", unit:" + unit + ", expiry: " + expiry + ">";
	}
}
