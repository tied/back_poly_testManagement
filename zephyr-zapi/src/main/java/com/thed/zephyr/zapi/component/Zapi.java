package com.thed.zephyr.zapi.component;

import com.atlassian.jira.util.json.JSONObject;
/**
 * Created with IntelliJ IDEA.
 * User: mukul
 * Date: 12/30/13
 * Time: 12:05 PM
 */

/**
 * Component Interface to be exported as OSGi component/service
 */
public interface Zapi {

    JSONObject getZapiConfig();
}
