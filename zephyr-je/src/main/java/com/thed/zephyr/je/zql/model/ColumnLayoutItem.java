package com.thed.zephyr.je.zql.model;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface ColumnLayoutItem extends Entity {
	public void setZFJColumnLayout(ZFJColumnLayout zfjColumnLayout);
	public ZFJColumnLayout getZFJColumnLayout();

	public String getFieldIdentifier();
	public void setFieldIdentifier(String fieldIdentifier);
	
	public Integer getOrderId();
	public void setOrderId(Integer orderId);

    public Integer getCustomFieldId();
    public void setCustomFieldId(Integer customFieldId);
}
