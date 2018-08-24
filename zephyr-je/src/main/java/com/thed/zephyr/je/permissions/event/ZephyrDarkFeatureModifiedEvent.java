package com.thed.zephyr.je.permissions.event;

import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ZephyrEvent;

import java.util.Map;

/**
 * Dark Feature Global Permission event.
 */
public class ZephyrDarkFeatureModifiedEvent extends ZephyrEvent {

    String userName;
    String featureKey;

    public ZephyrDarkFeatureModifiedEvent(String featureKey, Map<String, Object> params, EventType eventType, String userName) {
        super(params, eventType);
        this.userName = userName;
        this.featureKey = featureKey;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (null != userName ? userName.hashCode() : 0);
        result = 29 * result + (null != featureKey ? featureKey.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ZephyrDarkFeatureModifiedEvent)) {
            return false;
        }

        final ZephyrDarkFeatureModifiedEvent event = (ZephyrDarkFeatureModifiedEvent) o;

        if (null != getParams() ? !getParams().equals(event.getParams()) : event.getParams() != null) {
            return false;
        }
        if (null != eventType ? !eventType.equals(event.eventType) : event.eventType != null) {
            return false;
        }
        if (null != featureKey ? !featureKey.equals(event.featureKey) : event.featureKey != null) {
            return false;
        }
        return true;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }
}
