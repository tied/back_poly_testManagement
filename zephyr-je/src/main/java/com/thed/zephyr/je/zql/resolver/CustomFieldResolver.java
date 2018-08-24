package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.issue.fields.LabelsSystemField;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.atlassian.query.operand.Operand;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.CustomFieldOption;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Created by niravshah on 3/15/18.
 */
public class CustomFieldResolver implements NameResolver<CustomField> {
    private final CustomFieldValueManager customFieldValueManager;

    public CustomFieldResolver(CustomFieldValueManager customFieldValueManager) {
        this.customFieldValueManager = customFieldValueManager;
    }

    public List<String> getIdsFromName(String name) {
        List<CustomField> values = customFieldValueManager.getAllExecutionCustomFieldValue(name);
        List<String> ids = new ArrayList<>();
        values.stream().forEach(customField -> {
            if(StringUtils.equalsIgnoreCase(customField.getName(),name)) {
                ids.add(String.valueOf(customField.getID()));
            }
        });
        return ids;
    }


    public boolean nameExists(String name) {
        List<CustomField> values = customFieldValueManager.getAllExecutionCustomFieldValue(name);
        for(CustomField customField : values) {
            if(StringUtils.equalsIgnoreCase(customField.getName(),name)) {
                return true;
            }
        }
        return false;
    }

    public boolean idExists(Long id) {
        List<CustomField> values = customFieldValueManager.getAllExecutionCustomFieldValueById(id);
        for(CustomField customField : values) {
            if(customField.getID() == id.intValue()) {
                return true;
            }
        }
        return false;
    }

    public CustomField get(Long id) {
        List<CustomField> allExecutionCustomFieldValueById = customFieldValueManager.getAllExecutionCustomFieldValueById(id);
        if(!allExecutionCustomFieldValueById.isEmpty()) {
            return allExecutionCustomFieldValueById.get(0);
        }
        return null;
    }

    public Collection<CustomField> getAll() {
        return  customFieldValueManager.getAllExecutionCustomFieldValueByType();
    }
}
