package com.thed.zephyr.je.event.listener;

import com.google.common.collect.Table;
import net.java.ao.Entity;

import java.util.HashMap;
import java.util.Map;

public class AuditUtils {
	
	/**
	 * 
	 * @param entityID
	 * @param entitytType
	 * @param fieldName TODO
	 * @param oldValue
	 * @return
	 */
	public static Map<String, Object> createDeleteChangePropertiesFor(Integer entityID, String entitytType, String fieldName, String oldValue){
    	Map<String, Object> changeLogProperties = new HashMap<String, Object>();
		changeLogProperties.put("ZEPHYR_ENTITY_ID", entityID);
		changeLogProperties.put("ZEPHYR_ENTITY_TYPE", entitytType);
    	changeLogProperties.put("ZEPHYR_FIELD_TYPE", "zephyr");
		changeLogProperties.put("ZEPHYR_FIELD", fieldName);	
		changeLogProperties.put("OLD_VALUE", oldValue);	
		changeLogProperties.put("NEW_VALUE", null);
		return changeLogProperties;
    }
	
	/**
	 * @param changePropertyTable
	 * @param rowKey
	 * @return
	 */
	public static Map<String, Object> createChangeLogProperties(Table<String, String, Object> changePropertyTable, String rowKey) {
		Map<String, Object> changeLogProperties = new HashMap<String, Object>(4);
		changeLogProperties.put("ZEPHYR_FIELD_TYPE", "zephyr");
		changeLogProperties.put("ZEPHYR_FIELD", rowKey);
		changeLogProperties.put("OLD_VALUE", getPropertyFromTable(changePropertyTable, "OLD", rowKey));
		changeLogProperties.put("NEW_VALUE",  getPropertyFromTable(changePropertyTable, "NEW", rowKey));
		return changeLogProperties;
	}

	/**
	 * @param changePropertyTable
	 * @param rowKey
	 * @return
	 */
	public static Object getPropertyFromTable(Table<String, String, Object> changePropertyTable, String prop, String rowKey) {
		Object propVal = changePropertyTable.row(rowKey).get(prop);
		return (propVal == null || propVal.equals("NULL")) ? null : propVal;
	}

    /**
     *
     * @param entityEvent
     * @param entityType
     * @param aoEntity
     * @param author
     * @return
     */
    public static Map<String, Object> createChangeGroupProperties(String entityEvent, String entityType, Entity aoEntity, String author, Integer issueId, Integer scheduleId, Long projectId) {
        Map<String, Object> changeGroupProperties = new HashMap<String, Object>(8);
        changeGroupProperties.put("ZEPHYR_ENTITY_ID", aoEntity.getID());
        changeGroupProperties.put("ISSUE_ID", issueId);
        changeGroupProperties.put("CYCLE_ID", -1);
        changeGroupProperties.put("SCHEDULE_ID", scheduleId);
        changeGroupProperties.put("ZEPHYR_ENTITY_TYPE", entityType);
        changeGroupProperties.put("ZEPHYR_ENTITY_EVENT", entityEvent);
        changeGroupProperties.put("PROJECT_ID", projectId);
        changeGroupProperties.put("AUTHOR", author);
        changeGroupProperties.put("CREATED", System.currentTimeMillis());
        return changeGroupProperties;
    }

}
