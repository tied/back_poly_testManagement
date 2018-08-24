/**
 * 
 */
package com.thed.zephyr.je.index.cluster;


import com.atlassian.event.api.EventListener;
import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.jira.event.ClearCacheEvent;
import com.atlassian.util.concurrent.ResettableLazyReference;
import com.google.common.collect.Maps;
import com.thed.zephyr.util.JiraUtil;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;

/**
 * Responsible for loading the cluster properties from file, if it exists
 *
 */
public class ClusterPropertiesImpl implements ClusterProperties
{
    private static final Logger log = Logger.getLogger(ClusterPropertiesImpl.class);

    public static final String JIRA_FAILOVER_CONFIG_PROPERTIES = "cluster.properties";
    public static final String JIRA_SHARED_HOME = "jira.shared.home";
    public static final String JIRA_NODE_ID = "jira.node.id";

    private File overlayFile = null;
    public ClusterPropertiesImpl(JiraHome jiraHome)
    {
    	if (JiraUtil.isJIRAGreaterThan63()) { 
			try {
		    	Method localHomeMethod;
				localHomeMethod = JiraHome.class.getDeclaredMethod("getLocalHomePath");
		        if(localHomeMethod != null) {
		        	String localHomePath = (String)localHomeMethod.invoke(jiraHome);
		        	overlayFile = new File(localHomePath, JIRA_FAILOVER_CONFIG_PROPERTIES);
		        }
			} catch (SecurityException e) {
				log.info("SecurityException reading File:"+e.getMessage());
			} catch (NoSuchMethodException e) {
				log.info("NoSuchMethodException reading LocalHome:"+e.getMessage());
			} catch (IllegalArgumentException e) {
				log.info("IllegalArgumentException:"+e.getMessage());
			} catch (IllegalAccessException e) {
				log.info("IllegalAccessException:"+e.getMessage());
			} catch (InvocationTargetException e) {
				log.info("InvocationTargetException:"+e.getMessage());
			}
    	}
    }

    @Nullable
    public String getProperty(String property)
    {
        return failoverPropertiesRef.get().get(property);
    }

    @Override
    public String getSharedHome()
    {
        return getProperty(JIRA_SHARED_HOME);
    }

    @Override
    public String getNodeId()
    {
        return getProperty(JIRA_NODE_ID);
    }

    public void refresh()
    {
        failoverPropertiesRef.reset();
    }


    @EventListener
    public void onClearCache(final ClearCacheEvent event)
    {
        refresh();
    }

    /** This is a reference to the holder for the local jira-ha.properties */
    private final ResettableLazyReference<Map<String, String>> failoverPropertiesRef = new ResettableLazyReference<Map<String, String>>() {
        protected Map<String, String> create() throws Exception {
            // We want to turn the Properties object into an immutable HashMap
            return Maps.fromProperties(loadProperties());
        }

        private Properties loadProperties()
        {
            Properties properties = new Properties();
            InputStream in = null;
            try
            {
                if (overlayFile != null && overlayFile.exists())
                {
                    in = new FileInputStream(overlayFile);
                    properties.load(in);
                }
            } catch (final IOException e) {
                log.warn("Could not load config properties from '" + overlayFile + "'.");
            }
            finally {
                IOUtils.closeQuietly(in);
            }
            return properties;
        }
    };

}
