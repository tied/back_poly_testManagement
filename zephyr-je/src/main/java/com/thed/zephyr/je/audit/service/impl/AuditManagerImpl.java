package com.thed.zephyr.je.audit.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.google.common.collect.Table;
import com.thed.zephyr.je.audit.model.ChangeZJEGroup;
import com.thed.zephyr.je.audit.model.ChangeZJEItem;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.event.EntityType;
import com.thed.zephyr.je.event.listener.AuditUtils;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import net.java.ao.Query;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class abstracts Audit/Change logs "ao" operations.
 * (Create, Read, Delete Audit/Change logs).   
 * @author mukul
 *
 */
public class AuditManagerImpl implements AuditManager {
	protected final Logger log = Logger.getLogger(AuditManagerImpl.class);

	private final ActiveObjects ao;
    private final IssueManager issueManager;

	public AuditManagerImpl(ActiveObjects ao, IssueManager issueManager) {
		this.ao = checkNotNull(ao);
        this.issueManager = issueManager;
    }

    @Override
    public void saveZephyrChangeLog(Map<String, Object> zjeGroupProperties, Table<String, String, Object> changePropertyTable) {
        log.debug("# AuditManagerImpl.saveZephyrChangeLog() starts");
        ChangeZJEGroup changeZJEGroup = ao.create(ChangeZJEGroup.class, zjeGroupProperties);
        try{
            log.debug("# saving changeZJEItem item");
            for (String rowKey : changePropertyTable.rowKeySet()) {
                Map<String, Object> changeLogProperties = AuditUtils.createChangeLogProperties(changePropertyTable, rowKey);
                ChangeZJEItem changeZJEItem = ao.create(ChangeZJEItem.class, changeLogProperties);
                changeZJEItem.setChangeZJEGroup(changeZJEGroup);
                changeZJEItem.save();
            }
        } catch (Exception e) {
            log.error("# AuditManagerImpl.saveZephyrChangeLog() : Error saving Change Logs. Caused by : ",e);
        }
        log.debug("# AuditManagerImpl.saveZephyrChangeLog() ends");
    }

    @Override
	public List<ChangeZJEItem> getZephyrChangeLogs() {
		ChangeZJEItem[] changeZJEItems = null;
		changeZJEItems = ao.find(ChangeZJEItem.class);
		return Arrays.asList(changeZJEItems);
	}

	@Override
	public List<ChangeZJEItem> getZephyrChangeLogs(Map<String, Object> filters, Integer offset, Integer limit) {
		Set<String> filterSet = filters.keySet();
		List<String> whereClauses = new ArrayList<String>();
		List<Object> inputParams = new ArrayList<Object>();
		String orderByClause = "CREATED DESC";
		if(offset == null || offset == -1) {
			offset = -1;
			limit = -1;
		}

		for(String filterKey : filterSet) {
			//String filterValue = ZCollectionUtils.getAsString(filters,filterKey);
            if(!filterKey.equalsIgnoreCase("ORDER_BY")) {
                Object filterValue = filters.get(filterKey);
                if(filterSet.contains("GADGET")) {
                    if (StringUtils.equalsIgnoreCase(filterKey, "CREATED")) {
                        Calendar creationDate = Calendar.getInstance();
                        creationDate.setTime(new Date((Long) filterValue));
                        creationDate.add(Calendar.DATE, 1);
                        whereClauses.add("changeZJEGroup.CREATED >= ? AND changeZJEGroup.CREATED < " + creationDate.getTime().getTime());
                        inputParams.add(filterValue);
                    } else if (StringUtils.equalsIgnoreCase(filterKey, "CYCLE")) {
                        String[] cycles = (String[]) filterValue;
                        String ques = StringUtils.repeat("?", ",", cycles.length);
                        List<Integer> finalArray = Stream.of(cycles).map(Integer::parseInt).collect(Collectors.toList());
                        whereClauses.add("changeZJEGroup.CYCLE_ID IN (" + ques + ")");
                        inputParams.addAll(finalArray);
                    } else if (StringUtils.equalsIgnoreCase(filterKey, "ISSUE_ID")) {
                        Set<String> issues = (HashSet) filterValue;
                        String ques = StringUtils.repeat("?", ",", issues.size());
                        List<Integer> finalArray = Stream.of(issues.toArray(new String[issues.size()])).map(Integer::parseInt).collect(Collectors.toList());
                        whereClauses.add("changeZJEGroup.ISSUE_ID IN (" + ques + ")");
                        inputParams.addAll(finalArray);
                    } else if (StringUtils.equalsIgnoreCase(filterKey, "ZEPHYR_ENTITY_EVENT")) {
                        List<String> events = (List) filterValue;
                        String ques = StringUtils.repeat("?", ",", events.size());
                        whereClauses.add("changeZJEGroup.ZEPHYR_ENTITY_EVENT IN (" + ques + ")");
                        inputParams.addAll(events);
                    } else if (StringUtils.equalsIgnoreCase(filterKey, "ZEPHYR_FIELD")) {
                        whereClauses.add("changeItem." + filterKey + " = ?");
                        inputParams.add(filterValue);
                    } else if (!StringUtils.equalsIgnoreCase(filterKey, "GADGET")){
                        whereClauses.add("changeZJEGroup." + filterKey + " = ?");
                        inputParams.add(filterValue);
                    }
                } else {
                    if (StringUtils.equalsIgnoreCase(filterKey, "ZEPHYR_ENTITY_EVENT")) {
                        String value = (String) filterValue;
                        if(StringUtils.contains(value, ",")) {
                            String[] entityEventType = value.split(",");
                            String ques = StringUtils.repeat("?", ",", entityEventType.length);
                            whereClauses.add("changeZJEGroup.ZEPHYR_ENTITY_EVENT IN (" + ques + ")");
                            inputParams.addAll(Arrays.asList(entityEventType));
                        }else {
                            whereClauses.add("changeZJEGroup." + filterKey + " = ?");
                            inputParams.add(filterValue);
                        }
                    }else if (StringUtils.equalsIgnoreCase(filterKey, "ZEPHYR_ENTITY_TYPE")) {
                        String value = (String) filterValue;
                        if(StringUtils.contains(value, ",")) {
                            String[] entityEventType = value.split(",");
                            String ques = StringUtils.repeat("?", ",", entityEventType.length);
                            whereClauses.add("changeZJEGroup.ZEPHYR_ENTITY_TYPE IN (" + ques + ")");
                            inputParams.addAll(Arrays.asList(entityEventType));
                        }else {
                            whereClauses.add("changeZJEGroup." + filterKey + " = ?");
                            inputParams.add(filterValue);
                        }
                    }else {
                        whereClauses.add("changeZJEGroup." + filterKey + " = ?");
                        inputParams.add(filterValue);
                    }
                }
            }
		}
		
		Query query = Query.select();
        query.alias(ChangeZJEGroup.class, "changeZJEGroup");
        query.alias(ChangeZJEItem.class, "changeItem");
        query = query.join(ChangeZJEGroup.class,
                "changeItem.CHANGE_ZJEGROUP_ID = changeZJEGroup.ID");

        if(whereClauses.size() > 0){
			query.where(StringUtils.join(whereClauses," AND "),inputParams.toArray());
		}

		if(filters.containsKey("ORDER_BY")) {
            orderByClause = "CREATED "+ filters.get("ORDER_BY");
        }
        List<ChangeZJEItem> changeZJEItems = null;
        ChangeZJEItem[] changeZJEItemsArray = ao.find(ChangeZJEItem.class, query.offset(offset).limit(limit).order(orderByClause));
        if(changeZJEItemsArray != null) {
            log.debug("# Fetched all ChangeZJEItems " + changeZJEItemsArray.length);
            for(ChangeZJEItem changeZJEItem: changeZJEItemsArray){
                if(StringUtils.isNotEmpty(changeZJEItem.getZephyrField())) {
                    //check if they are execution defect or step defect
                    if (changeZJEItem.getZephyrField().equals(EntityType.SCHEDULE_DEFECT.getEntityType())
                            || changeZJEItem.getZephyrField().equals(EntityType.STEP_DEFECT.getEntityType())) {
                        if (StringUtils.isNotEmpty(changeZJEItem.getOldValue())){
                            String[] issueIdsOld = changeZJEItem.getOldValue().split(",");
                            String[] issueKeysOld = new String[issueIdsOld.length];
                            int indx = 0;
                            for (String issue : issueIdsOld) {
                                indx = getIssueKey(issueKeysOld, indx, issue);
                            }
                            changeZJEItem.setOldValue(StringUtils.join(issueKeysOld, ','));
                        }

                        if (StringUtils.isNotEmpty(changeZJEItem.getNewValue())) {
                            String[] issueIdsNew = changeZJEItem.getNewValue().split(",");
                            String[] issueKeysNew = new String[issueIdsNew.length];
                            int indx2 = 0;
                            for (String issue : issueIdsNew) {
                                indx2 = getIssueKey(issueKeysNew, indx2, issue);
                            }
                            changeZJEItem.setNewValue(StringUtils.join(issueKeysNew, ','));
                        }
                    }
                }
            }


            changeZJEItems =  Arrays.asList(changeZJEItemsArray);
        }
		return changeZJEItems;
	}

    /**
     * Get IssueKeyWith
     * @param issueKeys
     * @param indx
     * @param issue
     * @return
     */
    private int getIssueKey(String[] issueKeys, int indx, String issue) {
        if (StringUtils.isNotEmpty(issue)) {
            MutableIssue issue1 =issueManager.getIssueObject(Long.valueOf(issue));
            if(issue1 != null) {
                if(!JiraUtil.hasIssueViewPermission(null,issue1, ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser())) {
                    issueKeys[indx] = ApplicationConstants.MASKED_DATA;
                    indx++;
                }else{
                    issueKeys[indx] = issue1.getKey();
                    indx++;
                }
            }
        }
        return indx;
    }

    @Override
    public void removeZephyrChangeLogs(Map<String, Object> zjeGroupProperties, Map<String, Object> zjeItemProperties) {
        log.debug("# AuditManagerImpl.removeZephyrChangeLogs() starts");
        ChangeZJEGroup[] zephyrChangeGroups = null;
        ChangeZJEItem[] zephyrChangeItems = null;
        log.debug("# Fetch all ChangeZJEGroup Id's for given zephyrEntityId");
        List<Object> inputParams = new ArrayList<Object>();
        inputParams.add(zjeItemProperties.remove("ZEPHYR_ENTITY_ID"));
        inputParams.add(zjeItemProperties.remove("ZEPHYR_ENTITY_TYPE"));
        zephyrChangeGroups = ao.find(ChangeZJEGroup.class, Query.select().where("ZEPHYR_ENTITY_ID = ? AND ZEPHYR_ENTITY_TYPE = ?", inputParams.toArray()));
        log.debug("# Fetch all ChangeZJEItems for above fetched ChangeZJEGroup Id's");
        if(zephyrChangeGroups != null && zephyrChangeGroups.length > 0) {
            String ques[] = new String[zephyrChangeGroups.length];
            Object[] params = new Object[zephyrChangeGroups.length];
            int quesIndx = 0;
            for (ChangeZJEGroup changeZJEGroup : zephyrChangeGroups) {
                ques[quesIndx] = " ? ";
                params[quesIndx] = changeZJEGroup.getID();
                quesIndx++;
            }
            //This is a hack to get around AO's limitation of being able to pass only one object per query
            String whereClause = " CHANGE_ZJEGROUP_ID IN ( " + StringUtils.join(ques, ',') + " ) ";
            Query query = Query.select().where(whereClause,params);
            zephyrChangeItems = ao.find(ChangeZJEItem.class, query);
            log.debug("# Deleting all entries for above fetched ChangeZJEGroup(s) and ChangeZJEItem(s)");
            ao.delete(zephyrChangeItems);
            ao.delete(zephyrChangeGroups);
        }
        log.debug("# saving DELETE event");
        saveDeleteChangeLog(zjeGroupProperties, zjeItemProperties);
        log.debug("# AuditManagerImpl.removeZephyrChangeLogs() ends");
    }

    private void saveDeleteChangeLog(Map<String, Object> zjeGroupProperties, Map<String, Object> zjeItemProperties) {
        log.debug("# AuditManagerImpl.saveDeleteChangeLog() starts");
        ChangeZJEGroup changeZJEGroup = ao.create(ChangeZJEGroup.class, zjeGroupProperties);
        try{
            log.debug("# saving changeZJEItem item");
            ChangeZJEItem changeZJEItem = ao.create(ChangeZJEItem.class, zjeItemProperties);
            changeZJEItem.setChangeZJEGroup(changeZJEGroup);
            changeZJEItem.save();
        } catch (Exception e) {
            log.error("# AuditManagerImpl.saveDeleteChangeLog() : Error saving Change Logs. Caused by : ",e);
        }
        log.debug("# AuditManagerImpl.saveDeleteChangeLog() ends");
    }

    /**
	 * This method retrieves any existing ChangeZJEGroup.ID for given input params.
	 * @param zephyrEntityId
	 * @param zephyrEntityType
	 * @param zephyrEntityEvent
	 * @return zjeGroupId
	 */
	@Deprecated
	@Override
	public ChangeZJEGroup getZJEGroupId(Integer zephyrEntityId, String zephyrEntityType, String zephyrEntityEvent){
		ChangeZJEGroup changeZJEGroup = null;
		try{
			ChangeZJEGroup[] zjeGroups = ao.find(ChangeZJEGroup.class, Query.select().where("ZEPHYR_ENTITY_ID = ? AND ZEPHYR_ENTITY_TYPE = ? AND ZEPHYR_ENTITY_EVENT = ?", 
					zephyrEntityId, zephyrEntityType, zephyrEntityEvent));
			for (ChangeZJEGroup grp : zjeGroups) {
				changeZJEGroup = grp;
				break;
			}
		} catch (Exception e) {
			log.error("# AuditManagerImpl.getZJEGroupId() : Error reading changeZJEGroup, skipping the key. ");
		}
		return changeZJEGroup;
	}

	/**
	 * Returns the totalRecord count for this filter
	 * @param filters
	 * @return
	 */
	public Integer getChangeItemCount(Map<String, Object> filters) {
		Integer recordCount = 0;
		List<String> whereClauses = new ArrayList<String>();
		List<Object> inputParams = new ArrayList<Object>();
		Set<String> filterSet = filters.keySet();
		for(String filterKey : filterSet) {
            if(!filterKey.equalsIgnoreCase("ORDER_BY")) {
                if (StringUtils.equalsIgnoreCase(filterKey, "ZEPHYR_ENTITY_EVENT")) {
                    String value = (String) filters.get(filterKey);
                    if(StringUtils.contains(value, ",")) {
                        String[] entityEventType = value.split(",");
                        String ques = StringUtils.repeat("?", ",", entityEventType.length);
                        whereClauses.add("changeZJEGroup.ZEPHYR_ENTITY_EVENT IN (" + ques + ")");
                        inputParams.addAll(Arrays.asList(entityEventType));
                    }else {
                        whereClauses.add("changeZJEGroup." + filterKey + " = ?");
                        inputParams.add(value);
                    }
                }else if (StringUtils.equalsIgnoreCase(filterKey, "ZEPHYR_ENTITY_TYPE")) {
                    String value = (String) filters.get(filterKey);
                    if(StringUtils.contains(value, ",")) {
                        String[] entityEventType = value.split(",");
                        String ques = StringUtils.repeat("?", ",", entityEventType.length);
                        whereClauses.add("changeZJEGroup.ZEPHYR_ENTITY_TYPE IN (" + ques + ")");
                        inputParams.addAll(Arrays.asList(entityEventType));
                    }else {
                        whereClauses.add("changeZJEGroup." + filterKey + " = ?");
                        inputParams.add(value);
                    }
                }else {
                    Object filterValue = filters.get(filterKey);
                    whereClauses.add("changeZJEGroup." + filterKey + " = ?");
                    inputParams.add(filterValue);
                }
            }
		}
		
		Query query = Query.select();
        query.alias(ChangeZJEGroup.class, "changeZJEGroup");
        query.alias(ChangeZJEItem.class, "changeItem");
        query = query.join(ChangeZJEGroup.class,
                "changeItem.CHANGE_ZJEGROUP_ID = changeZJEGroup.ID");

        if(whereClauses.size() > 0){
			query.where(StringUtils.join(whereClauses," AND "),inputParams.toArray());
		}
        recordCount = ao.count(ChangeZJEItem.class, query);
		return recordCount != null ? recordCount : 0;
	}

	@Override
	public ChangeZJEGroup[] getZFJChangeLogsByDate(long dateTime, String eventType, OptionalLong projectId) {
		Query query = Query.select();
		if(projectId.isPresent()) {
			query.where("PROJECT_ID = ? AND ZEPHYR_ENTITY_EVENT = ? AND CREATED >= ?", projectId.getAsLong(), eventType, dateTime);
		} else {
			query.where("ZEPHYR_ENTITY_EVENT = ? AND CREATED >= ?", eventType, dateTime);
		}
		ChangeZJEGroup[] zjeGroups = ao.find(ChangeZJEGroup.class, query);
		return zjeGroups;
	}
}
