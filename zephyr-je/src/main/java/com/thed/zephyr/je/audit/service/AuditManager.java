package com.thed.zephyr.je.audit.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.google.common.collect.Table;
import com.thed.zephyr.je.audit.model.ChangeZJEGroup;
import com.thed.zephyr.je.audit.model.ChangeZJEItem;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

@Transactional
public interface AuditManager {

    /**
     * Saves change log pertaining to a Zephyr entity
     * @param zjeGroupProperties the object to be saved
     * @param changePropertyTable
     */
    void saveZephyrChangeLog(Map<String, Object> zjeGroupProperties, Table<String, String, Object> changePropertyTable);
	
	/**
	 * This method retrieves entire change logs for all ZEPHYR ENTITY Types.
	 * @return List of ChangeZJEItems
	 */
	List<ChangeZJEItem> getZephyrChangeLogs();
	
	/**
	 * This method retrieves change logs for one/all ZEPHYR ENTITY Types,
	 * for a given filter.
	 * @param filters A filter Map ( i.e. fromTime-toTime, zephyrEntityType etc)
	 * @param offset
	 * @param limit
	 * @return List of ChangeZJEItems for the given filters
	 */
	List<ChangeZJEItem> getZephyrChangeLogs(Map<String, Object> filters,Integer offset,Integer limit);	

    /**
     * Removes the change logs for a given Zephyr Entity.
     * @param zjeGroupProperties
     * @param zjeItemProperties
     */
    void removeZephyrChangeLogs(Map<String, Object> zjeGroupProperties, Map<String, Object> zjeItemProperties);

	/**
	 * This method retrieves any existing ChangeZJEGroup.ID for given input params.
	 * @param zephyrEntityId
	 * @param zephyrEntityType
	 * @param zephyrEntityEvent
	 * @return ChangeZJEGroup
	 */
	ChangeZJEGroup getZJEGroupId(Integer zephyrEntityId, String zephyrEntityType,
			String zephyrEntityEvent);

	/**
	 * This method retrieves count of all change logs for a given filter param.
	 * @return count of all ChangeZJEItems for a given Entity Event
	 */
	Integer getChangeItemCount(Map<String, Object> filters);
	
	ChangeZJEGroup[] getZFJChangeLogsByDate(long dateTime, String eventType, OptionalLong projectId);
}
