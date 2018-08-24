package com.thed.zephyr.util.logger;

import com.atlassian.jira.cluster.logging.LoggingManager;
import com.atlassian.jira.logging.JiraHomeAppender;
import com.atlassian.jira.mail.MailLoggingManager;
import com.atlassian.jira.web.action.admin.ConfigureLogging;
import com.atlassian.logging.log4j.NewLineIndentingFilteringPatternLayout;
import com.atlassian.mail.server.MailServerManager;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Created by niravshah on 1/29/17.
 */
public class ZephyrLogger extends ConfigureLogging {
    private String loggerName;
    private String levelName;
    private Logger logger;

    public ZephyrLogger(MailServerManager mailServerManager, MailLoggingManager mailLoggingManager, LoggingManager loggingManager) {
        super(mailServerManager, mailLoggingManager, loggingManager);
    }


    public void initializeZephyrLogs() throws Exception {
        Logger rootLogger = getRootLogger();
        rootLogger.warn("Zephyr logger  : ["+Logger.getLogger("com.thed.zephyr").getAppender("zephyrForJiraAppender")+"]");
        if(Logger.getLogger("com.thed.zephyr").getAppender("zephyrForJiraAppender") == null) {
            setLogger(JiraUtil.getZephyrLogLevel());
            loggingManager.setLogLevel(Logger.getLogger("com.thed.zephyr"),Level.toLevel(JiraUtil.getZephyrLogLevel()));
        } else {
            loggingManager.setLogLevel(Logger.getLogger("com.thed.zephyr"),Level.toLevel(JiraUtil.getZephyrLogLevel()));
        }
        /**
         * To enable jira apis call log.
         */
        //loggingManager.enableProfiling();
    }

    public void setLogLevel(Level logLevel) throws Exception {
        if(Logger.getLogger("com.thed.zephyr").getAppender("zephyrForJiraAppender") == null) {
            loggingManager.setLogLevel(Logger.getLogger("com.thed.zephyr"), logLevel);
        }else {
            loggingManager.setLogLevel(Logger.getLogger("com.thed.zephyr"), logLevel);
            JiraHomeAppender appender = (JiraHomeAppender) Logger.getLogger("com.thed.zephyr").getAppender("zephyrForJiraAppender");
            appender.setThreshold(logLevel);
        }
    }

    public Logger setLogger(String logLevel) {
        logger = getRootLogger();
        JiraHomeAppender appender = new JiraHomeAppender();
        appender.setName("zephyrForJiraAppender");
        NewLineIndentingFilteringPatternLayout patternLayout = new NewLineIndentingFilteringPatternLayout();
        patternLayout.setFilteredFrames("@jira-filtered-frames.properties");
        patternLayout.setConversionPattern("%d %t %p %X{jira.username} %X{jira.request.id} %X{jira.request.assession.id} %X{jira.request.ipaddr} %X{jira.request.url} [%q{2}] %m%n");
        patternLayout.setFilteringApplied(true);
        patternLayout.setStackTracePackagingExamined(false);
        patternLayout.setShowEludedSummary(false);
        patternLayout.activateOptions();
        appender.setLayout(patternLayout);
        appender.setFile("zephyr-jira.log");
        appender.setThreshold(Level.toLevel(logLevel));
        appender.setImmediateFlush(true);
        appender.setAppend(true);
        appender.setMaxFileSize(JiraUtil.getZephyrLogMaxSize()+ConfigurationConstants.MEGABYTE);
        appender.setMaxBackupIndex(Integer.parseInt(JiraUtil.getZephyrLogMaxBackup()));
        appender.activateOptions();
        logger.info("Adding zephyr appender to zephyr logger.");
        Logger.getLogger("com.thed.zephyr").addAppender(appender);
        Logger.getLogger("com.thed.zephyr").setAdditivity(false);
        return logger;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }
}
