package com.thed.zephyr.je.service;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.security.JiraAuthenticationContext;
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
import com.atlassian.event.api.EventPublisher;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.service.impl.CycleManagerImpl;
import static org.mockito.Mockito.*;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(CycleManagerTest.CycleManagerTestDatabaseUpdater.class)
//@NameConverters(table=TestActiveObjectsTableNameConverter.class, field=TestActiveObjectsFieldNameConverter.class)
public class CycleManagerTest {

	private EntityManager entityManager;
	private ActiveObjects ao;
	private CycleManagerImpl cycleManager;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        
        cycleManager = new CycleManagerImpl(ao, mock(EventPublisher.class),mock(JiraAuthenticationContext.class),mock(JobProgressService.class), mock(ZFJCacheService.class));
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetCycles() {
		Cycle cycle = cycleManager.getCycle(1l);
		assertFalse(cycle.getID() == 0);
        assertNotNull(cycle.getBuild());
        cycle = cycleManager.getCycle(10000l);
        assertNull(cycle);
	}

	@Test
	public void testGetCyclesByVersion() {
		List<Cycle> cycles = cycleManager.getCyclesByVersion(10001l, 10001l,-1);
        assertNotNull(cycles);
        assertTrue(cycles.size() > 0);
	}

	@Test
	public void testSaveCycle() {
		Cycle cycle = cycleManager.saveCycle(getCyclePropertyMap());
        assertFalse(cycle.getID() < 2);
        assertNotNull(cycle.getBuild());
	}

	@Test
	public void testRemoveCycle() {
		Cycle cycle = cycleManager.saveCycle(getCyclePropertyMap());
		int cycleId = cycle.getID();
		assertFalse(cycleId < 2);
		ao.delete(cycle);
		cycle = cycleManager.getCycle(new Long(cycleId));
		assertNull(cycle);
	}

	@Test
	public void testGetCyclesByCriteria() {
		//fail("Not yet implemented");
	}

	@Test
	public void testAddIssuesToCycle() {
		//fail("Not yet implemented");
	}
	
	public static class CycleManagerTestDatabaseUpdater implements DatabaseUpdater
    {
        @Override
        public void update(EntityManager em) throws Exception
        {
            em.migrate(Cycle.class);
            final Cycle cycle = em.create(Cycle.class);
            populateDummyCycleData(cycle);
        }
    }
	
	public static void populateDummyCycleData(Cycle cycle){
		long time = System.currentTimeMillis();
		cycle.setBuild("new build @ " + time);
		cycle.setEnvironment("Env @ " + time);
		cycle.setName("New Name @ " + time );
		cycle.setVersionId(10001l);
		cycle.setProjectId(10001l);
		cycle.setStartDate(new Date(time - 36000000).getTime());
		cycle.setEndDate(new Date(time).getTime());
		cycle.save();
	}
	
	public static Map<String, Object> getCyclePropertyMap(){
		Map<String, Object> cycleProperties = new HashMap<String, Object>(1);
		long time = System.currentTimeMillis();
		cycleProperties.put("BUILD", "new build @ " + time);
		cycleProperties.put("ENVIRONMENT","Env @ " + time);
		cycleProperties.put("NAME","New Name @ " + time );
		cycleProperties.put("VERSION_ID", new Long(10001l));
		cycleProperties.put("PROJECT_ID", new Long(10001l));
		cycleProperties.put("START_DATE", new Date(time - 36000000).getTime());
		cycleProperties.put("END_DATE", new Date(time).getTime());
		return cycleProperties;
	}

}
