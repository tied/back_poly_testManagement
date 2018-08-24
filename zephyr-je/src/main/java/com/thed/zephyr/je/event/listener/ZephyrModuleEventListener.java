package com.thed.zephyr.je.event.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.thed.zephyr.util.ConfigurationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Created with IntelliJ IDEA.
 * Author: mukul
 * Zephyr ModuleEventListener
 */
public class ZephyrModuleEventListener implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ZephyrModuleEventListener.class);
    private final EventPublisher eventPublisher;

    /**
     * Constructor.
     *
     * @param eventPublisher injected {@code EventPublisher} implementation.
     */
    public ZephyrModuleEventListener(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Called when the plugin has been enabled.
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // register ourselves with the EventPublisher
        eventPublisher.register(this);
    }

    /**
     * Called when the plugin is being disabled or removed.
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        // unregister ourselves with the EventPublisher
        eventPublisher.unregister(this);
    }

    /**
     * Receives any {@code PluginModuleDisabledEvent}s sent by JIRA.
     *
     * @param pluginModuleDisabledEvent the PluginModuleDisabledEvent passed to us
     */
    @EventListener
    public void onPluginModuleDisabledEvent(PluginModuleDisabledEvent pluginModuleDisabledEvent) {
        ModuleDescriptor module = pluginModuleDisabledEvent.getModule();
        String completeKey = module.getCompleteKey();
        if (ConfigurationConstants.ZEPHYR_REST_FILTER_MODULE_KEY.equals(completeKey)) {
            try {
                boolean zephyrRestFilterModuleEnabled = ComponentAccessor.getPluginAccessor().isPluginModuleEnabled(completeKey);
                if (!zephyrRestFilterModuleEnabled)
                    ComponentAccessor.getPluginController().enablePluginModule(completeKey);
            } catch (Exception e) {
                log.error("Error enabling Zephyr Rest Filter", e);
            }
        }
    }
}