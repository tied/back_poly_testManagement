package com.thed.zephyr.je.rest.filter;


import com.atlassian.jira.util.system.SystemInfoUtils;
import com.atlassian.jira.util.system.SystemInfoUtilsImpl;
import com.sun.jersey.spi.container.*;
import com.thed.zephyr.je.audit.logger.ZFJLoggingConstants;
import com.thed.zephyr.util.logger.AuditLoggerUtil;
import org.apache.log4j.MDC;

import java.util.UUID;

public class ZFJApiFilter implements ContainerRequestFilter, ContainerResponseFilter, ResourceFilter {

    /**
     * Constants
     */
    private static final String START_TIME = "startTime";
    private static final String UNIQUE_IDENTIFIER = "uniqueIdentifier";
    private final SystemInfoUtils systemInfoUtils = new SystemInfoUtilsImpl();

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        String uniqueIdentifier = UUID.randomUUID().toString();
        MDC.put(START_TIME, Long.valueOf(System.currentTimeMillis()));
        MDC.put(UNIQUE_IDENTIFIER, uniqueIdentifier);
        /*AuditLoggerUtil.logJVMStats(systemInfoUtils.getTotalMemory(), systemInfoUtils.getUsedMemory(), systemInfoUtils.getTotalPermGenMemory(), systemInfoUtils.getUsedPermGenMemory());*/
        AuditLoggerUtil.logJVMStats(uniqueIdentifier,systemInfoUtils.getTotalMemory(), systemInfoUtils.getUsedMemory());
        return request;
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        Long startTime = (Long) MDC.get(START_TIME);
        String uniqueIdentifier = (String) MDC.get(UNIQUE_IDENTIFIER);

        if (null == startTime) {
            return response;
        }
        Long totalExecutionTime = System.currentTimeMillis() - startTime;
        /*AuditLoggerUtil.logJVMStats(systemInfoUtils.getTotalMemory(), systemInfoUtils.getUsedMemory(), systemInfoUtils.getTotalPermGenMemory(), systemInfoUtils.getUsedPermGenMemory());*/
        AuditLoggerUtil.logJVMStats(uniqueIdentifier,systemInfoUtils.getTotalMemory(), systemInfoUtils.getUsedMemory());
        AuditLoggerUtil.logZFJApiCallExit(uniqueIdentifier,request.getMethod(), totalExecutionTime + ZFJLoggingConstants.MS);
        MDC.clear();
        return response;
    }

    @Override
    public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
        return this;
    }
}
