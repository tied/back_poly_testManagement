package com.thed.zephyr.je.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.thed.zephyr.je.service.ColumnLayoutItemManager;
import com.thed.zephyr.je.vo.ColumnItemLayoutBean;
import com.thed.zephyr.je.vo.ColumnLayoutBean;
import com.thed.zephyr.je.zql.model.ColumnLayoutItem;
import com.thed.zephyr.je.zql.model.ZFJColumnLayout;
import com.thed.zephyr.je.zql.model.ZQLFilter;
import com.thed.zephyr.je.zql.model.ZQLSharePermissions;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class ColumnLayoutItemManagerImpl extends BaseManagerImpl implements  ColumnLayoutItemManager {

	public ColumnLayoutItemManagerImpl(ActiveObjects ao) {
		super(checkNotNull(ao));
	}

	@Override
	public List<ColumnLayoutItem> getColumnLayoutItems(String userKey,Integer zqlFilterId) {
		List<ColumnLayoutItem> columnLayoutItems = new ArrayList<ColumnLayoutItem>();
		ZFJColumnLayout columnLayout = null;
		if(zqlFilterId == null || zqlFilterId <= 0) {
			ZFJColumnLayout[] columnLayouts =  ao.find(ZFJColumnLayout.class, Query.select().where("USER_NAME = ? AND ZQLFILTER_ID IS NULL", userKey));
			if(columnLayouts != null && columnLayouts.length > 0) {
				columnLayout = columnLayouts[0];
			}
		} else {
			//PostGres needs a space in between where clause,= sign and ?
			ZFJColumnLayout[] columnLayouts =  ao.find(ZFJColumnLayout.class, Query.select().where("USER_NAME = ? AND ZQLFILTER_ID = ?", userKey,zqlFilterId));
			if(columnLayouts != null && columnLayouts.length > 0) {
				columnLayout = columnLayouts[0];
			}
		}
		
		if(columnLayout != null) {
			ColumnLayoutItem[] arrColumnLayoutItems =  ao.find(ColumnLayoutItem.class, Query.select().where("ZFJCOLUMN_LAYOUT_ID = ?", columnLayout.getID()));
			CollectionUtils.addAll(columnLayoutItems, arrColumnLayoutItems);
		}
		return columnLayoutItems;
	}
	
	
	@Override
	public ZFJColumnLayout saveZFJColumnLayoutItem(final String userKey,final Integer zqlFilterId, final ColumnLayoutBean columnLayoutBean) {
		if(columnLayoutBean == null){
			throw new RuntimeException("Unable to save Column Selector with empty data");
		}
		
		final ZFJColumnLayout columnLayout = ao.executeInTransaction(new TransactionCallback<ZFJColumnLayout>() {
			Map<String, Object> params = new HashMap<String, Object>();
			@Override
			public ZFJColumnLayout doInTransaction() {
				final Map<String,Object> columnLayoutProps = createZFJColumnLayout(columnLayoutBean,userKey,zqlFilterId);
				ZFJColumnLayout zfjColumnLayout =  ao.create(ZFJColumnLayout.class,columnLayoutProps);
				if(zfjColumnLayout == null) {
					throw new RuntimeException("Unable to save Column Selector");
				}
				for(ColumnItemLayoutBean columnItemBean : columnLayoutBean.getColumnItemBean()) {
					params.clear();
					if(columnItemBean.isVisible()) {
						if(columnItemBean.getId() != null) {
							params.put("ID", columnItemBean.getId());
						}
						params.put("FIELD_IDENTIFIER", columnItemBean.getFilterIdentifier());
						params.put("ZFJCOLUMN_LAYOUT_ID", zfjColumnLayout.getID());
						params.put("ORDER_ID", columnItemBean.getOrderId());
						if(null != columnItemBean.getCustomFieldId()) {
							params.put("CUSTOM_FIELD_ID",columnItemBean.getCustomFieldId());
						}
						ao.create(ColumnLayoutItem.class, params);
					}
				}
				return zfjColumnLayout;
			}
		});
		return columnLayout;
	}
	
	
	@Override
	public ZFJColumnLayout updateZFJColumnLayoutItem(final Integer columnLayoutId, final String userKey,final Integer zqlFilterId, final ColumnLayoutBean columnLayoutBean) {
		if(columnLayoutBean == null || columnLayoutId == null) {
			return null;
		}
		
		final ZFJColumnLayout columnLayout = ao.executeInTransaction(new TransactionCallback<ZFJColumnLayout>() {
			Map<String, Object> params = new HashMap<String, Object>();
			@Override
			public ZFJColumnLayout doInTransaction() {
				ZFJColumnLayout zfjColumnLayout =  ao.get(ZFJColumnLayout.class,columnLayoutId);
				if(zfjColumnLayout == null) {
					throw new RuntimeException("Unable to save Column Selector");
				}
				ColumnLayoutItem[] arrColumnLayoutItems =  ao.find(ColumnLayoutItem.class, Query.select().where("ZFJCOLUMN_LAYOUT_ID = ?", columnLayoutId));
				Map<Integer, ColumnLayoutItem> existingItems = new HashMap<>();
				Map<String, ColumnLayoutItem> existingItemsByName = new HashMap<>();
				if(arrColumnLayoutItems != null && arrColumnLayoutItems.length > 0){
					for(ColumnLayoutItem cli : arrColumnLayoutItems){
						existingItems.put(cli.getID(), cli);
						existingItemsByName.put(cli.getFieldIdentifier(), cli);
					}
				}
				//ao.delete(arrColumnLayoutItems);
				for(ColumnItemLayoutBean columnItemBean : columnLayoutBean.getColumnItemBean()){
					params.clear();
					ColumnLayoutItem newItem = existingItems.get(columnItemBean.getId());
					if(newItem == null){
						//Try to get by using name
						newItem = existingItemsByName.get(columnItemBean.getFilterIdentifier());
					}
					if(columnItemBean.isVisible()) {
						if(newItem != null){
							newItem.setFieldIdentifier(columnItemBean.getFilterIdentifier());
							newItem.setZFJColumnLayout(zfjColumnLayout);
							newItem.setOrderId(columnItemBean.getOrderId());
							if(null != columnItemBean.getCustomFieldId()) {
								newItem.setCustomFieldId(columnItemBean.getCustomFieldId());
							}
							newItem.save();
						}else{
							//Below won't be required as the object might have identified in the above query.
							if(columnItemBean.getId() != null) {
								params.put("ID", columnItemBean.getId());
							}
							params.put("FIELD_IDENTIFIER", columnItemBean.getFilterIdentifier());
							params.put("ZFJCOLUMN_LAYOUT_ID", zfjColumnLayout.getID());
							params.put("ORDER_ID", columnItemBean.getOrderId());
							if(null != columnItemBean.getCustomFieldId()) {
							    params.put("CUSTOM_FIELD_ID",columnItemBean.getCustomFieldId());
	                        }
							ao.create(ColumnLayoutItem.class, params);
						}
					}else{
						if(newItem != null){
							ao.delete(newItem);
						}
					}
				}
				//For restroring defaults, we need to delete the custom field related settings.
				if(arrColumnLayoutItems != null && arrColumnLayoutItems.length > 0){
					for(ColumnLayoutItem cli : arrColumnLayoutItems){
						boolean found = false;
						for(ColumnItemLayoutBean columnItemBean : columnLayoutBean.getColumnItemBean()){
							if(StringUtils.equalsIgnoreCase(cli.getFieldIdentifier(), columnItemBean.getFilterIdentifier())){
								found = true;
								break;
							}
						}
						if(!found){
							ao.delete(cli);
						}
					}
				}
				return zfjColumnLayout;
			}
		});
		return columnLayout;
	}
	
	private Map<String,Object> createZFJColumnLayout(ColumnLayoutBean columnLayoutBean, String userKey, Integer zqlFilterId) {
		Map<String,Object> columnLayoutProps = new HashMap<String, Object>();
		if(columnLayoutBean.getId() != null) {
			columnLayoutProps.put("ID", columnLayoutBean.getId());
		}
		columnLayoutProps.put("USER_NAME", userKey);
		if(zqlFilterId != null && zqlFilterId > 0 ) {
			columnLayoutProps.put("ZQLFILTER_ID", zqlFilterId);
		}
		return columnLayoutProps;
	}

    @Override
    public boolean havePermissionOnZqlFilter(Integer zqlFilterId, String userKey) {
        return (ao.find(ZQLFilter.class, Query.select().where("ID = ?  AND CREATED_BY = ?", zqlFilterId, userKey)).length > 0 ||
                ao.find(ZQLSharePermissions.class, Query.select().where("ZQLFILTER_ID = ? AND SHARE_TYPE = ?", zqlFilterId, "global")).length > 0);
    }
}
