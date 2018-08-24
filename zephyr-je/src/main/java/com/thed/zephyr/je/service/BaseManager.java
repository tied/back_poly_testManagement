package com.thed.zephyr.je.service;

import java.util.List;

import net.java.ao.RawEntity;

public interface BaseManager {
	public <T extends RawEntity<K>, K> List<T> getEntities(Class<T> clazz, Integer offset, Integer limit);
}
