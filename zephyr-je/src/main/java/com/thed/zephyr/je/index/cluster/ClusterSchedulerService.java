package com.thed.zephyr.je.index.cluster;

import com.atlassian.sal.api.lifecycle.LifecycleAware;

/**
 * Created by niravshah on 7/24/17.
 */
public interface ClusterSchedulerService {
    void reschedule(long deleteInterval,long syncInterval);
}
