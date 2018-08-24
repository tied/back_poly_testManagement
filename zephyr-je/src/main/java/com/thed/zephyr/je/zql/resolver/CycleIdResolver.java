package com.thed.zephyr.je.zql.resolver;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.atlassian.jira.component.ComponentAccessor;
import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.jql.resolver.NameResolver;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.util.ApplicationConstants;

/**
 * Resolves cycle ids from their names.
 *
 */
public class CycleIdResolver implements NameResolver<Cycle>
{
	private final CycleManager cycleManager;
	
    public CycleIdResolver(CycleManager cycleManager) {
    	this.cycleManager=cycleManager;
    }

    public List<String> getIdsFromName(final String name) {
        notNull("name", name);
        List<String> values = new ArrayList<String>();

        if(StringUtils.equalsIgnoreCase(name, ApplicationConstants.AD_HOC_CYCLE_NAME) || StringUtils.equalsIgnoreCase(name, ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.cycle.adhoc"))) {
			values.add(String.valueOf(ApplicationConstants.AD_HOC_CYCLE_ID));
        } else {
	        List<String> names = new ArrayList<String>();
	        names.add(name);
			List<Cycle> allCycles = cycleManager.getValuesByKey("ID", names);
			for(Cycle cycle : allCycles) {
				values.add(String.valueOf(cycle.getID()));
			}
        }
        return values;
    }

    public boolean nameExists(final String name) {
        notNull("name", name);
        List<String> names = new ArrayList<String>();
        names.add(name);
		List<Cycle> allCycles = cycleManager.getValuesByKey("ID", names);
		for(Cycle cycle : allCycles) {
			if(StringUtils.equalsIgnoreCase(name, cycle.getName())) {
				return true;
			}
		}
        if(StringUtils.equalsIgnoreCase(name, ApplicationConstants.AD_HOC_CYCLE_NAME) || StringUtils.equalsIgnoreCase(name, ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.cycle.adhoc"))) {
			return true;
		}
		return false;
    }

    public boolean idExists(final Long id) {
        notNull("id", id);
		if(get(id) != null) {
			return true;
		} else if (id == ApplicationConstants.AD_HOC_CYCLE_ID) {
			return true;
		}
        return false;
    }

    public Cycle get(final Long id) {
		Cycle cycle = cycleManager.getCycle(id);
		return cycle;
    }

	@Override
	public Collection<Cycle> getAll() {
        return new ArrayList<Cycle>();
	}
}

