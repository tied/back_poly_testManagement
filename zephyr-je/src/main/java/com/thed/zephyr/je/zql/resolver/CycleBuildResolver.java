package com.thed.zephyr.je.zql.resolver;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.jql.resolver.NameResolver;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.service.CycleManager;

/**
 * Resolves cycle ids from their builds.
 *
 */
public class CycleBuildResolver implements NameResolver<Cycle>
{
	private final CycleManager cycleManager;
	
    public CycleBuildResolver(CycleManager cycleManager) {
    	this.cycleManager=cycleManager;
    }

    public List<String> getIdsFromName(final String build) {
        notNull("build", build);
        List<String> values = new ArrayList<String>();

        List<String> builds = new ArrayList<String>();
        builds.add(build);
		List<Cycle> allCycles = cycleManager.getValuesByKey("BUILD", builds);
		for(Cycle cycle : allCycles) {
			values.add(String.valueOf(cycle.getID()));
		}
        return values;
    }

    public boolean nameExists(final String build)
    {
        notNull("name", build);

        List<String> builds = new ArrayList<String>();
        builds.add(build);
		List<Cycle> allCycles = cycleManager.getValuesByKey("BUILD", builds);
		for(Cycle cycle : allCycles) {
			if(StringUtils.equalsIgnoreCase(build, cycle.getBuild())) {
				return true;
			}
		}
		return false;
    }

    public boolean idExists(final Long id)
    {
        notNull("id", id);
		if(get(id) != null) {
			return true;
		}
        return false;
    }

    public Cycle get(final Long id)
    {
		Cycle cycle = cycleManager.getCycle(id);
		return cycle;
    }

	@Override
	public Collection<Cycle> getAll() {
        return new ArrayList<Cycle>();
	}
}

