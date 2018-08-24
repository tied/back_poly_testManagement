package com.thed.zephyr.je.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.database.DatabaseConfig;
import com.atlassian.jira.config.database.DatabaseConfigurationManager;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.CustomFieldOption;
import com.thed.zephyr.je.model.CustomFieldProject;
import com.thed.zephyr.je.model.CustomFieldsMeta;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.util.ApplicationConstants;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ZephyrCustomFieldManagerImpl implements ZephyrCustomFieldManager {

	private final ActiveObjects activeObjects;

	public ZephyrCustomFieldManagerImpl(ActiveObjects activeObjects) {
		this.activeObjects = activeObjects;
	}

	@Override
	public CustomField saveCustomField(Map<String, Object> customProperties) {
		return activeObjects.create(CustomField.class, customProperties);
	}

	@Override
	public void updateCustomField(CustomField updatedCustomField) {
		updatedCustomField.save();
	}


	@Override
	public int deleteCustomField(Long fieldId) {
		CustomField customField = getCustomFieldById(fieldId);
		if (customField == null) {
			return 0;
		}
		activeObjects.delete(customField);
		return 1;
	}

	@Override
	public CustomField getCustomFieldById(Long customFieldId) {
		CustomField customField = null;
		if (customFieldId != null) {
			customField = activeObjects.get(CustomField.class, customFieldId.intValue());
		}
		return customField;
	}

	@Override
	public CustomField[] getCustomFieldsByProjectId(Long projectId) {
		CustomField[] customFields;
		if (null != projectId) {
            customFields = activeObjects.find(CustomField.class,
					Query.select().where("PROJECT_ID = ?", projectId));
		}else {
		    customFields = activeObjects.find(CustomField.class, Query.select().where("PROJECT_ID IS NULL"));
        }

		return customFields;

	}

	@Override
	public CustomField[] getCustomFieldsByEntityType(String entityType) {
		if (StringUtils.isNotBlank(entityType)) {
			CustomField[] customFields = activeObjects.find(CustomField.class,
					Query.select().where("ZFJENTITY_TYPE = ? AND PROJECT_ID IS NULL", entityType));
			return customFields;
		}
		return null;
	}

	@Override
	public List<CustomField> getAllCustomFieldsByEntityTypeForProject(Long projectId, String entityType) {
		if (StringUtils.isNotBlank(entityType)) {
			CustomField[] customFields = activeObjects.find(CustomField.class,
					Query.select().where("ZFJENTITY_TYPE = ? AND (PROJECT_ID = ? OR PROJECT_ID IS NULL)", entityType,projectId));
			return Arrays.asList(customFields);
		}
		return null;
	}

	@Override
	public CustomFieldOption getAllExecutionCustomFieldValue(String clauseName, String value) {
		Query query = Query.select();
		query.alias(CustomField.class, "customField");
		query.alias(CustomFieldOption.class, "customFieldOption");
		query = query.join(CustomField.class,
				"customFieldOption.CUSTOM_FIELD_ID=customField.ID");
		query.where("customField.NAME = ? AND customFieldOption.OPTION_VALUE = ?",clauseName,value);
		CustomFieldOption[] customFieldOptions = activeObjects.find(CustomFieldOption.class, query);
		if(customFieldOptions != null && customFieldOptions.length > 0) {
			return customFieldOptions[0];
		}
		return null;
	}

    @Override
	public CustomFieldOption createCustomFieldOption(Map<String, Object> fieldOptionProperties) {
		return activeObjects.create(CustomFieldOption.class, fieldOptionProperties);
	}

	@Override
	public CustomFieldsMeta[] getCustomFieldMeta() {
		return activeObjects.find(CustomFieldsMeta.class);
	}

	@Override
	public CustomFieldOption[] getCustomFieldOptions(Integer id) {
		return activeObjects.find(CustomFieldOption.class,Query.select().where("CUSTOM_FIELD_ID = ?", id));
	}

	@Override
	public CustomFieldOption getCustomFieldOptionById(Integer customFieldOptionId) {
		CustomFieldOption[] customFieldOptions = activeObjects.find(CustomFieldOption.class,Query.select().where("ID = ?",
				customFieldOptionId));
		if(customFieldOptions!= null && customFieldOptions.length > 0) {
		    return customFieldOptions[0];
        }
        return null;
	}

    @Override
    public void updateCustomFieldOption(CustomFieldOption existingCustomFieldOption) {
        existingCustomFieldOption.save();
    }

    @Override
    public void deleteCustomFieldOption(CustomFieldOption customFieldOption) {
        activeObjects.delete(customFieldOption);
    }

    @Override
    public void deleteCustomFieldOptions(CustomFieldOption[] customFieldOptions) {
        activeObjects.delete(customFieldOptions);
    }

	@Override
	public List<CustomField> getCustomFieldsByEntityTypeAndProject(String entityType, Long projectId) {
		CustomField[] customFields = activeObjects.find(CustomField.class,
				Query.select().where("ZFJENTITY_TYPE = ? AND IS_ACTIVE = ?", entityType, Boolean.TRUE));
		if(customFields != null) {
			List<CustomField> finalList = new ArrayList<>();
			CustomFieldProject[] activeCustomFields = getActiveCustomFieldProjectByProjectId(projectId);
			List<Integer> activeCustomFieldList = Arrays.stream(activeCustomFields).map(CustomFieldProject::getCustomFieldId).collect(Collectors.toList());
			Arrays.asList(customFields).stream().forEach(customField -> {
				if(activeCustomFieldList.contains(customField.getID())) {
					finalList.add(customField);
				}
			});
			return finalList;
		}
		return null;
	}


	@Override
	public CustomField getCustomFieldByName(String customFieldName) {
		CustomField[] customFields = activeObjects.find(CustomField.class,
				Query.select().where("ZFJENTITY_TYPE = ? AND NAME = ?", ApplicationConstants.ENTITY_TYPE.EXECUTION.name(), customFieldName));

		if(Objects.nonNull(customFields) && customFields.length > 0) {
		    return customFields[0];
        }
		return null;
	}

	@Override
	public CustomField[] getAllCustomFieldByName(String customFieldName) {
		CustomField[] customFields = activeObjects.find(CustomField.class,
				Query.select().where("ZFJENTITY_TYPE = ? AND NAME = ?", ApplicationConstants.ENTITY_TYPE.EXECUTION.name(), customFieldName));

		if(Objects.nonNull(customFields) && customFields.length > 0) {
			return customFields;
		}
		return null;
	}

	@Override
	public Integer getCustomFieldCount(String entityType) {
            return activeObjects.count(CustomField.class,
				Query.select().where("ZFJENTITY_TYPE = ?", entityType));
	}


    @Override
    public CustomField[] getAllCustomFieldsByEntityType(String entityType) {
		CustomField[] customFields = activeObjects.find(CustomField.class,
				Query.select().where("ZFJENTITY_TYPE = ?", entityType));

		CustomFieldProject[] allActiveCustomFieldProjects = getAllActiveCustomFieldsProject();
		if(allActiveCustomFieldProjects != null && allActiveCustomFieldProjects.length > 0) {
			Set<Integer> allActiveCustomFields = Arrays.asList(allActiveCustomFieldProjects).stream().map(CustomFieldProject::getCustomFieldId).collect(Collectors.toSet());
			List<CustomField> finalCustomFieldList = new ArrayList<>();
			if(Objects.nonNull(customFields)){
				Arrays.stream(customFields).forEach(customField -> {
					if(allActiveCustomFields.contains(customField.getID())) {
						finalCustomFieldList.add(customField);
					}
				});
				return finalCustomFieldList.toArray(new CustomField[finalCustomFieldList.size()]);
			}
		}
		return null;
    }

    @Override
    public boolean checkCustomFieldNameUniqueness(String entityType, Long projectId, String customFieldName) {
        DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();

        if(StringUtils.isNotBlank(customFieldName)) {
        	customFieldName = StringEscapeUtils.escapeSql(customFieldName);
		}
        Query query = Query.select();
        if(Objects.nonNull(projectId)) {
            if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres")){
                query.where("ZFJENTITY_TYPE = ? AND PROJECT_ID = ? AND lower(" +  "\"NAME\"" +  ") = lower(" + "\'" +customFieldName+ "\'" + ")",entityType, projectId);
            }else {
                query.where("ZFJENTITY_TYPE = ? AND PROJECT_ID = ? AND lower(NAME) = lower(" + "\'"+customFieldName+"\'" + ")", entityType,projectId);
            }
	        if(activeObjects.count(CustomField.class,query) > 0) {
	            return Boolean.FALSE;
            }else {
	            return Boolean.TRUE;
            }
        }else {
            if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres")){
                query.where("ZFJENTITY_TYPE = ? AND PROJECT_ID IS NULL AND lower(" +  "\"NAME\"" +  ") = lower(" + "\'"+customFieldName+"\'" + ")", entityType);
            }else {
                query.where("ZFJENTITY_TYPE = ? AND PROJECT_ID IS NULL AND lower(NAME) = lower(" + "\'"+customFieldName+"\'" + ")", entityType);
            }
            if(activeObjects.count(CustomField.class,query) > 0) {
                return Boolean.FALSE;
            }else {
                return Boolean.TRUE;
            }
        }
    }

	@Override
	public CustomField getCustomFieldByProjectIdAndCustomFieldName(Long projectId, String customFieldName, String entityType) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();

		Query query = Query.select();
		if(Objects.nonNull(projectId)) {
			if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres")){
				query.where("ZFJENTITY_TYPE = ? AND PROJECT_ID = ? AND lower(" +  "\"NAME\"" +  ") = lower(" + "\'" +customFieldName+ "\'" + ")",entityType, projectId);
			}else {
				query.where("ZFJENTITY_TYPE = ? AND PROJECT_ID = ? AND lower(NAME) = lower(" + "\'"+customFieldName+"\'" + ")", entityType,projectId);
			}
			CustomField[] customFields = activeObjects.find(CustomField.class,query);
			if (Objects.nonNull(customFields) && customFields.length > 0) {
				return customFields[0];
			}
		}
		return null;
	}

	/**
	 * This method will return the global custom fields by eliminating the disabled custom fields for the project.
	 * @param entityType
	 * @param projectId
	 * @return
	 */
	@Override
	public CustomField[] getCustomFieldsByEntityType(String entityType, Long projectId, Boolean isGlobal) {
        if (StringUtils.isNotBlank(entityType)) {
            CustomField[] customFields = activeObjects.find(CustomField.class,
                    Query.select().where("ZFJENTITY_TYPE = ? AND PROJECT_ID IS NULL", entityType));

            if(null != customFields && customFields.length > 0) {
				if(Objects.nonNull(projectId)) {
					CustomFieldProject[] activeCustomFields = null;
					if (Objects.nonNull(projectId)) {
						activeCustomFields = activeObjects.find(CustomFieldProject.class,
								Query.select().where("PROJECT_ID = ? AND IS_ACTIVE = ?", projectId, Boolean.TRUE));
					} else {
						activeCustomFields = activeObjects.find(CustomFieldProject.class,
								Query.select().where("IS_ACTIVE = ?", Boolean.TRUE));
					}

					if (null != activeCustomFields && activeCustomFields.length > 0) {
                    List<CustomField> customFieldList = Arrays.asList(customFields);
						List<CustomFieldProject> activeCustomFieldList = Arrays.asList(activeCustomFields);

						Set<Integer> activeCustomFieldIds = activeCustomFieldList.stream().map(CustomFieldProject::getCustomFieldId).collect(Collectors.toSet());
						List<CustomField> finalList = customFieldList.stream().filter(customField -> activeCustomFieldIds.contains(customField.getID())).collect(Collectors.toList());

                    if(CollectionUtils.isNotEmpty(finalList)) {
                        CustomField[] customFieldResponse = new CustomField[finalList.size()];
                        finalList.toArray(customFieldResponse);
                        return customFieldResponse;
                    }else {
                    	return null;
					}
                }
				} else if (Objects.nonNull(isGlobal) || isGlobal == Boolean.TRUE){
            return customFields;
				} else {
					return retrieveActiveCustomFieldProject(null,customFields);
				}
            }
        }
        return null;
	}


	@Override
	public boolean getCustomFieldByFilter(String entityType, String name, String customFieldName) {
		DatabaseConfig dbConfig = ComponentAccessor.getComponent(DatabaseConfigurationManager.class).getDatabaseConfiguration();

		Query query = Query.select();
		if(StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres")){
			query.where("ZFJENTITY_TYPE = ? AND lower(" +  "\"NAME\"" +  ") = lower(" + "\'"+customFieldName+"\'" + ")",entityType);
		}else {
			query.where("ZFJENTITY_TYPE = ? AND lower(NAME) = lower(" + "\'"+customFieldName+"\'" + ")",entityType);
		}
		if(activeObjects.count(CustomField.class,query) > 0) {
			return Boolean.FALSE;
		}else {
			return Boolean.TRUE;
		}
	}

    @Override
    public CustomField getDisabledCustomField(String customFieldName, String zfjEntityType, Long projectId) {
        customFieldName = StringEscapeUtils.escapeSql(customFieldName);

        CustomField[] disabledCustomFields = activeObjects.find(CustomField.class,
                Query.select().where("NAME = ? AND ZFJENTITY_TYPE = ? AND PROJECT_ID = ? AND IS_ACTIVE = ?",customFieldName,zfjEntityType,projectId,Boolean.FALSE));

        if(Objects.nonNull(disabledCustomFields) && disabledCustomFields.length > 0) {
            return disabledCustomFields[0];
        }
        return null;
    }

    @Override
    public void deleteCustomField(CustomField customField) {
        activeObjects.delete(customField);
    }

    @Override
    public CustomField[] getGlobalCustomFieldsByEntityTypeAndProjectId(String entityType, Long projectId) {
        if (StringUtils.isNotBlank(entityType)) {
            CustomField[] customFields = activeObjects.find(CustomField.class,
                    Query.select().where("ZFJENTITY_TYPE = ? AND PROJECT_ID IS NULL", entityType));


            if(Objects.nonNull(projectId) && null != customFields && customFields.length > 0) {
				CustomField[] customFieldResponse = retrieveActiveCustomFieldProject(projectId, customFields);
                        return customFieldResponse;
                    }
            return customFields;
        }
        return null;
    }

    @Override
    public CustomFieldProject[] getActiveCustomFieldsProjectByCustomFieldId(Integer customFieldId) {
		CustomFieldProject[] disabledCustomFields = activeObjects.find(CustomFieldProject.class,
                Query.select().where("CUSTOM_FIELD_ID = ? AND IS_ACTIVE = ? AND PROJECT_ID IS NOT NULL",customFieldId,Boolean.TRUE));

        if(Objects.nonNull(disabledCustomFields) && disabledCustomFields.length > 0) {
            return disabledCustomFields;
        }
        return null;
    }


	@Override
	public CustomFieldProject[] getAllActiveCustomFieldsProject() {
		CustomFieldProject[] activeCustomFields = activeObjects.find(CustomFieldProject.class,
				Query.select().where("IS_ACTIVE = ? ",Boolean.TRUE));

		if(Objects.nonNull(activeCustomFields) && activeCustomFields.length > 0) {
			return activeCustomFields;
		}
		return null;
	}

	@Override
	public CustomField[] getAllDisabledCustomFields() {
		CustomField[] disabledCustomFields = activeObjects.find(CustomField.class,
				Query.select().where("ZFJENTITY_TYPE = ? AND IS_ACTIVE = ?", ApplicationConstants.ENTITY_TYPE.EXECUTION.name() ,Boolean.FALSE));

		if(Objects.nonNull(disabledCustomFields) && disabledCustomFields.length > 0) {
			return disabledCustomFields;
		}
		return null;
	}

    @Override
    public void deleteCustomFields(CustomField[] customFields) {
        activeObjects.delete(customFields);
    }

    @Override
	public void deleteCustomFieldProject(Long projectId, Integer customFieldId) {
		CustomFieldProject[] customFieldProjects = activeObjects.find(CustomFieldProject.class,
				Query.select().where("PROJECT_ID = ? AND CUSTOM_FIELD_ID = ?", projectId, customFieldId));
		activeObjects.delete(customFieldProjects);
	}

	@Override
	public CustomFieldProject createCustomFieldProject(Map<String,Object> customFieldProjectProperties) {
		return activeObjects.create(CustomFieldProject.class,customFieldProjectProperties);
	}

	@Override
	public CustomFieldProject[] getDisabledCustomFieldProjectByProjectAndCustomField(Integer customFieldId, Long projectId) {
		CustomFieldProject[] customFieldProjects = activeObjects.find(CustomFieldProject.class,
				Query.select().where("PROJECT_ID = ? AND CUSTOM_FIELD_ID = ? AND IS_ACTIVE = ?", projectId, customFieldId,Boolean.TRUE));
		return customFieldProjects;
	}

	@Override
	public void deleteCustomFieldProjectMapping(CustomFieldProject[] disableCustomFieldProjectMappings) {
		activeObjects.delete(disableCustomFieldProjectMappings);
	}

	@Override
	public CustomFieldProject[] getActiveCustomFieldProjectByName(String customFieldName) {
		Query query = Query.select();
		query.alias(CustomField.class, "customField");
		query.alias(CustomFieldProject.class , "customFieldProject");
		query.join(CustomField.class , "customFieldProject.CUSTOM_FIELD_ID = customField.ID");
		query.where("customField.NAME = ? AND customFieldProject.IS_ACTIVE = ?",customFieldName, Boolean.TRUE);
		CustomFieldProject[] customFieldProjects = activeObjects.find(CustomFieldProject.class, query);
		if(Objects.nonNull(customFieldProjects) && customFieldProjects.length > 0) {
		return customFieldProjects;
	}
		return null;
	}


	@Override
	public CustomFieldProject getCustomFieldProjectByCustomFieldAndProjectId(Integer customFieldId, Long projectId) {
		CustomFieldProject[] customFieldProjects = activeObjects.find(CustomFieldProject.class, Query.select().where("CUSTOM_FIELD_ID = ? AND PROJECT_ID = ? ",customFieldId, projectId));
		if(customFieldProjects != null && customFieldProjects.length > 0) {
			return customFieldProjects[0];
		}
		return null;
	}


	@Override
	public CustomFieldProject[] getActiveCustomFieldProjects(Long customFieldId) {
		CustomFieldProject[] customFieldProjects = activeObjects.find(CustomFieldProject.class, Query.select().where("CUSTOM_FIELD_ID = ? AND IS_ACTIVE = ? ",customFieldId.intValue(),Boolean.TRUE));
		return customFieldProjects;
	}


	@Override
	public CustomFieldProject[] getActiveCustomFieldProjectsByEntity(String entityType) {
		Query query = Query.select();
		query.alias(CustomField.class, "customField");
		query.alias(CustomFieldProject.class , "customFieldProject");
		query.join(CustomField.class , "customFieldProject.CUSTOM_FIELD_ID = customField.ID");
		query.where("customFieldProject.IS_ACTIVE = ? AND customField.ZFJENTITY_TYPE = ?",Boolean.TRUE,entityType);
		CustomFieldProject[] customFieldProjects = activeObjects.find(CustomFieldProject.class, query);
		if(Objects.nonNull(customFieldProjects) && customFieldProjects.length > 0) {
			return customFieldProjects;
		}
		return null;
	}


	private CustomFieldProject[] getActiveCustomFieldProjectByProjectId(Long projectId) {
		CustomFieldProject[] customFieldProjects = activeObjects.find(CustomFieldProject.class, Query.select().where("PROJECT_ID = ? ",projectId));
		return customFieldProjects;
	}


	private CustomField[] retrieveActiveCustomFieldProject(Long projectId, CustomField[] customFields) {
		CustomFieldProject[] activeCustomFields = null;

		if(Objects.nonNull(projectId)) {
			activeCustomFields = activeObjects.find(CustomFieldProject.class,
					Query.select().where("PROJECT_ID = ? AND IS_ACTIVE = ?", projectId, Boolean.TRUE));
		} else {
			activeCustomFields = activeObjects.find(CustomFieldProject.class,
					Query.select().where("IS_ACTIVE = ?",  Boolean.TRUE));
		}

		List<CustomField> customFieldList = Arrays.asList(customFields);
		List<CustomField> finalList = Lists.newArrayList();
		Set<Integer> activeCustomFieldIds = Arrays.asList(activeCustomFields).stream().map(CustomFieldProject::getCustomFieldId).collect(Collectors.toSet());

		customFieldList.stream().forEach(customField -> {
			if(activeCustomFieldIds.contains(customField.getID())) {
				customField.setIsActive(Boolean.TRUE);
			} else {
				customField.setIsActive(Boolean.FALSE);
			}
			finalList.add(customField);
		});

		if(CollectionUtils.isNotEmpty(finalList)) {
			CustomField[] filteredCustomFields = finalList.toArray(new CustomField[finalList.size()]);
			return filteredCustomFields;
		}
		return null;
	}
}
