package com.thed.zephyr.je.zql.model;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface ZQLFavoriteAsoc extends Entity {
	
    public ZQLFilter getZQLFilter();
    public void setZQLFilter(ZQLFilter zqlFilter);
    
    public String getUser();
    public void setUser(String user);
    
}