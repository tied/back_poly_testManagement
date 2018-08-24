package com.thed.zephyr.je.zql.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.database.DatabaseConfig;
import com.atlassian.jira.config.database.DatabaseConfigurationManager;
import com.google.common.collect.ImmutableMap;
import com.thed.zephyr.je.config.license.PluginUtils;
import com.thed.zephyr.je.event.ZQLFilterShareType;
import com.thed.zephyr.je.zql.model.*;
import com.thed.zephyr.je.zql.service.ZQLFilterManager;
import com.thed.zephyr.util.FilterNameComparator;
import com.thed.zephyr.util.JiraUtil;
import net.java.ao.Query;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author mukul
 *
 */
public class ZQLFilterManagerImpl implements ZQLFilterManager{
	
	private static final Logger log = LoggerFactory.getLogger(ZQLFilterManagerImpl.class);
	private final ActiveObjects ao;

	public ZQLFilterManagerImpl(ActiveObjects ao) {
		this.ao = checkNotNull(ao);
	}
	

	/** Retrieves all ZQL filters with below conditions:
	 *  if offset == null || offset == -1
	 *  	ignore pagination
	 *  if user != null
	 * 		It will fetch all Filters for this USER
	 *  if isFav != null && isFav == true
	 * 		It will fetch all Favorites Filters
	 *  else
	 *  	It will retrieves all ZQL filters for all the USERS.
	 *  
     * @param user 
     * @param isFav
     * @param offset
     * @param maxRecords
	 * @return Map<String, Object>: totalCount and list of retrieved filters. 
	 */	
	@Override
	public Map<String, Object> getZQLFilters(String user, Boolean isFav, Integer offset, Integer maxRecords) {
		log.debug("Retrieving ZQL Filters");
		//If Offset comes as -1, no pagination
		offset = (null == offset) ? -1 : offset; 
		maxRecords = (null == maxRecords) ? -1 : maxRecords;
		List<String> whereClauses = new ArrayList<String>();
		List<Object> params = new ArrayList<Object>();
		
		// Create the "Query" based on input conditions
		if(null != isFav && isFav ){
			whereClauses.add("FAVORITE = ?"); params.add(1);
		}  
		if(null != user){			
			whereClauses.add("CREATED_BY = ?"); params.add(user);
		}

		Query query =  Query.select();
		query.alias(ZQLFilter.class, "AO_7DEABF_ZQLFILTER");
		query.alias(ZQLSharePermissions.class, "AO_7DEABF_ZQLSHARE_PERMISSIONS");
		query = query.join(ZQLSharePermissions.class, 
						"AO_7DEABF_ZQLSHARE_PERMISSIONS.ZQLFILTER_ID = AO_7DEABF_ZQLFILTER.ID");
		whereClauses.add("AO_7DEABF_ZQLSHARE_PERMISSIONS.SHARE_TYPE = ?"); 
		params.add(ZQLFilterShareType.getZQLFilterShareType(1).getShareType());
		
		if(whereClauses.size() > 0)
			query = query.where(StringUtils.join(whereClauses, " and "), params.toArray()); 
		
		// Count total records for the formed "Query".
		Integer totalCount = ao.count(ZQLFilter.class, query);
		// Fetch the records for the formed "Query" and given offset, maxRecords.
		ZQLFilter []zqlFilters = ao.find(ZQLFilter.class, query.offset(offset).limit(maxRecords));
		Map<String, Object> filtersMap = ImmutableMap.of("totalCount", totalCount, "zqlFilters", Arrays.asList(zqlFilters));
		return filtersMap;
	}	
	
	@Override
	public Map<String, Object> getAllFaoritesZQLFiltersByUser(String user, Integer offset, Integer maxRecords) {
		log.debug("Retrieving All Favorite ZQL Filters by User");
		//If Offset comes as -1, no pagination
		offset = (null == offset) ? -1 : offset; 
		maxRecords = (null == maxRecords) ? -1 : maxRecords;
		Query query =  Query.select();
		query.alias(ZQLFilter.class, "AO_7DEABF_ZQLFILTER");
		query.alias(ZQLFavoriteAsoc.class, "AO_7DEABF_ZQLFAVORITE_ASOC");		
		query = query.join(ZQLFavoriteAsoc.class, 
						"AO_7DEABF_ZQLFAVORITE_ASOC.ZQLFILTER_ID = AO_7DEABF_ZQLFILTER.ID");
		query = query.where("AO_7DEABF_ZQLFAVORITE_ASOC.USER = ?", user); 
		
		// Count total records for the formed "Query".
		Integer totalCount = ao.count(ZQLFilter.class, query);
		// Fetch the records for the formed "Query" and given offset, maxRecords.
		ZQLFilter []zqlFilters = ao.find(ZQLFilter.class, query.order("FILTER_NAME ASC").offset(offset).limit(maxRecords));
		Map<String, Object> filtersMap = ImmutableMap.of("totalCount", totalCount, "zqlFilters", Arrays.asList(zqlFilters));
		return filtersMap;
	}
	
	@Override
	public Map<String, Object> getAllNonFaoritesZQLFiltersByUser(String user, Integer offset, Integer maxRecords) {
		log.debug("Retrieving All Favorite ZQL Filters by User");
		//If Offset comes as -1, no pagination
		offset = (null == offset) ? -1 : offset; 
		maxRecords = (null == maxRecords) ? -1 : maxRecords;
		Query query =  Query.select().where("CREATED_BY = ? AND FAV_COUNT = ?", user, 0);
		
		// Count total records for the formed "Query".
		Integer totalCount = ao.count(ZQLFilter.class, query);
		// Fetch the records for the formed "Query" and given offset, maxRecords.
		ZQLFilter []zqlFilters = ao.find(ZQLFilter.class, query.order("FILTER_NAME ASC").offset(offset).limit(maxRecords));
		Map<String, Object> filtersMap = ImmutableMap.of("totalCount", totalCount, "zqlFilters", Arrays.asList(zqlFilters));
		return filtersMap;
	}	

	@Override
	public Map<String, Object> getAllZQLFiltersByUser(String user, Integer offset, Integer maxRecords) {
		log.debug("Retrieving All ZQL Filters by User");
		//If Offset comes as -1, no pagination
		offset = (null == offset) ? -1 : offset; 
		maxRecords = (null == maxRecords) ? -1 : maxRecords;
		Query query =  Query.select().where("CREATED_BY = ?", user);
		
		// Count total records for the formed "Query".
		Integer totalCount = ao.count(ZQLFilter.class, query);
		// Fetch the records for the formed "Query" and given offset, maxRecords.
		ZQLFilter []zqlFilters = ao.find(ZQLFilter.class, query.order("FILTER_NAME ASC").offset(offset).limit(maxRecords));
		Map<String, Object> filtersMap = ImmutableMap.of("totalCount", totalCount, "zqlFilters", Arrays.asList(zqlFilters));
		return filtersMap;
	}
	
	@Override
	public Map<String, Object> getPopularZQLFilters(Integer offset, Integer maxRecords, Boolean isFav) {
		log.debug("Retrieving All Popular ZQL Filters");
		Map<String, Object> filtersMap = null;
		//If Offset comes as -1, no pagination
		offset = (null == offset) ? -1 : offset; 
		maxRecords = (null == maxRecords) ? -1 : maxRecords;

		Query query =  Query.select();		
		query.alias(ZQLFilter.class, "AO_7DEABF_ZQLFILTER");
		query.alias(ZQLSharePermissions.class, "AO_7DEABF_ZQLSHARE_PERMISSIONS");
		query = query.join(ZQLSharePermissions.class, 
						"AO_7DEABF_ZQLSHARE_PERMISSIONS.ZQLFILTER_ID = AO_7DEABF_ZQLFILTER.ID");
		if(isFav)
			query = query.where("AO_7DEABF_ZQLSHARE_PERMISSIONS.SHARE_TYPE = ? AND AO_7DEABF_ZQLFILTER.FAV_COUNT > ?", ZQLFilterShareType.GLOBAL.getShareType(), 0);
		else
			query = query.where("AO_7DEABF_ZQLSHARE_PERMISSIONS.SHARE_TYPE = ? AND AO_7DEABF_ZQLFILTER.FAV_COUNT = ?", ZQLFilterShareType.GLOBAL.getShareType(), 0);
		
		// Count total records for the formed "Query".
		Integer totalCount = ao.count(ZQLFilter.class, query);
		// Fetch the records for the formed "Query" and given offset, maxRecords.
        DatabaseConfig dbConfig = ((DatabaseConfigurationManager) ComponentAccessor.getComponent(DatabaseConfigurationManager.class)).
                getDatabaseConfiguration();
        if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres") && PluginUtils.isAOVersionLessThan23())
            query.order("FAV_COUNT DESC, \"FILTER_NAME\" ASC");
        else
            query.order("FAV_COUNT DESC, FILTER_NAME ASC");

		ZQLFilter []zqlFilters = ao.find(ZQLFilter.class, query.offset(offset).limit(maxRecords));
		filtersMap = ImmutableMap.of("totalCount", totalCount, "zqlFilters", Arrays.asList(zqlFilters));		
		return filtersMap;
	}	
	
	@Override
	public Map<String, Object> getAllGlobalZQLFilters(Integer offset, Integer maxRecords) {
		log.debug("Retrieving All Global ZQL Filters");
		Map<String, Object> filtersMap = null;
		//If Offset comes as -1, no pagination
		offset = (null == offset) ? -1 : offset; 
		maxRecords = (null == maxRecords) ? -1 : maxRecords;

		Query query =  Query.select();		
		query.alias(ZQLFilter.class, "AO_7DEABF_ZQLFILTER");
		query.alias(ZQLSharePermissions.class, "AO_7DEABF_ZQLSHARE_PERMISSIONS");
		query = query.join(ZQLSharePermissions.class, 
						"AO_7DEABF_ZQLSHARE_PERMISSIONS.ZQLFILTER_ID = AO_7DEABF_ZQLFILTER.ID");	
		query = query.where("AO_7DEABF_ZQLSHARE_PERMISSIONS.SHARE_TYPE = ? ", ZQLFilterShareType.GLOBAL.getShareType());
		// Count total records for the formed "Query".
		Integer totalCount = ao.count(ZQLFilter.class, query);
		// Fetch the records for the formed "Query" and given offset, maxRecords.
		ZQLFilter []zqlFilters = ao.find(ZQLFilter.class, query.order("FILTER_NAME ASC").offset(offset).limit(maxRecords));	
		filtersMap = ImmutableMap.of("totalCount", totalCount, "zqlFilters", Arrays.asList(zqlFilters));		
		return filtersMap;
	}	
	
	@Override
	public ZQLFilter getZQLFilter(Integer id) {
		log.debug("Retrieving a ZQL Filter by ID");
		ZQLFilter [] zqlFilters =  ao.find(ZQLFilter.class, Query.select().where("ID = ?", id));
		if(null != zqlFilters && zqlFilters.length > 0){
			return zqlFilters[0];
		}
		return null;
	}
	
	@Override
	public ZQLFilter getZQLFilter(String filterName) {
		log.debug("Retrieving a ZQL Filter by FilterName");
		ZQLFilter [] zqlFilters =  ao.find(ZQLFilter.class, Query.select().where("FILTER_NAME = ?", filterName));
		if(null != zqlFilters && zqlFilters.length > 0){
			return zqlFilters[0];
		}
		return null;
	}	

	/* (non-Javadoc)
	 * @see com.thed.zephyr.je.zql.service.ZQLFilterManager#saveZQLFilter(Map<String, Object>)
	 */
	public ZQLFilter saveZQLFilter(Map<String, Object> zqlFilterProperties) {
		ZQLFilter zqlFilter = null;	
		ZQLSharePermissions zqlFilterSharePermission = null;
		Boolean favorite = (Boolean)zqlFilterProperties.remove("FAVORITE");
		Map<String, Object> permissionProperties = new HashMap<String, Object>();
		permissionProperties.put("SHARE_TYPE", zqlFilterProperties.remove("SHARE_TYPE"));
		permissionProperties.put("PARAM1", zqlFilterProperties.remove("PARAM1"));
		permissionProperties.put("PARAM2", zqlFilterProperties.remove("PARAM2"));
		log.debug("Saving a new ZQL Filter");
		zqlFilter = ao.create(ZQLFilter.class, zqlFilterProperties);
		log.debug("Saving ZQL Filter share permissions");
		zqlFilterSharePermission = ao.create(ZQLSharePermissions.class, permissionProperties);
		zqlFilterSharePermission.setZQLFilter(zqlFilter);
		zqlFilterSharePermission.save();
		//if favorite == true, save ZQLFavoriteAsoc
		if(null != favorite && favorite){
			zqlFilter.setFavCount(1);
			zqlFilter.save();
			ZQLFavoriteAsoc zqlFavoriteAsoc = ao.create(ZQLFavoriteAsoc.class, ImmutableMap.of("USER", zqlFilterProperties.get("CREATED_BY")));
			zqlFavoriteAsoc.setZQLFilter(zqlFilter);
			zqlFavoriteAsoc.save();		
		}
		return zqlFilter;
	}

	/* (non-Javadoc)
	 * @see com.thed.zephyr.je.zql.service.ZQLFilterManager#removeZQLFilter(java.lang.Integer)
	 */
	@Override
	public void removeZQLFilter(ZQLFilter zqlFilter) {
		log.debug("Removning ZQL Filter");
		if(null != zqlFilter) {
			ao.delete(zqlFilter.getZQLFilterSharePermissions());
			ao.delete(zqlFilter.getZQLFilterFavoriteAsoc());
            // clean up ZFJCOLUMN_LAYOUT and  COLUMN_LAYOUT_ITEM Tables
            cleanUpFilterColumnsAssociations(zqlFilter.getID());
			ao.delete(zqlFilter);
		}
	}

    private void cleanUpFilterColumnsAssociations(Integer zqlFilterId){
        ZFJColumnLayout[] ZFJColumnLayoutArr = ao.find(ZFJColumnLayout.class, Query.select().where("ZQLFILTER_ID = ?", zqlFilterId));
        if(ZFJColumnLayoutArr.length > 0){
            Integer[] ZFJColumnLayoutIdsArr = new Integer[ZFJColumnLayoutArr.length];
            String[] placeHolders = new String[ZFJColumnLayoutArr.length];
            int indx = 0;
            for(ZFJColumnLayout zfjColumnLayout : ZFJColumnLayoutArr){
                ZFJColumnLayoutIdsArr[indx] = zfjColumnLayout.getID();
                placeHolders[indx] = "?";
                indx++;
            }
            String criteria = "ZFJCOLUMN_LAYOUT_ID IN ("+StringUtils.join(placeHolders, " , ")+")";
            ColumnLayoutItem[] columnLayoutItemArr = ao.find(ColumnLayoutItem.class, Query.select().where(criteria, ZFJColumnLayoutIdsArr)) ;

            ao.delete(columnLayoutItemArr);
            ao.delete(ZFJColumnLayoutArr);
        }
    }

	/** 
	 * Updates a ZQL Filter with below conditions:
	 * If FILTER_NAME != null
	 * 		updates FILTER_NAME
	 * If FAVORITE == 1
	 * 		create new ZQLFavoriteAsoc, increase FAV_COUNT
	 * else
	 * 		remove ZQLFavoriteAsoc, decrease FAV_COUNT
	 * @param zqlFilterProperties (properties to be updated)
	 * @return ZQLFilter (updated ZQLFilter)
	 */	
	@Override
	public ZQLFilter updateZQLFilter(Map<String, Object> zqlFilterProperties) {
		log.debug("Updating a ZQL Filter by ID");
		Integer filterId = (Integer)zqlFilterProperties.get("ID");
		ZQLFilter zqlFilter = getZQLFilter(filterId);
		Integer favCountPrevVal = zqlFilter.getFavCount();
		if(StringUtils.isNotBlank((String)zqlFilterProperties.get("FILTER_NAME"))){
			zqlFilter.setFilterName((String)zqlFilterProperties.get("FILTER_NAME"));
			zqlFilter.setUpdatedBy((String)zqlFilterProperties.get("UPDATED_BY"));
			zqlFilter.setUpdatedOn((Long)zqlFilterProperties.get("UPDATED_ON"));
			zqlFilter.save();			
		}
		Boolean favorite = (Boolean)zqlFilterProperties.get("FAVORITE");
		if(null != favorite){		
			ZQLFavoriteAsoc[] zqlFavoriteAsocArr = ao.find(ZQLFavoriteAsoc.class, Query.select().where("ZQLFILTER_ID = ? AND USER = ?", filterId, zqlFilterProperties.get("UPDATED_BY")));
			if(favorite && zqlFavoriteAsocArr.length == 0){
				ZQLFavoriteAsoc zqlFavoriteAsoc = ao.create(ZQLFavoriteAsoc.class, ImmutableMap.of("USER", zqlFilterProperties.get("UPDATED_BY")));
				zqlFavoriteAsoc.setZQLFilter(zqlFilter);
				zqlFavoriteAsoc.save();
				// increase FAV_COUNT				
				zqlFilter.setFavCount(++favCountPrevVal);
				zqlFilter.save();
			} else if(!favorite && zqlFavoriteAsocArr.length > 0) {
				ao.delete(zqlFavoriteAsocArr);
				// decrease FAV_COUNT
				zqlFilter.setFavCount(--favCountPrevVal);
				zqlFilter.save();			
			} else {} //do nothing	
		}
		return zqlFilter;
	}

	@Override
	public ZQLSharePermissions getZQLSharePermissions(ZQLFilter zqlFilter) {
		ZQLSharePermissions[] zqlSharePermissions = null;
		log.debug("Fetching  a ZQLSharePermissions by ZQLFilter");
		zqlSharePermissions = ao.find(ZQLSharePermissions.class, Query.select().where("ZQLFILTER_ID = ? ", zqlFilter.getID()));
		return zqlSharePermissions.length >0 ? zqlSharePermissions[0] : null;
	}

	@Override
	public Map<String, Object> searchZQLFilters(String filterName, String owner, String loggedInUser, Integer sharePermType) {
		log.debug("Searching ZQL Filters");
		Map<String, Object> filtersMap = null;
		ZQLFilter[] zqlFilters = null;
		List<ZQLFilter> zqlFiltersAsList = null;
		// if sharePermType is PRIVATE and owner!=logggedInUser, return blank data map (we don't return private filters for other users). 
		if(StringUtils.isNotBlank(owner) && sharePermType == ZQLFilterShareType.PRIVATE.getShareTypeIntVal() && !loggedInUser.equals(owner)){
			List<ZQLFilter> filters = new ArrayList<ZQLFilter>();
			return ImmutableMap.of("totalCount", 0, "zqlFilters", filters);			
		}	
		// if sharePermType = -1 (fetch all global and private filters), owner is null and filterName is not null 
		if(sharePermType == -1 && StringUtils.isBlank(owner) && StringUtils.isNotBlank(filterName)){
			zqlFiltersAsList = quickSearchZQLFilters(filterName, loggedInUser);
			Collections.sort(zqlFiltersAsList,FilterNameComparator.COMPARATOR);
			filtersMap = ImmutableMap.of("totalCount", zqlFiltersAsList.size(), "zqlFilters", zqlFiltersAsList);
			return filtersMap;					
		}			
		// form Query for Searching filters
		Query query = formQueryForSearchFilter(filterName, owner, loggedInUser, sharePermType);
		// Fetch the records for the formed "Query".
		zqlFilters = ao.find(ZQLFilter.class, query);
		zqlFiltersAsList = Arrays.asList(zqlFilters);
		Collections.sort(zqlFiltersAsList,FilterNameComparator.COMPARATOR);
		filtersMap = ImmutableMap.of("totalCount", zqlFiltersAsList.size(), "zqlFilters", zqlFiltersAsList);
		return filtersMap;		
	}
	
	private Query formQueryForSearchFilter(String filterName, String owner, String loggedInUser, Integer sharePermType){
		Query query =  Query.select();
		query.alias(ZQLFilter.class, "AO_7DEABF_ZQLFILTER");
		query.alias(ZQLSharePermissions.class, "AO_7DEABF_ZQLSHARE_PERMISSIONS");
		List<String> whereClauses = new ArrayList<String>();
		List<Object> params = new ArrayList<Object>();		
		
		// Create the "Query" based on input conditions
		if(StringUtils.isNotBlank(filterName) ) {
			DatabaseConfig dbConfig = ((DatabaseConfigurationManager) ComponentAccessor.getComponent(DatabaseConfigurationManager.class)).
					getDatabaseConfiguration();
			if (JiraUtil.isCaseSensitiveDatabaseType(dbConfig.getDatabaseType())) {
				whereClauses.add("(UPPER(\"FILTER_NAME\") LIKE ? OR UPPER(\"DESCRIPTION\") LIKE ?)");
				params.add(filterName.toUpperCase()+"%");
				params.add("%"+filterName.toUpperCase()+"%");
			} else {
				whereClauses.add("(FILTER_NAME LIKE ? OR DESCRIPTION LIKE ?)");
				params.add(filterName+"%");
				params.add("%"+filterName+"%");
			}

		} 		
		// if sharePermType == ZQLFilterShareType.PRIVATE, fetch all private filters for logged in user.
		if(sharePermType == ZQLFilterShareType.PRIVATE.getShareTypeIntVal()){
			whereClauses.add("CREATED_BY = ?"); params.add(loggedInUser);
		}	
		// else fetch global filters for given owner name
		else if(StringUtils.isNotBlank(owner) && sharePermType == ZQLFilterShareType.GLOBAL.getShareTypeIntVal()){			
			whereClauses.add("CREATED_BY = ?"); params.add(owner);
		}
		//if sharePermType = -1 (fetch all global and private filters) and !loggedInUser.equals(owner),
		// fetch all global filters for the owner
		if(sharePermType == -1 && StringUtils.isNotBlank(owner) && !loggedInUser.equals(owner)){
			// joining on AO_7DEABF_ZQLSHARE_PERMISSIONS
			query = query.join(ZQLSharePermissions.class, 
							"AO_7DEABF_ZQLSHARE_PERMISSIONS.ZQLFILTER_ID = AO_7DEABF_ZQLFILTER.ID");
			whereClauses.add("CREATED_BY = ?"); params.add(owner);
			whereClauses.add("AO_7DEABF_ZQLSHARE_PERMISSIONS.SHARE_TYPE = ?"); 
			params.add(ZQLFilterShareType.GLOBAL.getShareType());
		} 
		//if sharePermType = -1 (fetch all global and private filters) and loggedInUser.equals(owner),
		// fetch all global filters for the owner
		else if(sharePermType == -1 && loggedInUser.equals(owner)){
			whereClauses.add("CREATED_BY = ?"); params.add(owner);
		} 
		//if sharePermType = -1 (fetch all global and private filters) and owner is null,
		// fetch all filters for logged in user.
		else if(sharePermType == -1 && StringUtils.isBlank(owner) && StringUtils.isBlank(filterName)){
			whereClauses.add("CREATED_BY = ?"); params.add(loggedInUser);
		}	
		//if sharePermType != -1, fetch all global filters for the owner		
		else if(sharePermType != -1){
			// joining on AO_7DEABF_ZQLSHARE_PERMISSIONS
			query = query.join(ZQLSharePermissions.class, 
							"AO_7DEABF_ZQLSHARE_PERMISSIONS.ZQLFILTER_ID = AO_7DEABF_ZQLFILTER.ID");
			whereClauses.add("AO_7DEABF_ZQLSHARE_PERMISSIONS.SHARE_TYPE = ?"); 
			params.add(ZQLFilterShareType.getZQLFilterShareType(sharePermType).getShareType());
		}		

		if(whereClauses.size() > 0)
			query = query.where(StringUtils.join(whereClauses, " and "), params.toArray()); 			
		
		return query;
	}
	
	@Override
	public List<ZQLFilter> quickSearchZQLFilters(String queryParam, String loggedInUser) {
		log.debug("Quick Searching ZQL Filters");
		List<ZQLFilter> zqlFiltersAsList = new ArrayList<ZQLFilter>();
        List<String> whereClauses = new ArrayList<String>();
        List<Object> params = new ArrayList<Object>();
		ZQLFilter[] zqlFilters = null;

        // first fetch all filters created by logged in user (both global and private)
        Query query =  Query.select();
        DatabaseConfig dbConfig = ((DatabaseConfigurationManager) ComponentAccessor.getComponent(DatabaseConfigurationManager.class)).
                getDatabaseConfiguration();
        if(JiraUtil.isCaseSensitiveDatabaseType(dbConfig.getDatabaseType())) {
			whereClauses.add("(UPPER(\"FILTER_NAME\") LIKE ? OR UPPER(\"DESCRIPTION\") LIKE ?)");
			params.add(queryParam.toUpperCase()+"%");
			params.add("%"+queryParam.toUpperCase()+"%");
		} else {
			whereClauses.add("(FILTER_NAME LIKE ? OR DESCRIPTION LIKE ?)");
			params.add(queryParam+"%");
			params.add("%"+queryParam+"%");
		}


        whereClauses.add("CREATED_BY = ?");
        params.add(loggedInUser);

        if(whereClauses.size() > 0)
            query = query.where(StringUtils.join(whereClauses, " and "), params.toArray());

		zqlFilters = ao.find(ZQLFilter.class, query.order("FILTER_NAME ASC"));
		zqlFiltersAsList.addAll(Arrays.asList(zqlFilters));

		// clause to fetch only "global" shared filters created by other users.		
		query =  Query.select();
		query.alias(ZQLFilter.class, "AO_7DEABF_ZQLFILTER");
		query.alias(ZQLSharePermissions.class, "AO_7DEABF_ZQLSHARE_PERMISSIONS");
		// joining on AO_7DEABF_ZQLSHARE_PERMISSIONS
		query = query.join(ZQLSharePermissions.class, 
						"AO_7DEABF_ZQLSHARE_PERMISSIONS.ZQLFILTER_ID = AO_7DEABF_ZQLFILTER.ID");
        whereClauses.remove("CREATED_BY = ?");
        whereClauses.add("CREATED_BY != ?");
        whereClauses.add("AO_7DEABF_ZQLSHARE_PERMISSIONS.SHARE_TYPE = ?");
        params.add(ZQLFilterShareType.getZQLFilterShareType(1).getShareType());
        if(whereClauses.size() > 0)
            query = query.where(StringUtils.join(whereClauses, " and "), params.toArray());
		// Fetch the records for the formed "Query".
		zqlFilters = ao.find(ZQLFilter.class, query.order("FILTER_NAME ASC"));
		zqlFiltersAsList.addAll(Arrays.asList(zqlFilters));
		Collections.sort(zqlFiltersAsList,FilterNameComparator.COMPARATOR);
		return zqlFiltersAsList;		
	}	
	
	@Override
	public ZQLFavoriteAsoc[] getZQLFavoriteAsoc(ZQLFilter zqlFilter, String user) {
		ZQLFavoriteAsoc[] zqlFavoriteAsocArr = null;
		log.debug("Fetching  a ZQLFavoriteAsoc by ZQLFilter");
		zqlFavoriteAsocArr = ao.find(ZQLFavoriteAsoc.class, Query.select().where("ZQLFILTER_ID = ? AND USER = ?", zqlFilter.getID(), user));
		return zqlFavoriteAsocArr;
	}	

	@Override
	public void createZQLFavoriteAsoc(ZQLFilter zqlFilter, String user) {
		ZQLFavoriteAsoc zqlFavoriteAsoc = ao.create(ZQLFavoriteAsoc.class, ImmutableMap.of("USER", (Object)user));
		zqlFavoriteAsoc.setZQLFilter(zqlFilter);
		zqlFavoriteAsoc.save();			
	}

	@Override
	public void deleteZQLFavoriteAsoc(ZQLFavoriteAsoc[] zqlFavoriteAsocArr) {
		ao.delete(zqlFavoriteAsocArr);		
	}
	
	@Override
	public int removeFavoriteFromPrivateFilters(String user, Integer zqlId){
		int removedCnt = 0;
		ZQLFavoriteAsoc[] zqlFavoriteAsoc = ao.find(ZQLFavoriteAsoc.class, Query.select().where("USER != ? AND ZQLFILTER_ID = ? ", user, zqlId));
		removedCnt = zqlFavoriteAsoc.length;
		ao.delete(zqlFavoriteAsoc);	
		return removedCnt;
	}
}
