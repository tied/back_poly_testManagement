package com.thed.zephyr.je.service;


import com.atlassian.jira.issue.customfields.option.Option;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.ExecutionCf;
import com.thed.zephyr.je.model.TestStepCf;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CustomFieldValueManager {

    /**
     * This method will be used to store custom field value for the entity type 'TEST STEP'
     * @param customFieldValueProperties
     * @return
     */
    TestStepCf saveTestStepCustomFieldValue(Map<String, Object> customFieldValueProperties);

    /**
     * This method will be used to store custom field value for the entity type 'Execution'
     * @param customFieldValueProperties
     * @return
     */
    ExecutionCf saveExecutionCustomFieldValue(Map<String, Object> customFieldValueProperties);

    /**
     * Get list of custom field values for given test step id.
     * @param entityId
     * @return
     */
    List<TestStepCf> getCustomFieldValuesForTeststep(Integer entityId);

    /**
     * Get list of custom field values for given execution id.
     * @param entityId
     * @return
     */
    List<ExecutionCf> getCustomFieldValuesForExecution(Integer entityId);

    /**
     * Get test step custom field value by id.
     * @param customFieldValueId
     * @return
     */
    TestStepCf getTeststepCustomFieldValue(Long customFieldValueId);

    /**
     * Update custom field value.
     * @param testStepCf
     */
    void updateTestStepCustomFieldValue(TestStepCf testStepCf);

    /**
     * Get execution custom field value object by Id.
     * @param customFieldValueId
     * @return
     */
    ExecutionCf getExecutionCustomFieldValue(Long customFieldValueId);

    /**
     * Update execution custom field value.
     * @param executionCf
     */
    void updateExecutionCustomFieldValue(ExecutionCf executionCf);

    /**
     * Delete custom field values by given test step Id.
     * @param testStepId
     */
    void deleteTestStepCustomFieldValues(Integer testStepId);

    /**
     *
     * @param customFieldId
     * @return
     */
    ExecutionCf[] getCustomFieldValuesForExecutionByCustomFieldId(Long customFieldId);

    /**
     *
     * @param executionCfs
     */
    void deleteExecutionCustomFieldValues(ExecutionCf[] executionCfs);

    /**
     *
     * @param customFieldId
     * @return
     */
    TestStepCf[] getCustomFieldValuesForTestStepByCustomFieldId(Long customFieldId);

    /**
     *
     * @param testStepCfs
     */
    void deleteTestStepCustomFieldValues(TestStepCf[] testStepCfs);

    /**
     *
     * @param customFieldType
     * @param customFieldValue
     * @param customFieldId
     * @return
     */
    List<ExecutionCf> checkCustomFieldValueExistByTypeAndKey(String customFieldType, String customFieldValue, Integer customFieldId);

    /**
     *
     * @param customFieldValueProperties
     * @return
     */
	ExecutionCf saveOrUpdateExecutionCustomFieldValue(Map<String, Object> customFieldValueProperties);

    /**
     *
     * @param customFieldType
     * @param valuePrefix
     * @param customFieldId
     * @return
     */
    List<ExecutionCf> getCustomFieldValueByType(String customFieldType, String valuePrefix, Integer customFieldId);

    /**
     *
     * @param customFieldName
     * @return
     */
    List<CustomField> getAllExecutionCustomFieldValue(String customFieldName);

    /**
     *
     * @param id
     * @return
     */
    List<CustomField> getAllExecutionCustomFieldValueById(Long id);

    /**
     *
     * @return
     */
    Collection<CustomField> getAllExecutionCustomFieldValueByType();

    /**
     *
     * @param customFieldId
     * @param executionId
     * @return
     */
    ExecutionCf getCustomFieldForExecutionByCustomFieldIdAndExecutionId(Long customFieldId, Integer executionId);

    /**
     *
     * @param executionCf
     */
    void deleteExecutionCustomFieldValue(ExecutionCf executionCf);


    boolean getCustomFieldAndEntityRecord(Long customFieldId, Integer entityId, String entityType);
}
