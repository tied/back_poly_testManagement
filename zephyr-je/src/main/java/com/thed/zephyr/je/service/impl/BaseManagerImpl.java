package com.thed.zephyr.je.service.impl;

import java.util.Arrays;
import java.util.List;

import net.java.ao.Query;
import net.java.ao.RawEntity;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.thed.zephyr.je.service.BaseManager;

public class BaseManagerImpl implements BaseManager {
	
	public final ActiveObjects ao;
	
	public BaseManagerImpl(ActiveObjects ao) {
		super();
		this.ao = ao;
	}

	public <T extends RawEntity<K>, K> List<T> getEntities(Class<T> clazz, Integer offset, Integer limit) {
		if(limit == null) limit = 50;
		if(offset == null) offset = 0;
		Query query = Query.select();
		if(limit != -1) query.setLimit(limit);
		if(offset != -1) query.setOffset(offset);
		
		T []entities = ao.find(clazz, query);
		return Arrays.asList(entities);
	}

}
