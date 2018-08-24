package com.thed.zephyr.je.config.license;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.VersionKit;
import com.thed.zephyr.util.ZephyrLicenseException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.osgi.framework.Bundle;

import java.net.URI;

public class PluginUtils {

	private static final Logger log = Logger.getLogger(PluginUtils.class);
    private static Boolean isAOVersionLessThan23 = null;

	/**
	 * @param plugin
	 * @return
	 * @throws ZephyrLicenseException
	 */
	public static DateTime getPluginBuildDate(Plugin plugin) throws ZephyrLicenseException {
		String newZephyrJEVersion = plugin.getPluginInformation().getVersion();
		Bundle bundle = ((OsgiPlugin) plugin ).getBundle();
	    if (bundle == null){
	    	String upgradeErrorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.version.upgrade.error", newZephyrJEVersion);
			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_VERSION_UPGRADE_ERROR, upgradeErrorMessage);
	    }
	    Object value = bundle.getHeaders().get("Atlassian-Build-Date");

	    DateTime pluginBuildDate = new DateTime(); 
		if (value != null) {
			try {
				pluginBuildDate = (DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withOffsetParsed().parseDateTime(value.toString()));
			} catch (IllegalArgumentException e) {
				log.warn("Plugin with key \"" + plugin.getKey() + "\" has invalid Atlassian-Build-Date of \"" + value + "\"");
			}
		}
		return pluginBuildDate;
	}

    public static boolean isAOVersionLessThan23(){
        if(null != isAOVersionLessThan23)
            return isAOVersionLessThan23;
        else{
            VersionKit.SoftwareVersion pluginVersion = VersionKit.parse(ComponentAccessor.getPluginAccessor().
                getPlugin("com.atlassian.activeobjects.activeobjects-plugin").getPluginInformation().getVersion());
            isAOVersionLessThan23 = pluginVersion.isLessThan(VersionKit.version(0,23,0));
            return isAOVersionLessThan23;
        }
    }

	public static URI getUPMManagePluginUri(){
		return URI.create(ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL) + "/plugins/servlet/upm#manage/" + ConfigurationConstants.PLUGIN_KEY);
	}
}