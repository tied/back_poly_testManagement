package com.thed.zephyr.je.service;

import com.thed.zephyr.je.vo.ColumnLayoutBean;
import com.thed.zephyr.je.zql.model.ColumnLayoutItem;
import com.thed.zephyr.je.zql.model.ZFJColumnLayout;

import java.util.List;

public interface ColumnLayoutItemManager {
	List<ColumnLayoutItem> getColumnLayoutItems(String userKey,Integer zqlFilterId);
	ZFJColumnLayout saveZFJColumnLayoutItem(String userKey,Integer zqlFilterId, ColumnLayoutBean columnLayout);
	ZFJColumnLayout updateZFJColumnLayoutItem(Integer id, String userKey, Integer zqlFitlerId, ColumnLayoutBean columnLayoutBean);

    /**
     * Check for a given user if he has the right permissions on given ZQLFilter.
     * @param zqlFilterId
     * @param userKey
     * @return boolean
     */
    boolean havePermissionOnZqlFilter(Integer zqlFilterId, String userKey);
}
