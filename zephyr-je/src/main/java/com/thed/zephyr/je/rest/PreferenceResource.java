package com.thed.zephyr.je.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.vo.*;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.*;

import static com.thed.zephyr.util.JiraUtil.getUserCustomizationPreferenceByKey;
import static com.thed.zephyr.util.JiraUtil.updateUserCustomizationPreferenceByKey;

@Api(value = "Preference Resource API(s)", description = "Following section describes the rest resources pertaining to PreferenceResource")
@Path("preference")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class PreferenceResource {

    final Logger log = Logger.getLogger(PreferenceResource.class);
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";
    private static final String TEST_STEP_COLUMN_KEY = "-TS";
    private static final String TEST_STEP_COLUMN_KEY_LATEST = "-TSL";
    private static final String CYCLE_SUMMARY_COLUMN_KEY = "-CS";
    private static final String CYCLE_SUMMARY_COLUMN_KEY_LATEST = "-CSL";
    private static final String EXECUTION_COLUMN_KEY = "-ES";
    private static final String EXECUTION_COLUMN_KEY_LATEST = "-ESL";
    private static final String EXECUTION_CUSTOM_FIELD_KEY = "-CF";
    private static final String TEST_EXECUTION_COLUMN_KEY = "-TE";
    private static final String WALK_THROUGH_DETAIL_KEY = "-WTD";
    private static final String ADV_OPT_IN_KEY = "-ADV";
    private static final String IMPORT_MAPPING_KEY = "-IM";
    private static final String PAGINATION_WIDTH_DETAIL_KEY = "-PW";
    private static final String ADV_OPT_IN = "advOptIn";
    private static final String DISPLAY_NAME = "displayName";
    private static final String IS_VISIBLE = "isVisible";

    private final JiraAuthenticationContext authContext;
    private final ZephyrCustomFieldManager zephyrCustomFieldManager;

    public PreferenceResource(final JiraAuthenticationContext authContext,
                              final ZephyrCustomFieldManager zephyrCustomFieldManager) {
        this.authContext = authContext;
        this.zephyrCustomFieldManager = zephyrCustomFieldManager;
    }


    @ApiOperation(value = "Set test step customization preference.", notes = "Set test step customization preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @POST
    @Path("setteststepcustomization")
    public Response setTeststepCustomizationPreference(PreferenceBean preferenceBean) {
        return setPreferenceResponse(TEST_STEP_COLUMN_KEY_LATEST, TEST_STEP_DEFAULT_COLUMNS_NAMES, preferenceBean);
    }

    @ApiOperation(value = "Get test step customization preference.", notes = "Get test step customization preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @GET
    @Path("getteststepcustomization")
    public Response getTeststepCustomizationPreference() throws IOException {
        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + TEST_STEP_COLUMN_KEY_LATEST);

        if(StringUtils.isBlank(userCustomizationPreference)) {
            //copy the existing preference to new key if its not null & update the old configuration.
            userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + TEST_STEP_COLUMN_KEY);

            if(StringUtils.isNotBlank(userCustomizationPreference)) {
                Map<String, Map<String, String>> preferenceMap = getPreferenceMapFromJsonResponse(userCustomizationPreference);

                if (MapUtils.isEmpty(preferenceMap)) {
                    Map<String, Map<String, String>> defaultPreference = getDefaultCustomizationPreference(TEST_STEP_DEFAULT_COLUMNS_NAMES);
                    updateUserCustomizationPreferenceByKey(userId + TEST_STEP_COLUMN_KEY_LATEST,
                            getObjectMapper().writeValueAsString(defaultPreference));
                }else {
                    updateUserCustomizationPreferenceByKey(userId + TEST_STEP_COLUMN_KEY_LATEST,
                            getObjectMapper().writeValueAsString(preferenceMap));
                    updateOldTestStepCustomization(preferenceMap,userId);
                }
            }
        }
        return getPreferenceResponse(TEST_STEP_COLUMN_KEY_LATEST, TEST_STEP_DEFAULT_COLUMNS_NAMES);
    }

    @ApiOperation(value = "Set cycle summary customization preference.", notes = "Get cycle summary column customization preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @POST
    @Path("setcyclesummarycustomization")
    public Response setCycleSummaryCustomizationPreference(PreferenceBean preferenceBean) {
        return setPreferenceResponse(CYCLE_SUMMARY_COLUMN_KEY_LATEST, CYCLE_SUMMARY_DEFAULT_COLUMNS_NAMES, preferenceBean);
    }

    @ApiOperation(value = "Get cycle summary customization preference.", notes = "Get cycle summary column customization preference.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "response", value = "")
    })
    @GET
    @Path("getcyclesummarycustomization")
    public Response getCycleSummaryCustomizationPreference() throws IOException {
        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + CYCLE_SUMMARY_COLUMN_KEY_LATEST);

        if(StringUtils.isBlank(userCustomizationPreference)) {
            //copy the existing preference to new key if its not null & update the old configuration.
            userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + CYCLE_SUMMARY_COLUMN_KEY);

            if(StringUtils.isNotBlank(userCustomizationPreference)) {
                Map<String, Map<String, String>> preferenceMap = getPreferenceMapFromJsonResponse(userCustomizationPreference);

                if (MapUtils.isEmpty(preferenceMap)) {
                    Map<String, Map<String, String>> defaultPreference = getDefaultCustomizationPreference(CYCLE_SUMMARY_DEFAULT_COLUMNS_NAMES);
                    updateUserCustomizationPreferenceByKey(userId + CYCLE_SUMMARY_COLUMN_KEY_LATEST,
                            getObjectMapper().writeValueAsString(defaultPreference));
                }else {
                    updateUserCustomizationPreferenceByKey(userId + CYCLE_SUMMARY_COLUMN_KEY_LATEST,
                            getObjectMapper().writeValueAsString(preferenceMap));
                    updateOldCycleSummaryCustomization(preferenceMap,userId);
                }
            }
        }
        return getPreferenceResponse(CYCLE_SUMMARY_COLUMN_KEY_LATEST, CYCLE_SUMMARY_DEFAULT_COLUMNS_NAMES);
    }

    @ApiOperation(value = "Set execution summary customization preference.", notes = "Set execution summary column customization preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @POST
    @Path("setexecutioncustomization")
    public Response setExecutionCustomizationPreference(PreferenceBean preferenceBean) {
        return setPreferenceResponse(EXECUTION_COLUMN_KEY_LATEST, EXECUTION_SUMMARY_DEFAULT_COLUMNS_NAMES, preferenceBean);
    }

    @ApiOperation(value = "Get execution summary customization preference.", notes = "Get execution summary column customization preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @GET
    @Path("getexecutioncustomization")
    public Response getExecutionColumnCustomizationPreference() throws IOException {
        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + EXECUTION_COLUMN_KEY_LATEST);

        if(StringUtils.isBlank(userCustomizationPreference)) {
            //copy the existing preference to new key if its not null & update the old configuration.
            userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + EXECUTION_COLUMN_KEY);

            if(StringUtils.isNotBlank(userCustomizationPreference)) {
                Map<String, Map<String, String>> preferenceMap = getPreferenceMapFromJsonResponse(userCustomizationPreference);

                if (MapUtils.isEmpty(preferenceMap)) {
                    Map<String, Map<String, String>> defaultPreference = getDefaultCustomizationPreference(EXECUTION_SUMMARY_DEFAULT_COLUMNS_NAMES);
                    updateUserCustomizationPreferenceByKey(userId + EXECUTION_COLUMN_KEY_LATEST,
                            getObjectMapper().writeValueAsString(defaultPreference));
                }else {
                    updateUserCustomizationPreferenceByKey(userId + EXECUTION_COLUMN_KEY_LATEST,
                            getObjectMapper().writeValueAsString(preferenceMap));
                    updateOldExecutionSummaryCustomization(preferenceMap,userId);
                }
            }
        }
        return getPreferenceResponse(EXECUTION_COLUMN_KEY_LATEST, EXECUTION_SUMMARY_DEFAULT_COLUMNS_NAMES);
    }

    @ApiOperation(value = "Set execution custom field customization preference.", notes = "Set execution summary column customization preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @POST
    @Path("setExecutionCustomFieldCustomization")
    public Response setExecutionCustomFieldCustomizationPreference(@QueryParam("projectId") Long projectId, PreferenceBean preferenceBean) {
        return setPreferenceForExecutionCustomField(EXECUTION_CUSTOM_FIELD_KEY, preferenceBean, projectId);
    }

    @ApiOperation(value = "Get execution custom field customization preference.", notes = "Get execution summary column customization preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @GET
    @Path("getExecutionCustomFieldCustomization")
    public Response getExecutionCustomFieldColumnCustomizationPreference(@QueryParam("projectId") Long projectId) {
        return getPreferenceForExecutionCustomField(EXECUTION_CUSTOM_FIELD_KEY,projectId);
    }

    @ApiOperation(value = "Set execution summary customization preference.", notes = "Set execution summary column customization preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @POST
    @Path("setTestExecutionCustomization")
    public Response setTestExecutionCustomizationPreference(PreferenceBean preferenceBean) {
        return setPreferenceResponse(TEST_EXECUTION_COLUMN_KEY, TEST_EXECUTION_DEFAULT_COLUMNS_NAMES, preferenceBean);
    }

    @ApiOperation(value = "Get execution summary customization preference.", notes = "Get execution summary column customization preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @GET
    @Path("getTestExecutionCustomization")
    public Response getTestExecutionCustomizationPreference() {
        return getPreferenceResponse(TEST_EXECUTION_COLUMN_KEY, TEST_EXECUTION_DEFAULT_COLUMNS_NAMES);
    }

    @ApiOperation(value = "Retrieve/update walk through preference details.", notes = "Retrieve/update walk through preference details..")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @PUT
    @Path("walkThroughPreference")
    public Response walkThroughPreference(WalkThroughDetail walkThroughDetail, @QueryParam("isUpdate") boolean isUpdate) {

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        WalkThroughDetail walkThroughDetailResponse = new WalkThroughDetail();

        String walkThroughPreference = getUserCustomizationPreferenceByKey(userId + WALK_THROUGH_DETAIL_KEY);
        try {
            if (StringUtils.isNotBlank(walkThroughPreference)) {

                if(isUpdate && Objects.nonNull(walkThroughDetail)) {
                    updateUserCustomizationPreferenceByKey(userId + WALK_THROUGH_DETAIL_KEY,
                            getObjectMapper().writeValueAsString(walkThroughDetail));
                }
            } else {
                updateUserCustomizationPreferenceByKey(userId + WALK_THROUGH_DETAIL_KEY,
                        getObjectMapper().writeValueAsString(walkThroughDetailResponse));
            }
            walkThroughDetailResponse = getObjectMapper().readValue(getUserCustomizationPreferenceByKey(userId + WALK_THROUGH_DETAIL_KEY), WalkThroughDetail.class);
        } catch (Exception e) {
            JSONObject jsonObject = new JSONObject();
            try {
                String errorMsg = "Unable to retrieve the walk through preference.";
                jsonObject.put("error", errorMsg);
                log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMsg ));
                return Response.status(Response.Status.BAD_REQUEST).entity(jsonObject.toString()).build();
            } catch (JSONException ex) {
                log.error("Error occurred while adding entry to json object.", ex);
            }
        }

        return Response.ok().entity(walkThroughDetailResponse).build();
    }

    @POST
    @Path("setImportMappingPreference")
    public Response setImportMappingPreference(ImportPreference importMappingPreference) {
        Map<String, ImportPreference> response = new HashMap<>();

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String userPrefkey = IMPORT_MAPPING_KEY+userId+importMappingPreference.getFileType().toUpperCase();
        String fileTypeMapKey = IMPORT_MAPPING_KEY+importMappingPreference.getFileType().toUpperCase();

        Object cacheOutput = getUserCustomizationPreferenceByKey(userPrefkey);
        Map<String, Map<String, ImportPreference>> userMappingPrefMap = null;
        Map<String, ImportPreference> mappingPrefMap = null;

        if (null == cacheOutput) {
            userMappingPrefMap = new HashMap<String,Map<String, ImportPreference>>();
            mappingPrefMap = new HashMap<String, ImportPreference>();
        } else {
            try {
                userMappingPrefMap = getObjectMapper().readValue(cacheOutput.toString(), new TypeReference<Map<String, Map<String, ImportPreference>>>(){});
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            mappingPrefMap = userMappingPrefMap.get(fileTypeMapKey);
            if(mappingPrefMap == null || mappingPrefMap.isEmpty()) {
                mappingPrefMap = new HashMap<String, ImportPreference>();
            }
        }
        // int id = (int)(Math.random() * 50);
        String key = importMappingPreference.getId();
        //importMappingPreference.setId(key);
        mappingPrefMap.put(key, importMappingPreference);
        userMappingPrefMap.put(fileTypeMapKey, mappingPrefMap);
        try {
            updateUserCustomizationPreferenceByKey(userPrefkey,
                    getObjectMapper().writeValueAsString(userMappingPrefMap));

        } catch (Exception ex) {
            log.error("Error updating the customization preference for test step for user : " + authContext.getLoggedInUser().getName(),ex);
        }

        // to do createOrUpdateCachePreferenceObject(userPrefkey, userMappingPrefMap);
        log.debug("Latest preference saved by the user : [" + authContext.getLoggedInUser().getName() + "] preference :" + importMappingPreference.toString());
        response.put(userPrefkey, importMappingPreference);
        return Response.ok(response).build();
    }

    @ApiOperation(value = "Get execution summary customization preference.", notes = "Get execution summary column customization preference.")
    @GET
    @Path("getImportMappingPreference")
    public Response getImportMappingPreferencesByFileType(@QueryParam("fileType") String fileType) {

        Map<String, Object> response = new HashMap<>();

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        if(fileType.equalsIgnoreCase("xls") || fileType.equalsIgnoreCase("xlsx")) {
            fileType="Excel";
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String userPrefkey = IMPORT_MAPPING_KEY+userId+fileType.toUpperCase();
        String fileTypeMapKey = IMPORT_MAPPING_KEY+fileType.toUpperCase();

        Object cacheOutput = getUserCustomizationPreferenceByKey(userPrefkey);
        Map<String, Map<String, ImportPreference>> userMappingPrefMap = null;
        Map<String, ImportPreference> fileTypeMappingMap = null;
        if (null == cacheOutput) {
            response.put(IMPORT_MAPPING_KEY,"");
        } else {
            try {
                userMappingPrefMap = getObjectMapper().readValue(cacheOutput.toString(), new TypeReference<Map<String, Map<String, ImportPreference>>>(){});
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            //userMappingPrefMap = (Map<String, Map<String, ImportPreference>>)cacheOutput;
            fileTypeMappingMap = userMappingPrefMap.get(fileTypeMapKey);
            if(fileTypeMappingMap != null && fileTypeMappingMap.size() > 0) {
                Collection<ImportPreference> preferences = fileTypeMappingMap.values();
                //response.put(IMPORT_MAPPING_KEY, preferences);
                response.put("mappings", preferences);
            } else {
                response.put("mappings", "");
            }

            log.debug("Latest preference retrieved for execution for the user [" + authContext.getLoggedInUser().getName() + "] " +
                    "Preference : " + cacheOutput.toString());
        }
        return Response.ok(response).build();
    }

    @ApiOperation(value = "Get execution summary customization preference.", notes = "Get execution summary column customization preference.")
    @POST
    @Path("deleteImportMappingPreference")
    public Response deleteImportMappingPreference(ImportPreference importMappingPreference) {

        Map<String, Object> response = new HashMap<>();

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }

        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String userPrefkey = IMPORT_MAPPING_KEY+userId+importMappingPreference.getFileType().toUpperCase();
        String fileTypeMapKey = IMPORT_MAPPING_KEY+importMappingPreference.getFileType().toUpperCase();

        Object cacheOutput = getUserCustomizationPreferenceByKey(userPrefkey);


        Map<String, Map<String, ImportPreference>> userMappingPrefMap = null;
        Map<String, ImportPreference> mappingPrefMap = null;

        if(cacheOutput != null) {
            try {
                userMappingPrefMap = getObjectMapper().readValue(cacheOutput.toString(), new TypeReference<Map<String, Map<String, ImportPreference>>>(){});
            } catch(Exception ex) {
                ex.printStackTrace();
            }
            mappingPrefMap = userMappingPrefMap.get(fileTypeMapKey);
            if(mappingPrefMap != null && !mappingPrefMap.isEmpty()) {
                String key = importMappingPreference.getId();
                ImportPreference preference = mappingPrefMap.get(key);
                mappingPrefMap.remove(key);
                userMappingPrefMap.put(fileTypeMapKey, mappingPrefMap);
                try {
                    updateUserCustomizationPreferenceByKey(userPrefkey,
                            getObjectMapper().writeValueAsString(userMappingPrefMap));

                } catch (Exception ex) {
                    log.error("Error updating the customization preference for test step for user : " + authContext.getLoggedInUser().getName(),ex);
                }

                response.put(IMPORT_MAPPING_KEY, preference);
            } else {
                response.put(IMPORT_MAPPING_KEY, StringUtils.EMPTY);
            }
        } else {
            response.put(IMPORT_MAPPING_KEY, StringUtils.EMPTY);
        }
        return Response.ok(response).build();
    }

    @ApiOperation(value = "Get advertisement opt in preference.", notes = "Get advertisement opt in preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @GET
    @Path("advoptin")
    public Response getAdvOptIn() {

        Map<String, String> response = new HashMap<>();

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String advOptInPreference = getUserCustomizationPreferenceByKey(userId + ADV_OPT_IN_KEY);

        if (StringUtils.isBlank(advOptInPreference)) {
            try {
                advOptInPreference = updateUserCustomizationPreferenceByKey(userId + ADV_OPT_IN_KEY,
                        getObjectMapper().writeValueAsString(Boolean.TRUE));

            } catch (Exception ex) {
                log.error("Error getting the customization preference for advertisement for user : " + authContext.getLoggedInUser().getName(),ex);
            }
        }
        response.put(ADV_OPT_IN, advOptInPreference);
        return Response.ok(response).build();
    }

    @ApiOperation(value = "Set advertisement opt in preference.", notes = "Set advertisement opt in preference.")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = ""),
            @ApiImplicitParam(name = "response", value = "")})
    @PUT
    @Path("advoptin")
    public Response setAdvOptIn(@QueryParam("isOptIn") Boolean isOptIn) {

        Map<String, String> response = new HashMap<>();

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String advOptInPreference = getUserCustomizationPreferenceByKey(userId + ADV_OPT_IN_KEY);
        try {

            if (Objects.nonNull(isOptIn)) {
                advOptInPreference = updateUserCustomizationPreferenceByKey(userId + ADV_OPT_IN_KEY, getObjectMapper().writeValueAsString(isOptIn));
            }
        } catch (Exception ex) {
            log.error("Error updating the customization preference for advertisement for user : " + authContext.getLoggedInUser().getName(),ex);
        }
        response.put(ADV_OPT_IN, advOptInPreference);
        return Response.ok(response).build();
    }

    @ApiOperation(value = "Retrieve pagination width preference details.", notes = "Retrieve pagination width preference details.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @GET
    @Path("paginationWidthPreference")
    public Response getPaginationWidthPreference() {

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String paginationWidthPreference = getUserCustomizationPreferenceByKey(userId + PAGINATION_WIDTH_DETAIL_KEY);
        Map<String,String> paginationWidthPreferenceMap = new HashMap<>();
        try {
            if (StringUtils.isNotBlank(paginationWidthPreference)) {
                paginationWidthPreferenceMap = getObjectMapper().readValue(getUserCustomizationPreferenceByKey(userId + PAGINATION_WIDTH_DETAIL_KEY),
                        new TypeReference<Map<String, String>>(){});
            } else {
                paginationWidthPreferenceMap = getDefaultPaginationWidthPreference();
                updateUserCustomizationPreferenceByKey(userId + PAGINATION_WIDTH_DETAIL_KEY, getObjectMapper().writeValueAsString(paginationWidthPreferenceMap));
            }
        } catch (Exception e) {
            try {
                JSONObject jsonObject = new JSONObject();
                String errorMsg = "Unable to retrieve the walk through preference.";
                jsonObject.put("error", errorMsg);
                log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMsg ));
                return Response.status(Response.Status.BAD_REQUEST).entity(jsonObject.toString()).build();
            } catch (JSONException ex) {
                log.error("Error occurred while adding entry to json object.", ex);
            }
        }

        return Response.ok().entity(paginationWidthPreferenceMap).build();
    }

    @ApiOperation(value = "Update pagination width preference details.", notes = "Update pagination width preference details.")
    @ApiImplicitParams({@ApiImplicitParam(name = "response", value = "")})
    @PUT
    @Path("paginationWidthPreference")
    public Response setPaginationWidthPreference(Map<String,String> paginationWidthPreferenceRequest) {

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String paginationWidthPreference = getUserCustomizationPreferenceByKey(userId + PAGINATION_WIDTH_DETAIL_KEY);
        Map<String,String> paginationWidthPreferenceMap = new HashMap<>();
        try {
            if (MapUtils.isNotEmpty(paginationWidthPreferenceRequest)) {
                updateUserCustomizationPreferenceByKey(userId + PAGINATION_WIDTH_DETAIL_KEY, getObjectMapper().writeValueAsString(paginationWidthPreferenceRequest));
                paginationWidthPreferenceMap = getObjectMapper().readValue(getUserCustomizationPreferenceByKey(userId + PAGINATION_WIDTH_DETAIL_KEY),
                        new TypeReference<Map<String, String>>(){});
            } else if (StringUtils.isNotBlank(paginationWidthPreference)){
                paginationWidthPreferenceMap = getObjectMapper().readValue(getUserCustomizationPreferenceByKey(userId + PAGINATION_WIDTH_DETAIL_KEY),
                        new TypeReference<Map<String, String>>(){});
            }
        } catch (Exception e) {
            try {
                JSONObject jsonObject = new JSONObject();
                String errorMsg = "Unable to retrieve the walk through preference.";
                jsonObject.put("error", errorMsg);
                log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.BAD_REQUEST.getStatusCode(), Response.Status.BAD_REQUEST,errorMsg ));
                return Response.status(Response.Status.BAD_REQUEST).entity(jsonObject.toString()).build();
            } catch (JSONException ex) {
                log.error("Error occurred while adding entry to json object.", ex);
            }
        }

        return Response.ok().entity(paginationWidthPreferenceMap).build();
    }

    /**
     * Returns the error response in case the logged in user is not authorized.
     *
     * @return Response
     */
    private Response loggedInErrorResponse() {

        JSONObject jsonObject = new JSONObject();
        try {
            String errorMessage = authContext.getI18nHelper().getText("zephyr.common.logged.user.error");
            jsonObject.put("error", errorMessage);
            log.error(String.format(ERROR_LOG_MESSAGE, Response.Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED, authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
            return Response.status(Response.Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
        } catch (JSONException ex) {
            log.error("Error occurred while adding entry to json object.", ex);
        }
        return null;
    }

    /**
     *
     * @param columnKey
     * @param defaultColumnNames
     * @return
     */
    private Response getPreferenceResponse(String columnKey, Map<String, String> defaultColumnNames)  {
        PreferenceBean response = new PreferenceBean();
        Map<String, Map<String, String>> preferenceMap = null;

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());

        String userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + columnKey);

        try {
            if (StringUtils.isBlank(userCustomizationPreference)) {

                userCustomizationPreference = updateUserCustomizationPreferenceByKey(userId + columnKey,
                        getObjectMapper().writeValueAsString(getDefaultCustomizationPreference(defaultColumnNames)));

            }

            preferenceMap = getPreferenceMapFromJsonResponse(userCustomizationPreference);

            if (MapUtils.isEmpty(preferenceMap)) {
                Map<String, Map<String, String>> defaultPreference = getDefaultCustomizationPreference(defaultColumnNames);
                updateUserCustomizationPreferenceByKey(userId + columnKey,
                        getObjectMapper().writeValueAsString(defaultPreference));
                response.setPreferences(defaultPreference);
            }else {
                response.setPreferences(preferenceMap);
            }
        } catch (Exception ex) {
            log.error("Unable to cache the customization preference for the user : " + authContext.getLoggedInUser().getName(), ex);
        }

        return Response.ok(response).build();
    }

    /**
     *
     * @param columnKey
     * @param defaultColumnNames
     * @param preferenceBean
     * @return
     */
    private Response setPreferenceResponse(String columnKey, Map<String, String> defaultColumnNames, PreferenceBean preferenceBean) {
        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        Map<String, Map<String, String>> preferenceMap;
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + columnKey);

        try {
            if(StringUtils.isNotBlank(userCustomizationPreference)) {
                if (Objects.nonNull(preferenceBean.getPreferences())) {
                    userCustomizationPreference = updateUserCustomizationPreferenceByKey(userId + columnKey,
                            getObjectMapper().writeValueAsString(preferenceBean.getPreferences()));
                }
            } else {
                updateUserCustomizationPreferenceByKey(userId + columnKey,
                        getObjectMapper().writeValueAsString(getDefaultCustomizationPreference(defaultColumnNames)));
            }


            preferenceMap = getPreferenceMapFromJsonResponse(userCustomizationPreference);

            if (MapUtils.isEmpty(preferenceMap)) {
                Map<String, Map<String, String>> defaultPreference = getDefaultCustomizationPreference(defaultColumnNames);
                updateUserCustomizationPreferenceByKey(userId + columnKey,
                        getObjectMapper().writeValueAsString(defaultPreference));
                preferenceBean.setPreferences(defaultPreference);
            }else {
                preferenceBean.setPreferences(preferenceMap);
            }

        }  catch (Exception ex) {
            log.error("Unable to cache the customization preference for the user : " + authContext.getLoggedInUser().getName(), ex);
        }

        return Response.ok(preferenceBean).build();
    }

    /**
     *
     * @param executionCFKey
     * @param preferenceBean
     * @param projectId
     * @return
     */
    private Response setPreferenceForExecutionCustomField(String executionCFKey, PreferenceBean preferenceBean, Long projectId) {
        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        Map<String, Map<String, String>> preferenceMap;

        String userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + executionCFKey);

        try {
            if(StringUtils.isNotBlank(userCustomizationPreference)) {
                if (Objects.nonNull(preferenceBean.getPreferences())) {
                    userCustomizationPreference = updateUserCustomizationPreferenceByKey(userId + executionCFKey,
                            getObjectMapper().writeValueAsString(preferenceBean.getPreferences()));
                }
            } else {
                updateUserCustomizationPreferenceByKey(userId + executionCFKey,
                        getObjectMapper().writeValueAsString(getCustomFieldNames(projectId)));
            }

            preferenceMap = getPreferenceMapFromJsonResponse(userCustomizationPreference);

            if (MapUtils.isEmpty(preferenceMap)) {
                Map<String, Map<String, String>> defaultPreference = getCustomFieldNames(projectId);
                updateUserCustomizationPreferenceByKey(userId + executionCFKey,
                        getObjectMapper().writeValueAsString(defaultPreference));
                preferenceBean.setPreferences(defaultPreference);
            }else {
                preferenceBean.setPreferences(preferenceMap);
            }

        }  catch (Exception ex) {
            log.error("Unable to cache the customization preference for the user : " + authContext.getLoggedInUser().getName(), ex);
        }

        return Response.ok(preferenceBean).build();
    }

    /**
     *
     * @param executionCFKey
     * @param projectId
     * @return
     */
    private Response getPreferenceForExecutionCustomField(String executionCFKey, Long projectId) {
        PreferenceBean response = new PreferenceBean();

        if (null == authContext.getLoggedInUser()) {
            return loggedInErrorResponse();
        }
        Map<String, Map<String, String>> preferenceMap;
        String userId = String.valueOf(authContext.getLoggedInUser().getId());
        String userCustomizationPreference = getUserCustomizationPreferenceByKey(userId + executionCFKey);

        try {
            if (StringUtils.isBlank(userCustomizationPreference)) {
                updateUserCustomizationPreferenceByKey(userId + executionCFKey,
                        getObjectMapper().writeValueAsString(getCustomFieldNames(projectId)));
            }

            preferenceMap = getPreferenceMapFromJsonResponse(userCustomizationPreference);

            if (MapUtils.isEmpty(preferenceMap)) {
                Map<String, Map<String, String>> defaultPreference = getCustomFieldNames(projectId);
                updateUserCustomizationPreferenceByKey(userId + executionCFKey,
                        getObjectMapper().writeValueAsString(defaultPreference));
                response.setPreferences(defaultPreference);
            }else {
                response.setPreferences(preferenceMap);
            }
        }  catch (Exception ex) {
            log.error("Unable to cache the customization preference for the user : " + authContext.getLoggedInUser().getName(), ex);
        }
        return Response.ok(response).build();
    }

    /**
     *
     * @param defaultColumnNames
     * @return
     */
    private Map<String, Map<String,String>> getDefaultCustomizationPreference(Map<String, String> defaultColumnNames) {
        Map<String, Map<String,String>> customizationMap = new HashMap<>();
        Map<String, String> preferenceObj;
        for(Map.Entry<String, String> column : defaultColumnNames.entrySet()) {
            preferenceObj = new HashMap<>();
            preferenceObj.put(DISPLAY_NAME, column.getValue());
            preferenceObj.put(IS_VISIBLE, Boolean.TRUE.toString());
            customizationMap.putIfAbsent(column.getKey(), preferenceObj);
        }
        return customizationMap;
    }

    /**
     * Get custom fields customization
     * @return
     * @param projectId
     */
    private Map<String, Map<String, String>> getCustomFieldNames(Long projectId) {
        CustomField[] customFields = zephyrCustomFieldManager.getCustomFieldsByEntityType(ApplicationConstants.ENTITY_TYPE.EXECUTION.name(), projectId, null);
        Map<String, Map<String, String>> customFieldsCustomizationMap = new HashMap<>();

        if(Objects.nonNull(customFields) && customFields.length > 1) {
            getCustomizationMapForCustomFields(customFields,customFieldsCustomizationMap);
        }

        if(null != projectId) {
            List<CustomField> projectLevelCustomFields = zephyrCustomFieldManager.getCustomFieldsByEntityTypeAndProject
                    (ApplicationConstants.ENTITY_TYPE.EXECUTION.name(),projectId);
            if(Objects.nonNull(projectLevelCustomFields) && projectLevelCustomFields.size() > 1) {
                getCustomizationMapForCustomFields(projectLevelCustomFields.toArray(new CustomField[projectLevelCustomFields.size()]),customFieldsCustomizationMap);
            }
        }
        return customFieldsCustomizationMap;
    }

    /**
     *
     * @param customFields
     * @param customFieldsCustomizationMap
     */
    private void getCustomizationMapForCustomFields(CustomField[] customFields, Map<String, Map<String, String>> customFieldsCustomizationMap) {
        Map<String, String> preferenceObj;
        for (CustomField customField : customFields) {
            preferenceObj = new HashMap<>();
            preferenceObj.put(DISPLAY_NAME, customField.getName());
            preferenceObj.put(IS_VISIBLE, Boolean.TRUE.toString());
            customFieldsCustomizationMap.putIfAbsent(customField.getID() + StringUtils.EMPTY, preferenceObj);
        }
    }

    /**
     * @param projectId
     * @return
     */
    private List<Integer> getCustomFields(Long projectId) {

        CustomField[] customFields = zephyrCustomFieldManager.getCustomFieldsByEntityType(ApplicationConstants.ENTITY_TYPE.EXECUTION.name(), projectId, null);
        List<Integer> customFieldList = new ArrayList<>();
        if (Objects.nonNull(customFields) && customFields.length > 1) {
            Arrays.stream(customFields).forEach(customField -> {
                customFieldList.add(customField.getID());
            });
        }

        if (null != projectId) {
            List<CustomField> projectLevelCustomFields = zephyrCustomFieldManager.getCustomFieldsByEntityTypeAndProject
                    (ApplicationConstants.ENTITY_TYPE.EXECUTION.name(), projectId);
            if (Objects.nonNull(projectLevelCustomFields) && projectLevelCustomFields.size() > 1) {
                projectLevelCustomFields.forEach(customField -> {
                    customFieldList.add(customField.getID());
                });
            }
        }
        return customFieldList;
    }

    /**
     *
     * @param userCustomizationPreference
     * @return
     */
    private Map<String, Map<String, String>> getPreferenceMapFromJsonResponse(String userCustomizationPreference) {

        TypeReference<Map<String, Map<String, String>>> typeRef = new TypeReference<Map<String, Map<String, String>>>() {};
        Map<String, Map<String, String>> responseMap = null;
        try {
            responseMap = getObjectMapper().readValue(userCustomizationPreference,typeRef);
            return responseMap;
        } catch (JsonMappingException ex) {
            log.error("Unable to parse the customization preference from json string to map : " + authContext.getLoggedInUser().getName(), ex);
            return Maps.newHashMap();
        } catch (IOException io) {

        }finally {
            if(MapUtils.isEmpty(responseMap)) {
                return Maps.newHashMap();
            }else {
                return responseMap;
            }
        }
    }


    /**
     *
     * @param preferenceMap
     * @param userId
     */
    private void updateOldTestStepCustomization(Map<String, Map<String, String>> preferenceMap, String userId) {

        TeststepPreferenceBean teststepPreferenceBean = new TeststepPreferenceBean();
        preferenceMap.entrySet().stream().forEach(entry -> {
            Map<String, String> preference = entry.getValue();
            if(StringUtils.equalsIgnoreCase(entry.getKey(),"teststep")) {
                teststepPreferenceBean.setTestStepVisible(Boolean.valueOf(preference.get(IS_VISIBLE)));
            }else if(StringUtils.equalsIgnoreCase(entry.getKey(),"testdata")) {
                teststepPreferenceBean.setTestStepDataVisible(Boolean.valueOf(preference.get(IS_VISIBLE)));
            }else if(StringUtils.equalsIgnoreCase(entry.getKey(),"testresult")) {
                teststepPreferenceBean.setTestStepExpectedResultVisible(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"attachment")) {
                teststepPreferenceBean.setTestStepAttachmentVisible(Boolean.valueOf(preference.get(IS_VISIBLE)));
            }
        });

        try {
            updateUserCustomizationPreferenceByKey(userId + TEST_STEP_COLUMN_KEY,
                    getObjectMapper().writeValueAsString(teststepPreferenceBean));
        } catch (IOException e) {
            log.error("Exception occurred while saving the preference.");
        }

    }

    /**
     *
     * @param preferenceMap
     * @param userId
     */
    private void updateOldCycleSummaryCustomization(Map<String, Map<String, String>> preferenceMap, String userId) {

        CycleSummaryPreferenceBean cycleSummaryPreferenceBean = new CycleSummaryPreferenceBean();
        preferenceMap.entrySet().stream().forEach(entry -> {
            Map<String, String> preference = entry.getValue();
            if(StringUtils.equalsIgnoreCase(entry.getKey(),"status")) {
                cycleSummaryPreferenceBean.setStatus(Boolean.valueOf(preference.get(IS_VISIBLE)));
            }else if(StringUtils.equalsIgnoreCase(entry.getKey(),"summary")) {
                cycleSummaryPreferenceBean.setSummary(Boolean.valueOf(preference.get(IS_VISIBLE)));
            }else if(StringUtils.equalsIgnoreCase(entry.getKey(),"component")) {
                cycleSummaryPreferenceBean.setComponent(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"label")) {
                cycleSummaryPreferenceBean.setLabel(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"executedBy")) {
                cycleSummaryPreferenceBean.setExecutedBy(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"executedOn")) {
                cycleSummaryPreferenceBean.setExecutedOn(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"assignee")) {
                cycleSummaryPreferenceBean.setAssignee(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"defects")) {
                cycleSummaryPreferenceBean.setDefect(Boolean.valueOf(preference.get(IS_VISIBLE)));
            }
        });

        try {
            updateUserCustomizationPreferenceByKey(userId + CYCLE_SUMMARY_COLUMN_KEY,
                    getObjectMapper().writeValueAsString(cycleSummaryPreferenceBean));
        } catch (IOException e) {
            log.error("Exception occurred while saving the preference.");
        }
    }


    /**
     *
     * @param preferenceMap
     * @param userId
     */
    private void updateOldExecutionSummaryCustomization(Map<String, Map<String, String>> preferenceMap, String userId) {

        ExecutionPreferenceBean executionPreferenceBean = new ExecutionPreferenceBean();
        preferenceMap.entrySet().stream().forEach(entry -> {
            Map<String, String> preference = entry.getValue();
            if(StringUtils.equalsIgnoreCase(entry.getKey(),"testStep")) {
                executionPreferenceBean.setTestStep(Boolean.valueOf(preference.get(IS_VISIBLE)));
            }else if(StringUtils.equalsIgnoreCase(entry.getKey(),"testdata")) {
                executionPreferenceBean.setTestdata(Boolean.valueOf(preference.get(IS_VISIBLE)));
            }else if(StringUtils.equalsIgnoreCase(entry.getKey(),"expectedResult")) {
                executionPreferenceBean.setExpectedResult(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"stepAttachment")) {
                executionPreferenceBean.setStepAttachment(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"status")) {
                executionPreferenceBean.setStatus(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"comment")) {
                executionPreferenceBean.setComment(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"attachments")) {
                executionPreferenceBean.setAttachments(Boolean.valueOf(preference.get(IS_VISIBLE)));
            } else if(StringUtils.equalsIgnoreCase(entry.getKey(),"defects")) {
                executionPreferenceBean.setDefects(Boolean.valueOf(preference.get(IS_VISIBLE)));
            }
        });

        try {
            updateUserCustomizationPreferenceByKey(userId + EXECUTION_COLUMN_KEY,
                    getObjectMapper().writeValueAsString(executionPreferenceBean));
        } catch (IOException e) {
            log.error("Exception occurred while saving the preference.");
        }
    }
    /**
     *
     * @return
     */
    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return mapper;
    }   
    
    private Map<String, String> getDefaultPaginationWidthPreference() {
        return PAGINATION_WIDTH_DEFAULT;
    }

    private Map<String, String> TEST_STEP_DEFAULT_COLUMNS_NAMES = ImmutableMap.<String, String>builder()
            .put("teststep", "Test Step")
            .put("testdata", "Test Data")
            .put("testresult", "Expected Result")
            .put("attachment", "Attachment")
            .build();

    private Map<String, String> CYCLE_SUMMARY_DEFAULT_COLUMNS_NAMES = ImmutableMap.<String, String>builder()
            .put("status", "Status")
            .put("summary", "Summary")
            .put("component", "Component")
            .put("label", "Label")
            .put("executedBy", "Executed By")
            .put("executedOn", "Executed On")
            .put("assignee", "Assignee")
            .put("defects", "Defect")
            .build();

    private Map<String, String> EXECUTION_SUMMARY_DEFAULT_COLUMNS_NAMES = ImmutableMap.<String, String>builder()
            .put("testStep", "Test Step")
            .put("testdata", "Test Data")
            .put("expectedResult", "Expected Result")
            .put("stepAttachment", "Step Attachment")
            .put("status", "status")
            .put("comment", "Comment")
            .put("attachments", "Attachments")
            .put("defects", "Defects")
            .build();

    private Map<String, String> TEST_EXECUTION_DEFAULT_COLUMNS_NAMES = ImmutableMap.<String, String>builder()
            .put("version", "Version")
            .put("testCycle", "Test Cycle")
            .put("folder", "Folder")
            .put("status", "Status")
            .put("executedBy", "Executed By")
            .put("executedOn", "Executed On")
            .put("defects", "Defects")
            .build();

    private Map<String, String> PAGINATION_WIDTH_DEFAULT = ImmutableMap.<String, String>builder()
            .put("planCycleSummary", "10")
            .put("searchTestExecution", "10")
            .put("standaloneExecution", "10")
            .build();


    @XmlRootElement
    @ApiModel("walkThroughDetail")
    public static class WalkThroughDetail {

        @XmlElement(nillable = false)
        @ApiModelProperty(required = false)
        private boolean cycleSummary = Boolean.TRUE;

        @XmlElement(nillable = false)
        @ApiModelProperty(required = true)
        private boolean customField = Boolean.TRUE;

        @XmlElement(nillable = true)
        @ApiModelProperty(required = false)
        private boolean issueView = Boolean.TRUE;

        @XmlElement(nillable = false)
        @ApiModelProperty(required = true)
        private boolean executionPage = Boolean.TRUE;

        @XmlElement(nillable = false)
        @ApiModelProperty(required = true)
        private boolean skip = Boolean.TRUE;

        public boolean isCycleSummary() {
            return cycleSummary;
        }

        public void setCycleSummary(boolean cycleSummary) {
            this.cycleSummary = cycleSummary;
        }

        public boolean isCustomField() {
            return customField;
        }

        public void setCustomField(boolean customField) {
            this.customField = customField;
        }

        public boolean isIssueView() {
            return issueView;
        }

        public void setIssueView(boolean issueView) {
            this.issueView = issueView;
        }

        public boolean isExecutionPage() {
            return executionPage;
        }

        public void setExecutionPage(boolean executionPage) {
            this.executionPage = executionPage;
        }

        public boolean isSkip() { return skip; } 
        
        public void setSkip(boolean skip) { this.skip = skip; }
    }

}
