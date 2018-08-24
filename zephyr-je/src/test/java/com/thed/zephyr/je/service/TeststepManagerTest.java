package com.thed.zephyr.je.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import com.atlassian.beehive.compat.*;
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
import com.atlassian.jira.project.ProjectManager;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.service.impl.StepResultManagerImpl;
import com.thed.zephyr.je.service.impl.TeststepManagerImpl;

import javax.annotation.Nonnull;

import static org.mockito.Mockito.mock;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(TeststepManagerTest.TeststepManagerTestDatabaseUpdater.class)
//@NameConverters(table=TestActiveObjectsTableNameConverter.class, field=TestActiveObjectsFieldNameConverter.class)
public class TeststepManagerTest {

	private EntityManager entityManager;
	private ActiveObjects ao;
	private TeststepManagerImpl testManager;
	private StepResultManagerImpl stepResultManager;
	private ClusterLockServiceFactory clusterLockServiceFactory;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);

        stepResultManager = new StepResultManagerImpl(ao, mock(ProjectManager.class), mock(AttachmentManager.class));
		clusterLockServiceFactory = new ClusterLockServiceFactory(new ClusterLockServiceAccessor() {
			@Override
			public ClusterLockService getClusterLockService() {
				return new ClusterLockService() {
					@Override
					public ClusterLock getLockForName(@Nonnull String s) {
						return new ClusterLock() {
							@Nonnull
							@Override
							public Condition newCondition() {
								return null;
							}

							@Override
							public void lock() {

							}

							@Override
							public void lockInterruptibly() throws InterruptedException {

							}

							@Override
							public boolean tryLock() {
								return true;
							}

							@Override
							public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
								return true;
							}

							@Override
							public void unlock() {

							}
						};
					}
				};
			}
		});
		assertNotNull(stepResultManager);

		testManager = new TeststepManagerImpl(ao, stepResultManager, clusterLockServiceFactory);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testTeststepManagerImpl() {
		//fail("Not yet implemented");
	}

	@Test
	public void testGetTeststeps() {
		List<Teststep> steps = testManager.getTeststeps(10001l, Optional.empty(), Optional.empty());
        assertNotNull(steps);
        assertTrue(steps.size() > 0);
        assertEquals(steps.get(0).getOrderId().intValue(), 1);
	}

	@Test
	public void testCopySteps() {
		//fail("Not yet implemented");
	}

	@Test
	public void testCopyStepsInBulk() {
		//fail("Not yet implemented");
	}

	@Test
	public void testRemoveTeststep() {
		//fail("Not yet implemented");
	}

	@Test
	public void testSaveTeststeps() {
		Teststep ts = ao.create(Teststep.class, getTeststepPropertyMap());
		assertNotNull(ts);
		assertTrue("", ts.getID() > 0);
		ts.setIssueId(10002l);
		ts.setResult("This is a BLOB");
		ts.save();
		List<Teststep> afterSave = testManager.getTeststeps(10002l, Optional.empty(), Optional.empty());
		assertNotNull(afterSave);
		assertTrue("", afterSave.size() > 0);
		assertEquals("This is a BLOB", afterSave.get(0).getResult());
	}
	
	public static class TeststepManagerTestDatabaseUpdater implements DatabaseUpdater
    {
        @Override
        public void update(EntityManager em) throws Exception
        {
            em.migrate(Teststep.class);
            final Teststep step = em.create(Teststep.class);
            populateDummyCycleData(step);
            step.save();
        }
    }
	
	public static void populateDummyCycleData(Teststep step){
		long time = System.currentTimeMillis();
		step.setOrderId(1);
		step.setStep("Env @ " + time);
		step.setData("New Name @ " + time );
		step.setIssueId(10001l);
		step.setResult("This testcase should pass the test");
	}
	
	public static Map<String, Object> getTeststepPropertyMap(){
		Map<String, Object> stepProperties = new HashMap<String, Object>(1);
		long time = System.currentTimeMillis();
		stepProperties.put("ORDER_ID", new Integer(1));
		stepProperties.put("STEP","Env @ " + time);
		stepProperties.put("DATA","New Name @ " + time );
		stepProperties.put("RESULT", "Something " + time);
		stepProperties.put("ISSUE_ID", 10001l);
		return stepProperties;
	}

}
