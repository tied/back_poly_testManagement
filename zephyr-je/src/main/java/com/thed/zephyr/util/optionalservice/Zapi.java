package com.thed.zephyr.util.optionalservice;

import com.atlassian.jira.util.json.JSONObject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: mukul
 * Date: 12/30/13
 * Time: 12:15 PM
 */
public class Zapi {

    private final com.thed.zephyr.zapi.component.Zapi zapi;

    public Zapi(com.thed.zephyr.zapi.component.Zapi zapi)
    {
        this.zapi = checkNotNull(zapi, "zapi");
    }

    public JSONObject getZapiConfig()
    {
        return zapi.getZapiConfig();
    }
}
