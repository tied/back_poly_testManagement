package com.thed.zephyr.je.config;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.util.concurrent.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.permissions.event.ZephyrDarkFeatureModifiedEvent;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static com.atlassian.jira.util.dbc.Assertions.notNull;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * ZephyrFeatureManagerImpl to enable/diasble Zephyr Dark Features
 */
public class ZephyrFeatureManagerImpl implements ZephyrFeatureManager {

    private static final Logger log = LoggerFactory.getLogger(ZephyrFeatureManagerImpl.class);
    private static final String CORE_FEATURES_RESOURCE = "zephyr-features.properties";

//    private static final Function<String, InputStream> APP_CLASS_LOADER = new Function<String, InputStream>() {
//        @Override
//        public InputStream apply(@Nullable String name) {
//            return ZephyrFeatureManagerImpl.class.getClassLoader().getResourceAsStream(name);
//        }
//    };

    private final JiraAuthenticationContext authenticationContext;
    private final EventPublisher eventPublisher;
    private final PermissionManager permissionManager;

    private static Function<String, InputStream> pluginLoader(final Plugin plugin) {
        return new Function<String, InputStream>() {
            @Override
            public InputStream apply(@Nullable String name) {
                return plugin.getResourceAsStream(name);
            }
        };
    }

    public ZephyrFeatureManagerImpl(EventPublisher eventPublisher, PermissionManager permissionManager, JiraAuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        this.eventPublisher = eventPublisher;
        this.permissionManager = permissionManager;
    }

    private static Properties loadPluginFeatureProperties() {
        final PluginAccessor pluginAccessor = ComponentAccessor.getPluginAccessor();
        Plugin plugin = pluginAccessor.getEnabledPlugin(ApplicationConstants.ZFJ_PLUGIN_KEY);
        return loadProperties(CORE_FEATURES_RESOURCE, pluginLoader(plugin));
    }

    private static Properties loadProperties(String path, Function<String, InputStream> loader) {
        final InputStream propsStream = notNull(String.format("Resource %s not found", path), loader.apply(path));
        try {
            final Properties props = new Properties();
            props.load(propsStream);
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load properties from " + path, e);
        } finally {
            closeQuietly(propsStream);
        }
    }

    @Override
    public boolean isEnabled(String featureKey) {
        Boolean isEnabled;
        String val = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getText(featureKey);
        isEnabled = Boolean.valueOf(val);
        return isEnabled;
    }

    @Override
    public List<String> getEnabledFeatureKeys() {
        Collection<String> features = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getKeys();

        Collection<String> enabledFeatures = Collections2.filter(features, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                Boolean isApplicable = Boolean.FALSE;
                if (StringUtils.startsWith(input, ConfigurationConstants.ZEPHYR_DARK_FEATURE_PREFIX)) {
                    String val = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getText(input);
                    isApplicable = Boolean.valueOf(val) ? true : false;
                }
                return isApplicable;
            }
        });
        return Lists.newArrayList(enabledFeatures);
    }

    @Override
    public List<String> getSystemEnabledFeatureKeys() {
        return Lists.newArrayList(loadPluginFeatureProperties().stringPropertyNames());
    }

    @Override
    public boolean isOnDemand() {
        return false;
    }

    @Override
    public void enableSiteDarkFeature(String featureKey) {
        if (validateDarkFeatureKey(featureKey)) {
            JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).setString(featureKey, "true");
            EventType eventType = getEventTypeByFeatureKey(featureKey, true);
            eventPublisher.publish(new ZephyrDarkFeatureModifiedEvent(featureKey, null, eventType, authenticationContext.getLoggedInUser().getName()));
        }
    }

    @Override
    public void disableSiteDarkFeature(String featureKey) {
        if (validateDarkFeatureKey(featureKey)) {
            JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).setString(featureKey, "false");
            EventType eventType = getEventTypeByFeatureKey(featureKey, false);
            eventPublisher.publish(new ZephyrDarkFeatureModifiedEvent(featureKey, null, eventType, authenticationContext.getLoggedInUser().getName()));
        }
    }

    private EventType getEventTypeByFeatureKey(String featureKey, Boolean enabling) {
        EventType eventType = null;
        switch (featureKey) {
            case ConfigurationConstants.ZEPHYR_PROJECT_PERMISSION_FEATURE_KEY:
                eventType = enabling ? EventType.ZEPHYR_PROJECT_PERMISSION_ENABLED : EventType.ZEPHYR_PROJECT_PERMISSION_DISABLED;
                break;
            /*Add overall darkFeature key - event here */
//            case ConfigurationConstants.ZEPHYR_PROJECT_PERMISSION_FEATURE_KEY:
//                eventType = enabling ? EventType.ZEPHYR_PROJECT_PERMISSION_ENABLED : EventType.ZEPHYR_PROJECT_PERMISSION_DISABLED;
//                break;
        }
        return eventType;
    }

    private Boolean validateDarkFeatureKey(String featureKey) {
        if (StringUtils.isBlank(featureKey))
            return false;

        if (CollectionUtils.isEmpty(getSystemEnabledFeatureKeys()))
            return false;

        if (!getSystemEnabledFeatureKeys().contains(StringUtils.trim(featureKey)))
            return false;

        return true;
    }

    @Override
    public boolean hasSiteEditPermission() {
        ApplicationUser loggedInUser = this.authenticationContext.getUser();
        return permissionManager.hasPermission(0, loggedInUser);
    }
}
