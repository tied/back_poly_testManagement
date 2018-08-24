package com.thed.zephyr.je.zql.service;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.thed.zephyr.je.zql.model.ZQLFilter;
import com.thed.zephyr.je.zql.model.ZQLSharePermissions;
import com.thed.zephyr.je.zql.model.ZQLFavoriteAsoc;
import com.thed.zephyr.je.zql.service.impl.ZQLFilterManagerImpl;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(ZQLFilterManagerTest.ZQLFilterManagerTestDatabaseUpdater.class)
public class ZQLFilterManagerTest {
	
	private EntityManager entityManager;
	private ActiveObjects ao;
	private ZQLFilterManager zqlFilterManager;
	private static ZQLFilter zqlFilter;	
	private static ZQLFavoriteAsoc zqlFavoriteAsoc;
	private static ZQLSharePermissions zqlSharePermissions;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(entityManager);
		ao = new TestActiveObjects(entityManager);
		zqlFilterManager = new ZQLFilterManagerImpl(ao);
		assertNotNull(zqlFilterManager);
	}

	@After
	public void tearDown() throws Exception {
	}	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSaveZQLFilter(){
		ao.migrate(ZQLFilter.class);
		ZQLFilter zqlFilter = ao.create(ZQLFilter.class);
		populateDummyZQLFilterData(zqlFilter);
		ZQLFilter zqlFilter1 = zqlFilterManager.getZQLFilter(zqlFilter.getID());
		assertNotNull(zqlFilter1);
		assertTrue(zqlFilter1.getID() == zqlFilter.getID());
		
		ao.migrate(ZQLFavoriteAsoc.class);
		ZQLFavoriteAsoc zqlFavoriteAsoc = ao.create(ZQLFavoriteAsoc.class);
		populateDummyZQLFavAsocData(zqlFilter, zqlFavoriteAsoc);
		ZQLFavoriteAsoc zqlFavoriteAsoc1 = zqlFilterManager.getZQLFavoriteAsoc(zqlFilter, zqlFilter.getCreatedBy())[0];
		assertNotNull(zqlFavoriteAsoc1);
		assertTrue(zqlFavoriteAsoc1.getID() == zqlFavoriteAsoc.getID());
		
		ao.migrate(ZQLSharePermissions.class);		
		ZQLSharePermissions zqlSharePermissions = ao.create(ZQLSharePermissions.class);
		populateDummyZQLSharePermissions(zqlFilter, zqlSharePermissions);
		ZQLSharePermissions zqlSharePermissions1 = zqlFilterManager.getZQLSharePermissions(zqlFilter);
		assertNotNull(zqlSharePermissions1);
		assertTrue(zqlSharePermissions1.getID() == zqlSharePermissions.getID());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetAllZQLFiltersByUser(){
		ZQLFilter favFilter = ao.create(ZQLFilter.class);
		favFilter.setCreatedBy("dummy");
		favFilter.save();
		Map<String, Object> filtersMap = zqlFilterManager.getAllZQLFiltersByUser(favFilter.getCreatedBy(), 0, 20);	
		assertNotNull(filtersMap);
		List<ZQLFilter> zqlFilters = (List<ZQLFilter>)filtersMap.get("zqlFilters");
		assertNotNull(zqlFilters);
		assertTrue("All Favorite filters by users - ", zqlFilters.size() > 0);	
	}	
	
	@Test
	public void testGetZQLFilter(){
		ZQLFilter filter = zqlFilterManager.getZQLFilter(zqlFilter.getID());
		assertNotNull(filter);
		assertTrue(filter.getID() != 0);
	}
		
	@SuppressWarnings("unchecked")
	@Test
	public void testRemoveZQLFilter(){		
		ZQLFilter zqlFilter = ao.create(ZQLFilter.class);
		populateDummyZQLFilterData(zqlFilter);
		
		ZQLSharePermissions zqlSharePermissions = ao.create(ZQLSharePermissions.class);
		populateDummyZQLSharePermissions(zqlFilter, zqlSharePermissions);
		
		ao.migrate(ZQLFavoriteAsoc.class);
		ZQLFavoriteAsoc zqlFavoriteAsoc = ao.create(ZQLFavoriteAsoc.class);
		populateDummyZQLFavAsocData(zqlFilter, zqlFavoriteAsoc);
		
//		zqlFilterManager.removeZQLFilter(zqlFilter.getID());
//		ZQLFilter zqlFilter1 = zqlFilterManager.getZQLFilter(zqlFilter.getID());
//		org.junit.Assert.assertNull(zqlFilter1);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateZQLFilter(){		
		ZQLFilter filter = ao.create(ZQLFilter.class);
		filter.setFilterName("abc");
		filter.setCreatedBy("dummy");		
		filter.setCreatedOn(System.currentTimeMillis());
		filter.setUpdatedOn(System.currentTimeMillis());
		filter.save();
		Map<String, Object> zqlFilterProperties = new HashMap<String, Object>();
		zqlFilterProperties.put("ID", filter.getID());
		zqlFilterProperties.put("FILTER_NAME", "xyz");
		zqlFilterProperties.put("UPDATED_ON", System.currentTimeMillis());
		zqlFilterProperties.put("UPDATED_BY", "dummy2");
		zqlFilterProperties.put("FAVORITE", false);
		ao.migrate(ZQLFavoriteAsoc.class);
		
		ZQLFilter filter2 = zqlFilterManager.updateZQLFilter(zqlFilterProperties);
		assertNotNull(filter2);
		assertTrue(filter2.getID() == filter.getID());
		assertTrue(filter2.getFilterName().equals("xyz"));
		assertTrue(filter2.getUpdatedBy().equals("dummy2"));
		assertTrue(filter2.getUpdatedOn() >= filter.getUpdatedOn());
	}	
	
	public static class ZQLFilterManagerTestDatabaseUpdater implements
			DatabaseUpdater {
		@SuppressWarnings("unchecked")
		@Override
		public void update(EntityManager em) throws Exception {
			em.migrate(ZQLFilter.class);
			zqlFilter = em.create(ZQLFilter.class);
			populateDummyZQLFilterData(zqlFilter);
			em.flushAll();
			
			em.migrate(ZQLFavoriteAsoc.class);
			zqlFavoriteAsoc = em.create(ZQLFavoriteAsoc.class);
			populateDummyZQLFavAsocData(zqlFilter, zqlFavoriteAsoc);
			em.flushAll();
			
			em.migrate(ZQLSharePermissions.class);
			zqlSharePermissions = em.create(ZQLSharePermissions.class);
			populateDummyZQLSharePermissions(zqlFilter, zqlSharePermissions);
			em.flushAll();
		}
	}
	
	public static void populateDummyZQLFilterData(ZQLFilter zqlFilter){
		long time = System.currentTimeMillis();
		zqlFilter.setFilterName("New Name @ " + time);
		zqlFilter.setZqlQuery("New Query @ " + time);
		zqlFilter.setDescription("New Desc @ " + time );
		zqlFilter.setCreatedBy("dummy");
		zqlFilter.setCreatedOn(new Date(time).getTime());
		zqlFilter.setUpdatedBy(String.valueOf(time));
		zqlFilter.setUpdatedOn(new Date(time).getTime());
		zqlFilter.save();
	}
	public static void populateDummyZQLFavAsocData(ZQLFilter zqlFilter, ZQLFavoriteAsoc zqlFavoriteAsoc){
		zqlFavoriteAsoc.setUser(zqlFilter.getCreatedBy());
		zqlFavoriteAsoc.setZQLFilter(zqlFilter);
		zqlFavoriteAsoc.save();
	}
	public static void populateDummyZQLSharePermissions(ZQLFilter zqlFilter, ZQLSharePermissions zqlSharePermissions){
		zqlSharePermissions.setShareType("global");
		zqlSharePermissions.setZQLFilter(zqlFilter);
		zqlSharePermissions.setParam1("param1");
		zqlSharePermissions.setParam2("param2");
		zqlSharePermissions.save();
	}	
}
