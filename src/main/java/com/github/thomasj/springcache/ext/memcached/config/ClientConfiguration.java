package com.github.thomasj.springcache.ext.memcached.config;

/**
 * 
 * @author stantnks@gmail.com
 * 
 */
public class ClientConfiguration {
	// if value is these
	// types[Boolean,Byte,Character,Short,Integer,Long,Float,Double,String,Date,byte[]],suggest
	// set primitiveAsString=true
	private boolean	primitiveAsString	= true;
	// when primitiveAsString is true then charset
	private String	defaultEncoding		= "utf-8";

	public ClientConfiguration () {
	}
	public ClientConfiguration ( boolean primitiveAsString,
	String defaultEncoding ) {
		this.primitiveAsString = primitiveAsString;
		this.defaultEncoding = defaultEncoding;
	}
	/**
	 * wether - if transport object is string,
	 * improve speed by set true
	 * 
	 * @return
	 */
	public boolean isPrimitiveAsString() {
		return primitiveAsString;
	}
	/**
	 * set - if transport object is string,
	 * improve speed by set true
	 * 
	 * @param primitiveAsString
	 */
	public void setPrimitiveAsString( boolean primitiveAsString ) {
		this.primitiveAsString = primitiveAsString;
	}
	/**
	 * get - when primitiveAsString is true then
	 * charset
	 * 
	 * @return
	 */
	public String getDefaultEncoding() {
		return defaultEncoding;
	}
	/**
	 * set - when primitiveAsString is true then
	 * charset
	 * 
	 * @param defaultEncoding
	 */
	public void setDefaultEncoding( String defaultEncoding ) {
		this.defaultEncoding = defaultEncoding;
	}
	@Override
	public String toString() {
		return String.format(
			"ClientConfiguration [primitiveAsString=%s, defaultEncoding=%s]",
			primitiveAsString, defaultEncoding);
	}
}
