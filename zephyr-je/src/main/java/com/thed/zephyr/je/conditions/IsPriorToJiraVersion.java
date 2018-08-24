package com.thed.zephyr.je.conditions;

import java.util.Map;

import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.thed.zephyr.util.VersionKit;

public class IsPriorToJiraVersion implements Condition {
	private final VersionKit.SoftwareVersion jiraVersion;
	private VersionKit.SoftwareVersion maxVersion;

	public IsPriorToJiraVersion(BuildUtilsInfo buildUtilsInfo) {
		String versionString = buildUtilsInfo.getVersion();
		jiraVersion = VersionKit.parse(versionString);
	}

	public void init(final Map<String, String> paramMap)
			throws PluginParseException {
		maxVersion = VersionKit.version(toInt(paramMap, "majorVersion"),
				toInt(paramMap, "minorVersion"));
	}

	private int toInt(Map<String, String> paramMap, final String paramName) {
		return Integer.decode(paramMap.get(paramName)).intValue();
	}

	public boolean shouldDisplay(final Map<String, Object> context) {
		return jiraVersion.isLessThan(maxVersion);
	}
}
