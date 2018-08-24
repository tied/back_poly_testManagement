package com.thed.zephyr.je.config.upgrade;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import com.atlassian.jira.user.ApplicationUser;
import net.java.ao.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.usercompatibility.ApplicationUserUtilAccessException;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.thed.zephyr.je.model.Attachment;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.StepResult;

public class ZFJUpgradeForJIRA6Task2 implements PluginUpgradeTask{

	protected final I18nHelper i18n;
	private final ActiveObjects ao;
	private static final Logger log = Logger.getLogger(ZFJUpgradeForJIRA6Task2.class);
	
	public ZFJUpgradeForJIRA6Task2(JiraAuthenticationContext authenticationContext, ActiveObjects ao) {
		i18n = authenticationContext.getI18nHelper();
		this.ao = ao;
	}
	
	@Override
	/**
     * The build number for this upgrade task. Once this upgrade task has run the plugin manager will store this
     * build number against this plugin type.  After this only upgrade tasks with higher build numbers will be run
     */
	public int getBuildNumber() {
        if(!isRenameUserImplemented())
        	return 1;
        else
        	return 2 ;
	}

	@Override
	/* should be < 50 char*/
	public String getShortDescription() {
		return "Migrate username to userKey";
	}

	@Override
	public Collection<Message> doUpgrade() throws Exception {
		log.info("Performing upgrade 2 task - userName will be converted to userKeys.");
		final Map<String, String> userNameUserKeyMap = Maps.newHashMap();
		ao.executeInTransaction(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction() {
				final Schedule[] executions = ao.find(Schedule.class, Query.select().where("EXECUTED_BY IS NOT NULL "));
				for(Schedule execution : executions){
					String userName = execution.getExecutedBy();
					String userKey = getUserKey(userName);
					if(userKey != null && !StringUtils.equalsIgnoreCase(userKey, userName)){
						execution.setExecutedBy(userKey);
						execution.save();
					}
				}
				if(executions != null)
					log.debug("Total " + executions.length + " executions migrated");
				final StepResult[] stepsResults = ao.find(StepResult.class, Query.select().where("EXECUTED_BY IS NOT NULL "));
				for(StepResult stepResult : stepsResults){
					String userName = stepResult.getExecutedBy();
					String userKey = getUserKey(userName);
					if(userKey != null){
						stepResult.setExecutedBy(userKey);
						stepResult.save();
					}else{
						log.fatal("UNABLE TO PERFORM MIGRATION, No userKey found for user " + userName + " scheduleId " + stepResult.getID() + " issueId " + stepResult.getScheduleId());
					}
				}
				if(stepsResults != null)
					log.debug("Total " + stepsResults.length + " executions migrated");
				final Attachment[] attachments = ao.find(Attachment.class, Query.select().where("AUTHOR IS NOT NULL "));
				for(Attachment attachment : attachments){
					String userName = attachment.getAuthor();
					String userKey = getUserKey(userName);
					if(userKey != null){
						attachment.setAuthor(userKey);
						attachment.save();
					}else{
						log.fatal("UNABLE TO PERFORM MIGRATION, No userKey found for user " + userName + " attachment " + attachment.getID() + " for " + attachment.getEntityType() + " with id " + attachment.getEntityId());
					}
				}
				return null;
			}

			/**
			 * @param userNameUserKeyMap
			 * @param userName
			 * @return
			 */
			private String getUserKey(String userName) {
				String userKey = userNameUserKeyMap.get(userName);
				if(userKey == null){
					ApplicationUser user = ComponentAccessor.getUserManager().getUser(userName);
					if(user != null){
						userKey = getUserKeyFromUser(user.getDirectoryUser());
						userNameUserKeyMap.put(userName, userKey);
					}
				}
				return userKey;
			}

			/**
			 * @param user
			 * @return userKey
			 */
			private String getUserKeyFromUser(User user) {
				try{
				    Class<?> appUsersClass = Class.forName("com.atlassian.jira.user.ApplicationUsers");
				    Method getAppUserKeyMethod = appUsersClass.getMethod("getKeyFor", User.class);
				    return (String) getAppUserKeyMethod.invoke(null, user);
				}
				catch (ClassNotFoundException e){
				    throw new ApplicationUserUtilAccessException(e);
				}
				catch (NoSuchMethodException e){
				    throw new ApplicationUserUtilAccessException(e);
				}
				catch (InvocationTargetException e){
				    throw new ApplicationUserUtilAccessException(e);
				}
				catch (IllegalAccessException e){
				    throw new ApplicationUserUtilAccessException(e);
				}
			}
		});
		
       return null;
	}
	
	private static boolean isRenameUserImplemented(){
        final int[] versionNumbers = ComponentAccessor.getComponent(BuildUtilsInfo.class).getVersionNumbers();
        return versionNumbers[0] >= 6;

    }

	/**
     * Identifies the plugin that will be upgraded.
     */
	@Override
	public String getPluginKey() {
		return "com.thed.zephyr.je";
	}

}
