package com.thed.zephyr.je.rest.delegate;

import com.atlassian.jira.issue.Issue;
import com.google.common.collect.Table;
import com.thed.zephyr.je.model.ExecutionCf;
import com.thed.zephyr.je.model.TestStepCf;
import com.thed.zephyr.je.rest.CustomFieldValueResource;
import com.thed.zephyr.je.rest.CustomFieldValueResource.CustomFieldValueRequest;
import com.thed.zephyr.je.rest.CustomFieldValueResource.CustomFieldValueResponse;

import java.util.Map;

/**
 * Custom field value resource delegator which will perform the business logic.
 */
public interface CustomFieldValueResourceDelegate {

    /**
     * Create custom field value record in database as per entity type.
     * @param customFieldValueRequest
     * @param entityType
     * @return
     */
    CustomFieldValueResponse createCustomFieldValue(CustomFieldValueRequest customFieldValueRequest, String entityType);

    /**
     * Get custom field value data for the given entity id.
     * @param entityId
     * @param entityType
     * @param issue
     * @return
     */
    Map<String, CustomFieldValueResource.CustomFieldValueResponse> getCustomFieldValuesByEntityId(Integer entityId, String entityType, Issue issue);

    /**
     * Update custom field value for the given custom value id by entity type.
     * @param customFieldValueRequest
     * @param entityType
     * @param customFieldValueId
     * @return
     */
    CustomFieldValueResponse updateCustomFieldValue(CustomFieldValueRequest customFieldValueRequest, String entityType, Long customFieldValueId);

    /**
     *
     * @param customFieldValueRequests
     * @param entityType
     * @param entityId
     * @return
     */
    Map<String, CustomFieldValueResponse> createCustomFieldValues(Map<String, CustomFieldValueRequest> customFieldValueRequests, String entityType, Integer entityId);

    /**
     *
     * @param customFieldValueRequests
     * @param entityType
     * @param entityId
     * @return
     */
    Map<String, CustomFieldValueResponse> updateCustomFieldValues(Map<String, CustomFieldValueRequest> customFieldValueRequests, String entityType, Integer entityId);

    /**
     *
     * @param testStepId
     */
    void deleteCustomFieldValuesForTeststep(Integer testStepId);

    /**
     *
     * @param customFieldValueProperties
     * @param customFieldName
     * @return
     */
    Table<String,String,Object> createOrUpdateCustomFieldRecordForBulkAssign(Map<String, Object> customFieldValueProperties, String customFieldName);

    /**
     *
     * @param customFieldValueId
     * @return
     */
    ExecutionCf getExecutionCustomFieldValue(Long customFieldValueId);

    /**
     *
     * @param customFieldValueId
     * @return
     */
    TestStepCf getTeststepCustomFieldValue(Long customFieldValueId);

    /**
     *
     * @param customFieldValueRequest
     * @return
     */
    boolean getCustomFieldAndEntityRecord(CustomFieldValueRequest customFieldValueRequest);
}
