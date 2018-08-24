package com.thed.zephyr.je.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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
import org.springframework.test.annotation.Repeat;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.util.UserManager;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.ScheduleDefect;
import com.thed.zephyr.je.service.impl.CustomFieldValueManagerImpl;
import com.thed.zephyr.je.service.impl.CycleManagerImpl;
import com.thed.zephyr.je.service.impl.FolderManagerImpl;
import com.thed.zephyr.je.service.impl.ScheduleManagerImpl;
import com.thed.zephyr.je.service.impl.StepResultManagerImpl;
import com.thed.zephyr.je.service.impl.ZephyrCustomFieldManagerImpl;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(ScheduleManagerTest.ScheduleManagerTestDatabaseUpdater.class)
//@NameConverters(table=TestActiveObjectsTableNameConverter.class, field=TestActiveObjectsFieldNameConverter.class)
public class ScheduleManagerTest {

	private static final long VERSION_ID = 10001l;
	private static final long PROJECT_ID = 10001l;
	private EntityManager entityManager;
	private ActiveObjects ao;
	private CycleManager cycleManager;
	private ScheduleManagerImpl scheduleManager;
	private StepResultManagerImpl stepResultManager;
	private JobProgressService jobProgressService;
	private FolderManager folderManager;
	private CustomFieldValueManager customFieldValueManager;
	private ZephyrCustomFieldManager zephyrCustomFieldManager;

	private static Cycle cycle;

	@Before
	public void setUp() throws Exception {
		assertNotNull(entityManager);
		ao = new TestActiveObjects(entityManager);
		cycleManager = new CycleManagerImpl(ao, mock(EventPublisher.class),mock(JiraAuthenticationContext.class),mock(JobProgressService.class), mock(ZFJCacheService.class));
		folderManager = new FolderManagerImpl(ao);

		customFieldValueManager = new CustomFieldValueManagerImpl(ao);
		zephyrCustomFieldManager = new ZephyrCustomFieldManagerImpl(ao);
		UserManager userManager = null;
		
        stepResultManager = new StepResultManagerImpl(ao, mock(ProjectManager.class), mock(AttachmentManager.class));
		assertNotNull(stepResultManager);
		scheduleManager = new ScheduleManagerImpl(ao, cycleManager, userManager, null,null, stepResultManager, ((TeststepManager)null), mock(IssueManager.class),
				null,mock(JobProgressService.class),mock(JiraAuthenticationContext.class), folderManager, customFieldValueManager, zephyrCustomFieldManager);

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testScheduleManagerImpl() {
		//fail("Not yet implemented");
	}

	@Test
	@Repeat(1000)
	public void testGetSchedules() {
		List<Schedule> schedules = scheduleManager.getSchedules(null, 0, 50);
		assertNotNull(schedules);
		assertEquals(1, schedules.size());
		assertTrue(schedules.get(0).getID() > 0);
	}

	@Test
	public void testGetSchedule() {
		Schedule schedule = scheduleManager.getSchedule(1);
		assertNotNull(schedule);
		assertTrue(schedule.getID() > 0);
	}

	@Test
	public void testGetSchedulesByCycleId() {
		List<Schedule> schedules = scheduleManager.getSchedulesByCycleId(VERSION_ID, PROJECT_ID, cycle.getID(),-1, /*sortQry*/"", /*Expandos*/"");
		assertNotNull(schedules);
		assertEquals(1, schedules.size());
		assertTrue(schedules.get(0).getID() > 0);
	}

	@Test
	public void testGetSchedulesByIssueId() {
		List<Schedule> schedules = scheduleManager.getSchedulesByIssueId(10001,-1,null);
		assertNotNull(schedules);
		assertEquals(1, schedules.size());
		assertTrue(schedules.get(0).getID() > 0);
	}

	@Test
	public void testGetExecutionDetailsByIssueId() {
		//fail("Not yet implemented");
	}

	@Test
	public void testSaveSchedule() {
		Schedule sch = ao.create(Schedule.class);
		populateDummyScheduleData(sch);
		List<Schedule> schedules = scheduleManager.getSchedulesByCycleId(VERSION_ID, PROJECT_ID, cycle.getID(),-1, /*sortQry*/"", /*Expandos*/"");
		assertNotNull(schedules);
		assertEquals(2, schedules.size());
		assertTrue(schedules.get(1).getID() > 0);
		System.out.println("testing");
	}
	
	@Test
	public void testGetAssociatedDefects(){
		ScheduleDefect sd = ao.create(ScheduleDefect.class);
		sd.setDefectId(10001);
		sd.setScheduleId(1);
		sd.save();
		
		ScheduleDefect [] sds = ao.find(ScheduleDefect.class);
		assertNotNull(sds);
		assertEquals(1, sds.length);
		
		List<ScheduleDefect> sdList = scheduleManager.getAssociatedDefects(1);
		assertNotNull(sdList);
		assertEquals(1, sdList.size());
		assertEquals(new Integer(10001), sdList.get(0).getDefectId());
		
		sdList = scheduleManager.getAssociatedDefects(2);
		assertNotNull(sdList);
		assertEquals(0, sdList.size());
	}

	@Test
	public void testRemoveSchedule() {
		//fail("Not yet implemented");
	}

	@Test
	public void testGetSchedulesByCriteria() {
		//fail("Not yet implemented");
	}

	@Test
	public void testCreateBulkSchedule() {
		//fail("Not yet implemented");
	}

	public static class ScheduleManagerTestDatabaseUpdater implements
			DatabaseUpdater {
		@Override
		public void update(EntityManager em) throws Exception {
			em.migrate(Schedule.class, Cycle.class, ScheduleDefect.class);
			cycle = em.create(Cycle.class);
            populateDummyCycleData(cycle);
			final Schedule schedule = em.create(Schedule.class);
			populateDummyScheduleData(schedule);
		}
	}

	public static void populateDummyScheduleData(Schedule schedule) {
		long time = System.currentTimeMillis();
		schedule.setStatus("1");
		schedule.setComment("This is a new comment");
		schedule.setActualExecutionTime(1500l);
		schedule.setExecutedBy("admin");
		schedule.setIssueId(10001);
		if(cycle != null){
			schedule.setCycle(cycle);
		}
		schedule.save();
	}
	
	public static void populateDummyCycleData(Cycle cycle){
		long time = System.currentTimeMillis();
		cycle.setBuild("new build @ " + time);
		cycle.setEnvironment("Env @ " + time);
		cycle.setName("New Name @ " + time );
		cycle.setVersionId(VERSION_ID);
		cycle.setStartDate(new Date(time - 36000000).getTime());
		cycle.setEndDate(new Date(time).getTime());
		cycle.save();
	}

	public static Map<String, Object> getSchedulePropertyMap() {
		Map<String, Object> scheduleProperties = new HashMap<String, Object>(1);
		long time = System.currentTimeMillis();
		scheduleProperties.put("status", "Pass" + time);
		scheduleProperties.put("comment", "new comment " + time);
		scheduleProperties.put("actualExecutionTime", "New Name @ " + time);
		scheduleProperties.put("executedBy", 1);
		scheduleProperties.put("issueId", 10001);
		scheduleProperties.put("versionId", VERSION_ID);
		scheduleProperties.put("projectId", PROJECT_ID);
		return scheduleProperties;
	}
}
