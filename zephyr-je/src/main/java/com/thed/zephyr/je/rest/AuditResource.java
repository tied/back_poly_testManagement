package com.thed.zephyr.je.rest;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.opensymphony.util.TextUtils;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.audit.model.ChangeZJEItem;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.vo.AuditItemBean;
import com.thed.zephyr.je.vo.AuditLogBean;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;
import com.thed.zephyr.util.ZephyrWikiParser;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.opensymphony.util.TextUtils.htmlEncode;

@Api(value = "Audit Resource API(s)", description = "Following section describes the rest resources pertaining to AuditResource")
@Path("audit")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class AuditResource {

    protected final Logger log = Logger.getLogger(AuditResource.class);
    private final AuditManager auditManager;
    private final JiraAuthenticationContext authContext;
    private final IssueManager issueManager;
    private final AvatarService avatarService;
    private static final String STEP = "step";
    private static final String DATA = "data";
    private static final String RESULT = "result";
    private static final String EXECUTION_DEFECT = "EXECUTION_DEFECT";
    private static final String EXECUTION_CUSTOM_FIELD = "EXECUTION_CUSTOMFIELD_UPDATED";
    private static final String USER_AVATAR = "useravatar";

    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

    public AuditResource(AuditManager auditManager, JiraAuthenticationContext authContext, IssueManager issueManager,
                         AvatarService avatarService, DateTimeFormatterFactory dateTimeFormatterFactory) {
        this.auditManager = auditManager;
        this.authContext = authContext;
        this.issueManager = issueManager;
        this.avatarService = avatarService;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
    }

    @ApiOperation(value = "Get Audit Log", notes = "Get Audit Log by Entity Type, Event, User")
    @ApiImplicitParams({ @ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"totalItemsCount\":6859,\"auditLogs\":[{\"entityId\":\"10737\",\"entityType\":\"execution\",\"entityEvent\":\"execution attachment added\",\"auditItems\":{\"id\":7503,\"field\":\"attachment\",\"oldValue\":\"\",\"newValue\":\"application--7--soapui-project.xml\"},\"creationDate\":\"Today 3:53 PM\",\"issueKey\":\"TEST-5\",\"executionId\":\"10737\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"141\",\"entityType\":\"stepresult\",\"entityEvent\":\"stepresult attachment added\",\"auditItems\":{\"id\":7502,\"field\":\"attachment\",\"oldValue\":\"\",\"newValue\":\"ZFJ_BVT_3.2.xls\"},\"creationDate\":\"Today 3:44 PM\",\"executionId\":\"10737\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"141\",\"entityType\":\"stepresult\",\"entityEvent\":\"stepresult attachment added\",\"auditItems\":{\"id\":7501,\"field\":\"attachment\",\"oldValue\":\"\",\"newValue\":\"ZFJ_BVT_je-3.2.0.32002574_JiraSoftware_7.xls\"},\"creationDate\":\"Today 3:44 PM\",\"executionId\":\"10737\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"141\",\"entityType\":\"stepresult\",\"entityEvent\":\"stepresult attachment added\",\"auditItems\":{\"id\":7500,\"field\":\"attachment\",\"oldValue\":\"\",\"newValue\":\"zfj-bamboo-bvt.xls\"},\"creationDate\":\"Today 3:44 PM\",\"executionId\":\"10737\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"10737\",\"entityType\":\"execution\",\"entityEvent\":\"execution attachment added\",\"auditItems\":{\"id\":7499,\"field\":\"attachment\",\"oldValue\":\"\",\"newValue\":\"je-3.2.0.32002543.obr\"},\"creationDate\":\"Today 3:44 PM\",\"issueKey\":\"TEST-5\",\"executionId\":\"10737\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"10737\",\"entityType\":\"execution\",\"entityEvent\":\"execution attachment added\",\"auditItems\":{\"id\":7498,\"field\":\"attachment\",\"oldValue\":\"\",\"newValue\":\"je-3.2.0.32002548.obr\"},\"creationDate\":\"Today 3:44 PM\",\"issueKey\":\"TEST-5\",\"executionId\":\"10737\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"10737\",\"entityType\":\"execution\",\"entityEvent\":\"execution attachment added\",\"auditItems\":{\"id\":7497,\"field\":\"attachment\",\"oldValue\":\"\",\"newValue\":\"je-3.2.0.32002550.obr\"},\"creationDate\":\"Today 3:44 PM\",\"issueKey\":\"TEST-5\",\"executionId\":\"10737\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"10737\",\"entityType\":\"execution\",\"entityEvent\":\"execution updated\",\"auditItems\":{\"id\":7496,\"field\":\"execution_defect\",\"oldValue\":\"\",\"newValue\":\"12705\"},\"creationDate\":\"Today 3:09 PM\",\"issueKey\":\"TEST-5\",\"executionId\":\"10737\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"54\",\"entityType\":\"execution\",\"entityEvent\":\"execution deleted\",\"auditItems\":{\"id\":7495,\"field\":\"execution_id\",\"oldValue\":\"54\",\"newValue\":\"\"},\"creationDate\":\"Today 2:28 PM\",\"issueKey\":\"MEMO-17\",\"executionId\":\"54\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"2097\",\"entityType\":\"teststep\",\"entityEvent\":\"teststep deleted\",\"auditItems\":{\"id\":7494,\"field\":\"N/A\",\"oldValue\":\"2097\",\"newValue\":\"\"},\"creationDate\":\"Today 1:03 PM\",\"issueKey\":\"TEST-5\",\"executionId\":\"-1\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"2100\",\"entityType\":\"teststep\",\"entityEvent\":\"teststep deleted\",\"auditItems\":{\"id\":7493,\"field\":\"N/A\",\"oldValue\":\"2100\",\"newValue\":\"\"},\"creationDate\":\"Today 12:52 PM\",\"issueKey\":\"TEST-5\",\"executionId\":\"-1\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"2099\",\"entityType\":\"teststep\",\"entityEvent\":\"teststep deleted\",\"auditItems\":{\"id\":7492,\"field\":\"N/A\",\"oldValue\":\"2099\",\"newValue\":\"\"},\"creationDate\":\"Today 12:52 PM\",\"issueKey\":\"TEST-5\",\"executionId\":\"-1\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"136\",\"entityType\":\"stepresult\",\"entityEvent\":\"stepresult updated\",\"auditItems\":{\"id\":7489,\"field\":\"status\",\"oldValue\":\"-1\",\"newValue\":\"1\"},\"creationDate\":\"Today 12:28 PM\",\"executionId\":\"10734\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"136\",\"entityType\":\"stepresult\",\"entityEvent\":\"stepresult updated\",\"auditItems\":{\"id\":7490,\"field\":\"executed_on\",\"oldValue\":\"\",\"newValue\":\"1460132887399\"},\"creationDate\":\"Today 12:28 PM\",\"executionId\":\"10734\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"136\",\"entityType\":\"stepresult\",\"entityEvent\":\"stepresult updated\",\"auditItems\":{\"id\":7491,\"field\":\"executed_by\",\"oldValue\":\"\",\"newValue\":\"vm_admin\"},\"creationDate\":\"Today 12:28 PM\",\"executionId\":\"10734\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"10733\",\"entityType\":\"execution\",\"entityEvent\":\"execution updated\",\"auditItems\":{\"id\":7486,\"field\":\"status\",\"oldValue\":\"7\",\"newValue\":\"4\"},\"creationDate\":\"Today 10:45 AM\",\"issueKey\":\"SONY-2036\",\"executionId\":\"10733\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"10733\",\"entityType\":\"execution\",\"entityEvent\":\"execution updated\",\"auditItems\":{\"id\":7487,\"field\":\"executed_on\",\"oldValue\":\"1460126745329\",\"newValue\":\"1460126749635\"},\"creationDate\":\"Today 10:45 AM\",\"issueKey\":\"SONY-2036\",\"executionId\":\"10733\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"10733\",\"entityType\":\"execution\",\"entityEvent\":\"execution updated\",\"auditItems\":{\"id\":7488,\"field\":\"executed_by\",\"oldValue\":\"vm_admin\",\"newValue\":\"vm_admin\"},\"creationDate\":\"Today 10:45 AM\",\"issueKey\":\"SONY-2036\",\"executionId\":\"10733\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"10733\",\"entityType\":\"execution\",\"entityEvent\":\"execution updated\",\"auditItems\":{\"id\":7483,\"field\":\"status\",\"oldValue\":\"2\",\"newValue\":\"7\"},\"creationDate\":\"Today 10:45 AM\",\"issueKey\":\"SONY-2036\",\"executionId\":\"10733\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"},{\"entityId\":\"10733\",\"entityType\":\"execution\",\"entityEvent\":\"execution updated\",\"auditItems\":{\"id\":7484,\"field\":\"executed_on\",\"oldValue\":\"1460126715651\",\"newValue\":\"1460126745329\"},\"creationDate\":\"Today 10:45 AM\",\"issueKey\":\"SONY-2036\",\"executionId\":\"10733\",\"creator\":\"vm_admin\",\"creatorKey\":\"vm_admin\",\"totalItems\":0,\"creatorActive\":true,\"creatorExists\":true,\"avatarUrl\":\"http://www.gravatar.com/avatar/3cdeda834c6f5fe79c03005686ed18b4?d=mm&s=16\"}]}")})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @AnonymousAllowed
    public Response getAuditLog(@QueryParam(value = "entityType") final String entityType,
                                @QueryParam(value = "event") final String event,
                                @QueryParam(value = "user") final String user,
                                @QueryParam("offset") @DefaultValue("0") Integer offset,
                                @QueryParam("maxRecords") @DefaultValue("20") Integer limit,
                                @QueryParam("issueId") Integer issueId,
                                @QueryParam("executionId") Integer executionId,
                                @QueryParam(value = "orderBy") String orderBy) {

        JSONObject jsonObject = new JSONObject();
        try {
            if (authContext.getLoggedInUser() == null && !JiraUtil.hasAnonymousPermission(authContext.getLoggedInUser())) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(),Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            log.error("Error Retrieving Attachments",e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        limit = Math.min(limit, 100);

        Map<String, Object> filterMap = new HashMap<String, Object>();
        if (StringUtils.isNotBlank(entityType)) {
            filterMap.put("ZEPHYR_ENTITY_TYPE", entityType);
        }
        if (StringUtils.isNotBlank(event)) {
            filterMap.put("ZEPHYR_ENTITY_EVENT", event);
        }
        if (StringUtils.isNotBlank(user)) {
            filterMap.put("AUTHOR", user);
        }
        if (null != issueId && issueId > 0) {
            filterMap.put("ISSUE_ID", issueId);
        }
        if (null != executionId && executionId > 0) {
            filterMap.put("SCHEDULE_ID", executionId);
        }

        if (StringUtils.isNotBlank(orderBy)) {
            filterMap.put("ORDER_BY", orderBy);
        }

        List<ChangeZJEItem> item = auditManager.getZephyrChangeLogs(filterMap, offset, limit);
        List<AuditLogBean> beans = buildResponse(item);

        Integer totalRecordCount = auditManager.getChangeItemCount(filterMap);
        AuditResponse auditResponse = new AuditResponse();
        auditResponse.totalItemsCount = totalRecordCount;
        auditResponse.auditLogs = beans;
        return Response.ok().entity(auditResponse).build();
    }


    private List<AuditLogBean> buildResponse(List<ChangeZJEItem> items) {
        List<AuditLogBean> auditBeans = new ArrayList<AuditLogBean>();
        for (ChangeZJEItem item : items) {
            AuditLogBean auditBean = new AuditLogBean();
            auditBean.setEntityId(String.valueOf(item.getChangeZJEGroup().getZephyrEntityId()));
            String entityTypeStr = item.getChangeZJEGroup().getZephyrEntityType();
            entityTypeStr = null != entityTypeStr ? entityTypeStr.replaceAll("_", " ").toLowerCase() : "N/A";
            auditBean.setEntityType(entityTypeStr);
            String entityEventStr = item.getChangeZJEGroup().getZephyrEntityEvent();
            entityEventStr = null != entityEventStr ? entityEventStr.replaceAll("_", " ").toLowerCase() : "N/A";
            auditBean.setEntityEvent(entityEventStr);
            auditBean.setExecutionId(String.valueOf(item.getChangeZJEGroup().getScheduleId()));

            Long dateCreatedLong = item.getChangeZJEGroup().getCreated();
            String formattedCreationDate = "";

            if (dateCreatedLong != null) {
                DateTimeFormatter userDateTimeFormatter = dateTimeFormatterFactory != null ? dateTimeFormatterFactory.formatter().forLoggedInUser() : null;
                formattedCreationDate = htmlEncode(userDateTimeFormatter.format(new Date(dateCreatedLong)));
            }
            auditBean.setCreationDate(formattedCreationDate);
            User creator = UserCompatibilityHelper.getUserForKey(item.getChangeZJEGroup().getAuthor());
            String creatorName = null;
            String creatorDisplayName = null;
            URI avatarUri = null;
            String avatarUriString = null;
            boolean creatorExists = false;
            boolean creatorActive = false;
            if (null == creator) {
                creatorDisplayName = item.getChangeZJEGroup().getAuthor();
                creatorName = item.getChangeZJEGroup().getAuthor();
                avatarUri = avatarService.getAvatarURL((ApplicationUser) null, "unknown_user", Avatar.Size.SMALL);
            } else {
                creatorDisplayName = creator.getDisplayName();
                creatorExists = true;
                creatorActive = creator.isActive();
                creatorName = creator.getName();
                avatarUri = avatarService.getAvatarURL(ApplicationUsers.from(creator), creator.getName(), Avatar.Size.SMALL);
            }
            avatarUriString = null != avatarUri ? avatarUri.toString() : null;

            if(StringUtils.isNotBlank(avatarUriString) && StringUtils.contains(avatarUriString,USER_AVATAR)) {
                auditBean.setDefaultAvatar(Boolean.FALSE);
            }else {
                auditBean.setDefaultAvatar(Boolean.TRUE);
            }

            auditBean.setAvatarUrl(avatarUriString);

            auditBean.setCreatorKey(creatorName);
            auditBean.setCreator(creatorDisplayName);
            auditBean.setCreatorExists(creatorExists);
            auditBean.setCreatorActive(creatorActive);

            Issue issue = null;
            boolean hasIssueViewPermission = true;
            if (item.getChangeZJEGroup().getIssueId() != null) {
                issue = issueManager.getIssueObject(item.getChangeZJEGroup().getIssueId().longValue());
                hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,authContext.getLoggedInUser());
                if (issue != null && issue.getId() > 0 && hasIssueViewPermission) {
                    auditBean.setIssueKey(issue.getKey());
                } else if(issue != null && !hasIssueViewPermission) {
                    auditBean.setIssueKey(JiraUtil.maskIssueKey(issue));
                }
            }

            AuditItemBean auditItem = new AuditItemBean();
            auditItem.setId(item.getID());
            String zephyrFieldStr = item.getZephyrField();
            zephyrFieldStr = null != zephyrFieldStr ? zephyrFieldStr.toLowerCase() : "N/A";
            auditItem.setField(zephyrFieldStr);
            /**
             * If the fields support markup (i.e. step, data or result), converting markup to HTML
             * else converting plain text to HTML
             */
            if (StringUtils.equals(zephyrFieldStr, STEP) || StringUtils.equals(zephyrFieldStr, DATA) || StringUtils.equals(zephyrFieldStr, RESULT)) {
                if(hasIssueViewPermission) {
                    auditItem.setOldValue(ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(item.getOldValue(), issue));
                    auditItem.setNewValue(ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(item.getNewValue(), issue));
                } else {
                    auditItem.setOldValue(ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(ApplicationConstants.MASKED_DATA,issue));
                    auditItem.setNewValue(ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(ApplicationConstants.MASKED_DATA,issue));
                }
            } else if (StringUtils.equalsIgnoreCase(zephyrFieldStr, EXECUTION_DEFECT)) {
                List<String> defectList = new ArrayList<>();
                String[] defects = StringUtils.split(item.getOldValue(), ",");
                populateExecutionDefect(defectList, defects);
                auditItem.setOldValue(TextUtils.plainTextToHtml(StringUtils.join(defectList,","), "_blank", true));

                String[] newDefects = StringUtils.split(item.getNewValue(), ",");
                List<String> newDefectList = new ArrayList<>();
                populateExecutionDefect(newDefectList, newDefects);
                auditItem.setNewValue(TextUtils.plainTextToHtml(StringUtils.join(newDefectList,","), "_blank", true));
            } else if (StringUtils.equalsIgnoreCase(item.getChangeZJEGroup().getZephyrEntityEvent(), EXECUTION_CUSTOM_FIELD)) {
                String oldValue=item.getOldValue();
                String newValue=item.getNewValue();
                if(hasIssueViewPermission) {
                    auditItem.setOldValue(TextUtils.plainTextToHtml(oldValue, "_blank", true));
                    auditItem.setNewValue(TextUtils.plainTextToHtml(newValue, "_blank", true));
                } else {
                    auditItem.setOldValue(TextUtils.plainTextToHtml(ApplicationConstants.MASKED_DATA, "_blank", true));
                    auditItem.setNewValue(TextUtils.plainTextToHtml(ApplicationConstants.MASKED_DATA, "_blank", true));
                }
            } else {
            	String oldValue=item.getOldValue(); 
        		String newValue=item.getNewValue();
                if(hasIssueViewPermission) {
                	   ArrayList<ExecutionStatus> exec = JiraUtil.getExecutionStatusList();
                	   for (ExecutionStatus executionStatus : exec) {
                		   oldValue=String.valueOf(executionStatus.getId()).equals(oldValue)?executionStatus.getName():oldValue;
                		   newValue=String.valueOf(executionStatus.getId()).equals(newValue)?executionStatus.getName():newValue;
					}
                    auditItem.setOldValue(TextUtils.plainTextToHtml(oldValue, "_blank", true)); 
                    auditItem.setNewValue(TextUtils.plainTextToHtml(newValue, "_blank", true));
                } else {
                    auditItem.setOldValue(TextUtils.plainTextToHtml(ApplicationConstants.MASKED_DATA, "_blank", true));
                    auditItem.setNewValue(TextUtils.plainTextToHtml(ApplicationConstants.MASKED_DATA, "_blank", true));
                }
            }
            auditBean.setAuditItems(auditItem);
            auditBeans.add(auditBean);
        }
        return auditBeans;
    }

    private void populateExecutionDefect(List<String> defectList, String[] defects) {
        if(defectList==null){ defectList = new ArrayList<>();}
        if(defects != null && defects.length>0) {
            for (String defect : defects) {
                if(defect != null) {
                    Issue defectIssue = issueManager.getIssueObject(defect);
                    boolean hasIssuePermission = JiraUtil.hasIssueViewPermission(null, defectIssue, authContext.getLoggedInUser());
                    if (hasIssuePermission) {
                        defectList.add(defect);
                    } else {
                        defectList.add(ApplicationConstants.MASKED_DATA);
                    }
                }
            }
        }
    }

    @XmlRootElement
    public static class AuditResponse {
        @XmlElement
        private Integer totalItemsCount;

        @XmlElement
        private List<AuditLogBean> auditLogs;

        public AuditResponse() {
        }
    }

}
