package com.thed.zephyr.je.service;

import java.util.List;
import java.util.Map;

/**
 * @author manjunath
 *
 */
public interface DatacenterManager {
	
	List<Map<String, Object>> getExecutionCountByCycle(Integer offset, Integer limit);
    
	List<Map<String, Object>> getExecutionCountByFolder(Integer offset, Integer limit);
	
	List<Map<String, Object>> getTeststepResultCountByExecution(Integer offset, Integer limit);
	
	List<Map<String, Object>> getTeststepCountByIssue(Integer offset, Integer limit);
	
	List<Map<String, Object>> getIssueCountByProject(Integer offset, Integer limit);
	
	Map<String, Integer> getTotalCountForAllQueries();

}
