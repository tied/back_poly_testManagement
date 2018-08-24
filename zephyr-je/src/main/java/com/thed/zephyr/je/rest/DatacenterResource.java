
package com.thed.zephyr.je.rest;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.rest.delegate.DatacenterResourceDelegate;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.vo.RecoveryFormBean;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;

/**
 * @author manjunath
 *
 */
@Path("datacenter")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@ResourceFilters(ZFJApiFilter.class)
public class DatacenterResource {
	
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
	protected final Logger log = Logger.getLogger(DatacenterResource.class);

	private final JiraAuthenticationContext authContext;
	private final DatacenterResourceDelegate datacenterResourceDelegate;
	
	
	public DatacenterResource(JiraAuthenticationContext authContext, DatacenterResourceDelegate datacenterResourceDelegate) {
		this.authContext = authContext;
		this.datacenterResourceDelegate = datacenterResourceDelegate;
	}

	@PUT
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/backupIndex")
	public Response scheduleBackUpIndexJob(@QueryParam("flag") boolean flag,@QueryParam("expression") String expression,RecoveryFormBean recoveryFormBean) {
		final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution count.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
		try {
			 String userId = String.valueOf(authContext.getLoggedInUser().getId());
			return datacenterResourceDelegate.indexAllUsingCron(expression,flag,userId);
		} catch (Exception ex) {
        	log.error(String.format(ERROR_LOG_MESSAGE,Status.INTERNAL_SERVER_ERROR.getStatusCode(),Status.INTERNAL_SERVER_ERROR, ex.toString()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).cacheControl(ZephyrCacheControl.never()).build();
        }
	}
	
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/backupRecovery")
	public Response getrecoveryForm() {
		final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution count.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
		try {
			return datacenterResourceDelegate.getrecoveryForm();
		} catch (Exception ex) {
        	log.error(String.format(ERROR_LOG_MESSAGE,Status.INTERNAL_SERVER_ERROR.getStatusCode(),Status.INTERNAL_SERVER_ERROR, ex.toString()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).cacheControl(ZephyrCacheControl.never()).build();
        }
	}
	
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/integritycheck")
    public Response getIntegrityCheckData(@QueryParam("zicTotalExecutionCount") boolean zicTotalExecutionCount, @QueryParam("zicTotalCycleCount") boolean zicTotalCycleCount,
                                      @QueryParam("zicExecutionCountByCycle") boolean zicExecutionCountByCycle, @QueryParam("zicExecutionCountByFolder") boolean zicExecutionCountByFolder,
                                      @QueryParam("zicIssueCountByProject") boolean zicIssueCountByProject, @QueryParam("zicTeststepResultCountByExecution") boolean zicTeststepResultCountByExecution,
                                      @QueryParam("zicTeststepCountByIssue") boolean zicTeststepCountByIssue, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit)  {
        final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution count.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        if(offset == null) {
        	offset = 0;
        }
        
        if(limit == null) {
        	limit = 10;
        }
        

        try {
        	jsonObject =  datacenterResourceDelegate.getDataForIntegrityCheck(zicTotalExecutionCount, zicTotalCycleCount, zicExecutionCountByCycle,
        			zicExecutionCountByFolder, zicIssueCountByProject, zicTeststepResultCountByExecution, zicTeststepCountByIssue, offset, limit);
            return Response.ok().entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        } catch (Exception ex) {
        	log.error(String.format(ERROR_LOG_MESSAGE,Status.INTERNAL_SERVER_ERROR.getStatusCode(),Status.INTERNAL_SERVER_ERROR, ex.toString()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).cacheControl(ZephyrCacheControl.never()).build();
        }
    }	
	
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/downloadSupportzip")
	public Response downloadSupportZip(@Context HttpServletRequest request, @QueryParam("zfjlogs") boolean zfjlogs, @QueryParam("zfjdb") boolean zfjdb, @QueryParam("zfjshared") boolean zfjshared,
			@QueryParam("zfjtomcatlog") boolean zfjtomcatlog) throws Exception {

		Map<String, Boolean> downlaodFilesFlag = new HashMap<>();
		downlaodFilesFlag.put(ApplicationConstants.ZFJ_SUPPORT_CHECKLIST[0], zfjlogs);
		downlaodFilesFlag.put(ApplicationConstants.ZFJ_SUPPORT_CHECKLIST[1], zfjdb);
		downlaodFilesFlag.put(ApplicationConstants.ZFJ_SUPPORT_CHECKLIST[2], zfjshared);
		downlaodFilesFlag.put(ApplicationConstants.ZFJ_SUPPORT_CHECKLIST[3], zfjtomcatlog);
		final ApplicationUser user = authContext.getLoggedInUser();
		JSONObject jsonObject = new JSONObject();
		try {
			if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
				log.error(String.format(ERROR_LOG_MESSAGE, Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED,
						authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString())
						.cacheControl(ZephyrCacheControl.never()).build();
			}
		} catch (JSONException e) {
			log.error("Error occurred while getting execution count.", e);
			return Response.status(Status.BAD_REQUEST).build();
		}
		return datacenterResourceDelegate.getSupporttoolFiles(downlaodFilesFlag);
	}
	
	@PUT
	@Path("/indexRecovery")
	public Response indexRecovery(Map<String, Object> params) {
		final ApplicationUser user = authContext.getLoggedInUser();
        String errorMessage = null;
        JSONObject errorObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                errorObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(errorObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution count.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
		if(!params.containsKey("indexRecoveryFileName")) {
			errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.index.backup.filename.required");
			return constructErrorResponse(errorObject, errorMessage, Status.FORBIDDEN, null);
		}
		String indexRecoveryFileName = (String)params.get("indexRecoveryFileName");
        try {
        	String fullIndexRecoveryFilePath = ApplicationConstants.ZFJ_SHARED_HOME_PATH + ApplicationConstants.INDEX_BACKUP_FOLDER_NAME + "/" + indexRecoveryFileName + ApplicationConstants.ZIP_EXTENSION;
        	return datacenterResourceDelegate.recoverIndexBackup(fullIndexRecoveryFilePath, indexRecoveryFileName);
        } catch(FileNotFoundException ex) {
        	errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.index.backup.file.not.found", indexRecoveryFileName);
        	return constructErrorResponse(errorObject, errorMessage, Status.NOT_FOUND, null);
        } catch(ParseException ex) {
        	errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.index.backup.file.format.error", indexRecoveryFileName);
        	return constructErrorResponse(errorObject, errorMessage, Status.BAD_REQUEST, null);
        } catch(Exception ex) {
        	return constructErrorResponse(errorObject, authContext.getI18nHelper().getText("zephyr.common.internal.server.error"), Status.INTERNAL_SERVER_ERROR, ex);
        }     
	}
	
	@GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/exportIntegrityCheck")
    public Response exportExecution(@QueryParam("zicTotalExecutionCount") boolean zicTotalExecutionCount, @QueryParam("zicTotalCycleCount") boolean zicTotalCycleCount,
            @QueryParam("zicExecutionCountByCycle") boolean zicExecutionCountByCycle, @QueryParam("zicExecutionCountByFolder") boolean zicExecutionCountByFolder,
            @QueryParam("zicIssueCountByProject") boolean zicIssueCountByProject, @QueryParam("zicTeststepResultCountByExecution") boolean zicTeststepResultCountByExecution,
            @QueryParam("zicTeststepCountByIssue") boolean zicTeststepCountByIssue, @QueryParam("offset") Integer offset, @QueryParam("limit") Integer limit) {
		final ApplicationUser user = authContext.getLoggedInUser();
        JSONObject jsonObject = new JSONObject();
        try {
            if (user == null && !JiraUtil.hasAnonymousPermission(user)) {
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error occurred while getting execution count.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        
        if(offset == null) {
        	offset = 0;
        }
        
        if(limit == null || limit.equals(0)) {
        	limit = Integer.MAX_VALUE;
        }

        try {
        	return datacenterResourceDelegate.exportIntegrityCheckData(zicTotalExecutionCount, zicTotalCycleCount, zicExecutionCountByCycle,
        			zicExecutionCountByFolder, zicIssueCountByProject, zicTeststepResultCountByExecution, zicTeststepCountByIssue, offset, limit);
        } catch (Exception ex) {
        	log.error(String.format(ERROR_LOG_MESSAGE,Status.INTERNAL_SERVER_ERROR.getStatusCode(),Status.INTERNAL_SERVER_ERROR, ex.toString()));
            return Response.status(Status.INTERNAL_SERVER_ERROR).cacheControl(ZephyrCacheControl.never()).build();
        }
      
    }
	
	private Response constructErrorResponse(JSONObject jsonObject, String errorMessage, Status status, Exception exception) {
        try {
    		String finalErrorMessage = String.format(ERROR_LOG_MESSAGE, status.getStatusCode(), status, errorMessage);
            log.error(finalErrorMessage, exception);
			jsonObject.put("error", errorMessage);
			return Response.status(status).entity(jsonObject != null ? jsonObject.toString() : finalErrorMessage).cacheControl(ZephyrCacheControl.never()).build();
		} catch (JSONException e) {
			log.error("Eror while constructing the error response");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}		
	}
	}
