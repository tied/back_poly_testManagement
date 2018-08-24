package com.thed.zephyr.je.zql.model;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface ZFJColumnLayout extends Entity {

	public String getUserName() ;
    public void setUserName(String name);
	
    public ZQLFilter getZQLFilter();
    public void setZQLFilter(ZQLFilter zqlFilter);
}
