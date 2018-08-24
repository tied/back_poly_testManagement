package com.thed.zephyr.je.rest;

import java.util.List;
import java.util.Objects;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.query.Query;
import com.thed.zephyr.je.index.cluster.Node;
import com.thed.zephyr.je.index.cluster.NodeStateManager;
import com.thed.zephyr.je.service.AnalyticService;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.FolderManager;
import com.thed.zephyr.je.zql.core.LuceneSearchProvider;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.je.zql.helper.SearchResult;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.config.license.ZephyrLicense;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(value = "Analytics Resource API(s)", description = "Following section describes the rest resources related to Zephyr analytics.")
@Path("analytics")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class AnalyticsResource {

    protected final Logger log = Logger.getLogger(AnalyticsResource.class);
    private final ZephyrLicenseManager zLicenseManager;
    private final JiraAuthenticationContext authContext;
    private final NodeStateManager nodeStateManager;
    private final CycleManager cycleManager;
    private final FolderManager folderManager;
    private final AnalyticService analyticService;

    private static final String MP = "MP";
    private static final String ZEP = "ZEP";

    public AnalyticsResource(ZephyrLicenseManager zLicenseManager,
                             JiraAuthenticationContext authContext,
                             NodeStateManager nodeStateManager,
                             CycleManager cycleManager,
                             FolderManager folderManager,
                             AnalyticService analyticService) {
        this.zLicenseManager = zLicenseManager;
        this.authContext = authContext;
        this.nodeStateManager = nodeStateManager;
        this.cycleManager = cycleManager;
        this.folderManager = folderManager;
        this.analyticService = analyticService;
    }


    @ApiOperation(value = "Get User and License Information")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name="response", value = "{\"userName\":\"Abcd\",\"fullName\":\"abcd xyz\",\"userEmail\":\"abc@test.com\",\"pluginlicense\":\"xxxxxxxxxxxxxxxx\"}")})
    @GET
    public Response getUserLicenseInformation(@QueryParam("event") String event,
                                              @QueryParam("projectId") Long projectId,
                                              @QueryParam("versionId") Long versionId,
                                              @QueryParam("cycleId") Long cycleId
                                              ){
        JSONObject jsonObject = new JSONObject();
        try {
            if (Objects.isNull(authContext.getLoggedInUser())) {
                jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
                return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
            }
        } catch (JSONException e) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        try {

            String userName = ApplicationConstants.NO_DATA;
            String name = ApplicationConstants.NO_DATA;
            String emailId = ApplicationConstants.NO_DATA;

            ZephyrAnalyticDto analyticDto = new ZephyrAnalyticDto(name, userName, emailId);
            PluginLicense pluginLic = zLicenseManager.getZephyrMarketplaceLicense();
            analyticDto.setPluginKey(pluginLic.getPluginKey());
            if(Objects.nonNull(pluginLic)){
                analyticDto.setCustomerId(pluginLic.getOrganization().getName());
                analyticDto.setLicenseType(pluginLic.getLicenseType().name());
                analyticDto.setLicenseSource(MP);
                analyticDto.setLicenseId(pluginLic.getSupportEntitlementNumber().getOrElse(pluginLic.getServerId()));
            } else {
                ZephyrLicense lic = zLicenseManager.getLicense();
                if(Objects.nonNull(lic)){
                    analyticDto.setCustomerId(lic.getOrganisationId());
                    analyticDto.setLicenseType(lic.getLicenseType().name());
                    analyticDto.setLicenseSource(ZEP);
                    analyticDto.setLicenseId(lic.getLicenseId());
                    analyticDto.setLicenseId(lic.getLicenseId());
                } else {
                    log.fatal(" ZEPHYR FOR JIRA License is not installed");
                }
            }

            //check event and add additional fields, e.g: cycleCount, folderCount.
            if(event != null) {

                switch (event) {
                    case ApplicationConstants.CREATE_CYCLE:
                        if(nodeStateManager.isClustered()){
                            List<Node> nodes = nodeStateManager.getAllNodes();
                            if(nodes != null && nodes.size()>0){
                                Integer nodeCount = nodes.size();
                                analyticDto.setDatacenter(nodeCount);
                            }
                        }
                        if(projectId != null && versionId != null) {
                            Integer cycleCount = cycleManager.getCycleCountByVersionId(versionId,projectId);
                            analyticDto.setCycleCount(cycleCount);
                        }
                    break;
                    case ApplicationConstants.ADD_FOLDER:
                        if(projectId != null && versionId != null && cycleId != null) {
                            Integer folderCount = folderManager.getFoldersCountForCycle(
                                    projectId,versionId,cycleId);
                            analyticDto.setFolderCount(folderCount);
                        }
                    break;
                    case ApplicationConstants.ADD_TEST_TO_CYCLE:
                        analyticDto.setTestCaseCount(analyticService.getTestCaseCount());
                        analyticDto.setTestStepCount(analyticService.getTestStepCount());
                    break;
                    case ApplicationConstants.UPDATE_EXECUTION_STATUS:
                        analyticDto.setExecutionAttachmentCount(analyticService.getAttachmentCount(ApplicationConstants.SCHEDULE_TYPE));
                        analyticDto.setStepResultAttachmentCount(analyticService.getAttachmentCount(ApplicationConstants.TESTSTEPRESULT_TYPE));
                        analyticDto.setTestStepAttachmentCount(analyticService.getAttachmentCount(ApplicationConstants.TEST_STEP_TYPE));
                    break;
                }

            }
            return Response.ok().entity(analyticDto).build();
        } catch(RuntimeException ex) {
            log.error("Internal Server error while getting user information for zephyr analytics panel -> ", ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @XmlRootElement
    private static class ZephyrAnalyticDto {

        @XmlElement(nillable = true)
        private String fullName;

        @XmlElement(nillable = true)
        private String userName;

        @XmlElement(nillable = true)
        private String emailId;

        @XmlElement(nillable = true)
        private String customerId;

        @XmlElement(nillable = true)
        private String licenseId;

        @XmlElement(nillable = true)
        private String licenseType;

        @XmlElement(nillable = true)
        private String licenseSource;

        @XmlElement(nillable = true)
        private String pluginKey;

        @XmlElement(nillable = true)
        private Integer datacenter;

        @XmlElement(nillable = true)
        private Integer cycleCount;

        @XmlElement(nillable = true)
        private Integer folderCount;

        @XmlElement(nillable = true)
        private Integer testCaseCount;

        @XmlElement(nillable = true)
        private Integer testStepCount;

        @XmlElement(nillable = true)
        private Integer executionDefectCount;

        @XmlElement(nillable = true)
        private Integer stepResultDefectCount;

        @XmlElement(nillable = true)
        private Integer executionAttachmentCount;

        @XmlElement(nillable = true)
        private Integer testStepAttachmentCount;

        @XmlElement(nillable = true)
        private Integer stepResultAttachmentCount;


        public ZephyrAnalyticDto() {
        }

        public ZephyrAnalyticDto(String fullName, String userName, String emailId) {
            this.fullName = fullName;
            this.userName = userName;
            this.emailId = emailId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getEmailId() {
            return emailId;
        }

        public void setEmailId(String emailId) {
            this.emailId = emailId;
        }

        public String getLicenseId() {
            return licenseId;
        }

        public void setLicenseId(String licenseId) {
            this.licenseId = licenseId;
        }

        public String getCustomerId() {
            return customerId;
        }

        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }

        public String getLicenseType() {
            return licenseType;
        }

        public void setLicenseType(String licenseType) {
            this.licenseType = licenseType;
        }

        public String getLicenseSource() {
            return licenseSource;
        }

        public void setLicenseSource(String licenseSource) {
            this.licenseSource = licenseSource;
        }

        public String getPluginKey() {
            return pluginKey;
        }

        public void setPluginKey(String pluginKey) {
            this.pluginKey = pluginKey;
        }


        public Integer getDatacenter() {
            return datacenter;
        }

        public void setDatacenter(Integer datacenter) {
            this.datacenter = datacenter;
        }

        public Integer getCycleCount() {
            return cycleCount;
        }

        public void setCycleCount(Integer cycleCount) {
            this.cycleCount = cycleCount;
        }

        public Integer getFolderCount() {
            return folderCount;
        }

        public void setFolderCount(Integer folderCount) {
            this.folderCount = folderCount;
        }

        public Integer getTestCaseCount() {
            return testCaseCount;
        }

        public void setTestCaseCount(Integer testCaseCount) {
            this.testCaseCount = testCaseCount;
        }

        public Integer getTestStepCount() {
            return testStepCount;
        }

        public void setTestStepCount(Integer testStepCount) {
            this.testStepCount = testStepCount;
        }

        public Integer getExecutionDefectCount() {
            return executionDefectCount;
        }

        public void setExecutionDefectCount(Integer executionDefectCount) {
            this.executionDefectCount = executionDefectCount;
        }

        public Integer getStepResultDefectCount() {
            return stepResultDefectCount;
        }

        public void setStepResultDefectCount(Integer stepResultDefectCount) {
            this.stepResultDefectCount = stepResultDefectCount;
        }

        public Integer getExecutionAttachmentCount() {
            return executionAttachmentCount;
        }

        public void setExecutionAttachmentCount(Integer executionAttachmentCount) {
            this.executionAttachmentCount = executionAttachmentCount;
        }

        public Integer getTestStepAttachmentCount() {
            return testStepAttachmentCount;
        }

        public void setTestStepAttachmentCount(Integer testStepAttachmentCount) {
            this.testStepAttachmentCount = testStepAttachmentCount;
        }

        public Integer getStepResultAttachmentCount() {
            return stepResultAttachmentCount;
        }

        public void setStepResultAttachmentCount(Integer stepResultAttachmentCount) {
            this.stepResultAttachmentCount = stepResultAttachmentCount;
        }

        @Override
        public String toString() {
            return "ZephyrAnalyticDto{" +
                    "fullName='" + fullName + '\'' +
                    ", userName='" + userName + '\'' +
                    ", emailId='" + emailId + '\'' +
                    ", customerId='" + customerId + '\'' +
                    ", licenseId='" + licenseId + '\'' +
                    ", licenseType='" + licenseType + '\'' +
                    ", licenseSource='" + licenseSource + '\'' +
                    ", pluginKey='" + pluginKey + '\'' +
                    ", datacenter=" + datacenter +
                    ", cycleCount=" + cycleCount +
                    ", folderCount=" + folderCount +
                    ", testCaseCount=" + testCaseCount +
                    ", testStepCount=" + testStepCount +
                    ", executionDefectCount=" + executionDefectCount +
                    ", stepResultDefectCount=" + stepResultDefectCount +
                    ", executionAttachmentCount=" + executionAttachmentCount +
                    ", testStepAttachmentCount=" + testStepAttachmentCount +
                    ", stepResultAttachmentCount=" + stepResultAttachmentCount +
                    '}';
        }
    }
}