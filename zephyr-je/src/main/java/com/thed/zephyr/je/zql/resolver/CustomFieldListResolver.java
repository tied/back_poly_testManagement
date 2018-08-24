package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.jql.resolver.NameResolver;
import com.atlassian.query.clause.TerminalClause;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.CustomFieldOption;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by niravshah on 3/15/18.
 */
public class CustomFieldListResolver implements NameResolver<CustomFieldOption> {
    private final ZephyrCustomFieldManager zephyrCustomFieldManager;
    private final TerminalClause terminalClause;

    public CustomFieldListResolver(ZephyrCustomFieldManager zephyrCustomFieldManager,TerminalClause terminalClause) {
        this.zephyrCustomFieldManager = zephyrCustomFieldManager;
        this.terminalClause=terminalClause;
    }

    public List<String> getIdsFromName(String value) {
        CustomFieldOption customFieldOption = zephyrCustomFieldManager.getAllExecutionCustomFieldValue(terminalClause.getName(),value);
        List<String> ids = new ArrayList<>();
        if(customFieldOption != null) {
            ids.add(String.valueOf(customFieldOption.getID()));
        }
        return ids;
    }


    public boolean nameExists(String name) {
        return false;
    }

    public boolean idExists(Long id) {
       return false;
    }

    public CustomFieldOption get(Long id) {
        CustomFieldOption allExecutionCustomFieldValueById = zephyrCustomFieldManager.getCustomFieldOptionById(id.intValue());
        if(allExecutionCustomFieldValueById != null) {
            return allExecutionCustomFieldValueById;
        }
        return null;
    }

    public Collection<CustomFieldOption> getAll() {
        return new ArrayList<>();
    }
}
