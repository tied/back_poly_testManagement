package com.thed.zephyr.je.rest.delegate.impl;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.rest.CustomFieldResource;
import com.thed.zephyr.je.rest.delegate.CustomFieldResourceDelegate;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class CustomFieldResourceDelegateImpl implements CustomFieldResourceDelegate {

    /**
     * constants
     */
    private static final String COMMA = ",";
    protected final Logger log = Logger.getLogger(CustomFieldResourceDelegateImpl.class);

    private JiraAuthenticationContext authContext;
    private ZephyrCustomFieldManager zephyrCustomFieldManager;
    private CustomFieldValueManager customFieldValueManager;
    private final EventPublisher eventPublisher;
    private final ScheduleManager scheduleManager;
    private final TeststepManager teststepManager;

    public CustomFieldResourceDelegateImpl(JiraAuthenticationContext authContext,
                                           ZephyrCustomFieldManager zephyrCustomFieldManager,
                                           CustomFieldValueManager customFieldValueManager,
                                           EventPublisher eventPublisher,
                                           ScheduleManager scheduleManager,
                                           TeststepManager teststepManager) {
        this.authContext = authContext;
        this.zephyrCustomFieldManager = zephyrCustomFieldManager;
        this.customFieldValueManager = customFieldValueManager;
        this.eventPublisher = eventPublisher;
        this.scheduleManager = scheduleManager;
        this.teststepManager = teststepManager;
    }

    @Override
    @Transactional
    public CustomFieldResource.CustomFieldResponse createCustomField(CustomFieldResource.CustomFieldRequest customFieldRequest, Long projectId) {
        CustomFieldResource.CustomFieldResponse response = new CustomFieldResource.CustomFieldResponse();
        Map<String, Object> customProperties = createCustomFieldProperties(customFieldRequest,projectId);
        // Saving the CustomField information into CustomField table
        CustomField customField = zephyrCustomFieldManager.saveCustomField(customProperties);
        if (CollectionUtils.isNotEmpty(customFieldRequest.getFieldOptions()) && (
                ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(StringUtils.upperCase(customFieldRequest.getFieldType())).equalsIgnoreCase(ApplicationConstants.RADIO_BUTTON) ||
                        ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(StringUtils.upperCase(customFieldRequest.getFieldType())).equalsIgnoreCase(ApplicationConstants.CHECKBOX) ||
                        ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(StringUtils.upperCase(customFieldRequest.getFieldType())).equalsIgnoreCase(ApplicationConstants.SINGLE_SELECT) ||
                        ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(StringUtils.upperCase(customFieldRequest.getFieldType())).equalsIgnoreCase(ApplicationConstants.MULTI_SELECT)

        )) {
            List<String> distinctOptionValueList = customFieldRequest.getFieldOptions().stream().distinct().collect(Collectors.toList());
            distinctOptionValueList.forEach(optionValue -> {
                zephyrCustomFieldManager.createCustomFieldOption(createCustomFielOptionProperties(optionValue, customField.getID()));
            });
        }
        response.setResponseMessage(authContext.getI18nHelper().getText("zephyr.customfield.creation.label",
                customField.getName(), "created"));
        response = prepareResponseForRequest(response,customField,projectId);

        return response;
    }

    @Override
    public CustomFieldResource.CustomFieldResponse updateCustomField(Long customFieldId, CustomFieldResource.CustomFieldRequest customFieldUpdateRequest) {
        CustomField existingField = zephyrCustomFieldManager.getCustomFieldById(customFieldId);
        CustomFieldResource.CustomFieldResponse response = new CustomFieldResource.CustomFieldResponse();

        if (StringUtils.isNotBlank(customFieldUpdateRequest.getName())) {
            existingField.setName(customFieldUpdateRequest.getName());
        }

        if (StringUtils.isNotBlank(customFieldUpdateRequest.getDescription())) {
            existingField.setDescription(customFieldUpdateRequest.getDescription());
        }

        if (StringUtils.isNotBlank(customFieldUpdateRequest.getDefaultValue())) {
            existingField.setDefaultValue(customFieldUpdateRequest.getDefaultValue());
        }

        existingField.setModifiedOn(new Date());

        existingField.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));

        zephyrCustomFieldManager.updateCustomField(existingField);

        response.setResponseMessage(authContext.getI18nHelper().getText("zephyr.customfield.updated.label",
                existingField.getName(), "updated"));

        response = prepareResponseForRequest(response,existingField,existingField.getProjectId());
        return response;
    }

    @Override
    public CustomField[] getCustomFields(Long projectId) {
        return zephyrCustomFieldManager.getCustomFieldsByProjectId(projectId);
    }

    @Override
    public CustomField getCustomFieldById(Long customFieldId) {
        return zephyrCustomFieldManager.getCustomFieldById(customFieldId);
    }

    @Override
    public CustomField[] getCustomFieldsByEntityType(String entityType) {
        return zephyrCustomFieldManager.getCustomFieldsByEntityType(entityType);
    }

    @Override
    public CustomField[] getCustomFieldsByEntityType(String entityType, Long projectId, Boolean isGlobal) {
        return zephyrCustomFieldManager.getCustomFieldsByEntityType(entityType,projectId,isGlobal);
    }

    @Override
    public CustomFieldOption createCustomFieldOption(Integer customFieldId, Map<String, String> params) {
        Map<String, Object> fieldOptionProperties = new HashMap<>();
        fieldOptionProperties.put("CUSTOM_FIELD_ID", customFieldId);
        fieldOptionProperties.put("OPTION_VALUE", params.get("optionValue"));

        if(StringUtils.isNotBlank(params.get("isDisabled"))) {
            fieldOptionProperties.put("IS_DISABLED", Boolean.valueOf(params.get("isDisabled")));
        }

        if(StringUtils.isNotBlank(params.get("sequence"))) {
            fieldOptionProperties.put("SEQUENCE", Integer.valueOf(params.get("sequence")));
        }
        return zephyrCustomFieldManager.createCustomFieldOption(fieldOptionProperties);
    }

    @Override
    public CustomFieldsMeta[] getCustomFieldsMeta() {
        return zephyrCustomFieldManager.getCustomFieldMeta();
    }

    @Override
    public CustomFieldOption[] getCustomFieldOptions(int customfieldId) {
        return zephyrCustomFieldManager.getCustomFieldOptions(customfieldId);
    }

    @Override
    public CustomFieldOption getCustomFieldOptionById(Integer customFieldOptionId) {
        return zephyrCustomFieldManager.getCustomFieldOptionById(customFieldOptionId);
    }

    @Override
    public CustomFieldOption updateCustomFieldOption(Integer customFieldOptionId, Map<String, String> params) {
        CustomFieldOption existingCustomFieldOption = zephyrCustomFieldManager.getCustomFieldOptionById(customFieldOptionId);

        if (Objects.nonNull(existingCustomFieldOption)) {
            // preserve the old custom field option value
            // update the custom field option value.
            // Update the execution custom field option value data with new value.
            // trigger an event for schedule reindex

            String existingOptionValue = existingCustomFieldOption.getOptionValue();
            String newOptionValue = params.get("optionValue");

            if (StringUtils.isNotBlank(newOptionValue)) {
                existingCustomFieldOption.setOptionValue(newOptionValue);
            }
            if (StringUtils.isNotBlank(params.get("isDisabled"))) {
                existingCustomFieldOption.setIsDisabled(Boolean.getBoolean(params.get("isDisabled")));
            }
            zephyrCustomFieldManager.updateCustomFieldOption(existingCustomFieldOption);

            CustomField customField = existingCustomFieldOption.getCustomField();

            if (ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(customField.getZFJEntityType())) {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {
                    ExecutionCf[] executionCfs = customFieldValueManager.getCustomFieldValuesForExecutionByCustomFieldId(new Long(customField.getID()));

                    if (Objects.nonNull(executionCfs)) {
                        List<ExecutionCf> executionCfList = Arrays.asList(executionCfs);
                        List<Integer> scheduleIds = new ArrayList<>();
                        executionCfList.stream().forEach(executionCf -> {
                            if (StringUtils.contains(executionCf.getStringValue(), existingOptionValue)) {
                                List<String> values = Arrays.asList(StringUtils.split(executionCf.getStringValue(), COMMA));
                                if (CollectionUtils.isNotEmpty(values)) {
                                    List<String> updatedValueList = getUpdatedValueList(values, existingOptionValue);
                                    updatedValueList.add(newOptionValue);
                                    updateExecutionCfEntity(updatedValueList, null, executionCf);
                                    scheduleIds.add(executionCf.getExecutionId());
                                }
                            }
                        });
                        reindexSchedulesByIds(scheduleIds);
                    }
                });
                executorService.shutdown();
            } else if (ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(customField.getZFJEntityType())) {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {
                    TestStepCf[] testStepCfs = customFieldValueManager.getCustomFieldValuesForTestStepByCustomFieldId(new Long(customField.getID()));

                    if (Objects.nonNull(testStepCfs)) {
                        List<TestStepCf> testStepCfList = Arrays.asList(testStepCfs);
                        testStepCfList.stream().forEach(testStepCf -> {
                            if (StringUtils.contains(testStepCf.getStringValue(), existingOptionValue)) {
                                List<String> values = Arrays.asList(StringUtils.split(testStepCf.getStringValue(), COMMA));
                                if (CollectionUtils.isNotEmpty(values)) {
                                    List<String> updatedValueList = getUpdatedValueList(values, existingOptionValue);
                                    updatedValueList.add(newOptionValue);
                                    updateTestStepCfEntity(updatedValueList, null, testStepCf);
                                }
                            }
                        });
                    }
                });
                executorService.shutdown();
            }
        }
        return existingCustomFieldOption;
    }

    @Override
    public void deleteCustomFieldOption(Integer customFieldOptionId) {
        CustomFieldOption customFieldOption = zephyrCustomFieldManager.getCustomFieldOptionById(customFieldOptionId);
        if (Objects.nonNull(customFieldOption)) {
            // Update the execution custom field option value data if the custom field is of entity type 'EXECUTION'.
            // trigger an event for reindex the schedules.
            // delete the custom field option value.

            CustomField customField = customFieldOption.getCustomField();

            if(ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(customField.getZFJEntityType())) {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {
                    String customFieldOptionValue = customFieldOption.getOptionValue();
                    String customFieldOptionValueId = customFieldOption.getID()+ StringUtils.EMPTY;
                    ExecutionCf[] executionCfs = customFieldValueManager.getCustomFieldValuesForExecutionByCustomFieldId(new Long(customField.getID()));

                    if(Objects.nonNull(executionCfs)) {
                        List<ExecutionCf> executionCfList = Arrays.asList(executionCfs);
                        List<Integer> scheduleIds = new ArrayList<>();
                        executionCfList.stream().forEach(executionCf -> {
                            if(StringUtils.contains(executionCf.getStringValue(),customFieldOptionValue)) {
                                List<String> values = Arrays.asList(StringUtils.split(executionCf.getStringValue(),COMMA));
                                List<String> selectedOptionValues = null;
                                if(StringUtils.isNotBlank(executionCf.getSelectedOptions())) {
                                    selectedOptionValues = Arrays.asList(StringUtils.split(executionCf.getSelectedOptions(),COMMA));
                                }

                                if(CollectionUtils.isNotEmpty(values)) {
                                    List<String> updatedValueList = getUpdatedValueList(values,customFieldOptionValue);
                                    List<String> selectedOptionValuesList = null;
                                    if(CollectionUtils.isNotEmpty(selectedOptionValues)) {
                                        selectedOptionValuesList = getUpdatedValueList(selectedOptionValues,customFieldOptionValueId);
                                    }
                                    updateExecutionCfEntity(updatedValueList,selectedOptionValuesList,executionCf);
                                    scheduleIds.add(executionCf.getExecutionId());
                                }
                            }
                        });
                        reindexSchedulesByIds(scheduleIds);
                        zephyrCustomFieldManager.deleteCustomFieldOption(customFieldOption);
                    }
                });
                executorService.shutdown();
            } else if(ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(customField.getZFJEntityType())) {
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.execute(() -> {
                    String customFieldOptionValue = customFieldOption.getOptionValue();
                    String customFieldOptionValueId = customFieldOption.getID()+ StringUtils.EMPTY;
                    TestStepCf[] testStepCfs = customFieldValueManager.getCustomFieldValuesForTestStepByCustomFieldId(new Long(customField.getID()));

                    if(Objects.nonNull(testStepCfs)) {
                        List<TestStepCf> testStepCfsList = Arrays.asList(testStepCfs);
                        testStepCfsList.stream().forEach(testStepCf -> {
                            if(StringUtils.contains(testStepCf.getStringValue(),customFieldOptionValue)) {
                                List<String> values = Arrays.asList(StringUtils.split(testStepCf.getStringValue(),COMMA));
                                List<String> selectedOptionValues = null;
                                if(StringUtils.isNotBlank(testStepCf.getSelectedOptions())) {
                                    selectedOptionValues = Arrays.asList(StringUtils.split(testStepCf.getSelectedOptions(),COMMA));
                                }

                                if(CollectionUtils.isNotEmpty(values)) {
                                    List<String> updatedValueList = getUpdatedValueList(values,customFieldOptionValue);
                                    List<String> selectedOptionValuesList = null;
                                    if(CollectionUtils.isNotEmpty(selectedOptionValues)) {
                                        selectedOptionValuesList = getUpdatedValueList(selectedOptionValues,customFieldOptionValueId);
                                    }
                                    updateTestStepCfEntity(updatedValueList,selectedOptionValuesList,testStepCf);
                                }
                            }
                        });
                        zephyrCustomFieldManager.deleteCustomFieldOption(customFieldOption);
                    }
                });
                executorService.shutdown();
            }
        }
    }

    @Override
    public void deleteCustomField(Long customFieldId) {
        CustomField existingCustomField = zephyrCustomFieldManager.getCustomFieldById(customFieldId);

        if(Objects.nonNull(existingCustomField)) {
            // delete the custom field option values
            // delete the data from entity table
            // reindex the schedules
            // delete the associated disabled project level custom fields.
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                CustomFieldOption[] customFieldOptions = zephyrCustomFieldManager.getCustomFieldOptions(customFieldId.intValue());
                if(Objects.nonNull(customFieldOptions)) {
                    zephyrCustomFieldManager.deleteCustomFieldOptions(customFieldOptions);
                }
                if(ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(existingCustomField.getZFJEntityType())) {
                    ExecutionCf[] executionCfs = customFieldValueManager.getCustomFieldValuesForExecutionByCustomFieldId(customFieldId);

                    if(Objects.nonNull(executionCfs)) {
                        List<Integer> scheduleIds = Arrays.stream(executionCfs).map(ExecutionCf::getExecutionId).collect(Collectors.toList());
                        customFieldValueManager.deleteExecutionCustomFieldValues(executionCfs);
                        deleteActiveCustomFieldProjectMapping(getCustomFieldById(customFieldId));
                        zephyrCustomFieldManager.deleteCustomField(customFieldId);
                        reindexSchedulesByIds(scheduleIds);
                    }
                }else if(ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(existingCustomField.getZFJEntityType())) {
                    TestStepCf[] testStepCfs = customFieldValueManager.getCustomFieldValuesForTestStepByCustomFieldId(customFieldId);
                    if(Objects.nonNull(testStepCfs)) {
                        customFieldValueManager.deleteTestStepCustomFieldValues(testStepCfs);
                        deleteActiveCustomFieldProjectMapping(getCustomFieldById(customFieldId));
                        zephyrCustomFieldManager.deleteCustomField(customFieldId);
                    }
                }
            });
            executor.shutdown();
        }
    }

    @Override
    public List<CustomField> getCustomFieldsByEntityTypeAndProject(String entityType, Long projectId) {
        return zephyrCustomFieldManager.getCustomFieldsByEntityTypeAndProject(entityType,projectId);
    }

    @Override
    public Integer getCustomFieldCount(String entityType) {
        return zephyrCustomFieldManager.getCustomFieldCount(entityType);
    }

    @Override
    public boolean checkCustomFieldNameUniqueness(String entityType, Long projectId, String customFieldName) {
        return zephyrCustomFieldManager.checkCustomFieldNameUniqueness(entityType,projectId,customFieldName);
    }

    @Override
    public void enableOrDisableCustomFieldForProject(Long projectId, Boolean enable, CustomField customField) {
        CustomFieldProject customFieldProject = zephyrCustomFieldManager.getCustomFieldProjectByCustomFieldAndProjectId(customField.getID(), projectId);
        if(customFieldProject == null) {
        if(enable) {
                Map<String, Object> customFieldProjectProperties = new HashMap();
                customFieldProjectProperties.put("CUSTOM_FIELD_ID", customField.getID());
                customFieldProjectProperties.put("PROJECT_ID", projectId);
                customFieldProjectProperties.put("IS_ACTIVE", Boolean.TRUE);
                zephyrCustomFieldManager.createCustomFieldProject(customFieldProjectProperties);
            }
        } else {
            zephyrCustomFieldManager.deleteCustomFieldProject(projectId,customField.getID());
        }
    }

    @Override
    public CustomField[] getGlobalCustomFieldsByEntityTypeAndProjectId(String entityType, Long projectId) {
        return zephyrCustomFieldManager.getGlobalCustomFieldsByEntityTypeAndProjectId(entityType,projectId);
    }

    @Override
    public boolean getDisableCustomFieldForProjectAndCustomField(Long customFieldId, Long projectId) {
        CustomFieldProject[] customFieldProjects = zephyrCustomFieldManager.getDisabledCustomFieldProjectByProjectAndCustomField(customFieldId.intValue(),projectId);
        return (Objects.nonNull(customFieldProjects) && customFieldProjects.length > 0 );
    }

    private Map<String, Object> createCustomFielOptionProperties(String option, int id) {
        Map<String, Object> customFieldProperties = new HashMap<>();
        customFieldProperties.put("CUSTOM_FIELD_ID", id);
        customFieldProperties.put("OPTION_VALUE", StringEscapeUtils.escapeSql(option));
        return customFieldProperties;
    }

    /**
     * Method creates the map object which holds the column name and value to that
     * specific column name for the table(customFiellds).
     *
     * @return -- Returns the created map object which holds the column name and
     * value to that specific column name for the table(customFields).
     */
    private Map<String, Object> createCustomFieldProperties(
            CustomFieldResource.CustomFieldRequest customFieldRequest, Long projectId) {
        Map<String, Object> customFieldProperties = new HashMap<>();
        customFieldProperties.put("NAME", customFieldRequest.getName());
        customFieldProperties.put("DESCRIPTION",
                customFieldRequest.getDescription() == null ? "" : customFieldRequest.getDescription());
        customFieldProperties.put("DEFAULT_VALUE", customFieldRequest.getDefaultValue());
        customFieldProperties.put("IS_ACTIVE", Boolean.FALSE);
        customFieldProperties.put("CUSTOM_FIELD_TYPE", customFieldRequest.getFieldType());
        customFieldProperties.put("ALIAS_NAME", customFieldRequest.getAliasName());
        customFieldProperties.put("ZFJENTITY_TYPE", StringUtils.upperCase(customFieldRequest.getEntityType()));

        if(null != authContext) {
            customFieldProperties.put("CREATED_BY", UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
        }

        customFieldProperties.put("CREATED_ON",new Date());
        if(Objects.nonNull(projectId)) {
            customFieldProperties.put("PROJECT_ID",projectId);
        }
        customFieldProperties.put("DISPLAY_FIELD_TYPE", customFieldRequest.getDisplayFieldType());

        return customFieldProperties;
    }

    private CustomFieldResource.CustomFieldResponse prepareResponseForRequest(CustomFieldResource.CustomFieldResponse response, CustomField customField,
                                                                              Long projectId) {
        response.setId(customField.getID());
        response.setName(customField.getName());
        response.setFieldType(customField.getCustomFieldType());
        response.setEntityType(customField.getZFJEntityType());
        response.setAliasName(customField.getAliasName());
        response.setCreatedBy(customField.getCreatedBy());
        response.setCreatedOn(customField.getCreatedOn());

        CustomFieldOption[] customFieldOptions = zephyrCustomFieldManager.getCustomFieldOptions(customField.getID());
        if (customFieldOptions != null && customFieldOptions.length > 0) {
            Map<Integer,String> optionValues = new HashMap<>();
            for (CustomFieldOption customFieldOption : customFieldOptions) {
                optionValues.put(customFieldOption.getID(),customFieldOption.getOptionValue());
            }
            response.setCustomFieldOptionValues(optionValues);
        }
        response.setDescription(StringUtils.isNotBlank(customField.getDescription()) ? customField.getDescription() : StringUtils.EMPTY);
        response.setModifiedDate(null != customField.getModifiedOn() ? customField.getModifiedOn() : null);
        response.setModifiedBy(StringUtils.isNotBlank(customField.getModifiedBy()) ? customField.getModifiedBy() : StringUtils.EMPTY);
        response.setActive(customField.getIsActive());

        return response;
    }

    /**
     * Reindex schedules on custom field property update.
     * @param customFieldId
     */
    private void reindexSchedules(Integer customFieldId) {

        new Thread(() -> {
            ExecutionCf[] executionCfs = customFieldValueManager.getCustomFieldValuesForExecutionByCustomFieldId(new Long(customFieldId));

            if(Objects.nonNull(executionCfs) && executionCfs.length > 0) {
                List<Integer> scheduleIds = Arrays.stream(executionCfs).map(ExecutionCf::getExecutionId).collect(Collectors.toList());

                List<Schedule> schedulesList = new ArrayList<>();
                if(CollectionUtils.isNotEmpty(scheduleIds)) {
                    Schedule[] schedules = scheduleManager.getSchedules(scheduleIds);
                    schedulesList = Arrays.asList(schedules);
                }
                //Need re-Index hence publishing event to do it.
                Map<String, Object> params = new HashMap<>();
                params.put("ENTITY_TYPE", "SCHEDULE_ID");
                eventPublisher.publish(new SingleScheduleEvent(schedulesList, params, EventType.EXECUTION_UPDATED));
            }
        }).start();
    }

    /**
     * reindex schedules by schedule ids.
     * @param scheduleIds
     */
    private void reindexSchedulesByIds(List<Integer> scheduleIds) {
        new Thread(() -> {
                List<Schedule> schedulesList = new ArrayList<>();
                if(CollectionUtils.isNotEmpty(scheduleIds)) {
                    Schedule[] schedules = scheduleManager.getSchedules(scheduleIds);
                    schedulesList = Arrays.asList(schedules);
                }
                Map<String, Object> params = new HashMap<>();
                params.put("ENTITY_TYPE", "SCHEDULE_ID");
                eventPublisher.publish(new SingleScheduleEvent(schedulesList, params, EventType.EXECUTION_UPDATED));
        }).start();
    }

    /**
     *
     * @param schedules
     */
    private void reindexSchedules(List<Schedule> schedules) {
        new Thread(() -> {
            Map<String, Object> params = new HashMap<>();
            params.put("ENTITY_TYPE", "SCHEDULE_ID");
            eventPublisher.publish(new SingleScheduleEvent(schedules, params, EventType.EXECUTION_UPDATED));
        }).start();
    }

    /**
     *
     * @param values
     * @param customFieldOptionValue
     * @return
     */
    private List<String> getUpdatedValueList(List<String> values, String customFieldOptionValue) {
        List<String> updatedValueList = values.stream()
                .filter(value -> !customFieldOptionValue.equalsIgnoreCase(value))
                .collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(updatedValueList) ? updatedValueList : Lists.newArrayList();
    }

    /**
     * Updates the execution custom field entity with update values.
     * @param updatedValueList
     * @param selectedOptionValuesList
     * @param executionCf
     */
    private void updateExecutionCfEntity(List<String> updatedValueList, List<String> selectedOptionValuesList, ExecutionCf executionCf) {
        executionCf.setStringValue(StringUtils.join(updatedValueList,COMMA));
        if(CollectionUtils.isNotEmpty(selectedOptionValuesList)) {
            executionCf.setSelectedOptions(StringUtils.join(selectedOptionValuesList,COMMA));
        }
        executionCf.save();
    }

    /**
     * Updates the test step custom field entity with update values.
     * @param updatedValueList
     * @param selectedOptionValuesList
     * @param testStepCf
     */
    private void updateTestStepCfEntity(List<String> updatedValueList, List<String> selectedOptionValuesList, TestStepCf testStepCf) {
        testStepCf.setStringValue(StringUtils.join(updatedValueList,COMMA));
        if(CollectionUtils.isNotEmpty(selectedOptionValuesList)) {
            testStepCf.setSelectedOptions(StringUtils.join(selectedOptionValuesList,COMMA));
        }
        testStepCf.save();
    }

    private void deleteActiveCustomFieldProjectMapping(CustomField customField) {
        if(Objects.nonNull(customField)) {
            CustomFieldProject[] activeCustomFields = zephyrCustomFieldManager.getActiveCustomFieldsProjectByCustomFieldId(customField.getID());
            if(Objects.nonNull(activeCustomFields) && activeCustomFields.length > 0) {
                zephyrCustomFieldManager.deleteCustomFieldProjectMapping(activeCustomFields);
            }
        }
    }

    private void deleteAssociateCustomFieldValueDataForProject(CustomField customField, Long projectId) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            if(ApplicationConstants.ENTITY_TYPE.EXECUTION.name().equalsIgnoreCase(customField.getZFJEntityType())) {
                ExecutionCf[] executionCfs = customFieldValueManager.getCustomFieldValuesForExecutionByCustomFieldId(new Long(customField.getID()));

                if(Objects.nonNull(executionCfs) && executionCfs.length > 0) {
                    List<ExecutionCf> executionCfList = Lists.newArrayList();
                    List<Schedule> schedulesList = Lists.newArrayList();

                    Arrays.stream(executionCfs).forEach(executionCf -> {
                        Schedule schedule = scheduleManager.getSchedule(executionCf.getExecutionId());
                        if(Objects.nonNull(schedule) && schedule.getProjectId().equals(projectId)) {
                            executionCfList.add(executionCf);
                            schedulesList.add(schedule);
                        }
                    });

                    if(CollectionUtils.isNotEmpty(executionCfList) && CollectionUtils.isNotEmpty(schedulesList)) {
                        ExecutionCf[] executionCfsToBeDeleted = new ExecutionCf[executionCfList.size()];
                        executionCfsToBeDeleted = executionCfList.toArray(executionCfsToBeDeleted);
                        customFieldValueManager.deleteExecutionCustomFieldValues(executionCfsToBeDeleted);
                        reindexSchedules(schedulesList);
                    }
                }
            }else if(ApplicationConstants.ENTITY_TYPE.TESTSTEP.name().equalsIgnoreCase(customField.getZFJEntityType())) {
                TestStepCf[] testStepCfs = customFieldValueManager.getCustomFieldValuesForTestStepByCustomFieldId(new Long(customField.getID()));
                if(Objects.nonNull(testStepCfs) && testStepCfs.length > 0) {
                    List<TestStepCf> testStepCfsList = Lists.newArrayList();

                    Arrays.stream(testStepCfs).forEach(testStepCf -> {
                        Teststep teststep = teststepManager.getTeststep(testStepCf.getTestStepId());
                        MutableIssue issue = ComponentAccessor.getIssueManager().getIssueObject(teststep.getIssueId());
                        if(null != issue && issue.getProjectId().equals(projectId)) {
                            testStepCfsList.add(testStepCf);
                        }
                    });

                    if(CollectionUtils.isNotEmpty(testStepCfsList)) {
                        TestStepCf[] testStepCfsToBeDeleted = new TestStepCf[testStepCfsList.size()];
                        testStepCfsToBeDeleted = testStepCfsList.toArray(testStepCfsToBeDeleted);
                        customFieldValueManager.deleteTestStepCustomFieldValues(testStepCfsToBeDeleted);
                    }
                }
            }
        });
        executor.shutdown();
    }

}
