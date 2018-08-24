package com.thed.zephyr.je.config;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.issue.customfields.converters.DoubleConverter;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.portal.PortletConfigurationManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.BuildUtilsInfoImpl;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.compat.CompatibilityPluginScheduler;
import com.thed.zephyr.je.index.ScheduleIndexDirectoryFactory;
import com.thed.zephyr.je.index.ScheduleIndexer;
import com.thed.zephyr.je.index.cluster.MessageHandler;
import com.thed.zephyr.je.index.cluster.NodeStateManager;
import com.thed.zephyr.je.job.ZFJJobRunner;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import com.thed.zephyr.util.logger.ZephyrLogger;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

/**
 * This is the main entry point for Zephyr JE. All initialization
 * should take place here. Reference:
 * http://confluence.atlassian.com/display/DEVNET
 * /Plugin+Tutorial+-+Writing+event+listeners+with+the+atlassian-event+library
 * 
 * There is another way to initialize plugin. See GreenHopper Plugin for more
 * details. Take a look at: com.atlassian.greenhopper.Launcher on how to
 * initialize plugin during spring Context call on function
 * onSpringContextStarted().
 * 
 * Let's create new custom field and issue type scheme after plugin is enabled
 * in function afterPropertiesSet() Refer:com.pyxis.greenhopper.GreenHopper.java
 * / GreenHopperImpl.java
 */

public class ZephyrJELauncher implements LifecycleAware, InitializingBean, DisposableBean {

	protected static final Logger log = Logger.getLogger(ZephyrJEDefaultConfiguration.class);
	public PortletConfigurationManager portletConfigurationManager;
		
	private PortletConfigurationManager pConfigurationManager;
	private UserPreferencesManager userPreferencesManager;
    private final CompatibilityPluginScheduler compatibilityPluginScheduler;
	private ZephyrComponentAccessor zephyrComponentAccessor;
	private ZFJJobRunner zfjJobRunner;
    private final ActiveObjects ao;
    private final EventPublisher eventPublisher;
    private final MessageHandler messageHandler;
    private final NodeStateManager nodeStateManager;
    private final ZephyrLogger zephyrLogger;
    private final JiraAuthenticationContext authenticationContext;
    private final IndexPathManager indexPathManager;
    private final ScheduleIndexer scheduleIndexer;

    @GuardedBy("this")
    private final Set<LifecycleEvent> lifecycleEvents = EnumSet.noneOf(LifecycleEvent.class);

    public ZephyrJELauncher (final CustomFieldValuePersister customFieldValuePersister, 
                          final DoubleConverter doubleConverter, 
                          final GenericConfigManager genericConfigManager,
                          final PortletConfigurationManager pConfigurationManager,
                          final UserPreferencesManager userPreferencesManager,
                          final CompatibilityPluginScheduler compatibilityPluginScheduler,
                          final ActiveObjects ao,
                          final ZephyrComponentAccessor zephyrComponentAccessor,
                          final ZFJJobRunner zfjJobRunner,
                          final EventPublisher eventPublisher,
                          final MessageHandler messageHandler,
                          final NodeStateManager nodeStateManager,
                          final ZephyrLogger zephyrLogger,
                          final JiraAuthenticationContext authenticationContext,
                             final IndexPathManager indexPathManager,
                             final ScheduleIndexer scheduleIndexer) {
		this.pConfigurationManager = pConfigurationManager;
		this.userPreferencesManager = userPreferencesManager;
		this.compatibilityPluginScheduler = compatibilityPluginScheduler;
		/* Injecting ZCA just so that it can be initialized aggrasively.*/
		this.zephyrComponentAccessor = zephyrComponentAccessor;
		this.zfjJobRunner=zfjJobRunner;
		this.ao=ao;
		this.eventPublisher=eventPublisher;
		this.messageHandler=messageHandler;
		this.nodeStateManager=nodeStateManager;
		this.zephyrLogger=zephyrLogger;
		this.authenticationContext = authenticationContext;
		this.indexPathManager=indexPathManager;
		this.scheduleIndexer = scheduleIndexer;
    }
	
    /**
     * This is received from Spring after the bean's properties are set.  We need to accept this to know when
     * it is safe to register an event listener.
     */
    @Override
    public void afterPropertiesSet()
    {
    	log.info("Post Properties Set");
		/*
		 * Following piece of code is for Test purposes.
		 * It shows that plugin is not enabled when this function gets called.
		 * Message doesnt get printed as plugin is a null object!
		 */
		PluginAccessor pluginAccessor = ComponentAccessor.getPluginAccessor();
		Plugin zephyrPlugin = pluginAccessor.getPlugin("com.thed.zephyr.je");
		
		if( zephyrPlugin != null) {
			log.info("Is Plugin Enabled ? -> " + pluginAccessor.isPluginEnabled("com.thed.zephyr.je"));
		}
        registerListener();
        onLifecycleEvent(LifecycleEvent.AFTER_PROPERTIES_SET);
    }

    /**
     * This is received from SAL after the system is really up and running from its perspective.  This includes
     * things like the database being set up and other tricky things like that.  This needs to happen before we
     * try to schedule anything, or the scheduler's tables may not be in a good state on a clean install.
     */
    @Override
    public void onStart()
    {
		log.info("LifecycleAware event - onStart() - gets called after plugin system is fully loaded");

		//Basic initatilzation is done here such as version check setting and current version setting.
		ZephyrJEDefaultConfiguration zephyrConfiguration = new ZephyrJEDefaultConfiguration(pConfigurationManager, userPreferencesManager);
		
		String currentZephyrJEVersion = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
					.getString(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION);
		
		if(currentZephyrJEVersion == null){
			zephyrConfiguration.init();
		}
		else {
	    	String pluginVersionInfo = ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY).getPluginInformation().getParameters().get(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION);
	    	
	    	//Do not do version check as during development when PI is used to re-load ZFJ jar.
	    	//Hence we will not update version info here but rather do it manually from backend directly!
	        if(JiraUtil.isDevMode() && !isVersionValid(pluginVersionInfo)){
	        	log.warn("Development mode ... let's skip the version check as PI doesnt provide this information.");
	    		return;
	    	}

	    	int dbBuildNumber = 0;
	    	int pluginBuildNumber = 0;
	    	
	    	try{
	    		dbBuildNumber = Integer.parseInt(currentZephyrJEVersion.split("\\.")[3]);
	    		pluginBuildNumber = Integer.parseInt(pluginVersionInfo.split("\\.")[3]);
	    		log.info("DB Build Number - " + dbBuildNumber + "  New Plugin Version Number - " + pluginBuildNumber);
	    	}
	    	catch(Exception nfe){
	    		log.error("Invalid Application versions. Installed version - " + currentZephyrJEVersion + "  New Version - " + pluginVersionInfo, nfe);
	    	}
	    	
	    	if(pluginBuildNumber >= dbBuildNumber){
	    		//Check for license upgrade validity
	    		zephyrConfiguration.reInitalize(pluginVersionInfo);
	    	}
        }
		onLifecycleEvent(LifecycleEvent.LIFECYCLE_AWARE_ON_START);
    }

    /**
     * This is received from the plugin system after the plugin is fully initialized.  It is not safe to use
     * Active Objects before this event is received.
     */
    @EventListener
    public void onPluginEnabled(PluginEnabledEvent event)
    {
    	log.info("Plugin has been enabled");
        if (ConfigurationConstants.PLUGIN_KEY.equals(event.getPlugin().getKey()))
        {
            onLifecycleEvent(LifecycleEvent.PLUGIN_ENABLED);
            //if clustered, start the Index Messaging Handler
            if (nodeStateManager.isClustered()) {
            	startMessagingService();
            }
            //Enable Zephyr Logger
            try {
                zephyrLogger.initializeZephyrLogs();
                BuildUtilsInfo buildUtilsInfo = new BuildUtilsInfoImpl();
                for (final ScheduleIndexDirectoryFactory.EntityName type : ScheduleIndexDirectoryFactory.EntityName.values()) {
                    type.directory(indexPathManager).clearLock("write.lock");
                }
            } catch (Exception e) {
                for (final ScheduleIndexDirectoryFactory.EntityName type : ScheduleIndexDirectoryFactory.EntityName.values()) {
                    try {
                        type.directory(indexPathManager).clearLock("write.lock");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                log.error("Error initializing logger, will continue to write to atlassian logs:",e);
            }
        }
    }

    /**
     * This is received from Spring when we are getting destroyed.  We should make sure we do not leave any
     * event listeners or job runners behind; otherwise, we could leak the current plugin context, leading to
     * exceptions from destroyed OSGi proxies, memory leaks, and strange behaviour in general.
     */
    @Override
    public void destroy() throws Exception
    {
        unregisterListener();
        unregisterJobRunner();
		unscheduleVersionJob();
		stopMessagingService();
		try {
            for (final ScheduleIndexDirectoryFactory.EntityName type : ScheduleIndexDirectoryFactory.EntityName.values()) {
                type.directory(indexPathManager).clearLock("write.lock");
            }
            scheduleIndexer.shutdown();
        } catch(Exception e) {
            log.error("Error destroying Object on shutdown:",e);
        }

    }


    /**
     * The latch which ensures all of the plugin/application lifecycle progress is completed before we call
     * {@code registerRunner()}.
     */
    private void onLifecycleEvent(LifecycleEvent event)
    {
        log.info("Zephyr Plugin onLifeCycle Event received: " + event);
        if (isLifecycleReady(event)) {
            log.info("Received the last lifecycle event...");
            unregisterListener();

            try {
                registerJobs();
                scheduleVersionJob(24 * 60 * 60 * 1000);
            } catch (Exception ex) {
                log.error("Unexpected error during launch", ex);
            }
        }
    }

    /**
     * The event latch.
     * <p>
     * When something related to the plugin initialization happens, we call this with
     * the corresponding type of the event.  We will return {@code true} at most once, when the very last type
     * of event is triggered.  This method has to be {@code synchronized} because {@code EnumSet} is not
     * thread-safe and because we have multiple accesses to {@code lifecycleEvents} that need to happen
     * atomically for correct behaviour.
     * </p>
     *
     * @param event the lifecycle event that occurred
     * @return {@code true} if this completes the set of initialization-related events; {@code false} otherwise
     */
    synchronized private boolean isLifecycleReady(LifecycleEvent event) {
        return lifecycleEvents.add(event) && lifecycleEvents.size() == LifecycleEvent.values().length;
    }


    /**
     * Do all the things we can't do before the system is fully up.
     */
    private void registerJobs() throws Exception
    {
        initActiveObjects();
        registerJobRunner();
        log.info("Zephyr Jobs are registered successfully");
    }



    private void registerListener()
    {
        eventPublisher.register(this);
    }

    private void unregisterListener()
    {
        eventPublisher.unregister(this);
    }

    private void registerJobRunner()
    {
    	//Unregister prior created Job Runner before creating it again
    	unregisterJobRunner();
    	compatibilityPluginScheduler.registerJobHandler(ZFJJobRunner.ZFJ_JOB,zfjJobRunner);
    }

    private void unregisterJobRunner()
    {
    	compatibilityPluginScheduler.unregisterJobHandler(ZFJJobRunner.ZFJ_JOB);
    }

    /**
     * Prod AO to make sure it is really and truly ready to go.  If AO needs to do things like upgrade the
     * schema or if it is going to completely blow up on us, then hopefully that will happen here.  If we
     * don't do this, then AO will do all of these things when we first touch it at some arbitrary other
     * point in the code, meaning that the place where the upgrades, failures, etc. happen might not be
     * deterministic.  Explicitly prodding AO here makes the system more deterministic and therefore easier
     * to troubleshooting.
     */
    private void initActiveObjects()
    {
        ao.flushAll();
    }

    /**
     * Is ZFJ version valid
     * @param version
     * @return
     */
	private Boolean isVersionValid(String version){
	       if(StringUtils.isBlank(version))
	           return false;
	       String []tokens = version.split("\\.");
	       try{
	           return tokens.length == 4 && Integer.parseInt(tokens[3]) > 0;
	       }catch(Exception ex){
	           log.error("Invalid Application version " + version +  " " + ex.getMessage());
	       }
	       return false;
	}
	
	
	/**
	 * Starts version Check job
	 * @param interval
	 */
	private void scheduleVersionJob(long interval) {
//    	final RunMode runMode =  RUN_ONCE_PER_CLUSTER;
    	log.info("Request is currently served by node :" + nodeStateManager.getNode().getNodeId());
//    	JobConfig jobConfig = JobConfig.forJobRunnerKey(JobRunnerKey.of(ConfigurationConstants.JOB_NAME))
//    			.withSchedule(Schedule.forInterval(interval,  new Date(System.currentTimeMillis())))
//    			.withRunMode(runMode);
    	try {
        	//compatibilityPluginScheduler.registerJobHandler(ZFJJobRunner.ZFJ_JOB,zfjJobRunner);
        	compatibilityPluginScheduler.scheduleClusteredJob(ConfigurationConstants.JOB_NAME,ZFJJobRunner.ZFJ_JOB,new Date(System.currentTimeMillis()),interval);
            log.info(String.format("Zephyr version check scheduled to run every %dms", interval));
    	} catch(Exception e) {
			log.warn("Error scheduling job",e);
    	}
    }
	
	
	/**
	 * Stops the Version Job
	 */
	private void unscheduleVersionJob(){
		try{
			compatibilityPluginScheduler.unscheduleClusteredJob(ConfigurationConstants.JOB_NAME);
			compatibilityPluginScheduler.unregisterJobHandler(ZFJJobRunner.ZFJ_JOB);
		}catch(Exception ex){
			log.warn("Error in removing job",ex);
		}
	}
	

	/**
	 * Starts Cluster Messaging Service.
	 */
    public void startMessagingService() {
        messageHandler.start();
    }

	/**
	 * stops Cluster Messaging Service.
	 */
    public void stopMessagingService()
    {
     	messageHandler.stop();
    }
	
	
    /**
     * Used to keep track of everything that needs to happen before we are sure that it is safe
     * to talk to all of the components we need to use, particularly the {@code SchedulerService}
     * and Active Objects.  We will not try to initialize until all of them have happened.
     */
    static enum LifecycleEvent
    {
        AFTER_PROPERTIES_SET,
        PLUGIN_ENABLED,
        LIFECYCLE_AWARE_ON_START
    }
}
