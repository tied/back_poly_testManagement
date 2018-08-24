package com.thed.zephyr.util;

import java.util.Map;

import com.google.common.base.Optional;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;


public class ZCollectionUtils {
	protected static final Logger log = Logger.getLogger(ZCollectionUtils.class);
	
	public static String getAsString(Map<String, Object> params, String key) {
		Object val = params.get(key);
		if(val != null){
			return String.valueOf(val);
		}
		return null;
	}
	
	public static Boolean getAsBoolean(Map<String, Object> params, String key, Boolean defaultVal) {
		Object val = params.get(key);
		if(val != null){
			if(val instanceof Boolean){
				return (Boolean)val;
			}
			if(val instanceof String){
				return new Boolean((String)val);
			}
		}
		return defaultVal;
	}
	
	public static Integer getAsInteger(Map<String, Object> params, String key) {
			try{
				if(params.get(key) == null || StringUtils.equalsIgnoreCase("", String.valueOf(params.get(key)))) {
					return null;
				}
				return Integer.valueOf(String.valueOf(params.get(key)));
			}catch(Exception ex){
				log.error("Error in fetching/parsing value from map" + key, ex);
			}
		return null;
	}

	
	public static Long getAsLong(Map<String, Object> params, String key) {
		try{
			if(params.get(key) == null || StringUtils.equalsIgnoreCase("", String.valueOf(params.get(key)))) {
				return null;
			}
			return Long.parseLong(String.valueOf(params.get(key)));
		}catch(Exception ex){
			log.error("Error in fetching/parsing value from map" + key, ex);
		}
		return null;
	}

	public static Optional<Integer> getAsOptionalInteger(Object scheduleIdObject) {
		if(scheduleIdObject == null){
			return Optional.absent();
		}

		if(scheduleIdObject instanceof Number){
			return Optional.of(((Number) scheduleIdObject).intValue());
		}else{
			return Optional.of(NumberUtils.toInt(scheduleIdObject.toString()));
		}
	}
}
