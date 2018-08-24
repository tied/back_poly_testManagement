package com.thed.zephyr.je.zql.service;

import java.util.List;
import java.util.Map;

import com.atlassian.activeobjects.tx.Transactional;
import com.thed.zephyr.je.zql.model.ZQLFavoriteAsoc;
import com.thed.zephyr.je.zql.model.ZQLFilter;
import com.thed.zephyr.je.zql.model.ZQLSharePermissions;

@Transactional
public interface ZQLFilterManager {

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
	Map<String, Object> getZQLFilters(String user, Boolean isFav, Integer offset, Integer maxRecords);
	
	/** Retrieves all FAVORITE ZQL filters for the given user  
     * @param user 
     * @param offset
     * @param maxRecords
	 * @return Map<String, Object>: totalCount and list of retrieved filters. 
	 */	
	Map<String, Object> getAllFaoritesZQLFiltersByUser(String user, Integer offset, Integer maxRecords);
	
	/** Retrieves all NON-FAVORITE ZQL filters for the given user  
     * @param user 
     * @param offset
     * @param maxRecords
	 * @return Map<String, Object>: totalCount and list of retrieved filters. 
	 */	
	Map<String, Object> getAllNonFaoritesZQLFiltersByUser(String user, Integer offset, Integer maxRecords);	
	
	/** Retrieves all ZQL filters for the given user  
     * @param user 
     * @param offset
     * @param maxRecords
	 * @return Map<String, Object>: totalCount and list of retrieved filters. 
	 */	
	Map<String, Object> getAllZQLFiltersByUser(String user, Integer offset, Integer maxRecords);	
	
	/** Retrieves all Popular ZQL filters. 
     * @param offset
     * @param maxRecords
     * @param isFav 
	 * @return Map<String, Object>: totalCount and list of retrieved filters. 
	 */		
	Map<String, Object> getPopularZQLFilters(Integer offset, Integer maxRecords, Boolean isFav);	
	
	/** Retrieves all Global ZQL filters. 
     * @param offset
     * @param maxRecords
	 * @return Map<String, Object>: totalCount and list of retrieved filters. 
	 */		
	Map<String, Object> getAllGlobalZQLFilters(Integer offset, Integer maxRecords);		
    
    /**
     * Gets ZQLFilter's information based on id.
     * @param id the ZQLFilter's id
     * @return zqlFilter populated ZQLFilter object
     */
    ZQLFilter getZQLFilter(final Integer id);
    
    /**
     * Gets ZQLFilter's information based on FilterName.
     * @param filterName the ZQLFilter's name
     * @return zqlFilter populated ZQLFilter object
     */
    ZQLFilter getZQLFilter(final String filterName);    
    
    /**
     * Saves a ZQLFilter information
     * @param zqlFilterProperties the object to be saved
     * @return zqlFilter saved ZQLFilter object
     */
    ZQLFilter saveZQLFilter(Map<String, Object> zqlFilterProperties);
	
    /**
     * Removes a ZQLFilter from the database.
     * @param ZQLFilter
     */
    void removeZQLFilter(final ZQLFilter zqlFilter);
    
    /**
     * Updates a ZQLFilter information
     * @param zqlFilterProperties the object to be saved
     * @return zqlFilter updated ZQLFilter object
     */
    ZQLFilter updateZQLFilter(Map<String, Object> zqlFilterProperties); 
    
    /**
     * Get ZQLSharePermissions for a given ZQLFilter
     * @param zqlFilter
     * @return ZQLSharePermissions
     */
    ZQLSharePermissions getZQLSharePermissions(ZQLFilter zqlFilter);

	/**
     * Search for ZQL filters by provided params
     * @param filterName
     * @param owner
     * @param sharePermType
     * @return Map<String, Object>: totalCount and list of searched filters. 
     */
	Map<String, Object> searchZQLFilters(String filterName, String owner, String loggedInUser, Integer sharePermType);	
	
	/**
     * Quick Search for ZQL filters by provided query param
     * @param query
     * @param loggedInUser
     * @return List<ZQLFilter>: a list of searched filters. 
     */
	List<ZQLFilter> quickSearchZQLFilters(String queryParam, String loggedInUser);		
	
	/**
	 * Given a ZQLFilter and logged in user, finds the Favorite association.
	 * @param zqlFilter
	 * @param user
	 * @return ZQLFavoriteAsoc[]
	 */
	ZQLFavoriteAsoc[] getZQLFavoriteAsoc(ZQLFilter zqlFilter, String user);
	
	/**
	 * Delete a ZQLFavoriteAsoc
	 * @param zqlFavoriteAsocArr
	 * @param user
	 */
	void deleteZQLFavoriteAsoc(ZQLFavoriteAsoc[] zqlFavoriteAsocArr);
	
	/**
	 * Create a ZQLFavoriteAsoc
	 * @param zqlFilter
	 * @param user
	 */	
	void createZQLFavoriteAsoc(ZQLFilter zqlFilter, String user);
	
	/**
	 * Find favorite filters entries from ZQLFavoriteAsoc where user != user,
	 * remove those records and return the no. of records deleted.
	 * @param user
	 * @param zqlId
	 * @return deletedRecordsCount
	 */		
	int removeFavoriteFromPrivateFilters(String user, Integer zqlId);

}
