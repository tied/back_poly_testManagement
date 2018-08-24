package com.thed.zephyr.je.config.upgrade;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.thed.zephyr.je.audit.model.ChangeZJEGroup;
import com.thed.zephyr.je.audit.model.ChangeZJEItem;
import com.thed.zephyr.je.zql.model.ZQLFavoriteAsoc;
import com.thed.zephyr.je.zql.model.ZQLFilter;
import com.thed.zephyr.je.zql.model.ZQLSharePermissions;
import com.thed.zephyr.util.ConfigurationConstants;

public class ZFJUpgrade2Dot0Task3 implements PluginUpgradeTask{

	protected final I18nHelper i18n;
	private final ActiveObjects ao;
	private static final Logger log = Logger.getLogger(ZFJUpgrade2Dot0Task3.class);
	
	public ZFJUpgrade2Dot0Task3(JiraAuthenticationContext authenticationContext, ActiveObjects ao) {
		i18n = authenticationContext.getI18nHelper();
		this.ao = ao;
	}
	
	@Override
	/**
     * The build number for this upgrade task. Once this upgrade task has run the plugin manager will store this
     * build number against this plugin type.  After this only upgrade tasks with higher build numbers will be run
     */
	public int getBuildNumber() {
        return 3;
	}

	@Override
	/* should be < 50 char*/
	public String getShortDescription() {
		return "Migrates Older ZFJ versions to ZFJ 2.0";
	}

	@Override
	public Collection<Message> doUpgrade() throws Exception {
		log.info("Performing upgrade 3 task");
		ao.migrate(ZQLSharePermissions.class, ZQLFilter.class, ZQLFavoriteAsoc.class, ChangeZJEGroup.class, ChangeZJEItem.class);
		ao.executeInTransaction(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction() {
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
