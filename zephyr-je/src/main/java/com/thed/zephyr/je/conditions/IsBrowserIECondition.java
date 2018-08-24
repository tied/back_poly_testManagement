package com.thed.zephyr.je.conditions;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang3.StringUtils;
import com.atlassian.jira.web.ExecutingHttpRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import java.util.Map;

public class IsBrowserIECondition implements Condition {

    @Context
    HttpServletRequest req;

    private Boolean isIEResource = false;

    public void init(final Map<String, String> paramMap) throws PluginParseException {
        if(StringUtils.isNotBlank(paramMap.get("isIEResource"))) {
            this.isIEResource = Boolean.valueOf(paramMap.get("isIEResource"));
        }
    }

    public boolean shouldDisplay(final Map<String, Object> context) {
        HttpServletRequest servletRequest = ExecutingHttpRequest.get();
        Boolean isIE = JiraUtil.isIeBrowser(servletRequest.getHeader("User-Agent"));
        if(!isIE) {
            return (!this.isIEResource) ? true : false;
        } else {
            return (this.isIEResource) ? true : false;
        }
    }
}
