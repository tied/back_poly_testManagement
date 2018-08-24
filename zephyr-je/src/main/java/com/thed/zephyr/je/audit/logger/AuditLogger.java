package com.thed.zephyr.je.audit.logger;


import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Map;

public class AuditLogger {

    private static final Logger log = Logger.getLogger(AuditLogger.class);

    public static void trace(AuditEventType auditEventType, Map<String, String> eventData) {
        log.trace(buildEventDetails(auditEventType, eventData));
    }

    public static void debug(AuditEventType auditEventType, Map<String, String> eventData) {
        log.debug(buildEventDetails(auditEventType, eventData));
    }

    public static void info(AuditEventType auditEventType, Map<String, String> eventData) {
        log.info(buildEventDetails(auditEventType, eventData));
    }

    public static void warn(AuditEventType auditEventType, Map<String, String> eventData) {
        log.warn(buildEventDetails(auditEventType, eventData));
    }

    public static void error(AuditEventType auditEventType, Map<String, String> eventData) {
        log.error(buildEventDetails(auditEventType, eventData));
    }

    private static StringBuilder buildEventDetails(AuditEventType auditEventType, Map<String, String> eventData) {
        StringBuilder builder = new StringBuilder();
        builder.append(auditEventType.toString()).append(StringUtils.SPACE);

        // Prepare event data
        if (eventData != null) {
            for (Map.Entry<String, String> entry : eventData.entrySet()) {
                builder.append(StringUtils.SPACE).append(entry.getValue());
            }
        }
        return builder;
    }

    private AuditLogger() {
    }
}
