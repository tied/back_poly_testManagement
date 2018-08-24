package com.thed.zephyr.je.config.upgrade;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.util.ConfigurationConstants;
import net.java.ao.Query;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Upgrade task to migrate schedule/cycle versions to -1 for any deleted version.
 * (https://defects.yourzephyr.com/browse/ZFJ-1052)
 * Author: mukul
 * Date: 6/5/14
 * Time: 1:41 PM
 */

public class ZephyrUpgradeVersionMigrationTask implements PluginUpgradeTask {

    private static final Logger log = Logger.getLogger(ZephyrUpgradeVersionMigrationTask.class);
    protected final I18nHelper i18n;
    private final ActiveObjects ao;
    private final VersionManager versionManager;

    public ZephyrUpgradeVersionMigrationTask(JiraAuthenticationContext authenticationContext, ActiveObjects ao, VersionManager versionManager) {
        this.i18n = authenticationContext.getI18nHelper();
        this.ao = ao;
        this.versionManager = versionManager;
    }

    @Override
    /**
     * The build number for this upgrade task. Once this upgrade task has run the plugin manager will store this
     * build number against this plugin type.  After this only upgrade tasks with higher build numbers will be run
     */
    public int getBuildNumber() {
        return 6;
    }

    @Override
    /* should be < 50 char*/
    public String getShortDescription() {
        return "Set \"versionId=-1\" for any deleted versions";
    }

    @Override
    public Collection<Message> doUpgrade() throws Exception {
        log.info("Performing upgrade 6 executions/cycles version migration task");
        final Collection<Long> versionIds = Collections2.transform(
                versionManager.getAllVersions(),
                new Function<Version, Long>() {
                    @Override
                    public Long apply(final Version version) {
                        return version.getId();
                    }
                }
        );

        final List<Long> removedVersionIds = new ArrayList<Long>();

        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                final Schedule[] executions = ao.find(Schedule.class, Query.select("VERSION_ID").where("VERSION_ID != ?", new Long(-1L)).distinct());
                for (Schedule execution : executions) {
                    Long versionId = Long.valueOf(execution.getVersionId());
                    if (!(versionIds.contains(versionId)))
                        removedVersionIds.add(versionId);

                }
                return null;
            }
        });

        if (removedVersionIds.size() > 0) {
            ao.executeInTransaction(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction() {
                    for (Long removedVersionId : removedVersionIds) {
                        final Schedule[] executions = ao.find(Schedule.class, Query.select().where("VERSION_ID = ?", removedVersionId));
                        for (Schedule execution : executions) {
                            execution.setVersionId(-1L);
                            execution.save();
                        }
                    }
                    return null;
                }
            });
        }
        
        final List<Long> removedCycleVersionIds = new ArrayList<Long>();
        
        //change versions in cycles as well to -1
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                final Cycle[] cycles = ao.find(Cycle.class, Query.select("VERSION_ID").where("VERSION_ID != ?", new Long(-1L)).distinct());
                for (Cycle cycle : cycles) {
                    Long versionId = Long.valueOf(cycle.getVersionId());
                    if (!(versionIds.contains(versionId)))
                    	removedCycleVersionIds.add(versionId);

                }
                return null;
            }
        });

        if (removedCycleVersionIds.size() > 0) {
            ao.executeInTransaction(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction() {
                    for (Long removedVersionId : removedCycleVersionIds) {
                        final Cycle[] cycles = ao.find(Cycle.class, Query.select().where("VERSION_ID = ?", removedVersionId));
                        for (Cycle cycle : cycles) {
                            cycle.setVersionId(-1L);
                            cycle.save();
                        }
                    }
                    return null;
                }
            });
        }
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
