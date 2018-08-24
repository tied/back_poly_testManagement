package com.thed.zephyr.je.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.jdbc.SQLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.database.DatabaseConfig;
import com.atlassian.jira.config.database.DatabaseConfigurationManager;
import com.thed.zephyr.je.service.DatacenterManager;
import com.thed.zephyr.util.ApplicationConstants;

/**
 * @author manjunath
 *
 */
public class DatacenterManagerImpl extends BaseManagerImpl implements DatacenterManager {
	
	private static final Logger log = LoggerFactory.getLogger(DatacenterManagerImpl.class);	
	private static final String POSTGRES_DB = "postgres";
	private static final String MYSQL_DB = "mysql";
	private static final String ORACLE_DB = "oracle";
	private static final String SQL_SERVER = "mssql";
	
	private static final String EXECUTIONS_CYCLE_COUNT_SQL_OTHERS = "select cycle_id,count(*) from AO_7DEABF_SCHEDULE group by cycle_id order by cycle_id";
	private static final String EXECUTIONS_CYCLE_COUNT_SQL_POSTGRES = "select \"CYCLE_ID\",count(*) from \"AO_7DEABF_SCHEDULE\" group by \"CYCLE_ID\" order by \"CYCLE_ID\"";
	
	private static final String EXECUTIONS_FOLDER_COUNT_SQL_OTHERS = "select folder_id, count(*) from AO_7DEABF_SCHEDULE where folder_id is not null group by folder_id order by folder_id";
	private static final String EXECUTIONS_FOLDER_COUNT_SQL_POSTGRES = "select \"FOLDER_ID\", count(*) from \"AO_7DEABF_SCHEDULE\" where \"FOLDER_ID\" is not null group by \"FOLDER_ID\" order by \"FOLDER_ID\"";
	
	private static final String TESTSTEP_COUNT_EXECUTION_SQL_OTHERS = "select schedule_id, count(*) from AO_7DEABF_STEP_RESULT group by schedule_id order by schedule_id";
	private static final String TESTSTEP_COUNT_EXECUTION_SQL_POSTGRES = "select \"SCHEDULE_ID\", count(*) from \"AO_7DEABF_STEP_RESULT\" group by \"SCHEDULE_ID\" order by \"SCHEDULE_ID\"";
	
	
	private static final String TESTSTEP_COUNT_ISSUE_SQL_OTHERS = "select issue_id, count(*) from AO_7DEABF_TESTSTEP group by issue_id order by issue_id";
	private static final String TESTSTEP_COUNT_ISSUE_SQL_POSTGRES = "select \"ISSUE_ID\", count(*) from \"AO_7DEABF_TESTSTEP\" group by \"ISSUE_ID\" order by \"ISSUE_ID\"";
	
	private static final String ISSUE_COUNT_PROJECT_SQL = "select project, count(*) from jiraissue where issuetype IN (select id from issuetype where pname='Test') group by project order by project";
	
	private static final String EXECUTIONS_CYCLE_COUNT_SQLSERVER = "select DISTINCT COUNT(*) OVER () AS TotalRecords from AO_7DEABF_SCHEDULE group by cycle_id";	
	private static final String EXECUTIONS_FOLDER_COUNT_SQLSERVER = "select DISTINCT COUNT(*) OVER () AS TotalRecords from AO_7DEABF_SCHEDULE where folder_id is not null group by folder_id";	
	private static final String TESTSTEP_COUNT_EXECUTION_SQLSERVER = "select DISTINCT COUNT(*) OVER () AS TotalRecords from AO_7DEABF_STEP_RESULT group by schedule_id";	
	private static final String TESTSTEP_COUNT_ISSUE_SQLSERVER = "select DISTINCT COUNT(*) OVER () AS TotalRecords from AO_7DEABF_TESTSTEP group by issue_id";	
	private static final String ISSUE_COUNT_PROJECT_SQLSERVER = "select DISTINCT COUNT(*) OVER () AS TotalRecords from jiraissue where issuetype IN (select id from issuetype where pname='Test') group by project";
	
	public DatacenterManagerImpl(ActiveObjects ao) {
		super(checkNotNull(ao));
	}
	
	@Override
	public List<Map<String, Object>> getExecutionCountByCycle(Integer offset, Integer limit) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
			return getEntityCount(EXECUTIONS_CYCLE_COUNT_SQL_POSTGRES + " LIMIT " + limit + " OFFSET " + offset);
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), MYSQL_DB)) {
			return getEntityCount(EXECUTIONS_CYCLE_COUNT_SQL_OTHERS +  " LIMIT " +  limit + " OFFSET " + offset);
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ORACLE_DB)) {
			return getEntityCount(getOracleQueryWithOffsetAndLimit(EXECUTIONS_CYCLE_COUNT_SQL_OTHERS, offset, limit));
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), SQL_SERVER)) {
			return getEntityCount(getMSSQLQueryWithOffsetAndLimit(EXECUTIONS_CYCLE_COUNT_SQL_OTHERS, offset, limit));
		} else {
			return new ArrayList<>(0);
		}
	}
	
	@Override
	public List<Map<String, Object>> getExecutionCountByFolder(Integer offset, Integer limit) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
			return getEntityCount(EXECUTIONS_FOLDER_COUNT_SQL_POSTGRES + " LIMIT " + limit + " OFFSET " + offset);
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), MYSQL_DB)) {
			return getEntityCount(EXECUTIONS_FOLDER_COUNT_SQL_OTHERS + " LIMIT " +  limit + " OFFSET " + offset);
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ORACLE_DB)) {
			return getEntityCount(getOracleQueryWithOffsetAndLimit(EXECUTIONS_FOLDER_COUNT_SQL_OTHERS, offset, limit));
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), SQL_SERVER)){
			return getEntityCount(getMSSQLQueryWithOffsetAndLimit(EXECUTIONS_FOLDER_COUNT_SQL_OTHERS, offset, limit));
		} else {
			return new ArrayList<>(0);
		}
	}
	
	@Override
	public List<Map<String, Object>> getTeststepResultCountByExecution(Integer offset, Integer limit) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
			return getEntityCount(TESTSTEP_COUNT_EXECUTION_SQL_POSTGRES + " LIMIT " + limit + " OFFSET " + offset);
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), MYSQL_DB)){
			return getEntityCount(TESTSTEP_COUNT_EXECUTION_SQL_OTHERS + " LIMIT " + limit + " OFFSET " + offset);
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ORACLE_DB)) {
			return getEntityCount(getOracleQueryWithOffsetAndLimit(TESTSTEP_COUNT_EXECUTION_SQL_OTHERS, offset, limit));
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), SQL_SERVER)) {
			return getEntityCount(getMSSQLQueryWithOffsetAndLimit(TESTSTEP_COUNT_EXECUTION_SQL_OTHERS, offset, limit));
		} else {
			return new ArrayList<>(0);
		}
	}
	
	@Override
	public List<Map<String, Object>> getTeststepCountByIssue(Integer offset, Integer limit) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
			return getEntityCount(TESTSTEP_COUNT_ISSUE_SQL_POSTGRES + " LIMIT " + limit + " OFFSET " + offset);
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), MYSQL_DB)) {
			return getEntityCount(TESTSTEP_COUNT_ISSUE_SQL_OTHERS + " LIMIT " + limit + " OFFSET " + offset);
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ORACLE_DB)) {
			return getEntityCount(getOracleQueryWithOffsetAndLimit(TESTSTEP_COUNT_ISSUE_SQL_OTHERS, offset, limit));
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), SQL_SERVER)) {
			return getEntityCount(getMSSQLQueryWithOffsetAndLimit(TESTSTEP_COUNT_ISSUE_SQL_OTHERS, offset, limit));
		} else {
			return new ArrayList<>(0);
		}
	}
	
	@Override
	public List<Map<String, Object>> getIssueCountByProject(Integer offset, Integer limit) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB) || StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), MYSQL_DB)) {
			return getEntityCount(ISSUE_COUNT_PROJECT_SQL + " LIMIT " + limit + " OFFSET " + offset);
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), ORACLE_DB)) {
			return getEntityCount(getOracleQueryWithOffsetAndLimit(ISSUE_COUNT_PROJECT_SQL, offset, limit));
		} else if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), SQL_SERVER))  {
			return getEntityCount(getMSSQLQueryWithOffsetAndLimit(ISSUE_COUNT_PROJECT_SQL, offset, limit));
		}  else {
			return new ArrayList<>(0);
		}
	}
	
	private List<Map<String, Object>> getEntityCount(String sqlQuery) {
		SQLProcessor sqlProcessor = null;
		try {
			sqlProcessor = new SQLProcessor("defaultDS");
			ResultSet resultSet = sqlProcessor.executeQuery(sqlQuery);
			Map<String, Object> objectMap = null;
			List<Map<String, Object>> entityCountDtoList = new ArrayList<>(resultSet.getFetchSize());
			int columnCount = resultSet.getMetaData().getColumnCount();
			while(resultSet.next()) {
				objectMap = new HashMap<>();
				if(columnCount >= 3) {
					objectMap.put("id", resultSet.getLong(2));
					objectMap.put("count", resultSet.getLong(3));
					entityCountDtoList.add(objectMap);
				} else {
					objectMap.put("id", resultSet.getLong(1));
					objectMap.put("count", resultSet.getLong(2));
					entityCountDtoList.add(objectMap);
				}				
			}
			resultSet.close();
			return entityCountDtoList;
		} catch(Exception ex) {
			log.error("Error while executing the query - " + sqlQuery, ex);
			throw new RuntimeException("Error while executing the query - " + sqlQuery, ex);
		} finally {
			try {
				if(sqlProcessor != null) 
					sqlProcessor.close();
			} catch(Exception ex) {
				log.error("Error while closing the sql processor connection ", ex);
			}
		}
	}
	
	public Map<String, Integer> getTotalCountForAllQueries() {
		Map<String, Integer> totalCountsMap = new HashMap<>();
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();
		if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), POSTGRES_DB)) {
			totalCountsMap.put(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_CYCLE, getTotalCountForQuery(EXECUTIONS_CYCLE_COUNT_SQL_POSTGRES, true));
			totalCountsMap.put(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_FOLDER, getTotalCountForQuery(EXECUTIONS_FOLDER_COUNT_SQL_POSTGRES, true));
			totalCountsMap.put(ApplicationConstants.ZIC_ISSUE_COUNT_BY_PROJECT, getTotalCountForQuery(ISSUE_COUNT_PROJECT_SQL, true));
			totalCountsMap.put(ApplicationConstants.ZIC_TESTSTEP_RESULT_COUNT_BY_EXECUTION, getTotalCountForQuery(TESTSTEP_COUNT_EXECUTION_SQL_POSTGRES, true));
			totalCountsMap.put(ApplicationConstants.ZIC_TESTSTEP_COUNT_BY_ISSUE, getTotalCountForQuery(TESTSTEP_COUNT_ISSUE_SQL_POSTGRES, true));			
		} else if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), SQL_SERVER)) {
			totalCountsMap.put(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_CYCLE, getTotalCountForQuery(EXECUTIONS_CYCLE_COUNT_SQLSERVER, false));
			totalCountsMap.put(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_FOLDER, getTotalCountForQuery(EXECUTIONS_FOLDER_COUNT_SQLSERVER, false));
			totalCountsMap.put(ApplicationConstants.ZIC_ISSUE_COUNT_BY_PROJECT, getTotalCountForQuery(ISSUE_COUNT_PROJECT_SQLSERVER, false));
			totalCountsMap.put(ApplicationConstants.ZIC_TESTSTEP_RESULT_COUNT_BY_EXECUTION, getTotalCountForQuery(TESTSTEP_COUNT_EXECUTION_SQLSERVER, false));
			totalCountsMap.put(ApplicationConstants.ZIC_TESTSTEP_COUNT_BY_ISSUE, getTotalCountForQuery(TESTSTEP_COUNT_ISSUE_SQLSERVER, false));			
		}  else {
			totalCountsMap.put(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_CYCLE, getTotalCountForQuery(EXECUTIONS_CYCLE_COUNT_SQL_OTHERS, true));
			totalCountsMap.put(ApplicationConstants.ZIC_EXECUTION_COUNT_BY_FOLDER, getTotalCountForQuery(EXECUTIONS_FOLDER_COUNT_SQL_OTHERS, true));
			totalCountsMap.put(ApplicationConstants.ZIC_ISSUE_COUNT_BY_PROJECT, getTotalCountForQuery(ISSUE_COUNT_PROJECT_SQL, true));
			totalCountsMap.put(ApplicationConstants.ZIC_TESTSTEP_RESULT_COUNT_BY_EXECUTION, getTotalCountForQuery(TESTSTEP_COUNT_EXECUTION_SQL_OTHERS, true));
			totalCountsMap.put(ApplicationConstants.ZIC_TESTSTEP_COUNT_BY_ISSUE, getTotalCountForQuery(TESTSTEP_COUNT_ISSUE_SQL_OTHERS, true));
		}		
		return totalCountsMap;
	}
	
	private Integer getTotalCountForQuery(String sqlQuery, boolean appendCountStar) {
		SQLProcessor sqlProcessor = null;
		try {
			sqlProcessor = new SQLProcessor("defaultDS");
			ResultSet resultSet =  null;
			if(appendCountStar) {
				resultSet = sqlProcessor.executeQuery("select count(*) from (" + sqlQuery + ") temp");
			} else {
				resultSet = sqlProcessor.executeQuery(sqlQuery);
			}
			
			Integer totalCount = 0;
			while(resultSet.next()) { 
				totalCount = resultSet.getInt(1);
			}
			resultSet.close();
			return totalCount;
		} catch(Exception ex) {
			log.error("Error while executing the total count query - " + sqlQuery, ex);
			throw new RuntimeException("Error while executing the query - " + sqlQuery, ex);
		} finally {
			try {
				if(sqlProcessor != null) 
					sqlProcessor.close();
			} catch(Exception ex) {
				log.error("Error while closing the sql processor connection ", ex);
			}
		}
	}
	
	
	private String getOracleQueryWithOffsetAndLimit(String query, Integer offset, Integer limit) {
		StringBuilder finalQuery = new StringBuilder("select tmp.* from (select rownum offset, rs.* from (");
		finalQuery.append(query);
		finalQuery.append(") rs) tmp where rownum <= "+ limit + " and offset > "+ offset);		
		return finalQuery.toString();
	}
	
	private String getMSSQLQueryWithOffsetAndLimit(String query, Integer offset, Integer limit) {
		StringBuilder finalQuery = new StringBuilder(query);
		finalQuery.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(limit).append(" ROWS ONLY");
		return finalQuery.toString();
	}

}