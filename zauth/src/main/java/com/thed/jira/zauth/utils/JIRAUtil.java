package com.thed.jira.zauth.utils;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.atlassian.core.util.map.EasyMap;
import com.google.common.collect.Sets;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;

public class JIRAUtil {
	
	private static final Logger log = Logger.getLogger(JIRAUtil.class);
	public static final String ZAUTH_ENTITY_NAME = "ZAuthPlugin";
	public static final Long ZAUTH_ENTITY_ID = new Long(1l);
	
	private static Set<String> cachedWhileList = null;

	/**
	 * 
	 * @param entityName
	 * @param entityId
	 * @return
	 */
	public static PropertySet getPropertySet(String entityName, Long entityId) {
		PropertySet ofbizPs = PropertySetManager.getInstance("ofbiz", buildPropertySet(entityName, entityId));

		Map args = EasyMap.build("PropertySet", ofbizPs, "bulkload", Boolean.TRUE);
		return PropertySetManager.getInstance("cached", args);
	}

	private static Map<String, ? extends Serializable> buildPropertySet(String entityName, Long entityId) {
		return EasyMap.build("delegator.name", "default", "entityName", entityName, "entityId", entityId);
	}

	/**
	 * Fetch whitelist from cache/DB
	 * @return
	 */
	public static Set<String> getWhiteList() {
		if(cachedWhileList == null){
			String whiteListString = getPropertySet(ZAUTH_ENTITY_NAME, ZAUTH_ENTITY_ID).getText("whiteList");
			ObjectMapper mapper = new ObjectMapper();
			Set<String> whiteList = Sets.newHashSet();
			try {
				if(!StringUtils.isBlank(whiteListString))
					whiteList = mapper.readValue(whiteListString, new TypeReference<Set<String>>() {});
			} catch (Exception e) {
				log.fatal("Unable to fetch whiteList", e);
			}
			cachedWhileList = whiteList;
		}
		return cachedWhileList;
	}
	
	/**
	 * Update white list in DB
	 * @param whiteList
	 */
	public static void saveWhiteList(Set<String> whiteList){
		ObjectMapper mapper = new ObjectMapper();
		String whiteListString = "";
		try {
			whiteListString = mapper.writeValueAsString(whiteList);
		} catch (Exception e) {
			log.fatal("Unable to save whiteList", e);
		}
		getPropertySet(ZAUTH_ENTITY_NAME, ZAUTH_ENTITY_ID).setText("whiteList", whiteListString);
		cachedWhileList = whiteList;
	}

	/**
	 * 
	 * @param ip
	 * @return
	 */
	public static Boolean addToWhiteList(String ip) {
		Set<String> whiteList = getWhiteList();
		Boolean response = whiteList.add(ip);
		if(response){
			saveWhiteList(whiteList);
		}else{
			log.warn("Ip already found in DB. It may have already been added");
		}
		return response;
	}

	public static Boolean removeFromWhiteList(String ip) {
		Set<String> whiteList = getWhiteList();
		Boolean response = whiteList.remove(ip);
		if(response){
			saveWhiteList(whiteList);
		}else{
			log.warn("Ip adress not found in DB. It may have already been removed");
		}
		return response;
	}
}
