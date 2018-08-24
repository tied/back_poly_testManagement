package com.thed.zephyr.je.attachment;

import com.atlassian.jira.web.ExecutingHttpRequest;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Default implementation that uses the http session for storage.
 */
public class ZDefaultTemporaryAttachmentsMonitorLocator implements ZTemporaryAttachmentsMonitorLocator {
    protected final Logger log = Logger.getLogger(ZDefaultTemporaryAttachmentsMonitorLocator.class);

    @Override
    public ZTemporaryAttachmentsMonitor get(boolean create) {
        final HttpSession session = getCurrentSession(create);
        if (session == null) {
            return null;
        }

        ZTemporaryAttachmentsMonitor monitor = null;
        try {
            Object object = session.getAttribute(SessionKeys.EXECUTION_ATTACHMENTS);
            if (object instanceof ZTemporaryAttachmentsMonitor) {
                log.info("Its an instance of ZTemporaryAttachmentsMonitor.");
                monitor = (ZTemporaryAttachmentsMonitor) session.getAttribute(SessionKeys.EXECUTION_ATTACHMENTS);
            } else {
                log.info("Monitor is null & creating the new instance.");
                if (monitor == null && create) {
                    monitor = new ZTemporaryAttachmentsMonitor();
                    session.setAttribute(SessionKeys.EXECUTION_ATTACHMENTS, monitor);
                }
            }
        } catch (Exception e) {
            log.info("Error Retrieving Monitor from session, creating new one", e);
        }
        return monitor;
    }

    private HttpSession getCurrentSession(boolean create) {
        final HttpServletRequest request = ExecutingHttpRequest.get();
        return request == null ? null : request.getSession(create);
    }
}
