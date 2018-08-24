/**
 * 
 */
package com.thed.zephyr.je.service.impl;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettings;
import com.atlassian.cache.CacheSettingsBuilder;
import com.thed.zephyr.je.service.ZFJCacheService;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author niravshah
 *
 */
public class ZFJCacheServiceImpl implements ZFJCacheService {
	private CacheManager cacheManager;

	private static final CacheSettings ZFJ_TASKMAP_CACHE_SETTINGS =
			new CacheSettingsBuilder().unflushable().replicateViaCopy().build();

	
	public ZFJCacheServiceImpl(CacheManager cacheManager) {
		this.cacheManager=cacheManager;
	}

	/* (non-Javadoc)
	 * @see com.thed.zephyr.je.service.ZFJCacheService#createOrUpdateCache(java.lang.String, java.lang.Object)
	 */
	@Override
	public void createOrUpdateCache(String key, Object value) {
        Cache<String, Object> cache = cacheManager.getCache("ZFJ-Cache",null,ZFJ_TASKMAP_CACHE_SETTINGS);
        if(cache != null) {
        	cache.put(key, value);
        }
	}

	/* (non-Javadoc)
	 * @see com.thed.zephyr.je.service.ZFJCacheService#getCacheByKey(java.lang.Class, java.lang.String, java.lang.Object)
	 */
	@Override
	public Object getCacheByKey(String key, Object defaultValue) {
        Cache<String, Object> cache = cacheManager.getCache("ZFJ-Cache",null,ZFJ_TASKMAP_CACHE_SETTINGS);
        if(cache != null) {
        	return cache.get(key);
        }
		return defaultValue;
	}

	/* (non-Javadoc)
 * @see com.thed.zephyr.je.service.ZFJCacheService#getCacheByWildCardKey(java.lang.String)
 */
	@Override
	public boolean getCacheByWildCardKey(String key) {
		Cache<String, Object> cache = cacheManager.getCache("ZFJ-Cache",null,ZFJ_TASKMAP_CACHE_SETTINGS);
		if(cache != null) {
			for (String cacheKey : cache.getKeys()) {
				if (StringUtils.startsWith(cacheKey, key)) {
					return true;
				}
			}
		}
		return false;
	}



	/* (non-Javadoc)
 * @see com.thed.zephyr.je.service.ZFJCacheService#removeAllCycleCacheByWildCard()
 */
	@Override
	public boolean removeAllCycleCacheByWildCard() {
		Cache<String, Object> cache = cacheManager.getCache("ZFJ-Cache",null,ZFJ_TASKMAP_CACHE_SETTINGS);
		if(cache != null) {
			for (String cacheKey : cache.getKeys()) {
				if (StringUtils.startsWith(cacheKey, "FOLDER_ID_PROGRESS_CHK")) {
					removeCacheByKey(cacheKey);
				}
			}
		}
		return false;
	}




	@Override
	public void removeCacheByKey(String cacheKey) {
		Cache<String, Object> cache = cacheManager.getCache("ZFJ-Cache",null,ZFJ_TASKMAP_CACHE_SETTINGS);
		if(cache != null) {
			cache.remove(cacheKey);
		}
	}

}
