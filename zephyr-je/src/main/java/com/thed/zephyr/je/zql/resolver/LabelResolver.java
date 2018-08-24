package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.jql.resolver.NameResolver;

import java.util.*;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Resolves label ids from their names.
 *
 */
public class LabelResolver implements NameResolver<Label>
{
	private final LabelManager labelManager;

    public LabelResolver(LabelManager labelManager) {
    	this.labelManager=labelManager;
    }

    public List<String> getIdsFromName(final String name) {
        notNull("name", name);
        List<String> values = new ArrayList<String>();
        values.addAll(labelManager.getSuggestedLabels(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), null, ""));
        return values;
    }

    public boolean nameExists(final String name) {
        notNull("name", name);

        List<String> values = new ArrayList<String>();
        values.addAll(labelManager.getSuggestedLabels(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), null, "" ));

        if(values.contains(name)){
            return true;
        }else {
            return false;
        }
    }

    public boolean idExists(final Long id) {
        notNull("id", id);
		if(get(id) != null) {
			return true;
		}
        return false;
    }

    public Label get(final Long id) {
		Set<Label> label = labelManager.getLabels(id);
        Label lbl = null;
        for(Label l: label){ lbl = l;}
        return lbl;
    }



    ///CLOVER:OFF
    public Collection<Label> getAll()
    {
        return new ArrayList<Label>();
    }
    ///CLOVER:ON
}

