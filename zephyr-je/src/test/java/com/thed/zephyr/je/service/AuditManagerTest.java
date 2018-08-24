package com.thed.zephyr.je.service;

/**
 * Created with IntelliJ IDEA.
 * User: mukul
 * Date: 11/4/13
 * Time: 3:24 PM
 */

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.jira.issue.IssueManager;
import com.thed.zephyr.je.audit.model.ChangeZJEGroup;
import com.thed.zephyr.je.audit.model.ChangeZJEItem;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.audit.service.impl.AuditManagerImpl;
import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;


@RunWith(ActiveObjectsJUnitRunner.class)
@Data(AuditManagerTest.AuditManagerTestDatabaseUpdater.class)
public class AuditManagerTest {

    private EntityManager entityManager;
    private ActiveObjects ao;
    private AuditManager auditManager;
    private static ChangeZJEGroup changeZJEGroup;
    private static ChangeZJEItem changeZJEItem;

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        auditManager = new AuditManagerImpl(ao,mock(IssueManager.class));
        assertNotNull(auditManager);
    }

    @After
    public void tearDown() throws Exception {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetAllAuditLogs(){
        List<ChangeZJEItem> changeItems = auditManager.getZephyrChangeLogs(new HashMap<String, Object>(), 0, 1);
        assertNotNull(changeItems);
        for(ChangeZJEItem item : changeItems){
            assertNotNull(item);
            assertNotNull(item.getChangeZJEGroup());
            assertTrue(item.getID() > 0);
            assertTrue(item.getChangeZJEGroup().getID() > 0);
        }
    }



    public static class AuditManagerTestDatabaseUpdater implements
            DatabaseUpdater {
        @SuppressWarnings("unchecked")
        @Override
        public void update(EntityManager em) throws Exception {
            em.migrate(ChangeZJEGroup.class);
            changeZJEGroup = em.create(ChangeZJEGroup.class);
            populateDummyChangeGroupData(changeZJEGroup);
            em.flushAll();

            em.migrate(ChangeZJEItem.class);
            changeZJEItem = em.create(ChangeZJEItem.class);
            populateDummyChangeItemData(changeZJEGroup, changeZJEItem);
            em.flushAll();
        }
    }

    public static void populateDummyChangeGroupData(ChangeZJEGroup changeZJEGroup){
        long time = System.currentTimeMillis();
        changeZJEGroup.setAuthor("admin");
        changeZJEGroup.setCreated(time);
        changeZJEGroup.setCycleId(1);
        changeZJEGroup.setIssueId(10003);
        changeZJEGroup.setScheduleId(2);
        changeZJEGroup.setZephyrEntityType("CYCLE");
        changeZJEGroup.setZephyrEntityEvent("CYCLE_UPDATED");
        changeZJEGroup.setZephyrEntityId(1);
        changeZJEGroup.save();
    }

    public static void populateDummyChangeItemData(ChangeZJEGroup changeZJEGroup, ChangeZJEItem changeZJEItem){
        changeZJEItem.setChangeZJEGroup(changeZJEGroup);
        changeZJEItem.setZephyrField("DESCRIPTION");
        changeZJEItem.setOldValue("");
        changeZJEItem.setNewValue("New Desc");
        changeZJEItem.setZephyrFieldType("zephyr");
        changeZJEItem.save();
    }
}
