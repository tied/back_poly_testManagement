package com.thed.zephyr.util.logger;


import com.thed.zephyr.je.audit.logger.AuditEventType;
import com.thed.zephyr.je.audit.logger.AuditLogger;
import com.thed.zephyr.je.audit.logger.ZFJLoggingConstants;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuditLoggerUtil {


    public static void logZFJApiCallExit(String uuid, String httpMethod, String totalExecutionTime) {

        Map<String, String> logData = new LinkedHashMap<>();
        logData.put(ZFJLoggingConstants.UNIQUE_IDENTIFIER, uuid);
        logData.put(ZFJLoggingConstants.HTTP_REQUEST_METHOD, httpMethod);
        logData.put(ZFJLoggingConstants.TOTAL_EXECUTION_TIME, totalExecutionTime);
        AuditLogger.debug(AuditEventType.API_ROUND_TRIP_TIME, logData);
    }

    public static void logJVMStats(long totalMemory, long usedMemory, long totalPermGenMemory, long usedPermGenMemory) {
        Map<String, String> logData = new LinkedHashMap<>();
        logData.put(ZFJLoggingConstants.TOTAL_MEMORY_KEY, ZFJLoggingConstants.TOTAL_MEMORY_TEXT + totalMemory + ZFJLoggingConstants.MB);
        logData.put(ZFJLoggingConstants.USED_MEMORY_KEY, ZFJLoggingConstants.USED_MEMORY_TEXT + usedMemory + ZFJLoggingConstants.MB);
        logData.put(ZFJLoggingConstants.TOTAL_PERM_GEN_MEMORY_KEY, ZFJLoggingConstants.TOTAL_PERM_GEN_MEMORY_TEXT + totalPermGenMemory + ZFJLoggingConstants.MB);
        logData.put(ZFJLoggingConstants.USED_PERM_GEN_MEMORY_KEY, ZFJLoggingConstants.USED_PERM_GEN_MEMORY_TEXT + usedPermGenMemory + ZFJLoggingConstants.MB);
        AuditLogger.debug(AuditEventType.JVM_STATS, logData);
    }

    public static void logJVMStats(String uuid,long totalMemory, long usedMemory) {
        Map<String, String> logData = new LinkedHashMap<>();
        logData.put(ZFJLoggingConstants.UNIQUE_IDENTIFIER, uuid);
        logData.put(ZFJLoggingConstants.TOTAL_MEMORY_KEY, ZFJLoggingConstants.TOTAL_MEMORY_TEXT + totalMemory + ZFJLoggingConstants.MB);
        logData.put(ZFJLoggingConstants.USED_MEMORY_KEY, ZFJLoggingConstants.USED_MEMORY_TEXT + usedMemory + ZFJLoggingConstants.MB);
        AuditLogger.debug(AuditEventType.JVM_STATS, logData);
    }

}
