package com.thed.zephyr.je.config;

import java.util.List;
import java.util.Set;

/**
 * Created by mukul on 7/8/15.
 */
public interface ZephyrFeatureManager {

    boolean isEnabled(String featureKey);

    List<String> getEnabledFeatureKeys();

    List<String> getSystemEnabledFeatureKeys();

    boolean isOnDemand();

    void enableSiteDarkFeature(String featureKey);

    void disableSiteDarkFeature(String featureKey);

    boolean hasSiteEditPermission();
}
