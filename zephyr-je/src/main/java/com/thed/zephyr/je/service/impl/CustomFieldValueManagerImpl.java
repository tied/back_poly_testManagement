package com.thed.zephyr.je.service.impl;


import com.atlassian.activeobjects.external.ActiveObjects;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.util.ApplicationConstants;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CustomFieldValueManagerImpl implements CustomFieldValueManager {

    private static final Logger logger = LoggerFactory.getLogger(CustomFieldValueManagerImpl.class);

    private final ActiveObjects entityManager;

    public CustomFieldValueManagerImpl(final ActiveObjects entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public TestStepCf saveTestStepCustomFieldValue(Map<String, Object> customFieldValueProperties) {
        return entityManager.create(TestStepCf.class, customFieldValueProperties);
    }

    @Override
    public ExecutionCf saveExecutionCustomFieldValue(Map<String, Object> customFieldValueProperties) {
        return entityManager.create(ExecutionCf.class, customFieldValueProperties);
    }
    
    @Override
    public List<TestStepCf> getCustomFieldValuesForTeststep(Integer entityId) {
        Query query = Query.select();
        query.alias(TestStepCf.class, "testStep");
        query.alias(CustomField.class , "customField");
        query.join(CustomField.class , "customField.ID = testStep.CUSTOM_FIELD_ID");
        query.where("testStep.TEST_STEP_ID = ?",entityId);
        TestStepCf[] testStepCfs = entityManager.find(TestStepCf.class, query);

        if(Objects.nonNull(testStepCfs) && testStepCfs.length > 0) {
            return Arrays.asList(testStepCfs);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<ExecutionCf> getCustomFieldValuesForExecution(Integer entityId) {
        Query query = Query.select();
        query.alias(ExecutionCf.class, "execution");
        query.alias(CustomField.class , "customField");
        query.join(CustomField.class , "customField.ID = execution.CUSTOM_FIELD_ID");
        query.where("execution.EXECUTION_ID = ?",entityId);
        ExecutionCf[] executionCfs = entityManager.find(ExecutionCf.class, query);

        if(Objects.nonNull(executionCfs) && executionCfs.length > 0) {
            return Arrays.asList(executionCfs);
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public TestStepCf getTeststepCustomFieldValue(Long customFieldValueId) {

        TestStepCf[] testStepCfs = entityManager.find(TestStepCf.class, Query.select().where("ID = ?",customFieldValueId));

        if (Objects.isNull(testStepCfs) || testStepCfs.length == 0) {
            return null;
        }
        return testStepCfs[0];
    }

    @Override
    public void updateTestStepCustomFieldValue(TestStepCf testStepCf) {
        testStepCf.save();
    }

    @Override
    public ExecutionCf getExecutionCustomFieldValue(Long customFieldValueId) {
        ExecutionCf[] executionCfs = entityManager.find(ExecutionCf.class, Query.select().where("ID = ?",customFieldValueId));

        if (Objects.isNull(executionCfs) || executionCfs.length == 0) {
            return null;
        }
        return executionCfs[0];
    }

    @Override
    public void updateExecutionCustomFieldValue(ExecutionCf executionCf) {
        executionCf.save();
    }

    @Override
    public void deleteTestStepCustomFieldValues(Integer testStepId) {
        TestStepCf[] testStepCfs = entityManager.find(TestStepCf.class, Query.select().where("TEST_STEP_ID = ?", testStepId));

        if (Objects.nonNull(testStepCfs) || testStepCfs.length > 0) {
            entityManager.delete(testStepCfs);
        }
    }

    @Override
    public ExecutionCf[] getCustomFieldValuesForExecutionByCustomFieldId(Long customFieldId) {
        ExecutionCf[] executionCfs = entityManager.find(ExecutionCf.class, Query.select().where("CUSTOM_FIELD_ID = ?",customFieldId));
        return executionCfs;
    }

    @Override
    public void deleteExecutionCustomFieldValues(ExecutionCf[] executionCfs) {
        entityManager.delete(executionCfs);
    }

    @Override
    public TestStepCf[] getCustomFieldValuesForTestStepByCustomFieldId(Long customFieldId) {
        TestStepCf[] testStepCfs = entityManager.find(TestStepCf.class, Query.select().where("CUSTOM_FIELD_ID = ?",customFieldId));
        return testStepCfs;
    }

    @Override
    public void deleteTestStepCustomFieldValues(TestStepCf[] testStepCfs) {
        entityManager.delete(testStepCfs);
    }

    @Override
    public List<ExecutionCf> checkCustomFieldValueExistByTypeAndKey(String customFieldType, String customFieldValue, Integer customFieldId) {

        List<Object> params = new ArrayList<>();
        String whereClause = StringUtils.EMPTY;

        if (StringUtils.isNotBlank(customFieldType) && customFieldType.equalsIgnoreCase(ApplicationConstants.STRING_VALUE)) {
            String valuePrefix = (String) customFieldValue;

            whereClause += ApplicationConstants.STRING_VALUE + " LIKE ?";
            params.add("%" + valuePrefix + "%");
        } else if (StringUtils.isNotBlank(customFieldType) && StringUtils.isNotBlank(customFieldValue) &&
                customFieldType.equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)){
        	 Double dValue = Double.valueOf(customFieldValue);

             whereClause += ApplicationConstants.NUMBER_VALUE + " = ?";
             params.add(dValue);
        } else if (StringUtils.isNotBlank(customFieldType) && customFieldType.equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
            String valuePrefix = (String) customFieldValue;

            whereClause += ApplicationConstants.LARGE_VALUE + " LIKE ?";
            params.add("%" + valuePrefix + "%");
        } else {
        	return Lists.newArrayList();
        }

        if(Objects.nonNull(customFieldId)) {
            whereClause += " AND CUSTOM_FIELD_ID = ?";
            params.add(customFieldId);
        }

        Query query = Query.select();
        query.where(whereClause, params.toArray());
        query.setLimit(ApplicationConstants.MAX_LIMIT);

        ExecutionCf[] executionCfs = entityManager.find(ExecutionCf.class,query);

        if(Objects.nonNull(executionCfs) && executionCfs.length > 0) {
            return Arrays.asList(executionCfs);
        }

        return Lists.newArrayList();
    }

    @Override
    public ExecutionCf saveOrUpdateExecutionCustomFieldValue(Map<String, Object> customFieldValueProperties) {
        Query query = Query.select().where("EXECUTION_ID = ? AND CUSTOM_FIELD_ID = ?", customFieldValueProperties.get("EXECUTION_ID"),
                customFieldValueProperties.get("CUSTOM_FIELD_ID"));
        ExecutionCf[] executionCfs = entityManager.find(ExecutionCf.class, query);
        if (executionCfs.length > 0) {
            entityManager.delete(executionCfs[0]);
        }
        return entityManager.create(ExecutionCf.class, customFieldValueProperties);
    }

    @Override
    public List<ExecutionCf> getCustomFieldValueByType(String customFieldType, String valuePrefix, Integer customFieldId) {

        Query query = Query.select();
        String whereClause = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(customFieldType) &&
                customFieldType.equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)){
            whereClause += "NUMBER_VALUE IS NOT NULL" ;
        }

        if(Objects.nonNull(customFieldId)) {
            whereClause += " AND CUSTOM_FIELD_ID = "+customFieldId;
        }

        query.where(whereClause);
        query.setLimit(ApplicationConstants.MAX_LIMIT);

        ExecutionCf[] executionCfs = entityManager.find(ExecutionCf.class,query);

        if(Objects.nonNull(executionCfs) && executionCfs.length > 0) {
            return Arrays.asList(executionCfs);
        }

        return Lists.newArrayList();
    }


    @Override
    public List<CustomField> getAllExecutionCustomFieldValue(String customFieldName) {
        CustomField[] customFields = entityManager.find(CustomField.class, Query.select().where("ZFJENTITY_TYPE='EXECUTION' AND NAME = ?",customFieldName ));

        if (Objects.isNull(customFields) || customFields.length == 0) {
            return null;
        }
        return Arrays.asList(customFields);
    }

    @Override
    public List<CustomField> getAllExecutionCustomFieldValueById(Long id) {
        CustomField[] customFields = entityManager.find(CustomField.class, Query.select().where("ZFJENTITY_TYPE='EXECUTION' AND id = ?",id ));

        if (Objects.isNull(customFields) || customFields.length == 0) {
            return null;
        }
        return Arrays.asList(customFields);
    }


    @Override
    public List<CustomField> getAllExecutionCustomFieldValueByType() {
        CustomField[] customFields = entityManager.find(CustomField.class, Query.select().where("ZFJENTITY_TYPE='EXECUTION'"));

        if (Objects.isNull(customFields) || customFields.length == 0) {
            return null;
        }
        return Arrays.asList(customFields);
    }

    @Override
    public ExecutionCf getCustomFieldForExecutionByCustomFieldIdAndExecutionId(Long customFieldId, Integer executionId) {
        Query query = Query.select().where("EXECUTION_ID = ? AND CUSTOM_FIELD_ID = ?", executionId, customFieldId);
        ExecutionCf[] executionCfs = entityManager.find(ExecutionCf.class, query);
        if (executionCfs.length > 0) {
            return executionCfs[0];
        }
        return null;
    }

    @Override
    public void deleteExecutionCustomFieldValue(ExecutionCf executionCf) {
        if(null != executionCf) {
            entityManager.delete(executionCf);
        }
    }

    @Override
    public boolean getCustomFieldAndEntityRecord(Long customFieldId, Integer entityId, String entityType) {

        if(entityType.equalsIgnoreCase(ApplicationConstants.ENTITY_TYPE.TESTSTEP.name())) {
            TestStepCf[] testStepCfs = entityManager.find(TestStepCf.class, Query.select().where("TEST_STEP_ID = ? AND CUSTOM_FIELD_ID = ? ", entityId,customFieldId));
            return (Objects.nonNull(testStepCfs) && testStepCfs.length > 0);
        }else if(entityType.equalsIgnoreCase(ApplicationConstants.ENTITY_TYPE.EXECUTION.name())) {
            ExecutionCf[] executionCfs = entityManager.find(ExecutionCf.class, Query.select().where("EXECUTION_ID = ? AND CUSTOM_FIELD_ID = ? ", entityId,customFieldId));
            return (Objects.nonNull(executionCfs) && executionCfs.length > 0);
        }

        return Boolean.FALSE;
    }
}
