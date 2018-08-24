package com.thed.zephyr.je.zql.core;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.statistics.SelectStatisticsMapper;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.statistics.*;
import com.atlassian.jira.project.version.VersionManager;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.zql.core.mapper.*;
import com.thed.zephyr.je.zql.core.mapper.FixForVersionStatisticsMapper;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SorterFactory {
	String[] stringValues = new String[]{SystemSearchConstant.forExecutedBy().getFieldId(),SystemSearchConstant.forAssignee().getFieldId(),
			SystemSearchConstant.forLinkedDefectKey().getFieldId()};
	String[] numericValues = new String[]{SystemSearchConstant.forSchedule().getFieldId(),
			SystemSearchConstant.forIssue().getFieldId(),SystemSearchConstant.forStatus().getFieldId()};
	String[] dateValues = new String[]{SystemSearchConstant.forExecutionDate().getFieldId(),SystemSearchConstant.forDateCreated().getFieldId()};
	
	public SorterFactory() {
	}
	
	public LuceneFieldSorter<?> getFieldSorter(String documentConstant) {
		ZephyrCustomFieldManager zephyrCustomFieldManager = (ZephyrCustomFieldManager)ZephyrComponentAccessor.getInstance().getComponent("zephyrcf-manager");
		List<String> stringFields = Lists.newArrayList(stringValues);
		List<String> numericFields = Lists.newArrayList(numericValues);
		List<String> dateFields = Lists.newArrayList(dateValues);

		if (Pattern.matches("[1-9][0-9]*", documentConstant)) {
			CustomField customField = zephyrCustomFieldManager.getCustomFieldById(Long.valueOf(documentConstant));
			if(customField != null) {
				if(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.STRING_VALUE) ||
						ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
					return new TextFieldSorter(DocumentConstants.LUCENE_SORTFIELD_PREFIX +String.valueOf(customField.getID()));
				} else if(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
					numericFields.add(String.valueOf(customField.getID()));
				} else if(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_VALUE) ||
						ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
					return new DateFieldSorter(DocumentConstants.LUCENE_SORTFIELD_PREFIX + String.valueOf(customField.getID()));
				} else if(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
					return new CustomFieldListStatisticsMapper(String.valueOf(customField.getID()));
				}
			}
		}

		if(stringFields.contains(documentConstant)) {
			return new TextFieldSorter(documentConstant);
		} else if(numericFields.contains(documentConstant)) {
			if(documentConstant == "schedule_id") {
				documentConstant = "ORDER_ID";
			}
			return new NumericFieldStatisticsMapper(documentConstant);			
		} else if(dateFields.contains(documentConstant)) {
			return new DateFieldSorter(documentConstant);			
		} else if(StringUtils.equalsIgnoreCase(documentConstant,"COMPONENT_ID")) {
			return new ComponentStatisticsMapper();	
		} else if(StringUtils.equalsIgnoreCase(documentConstant,"PRIORITY_ID")) {
			return new ReversePriorityStatisticsMapper(ComponentAccessor.getConstantsManager());	
		} else if(StringUtils.equalsIgnoreCase(documentConstant,"PROJECT_ID")) {
			return new ProjectStatisticsMapper(ComponentAccessor.getProjectManager());	
		} else if(StringUtils.equalsIgnoreCase(documentConstant,"cycle")) {
			return new CycleStatisticsMapper("sort_cycle");	
		} else if(StringUtils.equalsIgnoreCase(documentConstant,"summary")) {
			return new TextFieldSorter("sort_summary");	
		} else if(StringUtils.equalsIgnoreCase(documentConstant,"VERSION_ID")) {
			VersionManager versionManager = (VersionManager)ComponentAccessor.getVersionManager();
			return new FixForVersionStatisticsMapper(versionManager);	
		} else if(StringUtils.equalsIgnoreCase(documentConstant,"folder")) {
			return new FolderStatisticsMapper("sort_folder");
		} else if(StringUtils.equalsIgnoreCase(documentConstant,"ESTIMATED_TIME")) {
			return new LongStatisticsMapper("sort_estimatedtime");
		} else if(StringUtils.equalsIgnoreCase(documentConstant,"LOGGED_TIME")) {
			return new LongStatisticsMapper("sort_loggedtime");
		}
		return null;
	}
}
