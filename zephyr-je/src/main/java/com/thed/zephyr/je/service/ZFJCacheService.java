package com.thed.zephyr.je.service;

public interface ZFJCacheService {
	void createOrUpdateCache(String key, Object value);
	Object getCacheByKey(String key, Object defaultValue);
	void removeCacheByKey(String cacheKey);
	boolean getCacheByWildCardKey(String key);
	boolean removeAllCycleCacheByWildCard();
}
