package com.thed.zephyr.je.index.cluster;

import org.apache.log4j.Logger;

/**
 * Created by niravshah on 7/24/17.
 */
public class ExecClusterIndexCleanServiceImpl implements ExecClusterIndexCleanService {
    private static final Logger log = Logger.getLogger(ExecClusterIndexCleanServiceImpl.class);
    private final ZFJClusterMessageStore zfjClusterMessageStore;


    public ExecClusterIndexCleanServiceImpl(ZFJClusterMessageStore zfjClusterMessageStore) {
        this.zfjClusterMessageStore=zfjClusterMessageStore;
    }

    @Override
    public void deleteMessages() {
        log.debug("Triggered Message Deletion...");
        zfjClusterMessageStore.deleteMessage();
    }
}
