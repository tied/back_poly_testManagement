package com.thed.zephyr.je.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.database.DatabaseConfig;
import com.atlassian.jira.config.database.DatabaseConfigurationManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.FolderCycleMapping;
import com.thed.zephyr.je.service.FolderManager;
import com.thed.zephyr.util.ApplicationConstants;

import net.java.ao.Query;

/**
 * Implementation of FolderManager class.
 * 
 * @author manjunath
 * @see com.thed.zephyr.je.service.FolderManager
 *
 */
public class FolderManagerImpl implements FolderManager {
	
	private static final Logger log = LoggerFactory.getLogger(FolderManagerImpl.class);
	private final ActiveObjects activeObjects;
	
	public FolderManagerImpl(ActiveObjects activeObjects) {
		this.activeObjects =  activeObjects;
	}

	@Override
	public Folder saveFolder(Map<String, Object> folderProperties) {
		return activeObjects.create(Folder.class, folderProperties);
	}

	@Override
	public void updateFolder(Folder folder, String folderName, String folderDescription, String modifiedBy, Map<String, Boolean> cacheFolderMap) {
		String oldFolderName = folder.getName();
		folder.setName(folderName);
		folder.setDescription(folderDescription == null ? "" : folderDescription);
		folder.setModifiedBy(modifiedBy);
		folder.setModifiedDate(Calendar.getInstance().getTime());
		folder.save();
		if(cacheFolderMap != null) cacheFolderMap.put(oldFolderName, false);
	}

	@Override
	public int removeFolder(Long folderId) {
		Folder folder = getFolder(folderId);
		if(folder == null) {
			return 0;
		}
		activeObjects.delete(folder);
		log.debug("successfully deleted the folder - " + folderId);
		return 1;
	}
	
	@Override
	public int removeFolder(Long folderId, Map<String, Boolean> cacheFolderMap) {
		Folder folder = getFolder(folderId);
		if(folder == null) {
			return 0;
		}
		String folderName = folder.getName();
		activeObjects.delete(folder);
		if(cacheFolderMap != null) cacheFolderMap.put(folderName, false);
		log.debug("successfully deleted the folder - " + folderId);
		return 1;
	}

	@Override
	public Folder getFolder(Long folderId) {
		Folder[] folders = activeObjects.find(Folder.class, Query.select().where("ID = ?", folderId));
		if(folders == null || folders.length == 0) {
			return null;
		}
		return folders[0];
	}

	@Override
	public FolderCycleMapping saveFolderCycleMapping(Map<String, Object> folderCycleMappingProperties) {
		return activeObjects.create(FolderCycleMapping.class, folderCycleMappingProperties);
	}

	@Override
	public int removeCycleFolderMapping(Long projectId, Long versionId, long cycleId, Long folderId) {
		Query query = Query.select();
		if(cycleId == ApplicationConstants.AD_HOC_CYCLE_ID_LONG) {
			query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL AND FOLDER_ID = ?", projectId, versionId, folderId);
		} else {
			query.where("CYCLE_ID = ? AND FOLDER_ID = ?", cycleId, folderId);
		}
		FolderCycleMapping[] folderCycleMappings = activeObjects.find(FolderCycleMapping.class, query);
		if(folderCycleMappings == null || folderCycleMappings.length == 0) {
			return 0;
		}
		activeObjects.delete(folderCycleMappings[0]);
		return 1;
	}
	
	@Override
	public int getFoldersCountForCycle(Long projectId, Long versionId, Long cycleId) {
		Query query = Query.select();
		if(cycleId != null && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
			query.where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID IS NULL", projectId, versionId);
		} else {
			query.where("CYCLE_ID = ?", cycleId);
		}
		return activeObjects.count(FolderCycleMapping.class, query);
	}

	@Override
	public boolean isFolderNameUniqueUnderCycle(Long projectId, Long versionId,Long cycleId, String folderName) {
		Query query = Query.select();
		query.alias(Folder.class, "folder");
		query.alias(FolderCycleMapping.class , "mapping");
		query.join(FolderCycleMapping.class , "mapping.FOLDER_ID = folder.ID");
		if(cycleId != null && cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
			query.where("mapping.PROJECT_ID = ? AND mapping.VERSION_ID = ? AND mapping.CYCLE_ID IS NULL and  folder.NAME = ?", projectId, versionId, folderName);
		} else {
			query.where("mapping.CYCLE_ID = ? and  folder.NAME = ?", cycleId, folderName);
		}
		Folder[] folderArr = activeObjects.find(Folder.class, query);
		if(folderArr != null && folderArr.length > 0) {
			return false;
		}
		return true;
	}

	@Override
	public int updateFoldersToSprint(Long folderId, Long cycleId, Long versionId, Long projectId, Long sprintId) {
		if(!folderId.equals(-1L)) {
			FolderCycleMapping folderCycleMapping =  getFolderCycleMapping(folderId, cycleId, versionId, projectId);
			if(folderCycleMapping == null) {
				return 0;
			}
			folderCycleMapping.setSprintId(sprintId);
			folderCycleMapping.save();
			return 1;
		} else {
			return updateAllFoldersToSprint(cycleId, versionId, projectId, sprintId);
		}
	}

	@Override
	public Long getSprintIDForFolder(Long folderId, Long cycleId) {
		FolderCycleMapping mapping = this.getFolderCycleMapping(folderId, cycleId);
		if (mapping != null) {
			return mapping.getSprintId();
		}
		return null;
	}

	private int updateAllFoldersToSprint(Long cycleId, Long versionId, Long projectId, Long sprintId) {
		activeObjects.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
            	Query query = Query.select();
            	if(cycleId != null && cycleId.intValue() != ApplicationConstants.AD_HOC_CYCLE_ID) {
            		query.where("CYCLE_ID = ?", cycleId);
            	} else {
            		query.where("PROJECT_ID = ?  AND VERSION_ID = ? AND CYCLE_ID IS NULL", projectId, versionId);
            	}
                final FolderCycleMapping[] folderCycleMappings = activeObjects.find(FolderCycleMapping.class, query);
                for(FolderCycleMapping  folderCycleMapping : folderCycleMappings) {
                	folderCycleMapping.setSprintId(sprintId);
                	folderCycleMapping.save();
                }
                return null;
            }
        });
		return 1;
	}
	
	private FolderCycleMapping getFolderCycleMapping(Long folderId, Long cycleId) {
		Query query = Query.select();
		if(cycleId != null && !cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
			query.where("FOLDER_ID = ? and CYCLE_ID = ?", folderId, cycleId);
		} else {
			query.where("FOLDER_ID = ? and CYCLE_ID IS NULL", folderId);
		}
		FolderCycleMapping[] folderCycleMappings = activeObjects.find(FolderCycleMapping.class, query);
		if(folderCycleMappings == null || folderCycleMappings.length == 0) {
			return null;
		}
		return folderCycleMappings[0];
	}
	
	public FolderCycleMapping getFolderCycleMapping(Long folderId, Long cycleId, Long versionId, Long projectId) {
		Query query = Query.select();
		if(cycleId != null && !cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
			if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
				query.where("FOLDER_ID = ? and CYCLE_ID = ?", folderId, cycleId);
			} else {
				query.where("CYCLE_ID = ? AND FOLDER_ID IS NULL", cycleId);
			}
		} else {
			if(folderId != null && !folderId.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
				query.where("FOLDER_ID = ? and CYCLE_ID IS NULL", folderId);
			} else {
				query.where("PROJECT_ID = ? AND VERSION_ID = ? and CYCLE_ID IS NULL AND FOLDER_ID IS NULL", projectId, versionId);
			}
		}
		FolderCycleMapping[] folderCycleMappings = activeObjects.find(FolderCycleMapping.class, query);
		if(folderCycleMappings == null || folderCycleMappings.length == 0) {
			return null;
		}
		return folderCycleMappings[0];
	}

	@Override
	public Folder cloneFolderToCycle(Long projectId, Long versionId, Long folderId, Long newCycleId, Long oldCycleId, String loggedInUser) {
		Folder folder = getFolder(folderId);
		if(folder == null) {
			return null;
		}
		FolderCycleMapping folderCycleMapping = getFolderCycleMapping(folderId, oldCycleId);
		if(folderCycleMapping == null) {
			return null;
		}
		Folder newFolder  = saveFolder(createFolderPropertiesFromObj(folder, loggedInUser));
		saveFolderCycleMapping(createFolderCycleMappingPropertiesFromObj(projectId, versionId, newFolder.getID(), newCycleId, loggedInUser));
		return newFolder;
	}
	
	private Map<String, Object> createFolderPropertiesFromObj(Folder folder, String loggedInUser) {
		Map<String, Object> folderProperties = new HashMap<>();
		Date date = new Date();
		folderProperties.put("NAME", "Clone " + folder.getName());
		folderProperties.put("DESCRIPTION", folder.getDescription());
		folderProperties.put("DATE_CREATED", date);
		folderProperties.put("CREATED_BY", loggedInUser);
		return folderProperties;
	}
	
	private Map<String, Object> createFolderCycleMappingPropertiesFromObj(Long projectId, Long versionId, long folderId, Long newCycleId, String loggedInUser) {
		Map<String, Object> folderCycleMappingProperties = new HashMap<>();
		Date date = new Date();
		folderCycleMappingProperties.put("CYCLE_ID", newCycleId);
		folderCycleMappingProperties.put("FOLDER_ID", folderId);
		folderCycleMappingProperties.put("PROJECT_ID", projectId);
		folderCycleMappingProperties.put("VERSION_ID", versionId);
		folderCycleMappingProperties.put("DATE_CREATED", date);
		folderCycleMappingProperties.put("CREATED_BY", loggedInUser);
		return folderCycleMappingProperties;
	}
	
	public List<FolderCycleMapping> getFoldersForSprint(List<Long> projectIdList, String[] versionIds, String[] sprintIds, Integer offset, Integer maxRecords) {
		List<Object> params = new ArrayList<Object>();

		String ques[] = new String[versionIds.length];
		for(int i=0; i< versionIds.length; i++){
			ques[i] = "?";
		}

		String quesSprint[] = new String[sprintIds.length];
		if(sprintIds != null) {
			for(int i=0; i< sprintIds.length; i++){
				quesSprint[i] = "?";
			}			
		}
		
		String quesProj[] = new String[projectIdList.size()];
		if(projectIdList != null) {
			for(int i=0; i< projectIdList.size(); i++){
				quesProj[i] = "?";
			}			
		}
		
		String whereClause = "";
		if(quesProj.length > 0) {
			whereClause = "cycle.PROJECT_ID IN ( " + StringUtils.join(quesProj, ',') + " )";
		}
		if(ques.length > 0) {
			if(StringUtils.isBlank(whereClause)) {
				whereClause = "cycle.VERSION_ID IN ( " + StringUtils.join(ques, ',') + " )";
			} else {
				whereClause += " AND cycle.VERSION_ID IN ( " + StringUtils.join(ques, ',') + " )";
			}
		}
		if(quesSprint.length > 0) {
			if(StringUtils.isBlank(whereClause)) {
				whereClause = "mapping.SPRINT_ID IN ( " + StringUtils.join(quesSprint, ',') + " )";
			} else {
				whereClause += " AND mapping.SPRINT_ID IN ( " + StringUtils.join(quesSprint, ',') + " )";
			}
		}
		
		Iterator<Long> itrProj = projectIdList.iterator();
		while (itrProj.hasNext()) {
			Long longVal = 0l;
			Object val = itrProj.next();
			if(val instanceof Number)
				longVal = ((Number)val).longValue();
			if(val instanceof String)
				longVal = Long.parseLong((String)val);
			params.add(longVal);
		}
		
		List<String> versionList = Arrays.asList(versionIds);
		Iterator<String> itr = versionList.listIterator();
		while (itr.hasNext()) {
			Long longVal = 0l;
			Object val = itr.next();
			if(val instanceof Number)
				longVal = ((Number)val).longValue();
			if(val instanceof String)
				longVal = Long.parseLong((String)val);
			params.add(longVal);
		}
		
		List<String> sprintList = Arrays.asList(sprintIds);
		Iterator<String> itr1 = sprintList.listIterator();
		while (itr1.hasNext()) {
			Long longVal = 0l;
			Object val = itr1.next();
			if(val instanceof Number)
				longVal = ((Number)val).longValue();
			if(val instanceof String)
				longVal = Long.parseLong((String)val);
			params.add(longVal);
		}
		
		Query query = Query.select().offset(offset).limit(maxRecords);
		query.alias(FolderCycleMapping.class , "mapping");
		query.alias(Folder.class, "folder");
		query.join(Folder.class , "mapping.FOLDER_ID = folder.ID");
		query.alias(Cycle.class, "cycle");
		query.join(Cycle.class, "cycle.ID = mapping.CYCLE_ID");
		query.where(whereClause, params.toArray());
		FolderCycleMapping[] folderArr = activeObjects.find(FolderCycleMapping.class, query);
		if(folderArr != null && folderArr.length > 0) {
			return Arrays.asList(folderArr);
		}
		return new ArrayList<>(0);
	}

	@Override
	public void cloneEmptyFoldersFromCyle(Long oldProjectId, Long oldVersionId, Long clonedCycleId, 
			Long newProjectId, Long newVersionId, Long newCycleId, String loggedInUser, Map<String, Long> clonedFoldersMap) {
		int cycleFoldersCount = getFoldersCountForCycle(oldProjectId, oldVersionId, clonedCycleId);
		int clonedCycleFoldersCount = getFoldersCountForCycle(newProjectId, newVersionId, newCycleId);
		List<Long> cycleIds = new ArrayList<>(1);
		cycleIds.add(clonedCycleId);
		if(cycleFoldersCount != clonedCycleFoldersCount) {
			List<Folder> folders = fetchFolders(oldProjectId, oldVersionId, cycleIds, -1, 0);
			for(Folder folder : folders) {
				if(!clonedFoldersMap.containsKey(folder.getID()+"")) {
					Folder clonedFolder = cloneFolderToCycle(newProjectId, newVersionId, Long.valueOf(folder.getID()+""), newCycleId, clonedCycleId, loggedInUser);
					if(clonedFolder == null) {
            			throw new RuntimeException("Error while creating cloned folders.");
            		}
				}
			}
		}
	}
	
	@Override
	public List<Folder> getValuesByKey(final String clause, final List<String> values) {
		String ques[] = new String[values.size()];
		for(int i=0; i< values.size(); i++){
			ques[i] = "?";
		}
		String whereClause = " NAME IN ( " + StringUtils.join(ques, ',') + " ) ";
		Query query = Query.select().where(whereClause, values.toArray());
		Folder[] arrFolders = activeObjects.find(Folder.class, query);
		return Arrays.asList(arrFolders);
	}
	
	@Override
	public List<String> getValues(final String fieldName, final String value) {
		Query query = Query.select().where(fieldName + " = ?", value).distinct();       	
		Folder[] folders = activeObjects.find(Folder.class, query);
		List<String> allValues = new ArrayList<String>();
		if(folders != null && folders.length > 0) {
			for(Folder folder : folders) {
				try {
					if(StringUtils.equalsIgnoreCase(fieldName, "NAME")) {
						allValues.add(folder.getName());
					}
				} catch (SecurityException e) {
					log.error("Security exception fetching folder details for zql", e);
				}
			}
			return allValues;
		} else {
			return allValues;
		}
	}
	
	@Override
	public List<Folder> getFoldersByProjectId(final Collection<Integer> projectIds, String clauseName, String valuePrefix) {
		List<Object> params = new ArrayList<Object>();

		String ques[] = new String[projectIds.size()];
		for(int i=0; i< projectIds.size(); i++){
			ques[i] = "?";
		}
		String whereClause = " mapping.PROJECT_ID IN ( " + StringUtils.join(ques, ',') + " ) ";
		params.addAll(Arrays.asList(projectIds.toArray()));
		if(StringUtils.isNotBlank(valuePrefix)) {
			whereClause += " AND " + "folder." + clauseName + " LIKE ?";
			params.add(valuePrefix + "%");
		}
		Query query = Query.select();
		query.alias(Folder.class , "folder");
		query.alias(FolderCycleMapping.class, "mapping");
		query.join(FolderCycleMapping.class , "mapping.FOLDER_ID = folder.ID");
		query.where(whereClause, params.toArray());
		query.setLimit(ApplicationConstants.MAX_LIMIT);
		Folder[] arrfolders = activeObjects.find(Folder.class, query);
		return Arrays.asList(arrfolders);
	}

	@Override
	public void updateDeletedVersionId(Long projectId, Long srcVersionId, Long cycleId, Long targetVersionId) {
		if(cycleId != null && !cycleId.equals(ApplicationConstants.AD_HOC_CYCLE_ID_LONG)) {
			FolderCycleMapping[] arrFolderCycleMapping = activeObjects.find(FolderCycleMapping.class, Query.select().where("PROJECT_ID = ? AND VERSION_ID = ? AND CYCLE_ID = ?", projectId, srcVersionId, cycleId));
			for(FolderCycleMapping folderCycleMapping : arrFolderCycleMapping) {
				folderCycleMapping.setVersionId(targetVersionId);
				folderCycleMapping.save();
			}
		}
	}
	
	@Override
	public List<Folder> fetchFolders(Long projectId, Long versionId, List<Long> cycleIds, Integer limit, Integer offset) {
		List<String> whereClauses = new ArrayList<String>();
		List<Object> params = new ArrayList<Object>();
		if(limit == null || limit < -1) {
			limit = 1000;
		}
		if(offset == null || offset < 0) {
			offset = 0;
		}
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		Query query = Query.select();
		query.alias(Folder.class, "folder");
		query.alias(FolderCycleMapping.class , "mapping");
		query.join(FolderCycleMapping.class , "mapping.FOLDER_ID = folder.ID");
		//Adding project clause and value.
		whereClauses.add(" mapping.PROJECT_ID = ?");
		params.add(projectId);
		
		//Adding version clause and value.
		whereClauses.add(" mapping.VERSION_ID = ?");
		params.add(versionId);
		
		//Adding cycle clause and value.
		if(cycleIds.size() > 0) {
			whereClauses.add(" mapping.CYCLE_ID IN ( " + StringUtils.join(cycleIds.toArray(), ',') + " ) ");
		}
		if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres")){
			query.where(StringUtils.join(whereClauses, " and "), params.toArray());
			query.order("mapping.FOLDER_ID DESC"); 
		} else {
			query.where(StringUtils.join(whereClauses, " and ") + " ORDER BY mapping.FOLDER_ID DESC", params.toArray());
		}
		query.setLimit(limit);
		query.setOffset(offset);
		Folder[] cycleFolders = activeObjects.find(Folder.class, query);
		if(cycleFolders == null || cycleFolders.length == 0) {
			return new ArrayList<>(0);
		}
		return Arrays.asList(cycleFolders);
	}

}
