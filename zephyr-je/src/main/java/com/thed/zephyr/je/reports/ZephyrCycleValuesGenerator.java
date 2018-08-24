package com.thed.zephyr.je.reports;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.jira.component.ComponentAccessor;
import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import java.util.HashMap;
import java.util.Map;

public class ZephyrCycleValuesGenerator implements ValuesGenerator {
    private static final Logger log = Logger.getLogger(ZephyrCycleValuesGenerator.class);

    public ZephyrCycleValuesGenerator() {
    }

    @Override
    public Map getValues(Map params) {
        ComponentAccessor.getWebResourceManager().requireResource("com.thed.zephyr.je:zephyr-reports-resources");
        GenericValue projectGV = (GenericValue) params.get("project");
        OrderedMap cycles = ListOrderedMap.decorate(new HashMap(1));
        cycles.put(projectGV.getLong("id"), projectGV.getString("name"));
        return cycles;
    }
}
