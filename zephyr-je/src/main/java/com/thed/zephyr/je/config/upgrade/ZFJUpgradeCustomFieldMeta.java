package com.thed.zephyr.je.config.upgrade;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.google.gson.*;
import com.thed.zephyr.util.ConfigurationConstants;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.thed.zephyr.je.model.CustomFieldsMeta;

public class ZFJUpgradeCustomFieldMeta implements  PluginUpgradeTask {

	protected final I18nHelper i18n;
	private final ActiveObjects ao;
	private static final Logger log = Logger.getLogger(ZFJUpgradeCustomFieldMeta.class);

	public ZFJUpgradeCustomFieldMeta(JiraAuthenticationContext authenticationContext, ActiveObjects ao) {
		i18n = authenticationContext.getI18nHelper();
		this.ao = ao;
	}

	@Override
	/**
	 * The build number for this upgrade task. Once this upgrade task has run the plugin manager will store this
	 * build number against this plugin type.  After this only upgrade tasks with higher build numbers will be run
	 */
	public int getBuildNumber() {
		return 41;
	}

	@Override
	public String getShortDescription() {
		return "Add default metadata for Custom Fields.";
	}


	public Collection<Message> doUpgrade() throws Exception {
		log.info("Performing init for CustomFields");
		ao.executeInTransaction(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction() {
				JsonParser parser = new JsonParser();

				try {
					InputStream is = ZFJUpgradeCustomFieldMeta.class.getResourceAsStream("/files/CustomFieldMeta.json");

					String theString = IOUtils.toString(is, "UTF-8");
					log.info("Read CustomFields Metadata::"+theString);
					Object obj = parser.parse(theString);

					JsonObject jsonObject = (JsonObject) obj;

					JsonObject data = jsonObject.getAsJsonObject("data");

					if(Objects.nonNull(data)) {
						JsonArray customFieldsMetaData = data.getAsJsonArray("customFields");
						Gson gson = new Gson();
						Map<String, Object> customFieldProperties = new HashMap<>();
						if(null != customFieldsMetaData) {
							for (JsonElement jsonElement : customFieldsMetaData) {
								Map customFieldMetaMap = gson.fromJson(jsonElement.toString(), HashMap.class);
								customFieldProperties.put("IMAGE", customFieldMetaMap.get("imageClass"));
								customFieldProperties.put("DESCRIPTION", customFieldMetaMap.get("description"));
								customFieldProperties.put("LABEL", customFieldMetaMap.get("label"));
								customFieldProperties.put("OPTIONS", customFieldMetaMap.get("options"));
								customFieldProperties.put("TYPE", customFieldMetaMap.get("type"));
								ao.create(CustomFieldsMeta.class, customFieldProperties);
							}
						}
						log.debug("Successfully updated CustomField Metadata..");
					}
				} catch (Exception e) {
					log.error("Error Upgrading Custom Field Metadata",e);
				}

				return null;
			}
		});
		return null;
	}


	/**
	 * Identifies the plugin that will be upgraded.
	 */
	@Override
	public String getPluginKey() {
		return ConfigurationConstants.PLUGIN_KEY;
    }
}
