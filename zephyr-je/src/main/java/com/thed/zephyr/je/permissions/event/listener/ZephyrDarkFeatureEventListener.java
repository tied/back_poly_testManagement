package com.thed.zephyr.je.permissions.event.listener;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.permissions.event.ZephyrDarkFeatureModifiedEvent;
import com.thed.zephyr.je.permissions.service.PermissionConfigManager;
import com.thed.zephyr.util.ConfigurationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Dark Feature Global Permission Event Listener
 */
public class ZephyrDarkFeatureEventListener implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ZephyrDarkFeatureEventListener.class);

    private final EventPublisher eventPublisher;

    private final PermissionConfigManager permissionConfigManager;

    /**
     * Constructor.
     *
     * @param eventPublisher injected {@code EventPublisher} implementation.
     */
    public ZephyrDarkFeatureEventListener(EventPublisher eventPublisher, PermissionConfigManager permissionConfigManager) {
        log.debug("# Initializing ZephyrGlobalPermissionEventListener");
        this.eventPublisher = eventPublisher;
        this.permissionConfigManager = permissionConfigManager;
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

    @EventListener
    public void onPermissionChangeEvent(ZephyrDarkFeatureModifiedEvent zephyrDarkFeatureModifiedEvent) {
        EventType eventType = zephyrDarkFeatureModifiedEvent.getEventType();
        String featureKey = zephyrDarkFeatureModifiedEvent.getFeatureKey();

        switch (eventType) {
            case ZEPHYR_PROJECT_PERMISSION_ENABLED:
                performDarkFeatureUpdateAction(featureKey, true);
                break;
            case ZEPHYR_PROJECT_PERMISSION_DISABLED:
                performDarkFeatureUpdateAction(featureKey, false);
            case ZEPHYR_DARK_FEATURE_ENABLED:
                performDarkFeatureUpdateAction(featureKey, true);
                break;
            case ZEPHYR_DARK_FEATURE_DISABLED:
                performDarkFeatureUpdateAction(featureKey, false);
                break;
        }
    }

    private void performDarkFeatureUpdateAction(String featureKey, Boolean enabling) {
        try {
            switch (featureKey) {
                case ConfigurationConstants.ZEPHYR_PROJECT_PERMISSION_FEATURE_KEY:
                    if (enabling) {
                        permissionConfigManager.addAndConfigurePermissions();
                    } else {
                        permissionConfigManager.removeAndCleanUpPermissions();
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Error enabling/disabling ZEPHYR PROJECT PERMISSION MODULE : ", e);
        }
    }
}
