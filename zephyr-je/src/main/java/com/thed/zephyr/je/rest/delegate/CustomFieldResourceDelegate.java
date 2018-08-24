package com.thed.zephyr.je.rest.delegate;

import javax.ws.rs.core.Response;

import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.CustomFieldOption;
import com.thed.zephyr.je.model.CustomFieldsMeta;
import com.thed.zephyr.je.rest.CustomFieldResource.CustomFieldRequest;
import com.thed.zephyr.je.rest.CustomFieldResource.CustomFieldResponse;

import java.util.List;
import java.util.Map;

/**
 * CustomField resource delegate, which serves for actual CustomField Rest along
 * 
 * @author santosh
 *
 */
public interface CustomFieldResourceDelegate {

	/**
	 * Method invokes the CustomField manager api to create CustomFields
	 * 
	 * @param customFieldRequest
	 *            -- Holds the user posted customField creation request data.
	 * @param projectId
	 * @return -- Returns the response object with CustomField id and response
	 *         message.
	 */

	CustomFieldResponse createCustomField(CustomFieldRequest customFieldRequest, Long projectId);

	CustomFieldResponse updateCustomField(Long customFieldId, CustomFieldRequest customFieldUpdateRequest);

	CustomField[] getCustomFields(Long projectId);

	CustomField getCustomFieldById(Long customFieldId);

	CustomField[] getCustomFieldsByEntityType(String entityType);

	CustomFieldOption createCustomFieldOption(Integer customFieldId, Map<String, String> params);

	CustomFieldOption updateCustomFieldOption(Integer customFieldOptionId, Map<String, String> params);

	CustomFieldsMeta[] getCustomFieldsMeta();

	CustomFieldOption[] getCustomFieldOptions(int id);

	CustomFieldOption getCustomFieldOptionById(Integer customFieldOptionId);

	void deleteCustomFieldOption(Integer customFieldOptionId);

    void deleteCustomField(Long customFieldId);

	CustomField[] getCustomFieldsByEntityType(String entityType,Long projectId, Boolean isGlobal);


	List<CustomField> getCustomFieldsByEntityTypeAndProject(String entityType, Long projectId);

    Integer getCustomFieldCount(String entityType);

	boolean checkCustomFieldNameUniqueness(String entityType, Long projectId, String customFieldName);

    void enableOrDisableCustomFieldForProject(Long projectId, Boolean enable, CustomField customField);

	CustomField[] getGlobalCustomFieldsByEntityTypeAndProjectId(String entityType, Long projectId);

    boolean getDisableCustomFieldForProjectAndCustomField(Long customFieldId, Long projectId);
}
