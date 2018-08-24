package com.thed.zephyr.util;

public class ZephyrCacheControl {

	// prevent instantiation
	private ZephyrCacheControl()
	{
		// empty
	}
	
	private static final int ONE_YEAR = 60 * 60 * 24 * 365;

	/**
	 * Returns a CacheControl with noStore and noCache set to true.
	 *
	 * @return a CacheControl
	 */
	public static javax.ws.rs.core.CacheControl never()
	{
		javax.ws.rs.core.CacheControl cacheNever = new javax.ws.rs.core.CacheControl();
		cacheNever.setNoStore(true);
		cacheNever.setNoCache(true);

		return cacheNever;
	}

	/**
	 * Returns a CacheControl with a 1 year limit. Effectively forever.
	 *
	 * @return a CacheControl
	 */
	public static javax.ws.rs.core.CacheControl forever()
	{
		javax.ws.rs.core.CacheControl cacheForever = new javax.ws.rs.core.CacheControl();
		cacheForever.setPrivate(false);
		cacheForever.setMaxAge(ONE_YEAR);

		return cacheForever;
	}
}
