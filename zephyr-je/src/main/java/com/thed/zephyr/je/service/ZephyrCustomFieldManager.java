package com.thed.zephyr.je.service;

import java.util.List;
import java.util.Map;

import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.CustomFieldOption;
import com.thed.zephyr.je.model.CustomFieldProject;
import com.thed.zephyr.je.model.CustomFieldsMeta;

/**
 * This class acts as a service layer which interacts with active objects.
 * 
 * @author santosh
 *
 */
public interface ZephyrCustomFieldManager {

	CustomField saveCustomField(Map<String, Object> customProperties);

	int deleteCustomField(Long fieldId);

	void updateCustomField(CustomField updatedCustomField);

	CustomField getCustomFieldById(Long customFieldId);


	CustomField[] getCustomFieldsByProjectId(Long projectId);

	CustomField[] getCustomFieldsByEntityType(String entityType);

	CustomFieldOption createCustomFieldOption(Map<String, Object> fieldOptionProperties);

	CustomFieldsMeta[] getCustomFieldMeta();

	CustomFieldOption[] getCustomFieldOptions(Integer id);

	CustomFieldOption getCustomFieldOptionById(Integer customFieldOptionId);

	void updateCustomFieldOption(CustomFieldOption existingCustomFieldOption);

	void deleteCustomFieldOption(CustomFieldOption customFieldOption);

	void deleteCustomFieldOptions(CustomFieldOption[] customFieldOptions);

	List<CustomField> getCustomFieldsByEntityTypeAndProject(String entityType, Long projectId);

	CustomField getCustomFieldByName(String customFieldName);

	CustomField[] getAllCustomFieldByName(String customFieldName);

    Integer getCustomFieldCount(String entityType);

	CustomField[] getAllCustomFieldsByEntityType(String entityType);

    boolean checkCustomFieldNameUniqueness(String entityType, Long projectId, String customFieldName);

	boolean getCustomFieldByFilter(String entityType, String name, String stringValue);

    CustomField getCustomFieldByProjectIdAndCustomFieldName(Long projectId, String customFieldName, String entityType);

	CustomField[] getCustomFieldsByEntityType(String entityType, Long projectId, Boolean isGlobal);

	List<CustomField> getAllCustomFieldsByEntityTypeForProject(Long projectId, String entityType);

	CustomFieldOption getAllExecutionCustomFieldValue(String clauseName, String value);

    CustomField getDisabledCustomField(String customFieldName, String zfjEntityType, Long projectId);

    void deleteCustomField(CustomField customField);

    CustomField[] getGlobalCustomFieldsByEntityTypeAndProjectId(String entityType, Long projectId);

	CustomFieldProject[] getActiveCustomFieldsProjectByCustomFieldId(Integer customFieldId);

	CustomFieldProject[] getAllActiveCustomFieldsProject();

	void deleteCustomFields(CustomField[] customFields);

	CustomField[] getAllDisabledCustomFields();

	CustomFieldProject createCustomFieldProject(Map<String, Object> customFieldProject);

	void deleteCustomFieldProject(Long projectId, Integer customFieldId);

	CustomFieldProject[] getDisabledCustomFieldProjectByProjectAndCustomField(Integer customFieldId, Long projectId);

	void deleteCustomFieldProjectMapping(CustomFieldProject[] disableCustomFieldProjectMappings);

	CustomFieldProject[] getActiveCustomFieldProjectByName(String customFieldName);

	CustomFieldProject getCustomFieldProjectByCustomFieldAndProjectId(Integer customFieldId, Long projectId);

	CustomFieldProject[] getActiveCustomFieldProjects(Long customFieldId);

    CustomFieldProject[] getActiveCustomFieldProjectsByEntity(String entityType);
}
