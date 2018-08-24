package com.thed.zephyr.zapi.util;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.util.json.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.opensymphony.module.propertyset.PropertyException;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.atlassian.jira.rest.api.http.CacheControl.never;

public class JiraUtil {

    private static final Logger log = Logger.getLogger(JiraUtil.class);

    public static ApplicationProperties getApplicationProperties() {
        return ComponentAccessor.getApplicationProperties();
    }

    public static Locale getUserLocale() {
        return ComponentAccessor.getApplicationProperties().getDefaultLocale();
    }


    /**
     * Locate PropertySet using PropertyStore for this sequenceName/sequenceId
     * mapping.
     *
     * @param entityName
     */
    @SuppressWarnings("unchecked")
    public static PropertySet getPropertySet(String entityName, Long entityId) {
        PropertySet ofbizPs = PropertySetManager.getInstance("ofbiz", buildPropertySet(entityName, entityId));

        Map args = EasyMap.build("PropertySet", ofbizPs, "bulkload", Boolean.TRUE);
        return PropertySetManager.getInstance("cached", args);
    }

    private static Map<Object, Object> buildPropertySet(String entityName, Long entityId) {
        return new ImmutableMap.Builder<Object, Object>()
                .put("delegator.name", "default")
                .put("entityName", entityName)
                .put("entityId", entityId)
                .build();
    }

    public static ConstantsManager getConstantsManager() {
        return ComponentAccessor.getConstantsManager();
    }


    public static Response buildErrorResponse(Response.Status status, String errorId, String errorDesc, String errorHtml) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ApplicationConstants.ERROR_ID, errorId);
        map.put(ApplicationConstants.ERROR_DESC, errorDesc);
        map.put(ApplicationConstants.ERROR_DESC_HTML, errorHtml);
        JSONObject jsonResponse = new JSONObject(map);

        return Response.status(status).entity(jsonResponse.toString()).cacheControl(never()).build();
    }


    /**
     * @param propertyKey
     * @param defaultPropertyValue
     * @return
     */
    public static Object getSimpleDBProperty(final String propertyKey, Object defaultPropertyValue) {
        Object value = null;
        try {
            value = JiraUtil.getPropertySet(
                    ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getAsActualType(propertyKey);
        } catch (PropertyException e) {
            log.warn("No such key " + propertyKey + " found");
        }
        if (value == null) {
            value = defaultPropertyValue;
        }
        return value;
    }

    /**
     * Checks if JIRA is running in dev mode
     *
     * @return
     */
    public static Boolean isDevMode() {
        return new Boolean(System.getProperty("atlassian.dev.mode", "false"));
    }

    static class StatusComparator implements Comparator<Status> {

        public int compare(Status c1, Status c2) {
            int nameOrder = c1.getName().compareTo(c2.getName());
            return nameOrder;
        }
    }

}
