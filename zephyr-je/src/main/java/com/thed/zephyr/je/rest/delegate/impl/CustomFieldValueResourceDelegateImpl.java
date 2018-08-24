package com.thed.zephyr.je.rest.delegate.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.converters.DoubleConverter;
import com.atlassian.jira.issue.customfields.converters.DoubleConverterImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ScheduleModifyEvent;
import com.thed.zephyr.je.event.TeststepModifyEvent;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.rest.CustomFieldValueResource;
import com.thed.zephyr.je.rest.CustomFieldValueResource.CustomFieldValueRequest;
import com.thed.zephyr.je.rest.CustomFieldValueResource.CustomFieldValueResponse;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrWikiParser;
import net.java.ao.Entity;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;


public class CustomFieldValueResourceDelegateImpl implements CustomFieldValueResourceDelegate {

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(CustomFieldValueResourceDelegateImpl.class);


    private JiraAuthenticationContext authContext;
    private final CustomFieldValueManager customFieldValueManager;
    private final ZephyrCustomFieldManager zephyrCustomFieldManager;
    private final ScheduleManager scheduleManager;
    private final TeststepManager teststepManager;
    private final ScheduleIndexManager scheduleIndexManager;
    private final EventPublisher eventPublisher;
    private final static String RECORD_CREATED_SUCCESS_MESSAGE = "Custom field value record created successfully.";
    private final static String RECORD_UPDATED_SUCCESS_MESSAGE = "Custom field value record updated successfully.";

    public CustomFieldValueResourceDelegateImpl(final JiraAuthenticationContext authContext,
                                                final CustomFieldValueManager customFieldValueManager,
                                                final ScheduleManager scheduleManager,
                                                final TeststepManager teststepManager,
                                                final ScheduleIndexManager scheduleIndexManager,
                                                final EventPublisher eventPublisher,
                                                final ZephyrCustomFieldManager zephyrCustomFieldManager) {
        this.authContext = authContext;
        this.customFieldValueManager = customFieldValueManager;
        this.scheduleManager = scheduleManager;
        this.teststepManager = teststepManager;
        this.scheduleIndexManager = scheduleIndexManager;
        this.eventPublisher = eventPublisher;
        this.zephyrCustomFieldManager=zephyrCustomFieldManager;
    }

    @Override
    public CustomFieldValueResponse createCustomFieldValue(CustomFieldValueRequest customFieldValueRequest, String entityType) {
        CustomFieldValueResponse response = new CustomFieldValueResponse();

        Map<String, Object> customFieldValueProperties = prepareRequestForCustomFieldValue(customFieldValueRequest);
        if (ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(entityType)) {
            Table<String, String, Object> customFieldChangePropertyTable = HashBasedTable.create();
            customFieldValueProperties.put("TEST_STEP_ID", customFieldValueRequest.getEntityId());

            TestStepCf testStepCf = customFieldValueManager.saveTestStepCustomFieldValue(customFieldValueProperties);
            response = prepareResponseForTeststepCustomFieldValue(testStepCf, response, RECORD_CREATED_SUCCESS_MESSAGE);

            customFieldChangePropertyTable.put(response.getCustomFieldName(), ApplicationConstants.OLD, StringUtils.EMPTY);
            populateCustomFieldChangePropertyTableForTeststep(customFieldChangePropertyTable,testStepCf,ApplicationConstants.NEW);

            logExecutionHistoryDataForTeststep(customFieldChangePropertyTable,teststepManager.getTeststep(customFieldValueRequest.getEntityId()));

        } else if (ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(entityType)) {
            Table<String, String, Object> customFieldChangePropertyTable = HashBasedTable.create();
            customFieldValueProperties.put("EXECUTION_ID", customFieldValueRequest.getEntityId());

            ExecutionCf executionCf = customFieldValueManager.saveExecutionCustomFieldValue(customFieldValueProperties);
            response = prepareResponseForExecutionCustomFieldValue(executionCf, response, RECORD_CREATED_SUCCESS_MESSAGE);

            customFieldChangePropertyTable.put(response.getCustomFieldName(), ApplicationConstants.OLD, StringUtils.EMPTY);
            populateCustomFieldChangePropertyTable(customFieldChangePropertyTable,executionCf,ApplicationConstants.NEW);

            Schedule schedule = scheduleManager.getSchedule(customFieldValueRequest.getEntityId());
            logExecutionHistoryData(customFieldChangePropertyTable,schedule);
            reindexScheduleOnCustomFieldUpdate(schedule);
        }

        return response;
    }

    @Override
    public Map<String, CustomFieldValueResource.CustomFieldValueResponse> createCustomFieldValues(Map<String, CustomFieldValueRequest> customFieldValueRequests, String entityType, Integer entityId) {

        Map<String, CustomFieldValueResource.CustomFieldValueResponse> customFieldValueResponse = new HashMap<>();
        CustomFieldValueResponse response;
        Table<String, String, Object> customFieldChangePropertyTable = HashBasedTable.create();

        for(Map.Entry<String, CustomFieldValueRequest> customFieldValueRequest : customFieldValueRequests.entrySet()) {

            CustomFieldValueRequest request = customFieldValueRequest.getValue();
            response = new CustomFieldValueResponse();

            Map<String, Object> customFieldValueProperties;

            if (ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(entityType)) {
                if(StringUtils.isNotEmpty(request.getValue())) {

                    if(null != request.getCustomFieldValueId()) {
                        TestStepCf existingCfTestStepCf = customFieldValueManager.getTeststepCustomFieldValue(request.getCustomFieldValueId());
                        if(Objects.nonNull(existingCfTestStepCf)) {
                            populateCustomFieldChangePropertyTableForTeststep(customFieldChangePropertyTable,existingCfTestStepCf,ApplicationConstants.OLD);

                            existingCfTestStepCf = prepareUpdateRequestForTeststep(request, existingCfTestStepCf);
                            customFieldValueManager.updateTestStepCustomFieldValue(existingCfTestStepCf);
                            response = prepareResponseForTeststepCustomFieldValue(existingCfTestStepCf, response, RECORD_UPDATED_SUCCESS_MESSAGE);

                            populateCustomFieldChangePropertyTableForTeststep(customFieldChangePropertyTable,existingCfTestStepCf,ApplicationConstants.NEW);
                            logExecutionHistoryDataForTeststep(customFieldChangePropertyTable,teststepManager.getTeststep(existingCfTestStepCf.getTestStepId()));
                        }
                    }else {
                        customFieldValueProperties = prepareRequestForCustomFieldValue(request);
                        customFieldValueProperties.put("TEST_STEP_ID", entityId);

                        TestStepCf testStepCf = customFieldValueManager.saveTestStepCustomFieldValue(customFieldValueProperties);
                        response = prepareResponseForTeststepCustomFieldValue(testStepCf, response, RECORD_CREATED_SUCCESS_MESSAGE);

                        customFieldChangePropertyTable.put(response.getCustomFieldName(), ApplicationConstants.OLD, StringUtils.EMPTY);
                        populateCustomFieldChangePropertyTableForTeststep(customFieldChangePropertyTable,testStepCf,ApplicationConstants.NEW);

                        logExecutionHistoryDataForTeststep(customFieldChangePropertyTable,teststepManager.getTeststep(testStepCf.getTestStepId()));
                    }

                    customFieldValueResponse.putIfAbsent(String.valueOf(response.getCustomFieldId()), response);
                }

            } else if (ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(entityType)) {
                if(StringUtils.isNotEmpty(request.getValue())) {

                    if(null != request.getCustomFieldValueId()) {
                        ExecutionCf existingCfExecutionCf = customFieldValueManager.getExecutionCustomFieldValue(request.getCustomFieldValueId());
                        if(Objects.nonNull(existingCfExecutionCf)) {
                            existingCfExecutionCf = prepareUpdateRequestForExecution(request, existingCfExecutionCf);
                            customFieldValueManager.updateExecutionCustomFieldValue(existingCfExecutionCf);
                            response = prepareResponseForExecutionCustomFieldValue(existingCfExecutionCf, response, RECORD_UPDATED_SUCCESS_MESSAGE);
                        }
                    } else {
                        customFieldValueProperties = prepareRequestForCustomFieldValue(request);
                        customFieldValueProperties.put("EXECUTION_ID", entityId);
                        ExecutionCf executionCf = customFieldValueManager.saveExecutionCustomFieldValue(customFieldValueProperties);
                        response = prepareResponseForExecutionCustomFieldValue(executionCf, response, RECORD_CREATED_SUCCESS_MESSAGE);
                    }

                    customFieldValueResponse.putIfAbsent(String.valueOf(response.getCustomFieldId()), response);
                }
                Schedule schedule = scheduleManager.getSchedule(entityId);
                reindexScheduleOnCustomFieldUpdate(schedule);
            }
        }

        return customFieldValueResponse;
    }

    @Override
    public Map<String, CustomFieldValueResource.CustomFieldValueResponse> getCustomFieldValuesByEntityId(Integer entityId, String entityType, Issue issue) {

        Map<String, CustomFieldValueResource.CustomFieldValueResponse> responseMap = new HashMap<>();
        if (ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(entityType)) {
            List<TestStepCf> testStepCfList = customFieldValueManager.getCustomFieldValuesForTeststep(entityId);
            return prepareResponseForTestStepCustomFieldValues(testStepCfList, responseMap, StringUtils.EMPTY,issue);
        } else if (ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(entityType)) {
            List<ExecutionCf> executionCfList = customFieldValueManager.getCustomFieldValuesForExecution(entityId);
            return prepareResponseForExecutionCustomFieldValues(executionCfList, responseMap, StringUtils.EMPTY,issue);
        }
        return responseMap;
    }

    @Override
    public CustomFieldValueResponse updateCustomFieldValue(CustomFieldValueRequest customFieldValueRequest, String entityType,
                                                           Long customFieldValueId) {

        CustomFieldValueResponse response = new CustomFieldValueResponse();

        if (ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(entityType)) {
            Table<String, String, Object> customFieldChangePropertyTable = HashBasedTable.create();
            TestStepCf testStepCf = customFieldValueManager.getTeststepCustomFieldValue(customFieldValueId);
            if(Objects.nonNull(testStepCf)) {
                populateCustomFieldChangePropertyTableForTeststep(customFieldChangePropertyTable,testStepCf,ApplicationConstants.OLD);

                testStepCf = prepareUpdateRequestForTeststep(customFieldValueRequest,testStepCf);
                customFieldValueManager.updateTestStepCustomFieldValue(testStepCf);
                response = prepareResponseForTeststepCustomFieldValue(testStepCf, response, RECORD_UPDATED_SUCCESS_MESSAGE);

                populateCustomFieldChangePropertyTableForTeststep(customFieldChangePropertyTable,testStepCf,ApplicationConstants.NEW);
                logExecutionHistoryDataForTeststep(customFieldChangePropertyTable,teststepManager.getTeststep(testStepCf.getTestStepId()));

            } // add error response
        }else if (ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(entityType)) {

            ExecutionCf executionCf = customFieldValueManager.getExecutionCustomFieldValue(customFieldValueId);
            if(Objects.nonNull(executionCf)) {
                Table<String, String, Object> customFieldChangePropertyTable = HashBasedTable.create();
                populateCustomFieldChangePropertyTable(customFieldChangePropertyTable,executionCf,ApplicationConstants.OLD);

                executionCf = prepareUpdateRequestForExecution(customFieldValueRequest,executionCf);
                customFieldValueManager.updateExecutionCustomFieldValue(executionCf);
                response = prepareResponseForExecutionCustomFieldValue(executionCf, response, RECORD_UPDATED_SUCCESS_MESSAGE);

                populateCustomFieldChangePropertyTable(customFieldChangePropertyTable,executionCf,ApplicationConstants.NEW);
                Schedule schedule = scheduleManager.getSchedule(executionCf.getExecutionId());
                logExecutionHistoryData(customFieldChangePropertyTable,schedule);
                reindexScheduleOnCustomFieldUpdate(schedule);
            }
        }
        return response;
    }

    @Override
    public Map<String, CustomFieldValueResponse> updateCustomFieldValues(Map<String, CustomFieldValueRequest> customFieldValueRequests, String entityType, Integer entityId) {

        Map<String, CustomFieldValueResponse> customFieldValueResponse = new HashMap<>();
        CustomFieldValueResponse response;
        Table<String, String, Object> customFieldChangePropertyTable = HashBasedTable.create();

        for(Map.Entry<String, CustomFieldValueRequest> customFieldValueRequest : customFieldValueRequests.entrySet()) {
            response = new CustomFieldValueResponse();
            CustomFieldValueRequest customFieldValueRequestObject = customFieldValueRequest.getValue();
            if (ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(entityType)) {
                TestStepCf testStepCf = customFieldValueManager.getTeststepCustomFieldValue(customFieldValueRequestObject.getCustomFieldValueId());
                if(Objects.nonNull(testStepCf)) {
                    populateCustomFieldChangePropertyTableForTeststep(customFieldChangePropertyTable,testStepCf,ApplicationConstants.OLD);

                    testStepCf = prepareUpdateRequestForTeststep(customFieldValueRequestObject, testStepCf);
                    customFieldValueManager.updateTestStepCustomFieldValue(testStepCf);
                    response = prepareResponseForTeststepCustomFieldValue(testStepCf, response, RECORD_UPDATED_SUCCESS_MESSAGE);
                    customFieldValueResponse.putIfAbsent(String.valueOf(response.getCustomFieldId()), response);

                    populateCustomFieldChangePropertyTableForTeststep(customFieldChangePropertyTable,testStepCf,ApplicationConstants.NEW);
                    logExecutionHistoryDataForTeststep(customFieldChangePropertyTable,teststepManager.getTeststep(testStepCf.getTestStepId()));

                } else {
                    if(StringUtils.isNotEmpty(customFieldValueRequestObject.getValue())) {
                        Map<String, Object> customFieldValueProperties = prepareRequestForCustomFieldValue(customFieldValueRequestObject);
                        customFieldValueProperties.put("TEST_STEP_ID", entityId);
                        testStepCf = customFieldValueManager.saveTestStepCustomFieldValue(customFieldValueProperties);
                        response = prepareResponseForTeststepCustomFieldValue(testStepCf, response, RECORD_UPDATED_SUCCESS_MESSAGE);
                        customFieldValueResponse.putIfAbsent(String.valueOf(response.getCustomFieldId()), response);
                        populateCustomFieldChangePropertyTableForTeststep(customFieldChangePropertyTable,testStepCf,ApplicationConstants.NEW);
                        logExecutionHistoryDataForTeststep(customFieldChangePropertyTable,teststepManager.getTeststep(testStepCf.getTestStepId()));
                    }
                }
            } else if (ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(entityType)) {
                ExecutionCf executionCf = customFieldValueManager.getExecutionCustomFieldValue(customFieldValueRequest.getValue().getCustomFieldValueId());
                if(Objects.nonNull(executionCf)) {
                    executionCf = prepareUpdateRequestForExecution(customFieldValueRequest.getValue(), executionCf);
                    customFieldValueManager.updateExecutionCustomFieldValue(executionCf);
                    response = prepareResponseForExecutionCustomFieldValue(executionCf, response, RECORD_UPDATED_SUCCESS_MESSAGE);
                    customFieldValueResponse.putIfAbsent(String.valueOf(response.getCustomFieldId()), response);
                } else {
                    if(StringUtils.isNotEmpty(customFieldValueRequestObject.getValue())) {
                        Map<String, Object> customFieldValueProperties = prepareRequestForCustomFieldValue(customFieldValueRequestObject);
                        customFieldValueProperties.put("EXECUTION_ID", entityId);
                        executionCf = customFieldValueManager.saveExecutionCustomFieldValue(customFieldValueProperties);
                        response = prepareResponseForExecutionCustomFieldValue(executionCf, response, RECORD_UPDATED_SUCCESS_MESSAGE);
                        customFieldValueResponse.putIfAbsent(String.valueOf(response.getCustomFieldId()), response);
                    }
                }
            }
        }
        return customFieldValueResponse;
    }

    @Override
    public void deleteCustomFieldValuesForTeststep(Integer testStepId) {
        customFieldValueManager.deleteTestStepCustomFieldValues(testStepId);
    }

    @Override
    public Table<String, String, Object> createOrUpdateCustomFieldRecordForBulkAssign(Map<String, Object> customFieldValueProperties, String customFieldName) {

        Table<String, String, Object> changePropertyTable = HashBasedTable.create();
        ExecutionCf executionCf = customFieldValueManager.getCustomFieldForExecutionByCustomFieldIdAndExecutionId((Long) customFieldValueProperties.get("CUSTOM_FIELD_ID"),
                (Integer) customFieldValueProperties.get("EXECUTION_ID"));

        if(null != executionCf) {
            populateCustomFieldChangePropertyTable(changePropertyTable,executionCf,ApplicationConstants.OLD);
            customFieldValueManager.deleteExecutionCustomFieldValue(executionCf);
            ExecutionCf executionCustomFieldValue = customFieldValueManager.saveExecutionCustomFieldValue(customFieldValueProperties);
            populateCustomFieldChangePropertyTable(changePropertyTable,executionCustomFieldValue,ApplicationConstants.NEW);
        }else {
            changePropertyTable.put(customFieldName, ApplicationConstants.OLD, StringUtils.EMPTY);
            ExecutionCf executionCustomFieldValue = customFieldValueManager.saveExecutionCustomFieldValue(customFieldValueProperties);
            populateCustomFieldChangePropertyTable(changePropertyTable,executionCustomFieldValue,ApplicationConstants.NEW);
        }
        return changePropertyTable;
    }

    @Override
    public ExecutionCf getExecutionCustomFieldValue(Long customFieldValueId) {
        return customFieldValueManager.getExecutionCustomFieldValue(customFieldValueId);
    }

    @Override
    public TestStepCf getTeststepCustomFieldValue(Long customFieldValueId) {
        return customFieldValueManager.getTeststepCustomFieldValue(customFieldValueId);
    }

    @Override
    public boolean getCustomFieldAndEntityRecord(CustomFieldValueRequest customFieldValueRequest) {
        return customFieldValueManager.getCustomFieldAndEntityRecord(customFieldValueRequest.getCustomFieldId(),customFieldValueRequest.getEntityId(),
                customFieldValueRequest.getEntityType());
    }


    /**
     * @param request
     * @return
     */
    private Map<String, Object> prepareRequestForCustomFieldValue(CustomFieldValueRequest request) {
        Map<String, Object> customFieldValueProperties = new HashMap<>();

        customFieldValueProperties.put("CUSTOM_FIELD_ID", request.getCustomFieldId());


        if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.STRING_VALUE)) {
            customFieldValueProperties.put(ApplicationConstants.STRING_VALUE, request.getValue());
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
            customFieldValueProperties.put(ApplicationConstants.STRING_VALUE, getValueForSelectedOptions(request.getSelectedOptions()));
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
            customFieldValueProperties.put(ApplicationConstants.LARGE_VALUE, request.getValue());
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
            if(StringUtils.isEmpty(request.getValue())) {
                customFieldValueProperties.put(ApplicationConstants.NUMBER_VALUE, null);
            } else {
                customFieldValueProperties.put(ApplicationConstants.NUMBER_VALUE, NumberUtils.toDouble(request.getValue()));
            }
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)
                || ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
            try {
                if(StringUtils.isEmpty(request.getValue())) {
                    Date inputDate = null;
                    customFieldValueProperties.put(ApplicationConstants.DATE_VALUE, inputDate);
                } else {
                    Date userInputDate = new Date(Long.parseLong(request.getValue()) * 1000);
                    customFieldValueProperties.put(ApplicationConstants.DATE_VALUE, userInputDate);
                }
            } catch (Exception e) {
                logger.error("Exception occurred while parsing the date");
            }
        }

        customFieldValueProperties.put("SELECTED_OPTIONS", request.getSelectedOptions());
        return customFieldValueProperties;
    }

    /**
     *
     * @param request
     * @param testStepCf
     * @return
     */
    private TestStepCf prepareUpdateRequestForTeststep(CustomFieldValueRequest request, TestStepCf testStepCf) {
        if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.STRING_VALUE)) {
            testStepCf.setStringValue(request.getValue());
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
            testStepCf.setStringValue((getValueForSelectedOptions(request.getSelectedOptions())));
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
            testStepCf.setLargeValue(request.getValue());
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
            if(StringUtils.isEmpty(request.getValue())) {
                testStepCf.setNumberValue(null);
            }else {
                testStepCf.setNumberValue(NumberUtils.toDouble(request.getValue()));
            }
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)
                || ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(testStepCf.getCustomField().getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
            try {
                Date inputDate = null;
                if(StringUtils.isNotBlank(request.getValue())) {
                    inputDate = new Date(getEpochDateValue(request.getValue()));
                }
                testStepCf.setDateValue(inputDate);
            } catch (Exception e) {
                logger.error("Exception occurred while parsing the date", e);
            }
        }

        testStepCf.setSelectedOptions(request.getSelectedOptions());
        return testStepCf;
    }

    /**
     *
     * @param request
     * @param executionCf
     * @return
     */
    private ExecutionCf prepareUpdateRequestForExecution(CustomFieldValueRequest request, ExecutionCf executionCf) {
        String customFieldType = executionCf.getCustomField().getCustomFieldType();
        if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.STRING_VALUE)) {
            executionCf.setStringValue(request.getValue());
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
            executionCf.setStringValue((getValueForSelectedOptions(request.getSelectedOptions())));
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
            executionCf.setLargeValue(request.getValue());
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
            if(StringUtils.isEmpty(request.getValue())) {
                executionCf.setNumberValue(null);
            }else {
                executionCf.setNumberValue(NumberUtils.toDouble(request.getValue()));
            }
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)
                || ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(request.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
            try {
                Date inputDate = null;
                if(StringUtils.isNotBlank(request.getValue())) {
                    inputDate = new Date(getEpochDateValue(request.getValue()));
                }
                executionCf.setDateValue(inputDate);
            } catch (Exception e) {
                logger.error("Exception occurred while parsing the date", e);
            }
        }

        executionCf.setSelectedOptions(request.getSelectedOptions());

        return executionCf;
    }

    /**
     *
     * @param testStepCf
     * @param response
     * @return
     */
    private CustomFieldValueResponse prepareResponseForTeststepCustomFieldValue(TestStepCf testStepCf,
                                                                        CustomFieldValueResponse response, String responseMessage) {
        response.setCustomFieldValueId(testStepCf.getID());
        response.setResponseMessage(responseMessage);
        response.setCustomFieldId(Long.valueOf(testStepCf.getCustomField().getID()));
        if(Objects.nonNull(testStepCf.getCustomField().getProjectId())) {
            response.setProjectId(testStepCf.getCustomField().getProjectId());
        }
        response.setCustomFieldType(testStepCf.getCustomField().getCustomFieldType());
        response.setEntityId(testStepCf.getTestStepId());
        response.setValue(getValueForResponse(testStepCf.getCustomField().getCustomFieldType(), testStepCf));
        response.setCustomFieldName(testStepCf.getCustomField().getName());
        response.setCustomFieldDisplayName(testStepCf.getCustomField().getDisplayName());
        response.setSelectedOptions(null != testStepCf.getSelectedOptions() ? testStepCf.getSelectedOptions() : StringUtils.EMPTY);

        return response;
    }

    /**
     *
     * @param executionCf
     * @param response
     * @return
     */
    private CustomFieldValueResponse prepareResponseForExecutionCustomFieldValue(ExecutionCf executionCf,
                                                                                CustomFieldValueResponse response, String responseMessage) {
        response.setCustomFieldValueId(executionCf.getID());
        response.setResponseMessage(responseMessage);
        response.setCustomFieldId(Long.valueOf(executionCf.getCustomField().getID()));
        if(Objects.nonNull(executionCf.getCustomField().getProjectId())) {
            response.setProjectId(executionCf.getCustomField().getProjectId());
        }
        response.setCustomFieldType(executionCf.getCustomField().getCustomFieldType());
        response.setEntityId(executionCf.getExecutionId());
        response.setValue(getValueForResponse(executionCf.getCustomField().getCustomFieldType(), executionCf));
        response.setCustomFieldName(executionCf.getCustomField().getName());
        response.setCustomFieldDisplayName(executionCf.getCustomField().getDisplayName());
        response.setSelectedOptions(null != executionCf.getSelectedOptions() ? executionCf.getSelectedOptions() : StringUtils.EMPTY);

        return response;
    }
    /**
     * Custom field values response for list.
     * @param testStepCfList
     * @param responseMap
     * @param issue
     * @return
     */
    private Map<String, CustomFieldValueResource.CustomFieldValueResponse> prepareResponseForTestStepCustomFieldValues(List<TestStepCf> testStepCfList, Map<String, CustomFieldValueResponse> responseMap, String responseMessage, Issue issue) {
        CustomFieldValueResponse response;
        String customFieldType;
        for (TestStepCf testStepCf : testStepCfList) {
            if(Objects.nonNull(testStepCf.getCustomField().getName())) {
                response = new CustomFieldValueResponse();
                customFieldType = testStepCf.getCustomField().getCustomFieldType();
                response.setResponseMessage(responseMessage);
                response.setCustomFieldValueId(testStepCf.getID());
                response.setEntityId(testStepCf.getTestStepId());
                response.setCustomFieldId(Long.valueOf(testStepCf.getCustomField().getID()));
                if(Objects.nonNull(testStepCf.getCustomField().getProjectId())) {
                    response.setProjectId(testStepCf.getCustomField().getProjectId());
                }
                response.setCustomFieldType(customFieldType);
                response.setCustomFieldDisplayName(testStepCf.getCustomField().getDisplayName());
                response.setCustomFieldName(testStepCf.getCustomField().getName());
                response.setValue(getValueForResponse(customFieldType, testStepCf));
                if(StringUtils.equalsIgnoreCase(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType), ApplicationConstants.STRING_VALUE) ||
                        StringUtils.equalsIgnoreCase(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType), ApplicationConstants.LARGE_VALUE)) {
                    response.setHtmlValue(getHtmlValue(response.getValue(),issue));
                }
                response.setSelectedOptions(null != testStepCf.getSelectedOptions() ? testStepCf.getSelectedOptions() : StringUtils.EMPTY);
                responseMap.putIfAbsent(String.valueOf(testStepCf.getCustomField().getID()), response);
            }
        }
        return responseMap;
    }

    private Map<String, CustomFieldValueResource.CustomFieldValueResponse> prepareResponseForExecutionCustomFieldValues(List<ExecutionCf> executionCfList, Map<String, CustomFieldValueResponse> responseMap, String responseMessage, Issue issue) {
        CustomFieldValueResponse response;
        String customFieldType;
        for (ExecutionCf executionCf : executionCfList) {
           if (Objects.nonNull(executionCf.getCustomField().getName())) {
                response = new CustomFieldValueResponse();
                customFieldType = executionCf.getCustomField().getCustomFieldType();
                response.setResponseMessage(responseMessage);
                response.setCustomFieldValueId(executionCf.getID());
                response.setEntityId(executionCf.getExecutionId());
                response.setCustomFieldId(Long.valueOf(executionCf.getCustomField().getID()));
                if (Objects.nonNull(executionCf.getCustomField().getProjectId())) {
                    response.setProjectId(executionCf.getCustomField().getProjectId());
                }
                response.setCustomFieldType(customFieldType);
                response.setCustomFieldDisplayName(executionCf.getCustomField().getDisplayName());
                response.setCustomFieldName(executionCf.getCustomField().getName());
                response.setValue(getValueForResponse(customFieldType, executionCf));
                response.setSelectedOptions(null != executionCf.getSelectedOptions() ? executionCf.getSelectedOptions() : StringUtils.EMPTY);

                if (StringUtils.equalsIgnoreCase(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType), ApplicationConstants.STRING_VALUE) ||
                        StringUtils.equalsIgnoreCase(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType), ApplicationConstants.LARGE_VALUE)) {
                    response.setHtmlValue(getHtmlValue(response.getValue(), issue));
                }

                responseMap.putIfAbsent(String.valueOf(executionCf.getCustomField().getID()), response);
            }
        }
        return responseMap;
    }

    /**
     *
     * @param value
     * @param issue
     * @return
     */
    private String getHtmlValue(String value, Issue issue) {
        if(issue != null) {
            String htmlMarkup = ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(value, issue);

            if(StringUtils.isNotBlank(htmlMarkup)) {
                return htmlMarkup.replaceAll("\\r?\\n\n", "<br/>");
            }else {
                return htmlMarkup;
            }
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * This method will return the value mapped as per custom field value map.
     * @param customFieldType
     * @param entity
     * @return
     */
    private String getValueForResponse(String customFieldType, Entity entity) {

        if (entity instanceof TestStepCf) {
            TestStepCf testStepCf = (TestStepCf) entity;
            if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.STRING_VALUE) ||
                    ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
                return testStepCf.getStringValue();
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
                return testStepCf.getLargeValue();
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
                DoubleConverter doubleConverter = new DoubleConverterImpl(authContext);
                if(null != testStepCf.getNumberValue()) {
                    return doubleConverter.getStringForChangelog(testStepCf.getNumberValue());
                }else {
                    return StringUtils.EMPTY;
                }
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)
                    || ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
                return null != testStepCf.getDateValue() ? testStepCf.getDateValue().getTime() + StringUtils.EMPTY : StringUtils.EMPTY;
            }
        } else if (entity instanceof ExecutionCf) {
            ExecutionCf executionCf = (ExecutionCf) entity;
            if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.STRING_VALUE) ||
                    ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
                return executionCf.getStringValue();
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
                return executionCf.getLargeValue();
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
                DoubleConverter doubleConverter = new DoubleConverterImpl(authContext);
                if(null != executionCf.getNumberValue()) {
                    return doubleConverter.getStringForChangelog(executionCf.getNumberValue());
                }else {
                    return StringUtils.EMPTY;
                }
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)
                    || ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
                return null != executionCf.getDateValue() ? executionCf.getDateValue().getTime() + StringUtils.EMPTY : StringUtils.EMPTY;
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
                String[] multiSelect = StringUtils.split(executionCf.getSelectedOptions(),",");
                Set<String> customCFieldOptionValue = new LinkedHashSet<>();
                List<String> stringList = Arrays.asList(multiSelect);
                stringList.stream().forEach(selectedCustomFieldOptionId -> {
                    CustomFieldOption customFieldOptionById = zephyrCustomFieldManager.getCustomFieldOptionById(Integer.valueOf(selectedCustomFieldOptionId));
                    customCFieldOptionValue.add(customFieldOptionById.getOptionValue());
                });
                return StringUtils.join(customCFieldOptionValue,",");
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Reindex the schedule on addition of custom field value.
     * @param schedule
     */
    private void reindexScheduleOnCustomFieldUpdate(Schedule schedule) {
        if(Objects.nonNull(schedule)) {
            schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
            //setting modified date.
            schedule.setModifiedDate(new Date());
            schedule.save();
            try {
                logger.debug("Indexing Schedule while adding custom field :");
                Collection<Schedule> schedules = new ArrayList<>();
                schedules.add(schedule);
                EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(schedules);
                scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
            } catch (Exception e) {
                logger.error("Error Indexing Schedule while adding custom field :", e);
            }
        }
    }

    private Long getEpochDateValue(String dateString) {
        return Long.parseLong(dateString) * 1000;
    }

    /**
     * publishing ScheduleModifyEvent for change logs
     * @param customFieldChangePropertyTable
     * @param schedule
     */
    private void logExecutionHistoryData(Table<String, String, Object> customFieldChangePropertyTable, Schedule schedule) {
        eventPublisher.publish(new ScheduleModifyEvent(schedule, customFieldChangePropertyTable, EventType.EXECUTION_CUSTOMFIELD_UPDATED,
                UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
    }

    /**
     * populate custom field update changes for audit history.
     * @param customFieldChangePropertyTable
     * @param executionCf
     * @param valueType
     */
    private void populateCustomFieldChangePropertyTable(Table<String, String, Object> customFieldChangePropertyTable, ExecutionCf executionCf, String valueType) {
        if(null != executionCf) {
            String customFieldType = executionCf.getCustomField().getCustomFieldType();
            String value = getValueForResponse(executionCf.getCustomField().getCustomFieldType(), executionCf);
            if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
                DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
                customFieldChangePropertyTable.put(executionCf.getCustomField().getName(), valueType,
                        StringUtils.isNotBlank(value) ? dateFormat.format(new Date(Long.parseLong(value))) : StringUtils.EMPTY);
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)) {
                DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                customFieldChangePropertyTable.put(executionCf.getCustomField().getName(), valueType,
                        StringUtils.isNotBlank(value) ? dateFormat.format(new Date(Long.parseLong(value))) : StringUtils.EMPTY);
            } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
                DoubleConverter doubleConverter = new DoubleConverterImpl(authContext);
                if(StringUtils.isNotBlank(value) && !value.equalsIgnoreCase(StringUtils.EMPTY)) {
                    final String doubleValue = doubleConverter.getStringForChangelog(Double.valueOf(value));
                    customFieldChangePropertyTable.put(executionCf.getCustomField().getName(), valueType,doubleValue);
                } else {
                    customFieldChangePropertyTable.put(executionCf.getCustomField().getName(), valueType,StringUtils.EMPTY);
                }
            } else {
                if(StringUtils.isBlank(value)) {
                    value = StringUtils.EMPTY;
                }
                customFieldChangePropertyTable.put(executionCf.getCustomField().getName(), valueType, value);
            }
        }
    }

    /**
     *
     * @param customFieldChangePropertyTable
     * @param teststep
     */
    private void logExecutionHistoryDataForTeststep(Table<String, String, Object> customFieldChangePropertyTable, Teststep teststep) {
        eventPublisher.publish(new TeststepModifyEvent(teststep, customFieldChangePropertyTable, EventType.TESTSTEP_UPDATED,
                UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
    }

    /**
     *
     * @param customFieldChangePropertyTable
     * @param testStepCf
     * @param valueType
     */
    private void populateCustomFieldChangePropertyTableForTeststep(Table<String, String, Object> customFieldChangePropertyTable, TestStepCf testStepCf, String valueType) {
        String customFieldType = testStepCf.getCustomField().getCustomFieldType();
        String value = getValueForResponse(customFieldType, testStepCf);
        String customFieldName = testStepCf.getCustomField().getName();
        if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
            DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            customFieldChangePropertyTable.put(customFieldName, valueType,
                    StringUtils.isNotBlank(value) ? dateFormat.format(new Date(Long.parseLong(value))) : StringUtils.EMPTY);
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)) {
            DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            customFieldChangePropertyTable.put(customFieldName, valueType,
                    StringUtils.isNotBlank(value) ? dateFormat.format(new Date(Long.parseLong(value))) : StringUtils.EMPTY);
        } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
            DoubleConverter doubleConverter = new DoubleConverterImpl(authContext);
            if(StringUtils.isNotBlank(value) && !value.equalsIgnoreCase(StringUtils.EMPTY)) {
                final String doubleValue = doubleConverter.getStringForChangelog(Double.valueOf(value));
                customFieldChangePropertyTable.put(testStepCf.getCustomField().getName(), valueType,doubleValue);
            } else {
                customFieldChangePropertyTable.put(testStepCf.getCustomField().getName(), valueType,StringUtils.EMPTY);
            }
        } else {
            if(StringUtils.isBlank(value)) {
                value = StringUtils.EMPTY;
            }
            customFieldChangePropertyTable.put(customFieldName, valueType, value);
        }

    }

    /**
     *
     * @param selectedOptions
     * @return
     */
    private String getValueForSelectedOptions(String selectedOptions) {
        if(StringUtils.isNotBlank(selectedOptions)) {
            String[] selectedOptionsId = selectedOptions.split(",");

            if(null != selectedOptionsId && selectedOptionsId.length > 0) {
                StringBuilder sb = new StringBuilder(StringUtils.EMPTY);
                Arrays.stream(selectedOptionsId).forEach(optionId -> {
                    CustomFieldOption customFieldOption = zephyrCustomFieldManager.getCustomFieldOptionById(Integer.valueOf(optionId));
                    if(null != customFieldOption) {
                        if (sb.length() > 0) sb.append( ", " );
                        sb.append(customFieldOption.getOptionValue());
                    }
                });
                return sb.toString();
            }
        }
        return StringUtils.EMPTY;
    }
}
