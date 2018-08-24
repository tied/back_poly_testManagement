package com.thed.zephyr.je.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.test.TestActiveObjects;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.FolderCycleMapping;
import com.thed.zephyr.je.service.impl.FolderManagerImpl;

import net.java.ao.EntityManager;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import net.java.ao.test.junit.ActiveObjectsJUnitRunner;

@RunWith(ActiveObjectsJUnitRunner.class)
@Data(FolderManagerTest.FolderManagerTestDatabaseUpdater.class)
public class FolderManagerTest {
	
	private EntityManager entityManager;
	private ActiveObjects ao;
	private FolderManagerImpl folderManager;
	private static final long VERSION_ID = 10001l;
	private static final long PROJECT_ID = 10001l;
	
	private static Cycle cycle;
	
	private static Folder folder;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(entityManager);
        ao = new TestActiveObjects(entityManager);
        
        folderManager = new FolderManagerImpl(ao);
	}	
	
	@Test
	public void testSaveFolder() {
		Folder folder1 = folderManager.saveFolder(getFolderProperties("testFolder", "test folder"));
        assertNotNull(folder1);
        FolderCycleMapping mapping = folderManager.saveFolderCycleMapping(getFolderCycleMappingProperties(folder1));
        assertNotNull(mapping);
	}
	
	@Test
	public void testUpdateFolder() {
		Folder folder1 = folderManager.saveFolder(getFolderProperties("testFolder", "test folder"));
        assertNotNull(folder1);
        FolderCycleMapping mapping = folderManager.saveFolderCycleMapping(getFolderCycleMappingProperties(folder1));
        assertNotNull(mapping);
		folderManager.updateFolder(folder1, "updatedfolder", "Updated folder", "dummyuser", null);
	}
	
	@Test
	public void testGetFoldersCount() {
		Folder folder1 = folderManager.saveFolder(getFolderProperties("testFolder", "test folder"));
        assertNotNull(folder1);
        FolderCycleMapping mapping = folderManager.saveFolderCycleMapping(getFolderCycleMappingProperties(folder1));
        assertNotNull(mapping);
		int count = folderManager.getFoldersCountForCycle(PROJECT_ID, VERSION_ID, Long.valueOf(cycle.getID()+""));
		assertFalse(count == 0);

		List<Folder> foldersList = folderManager.fetchFolders(PROJECT_ID, VERSION_ID, Arrays.asList(new Long[]{Long.valueOf(cycle.getID()+"")}),  2, 0);
		assertFalse(foldersList.size() == 0);
	}
	
	@Test
	public void testdDeleteFolder() {
		Folder folder1 = folderManager.saveFolder(getFolderProperties("testFolder", "test folder"));
        assertNotNull(folder1);
        FolderCycleMapping mapping = folderManager.saveFolderCycleMapping(getFolderCycleMappingProperties(folder1));
        assertNotNull(mapping);
		int mappingResult = folderManager.removeCycleFolderMapping(PROJECT_ID, VERSION_ID,Long.valueOf(cycle.getID()+""), Long.valueOf(folder1.getID()+""));
		int folderResult = folderManager.removeFolder(1l);
        assertFalse(mappingResult == 0 && folderResult == 0);
	}
	
	@Test
	public void testUniqueFolderNameUnderCycle() {
		Folder folder1 = folderManager.saveFolder(getFolderProperties("testFolder", "test folder"));
        assertNotNull(folder1);
        FolderCycleMapping mapping = folderManager.saveFolderCycleMapping(getFolderCycleMappingProperties(folder1));
        assertNotNull(mapping);
        boolean result = folderManager.isFolderNameUniqueUnderCycle(PROJECT_ID, VERSION_ID, Long.valueOf(cycle.getID() + ""), "testfolder");
        assertFalse(!result);
	}
	
	@Test
	public void testLinkFoldersToSprint() {
		Folder folder1 = folderManager.saveFolder(getFolderProperties("testFolder", "test folder"));
        assertNotNull(folder1);
        FolderCycleMapping mapping = folderManager.saveFolderCycleMapping(getFolderCycleMappingProperties(folder1));
        assertNotNull(mapping);
        int linkResult = folderManager.updateFoldersToSprint(Long.valueOf(mapping.getFolder().getID()+""), Long.valueOf(mapping.getCycle().getID()+""), VERSION_ID, PROJECT_ID, 3535435L);
        assertFalse(linkResult == 0);
	}
	
	@After
	public void tearDown() throws Exception {
	}
	
	public static class FolderManagerTestDatabaseUpdater implements DatabaseUpdater
    {
        @Override
        public void update(EntityManager em) throws Exception
        {
            em.migrate(Folder.class, Cycle.class, FolderCycleMapping.class);
            folder = em.create(Folder.class);
            cycle = em.create(Cycle.class);
            populateDummyFolderData(folder);
            final FolderCycleMapping mapping = em.create(FolderCycleMapping.class);
            populateDummyFolderData(mapping, folder);
        }
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
	
	public static void populateDummyFolderData(Folder folder){
		folder.setName("testfolder");
		folder.setDescription("Test Folder Description");
		folder.setCreatedBy("dummuyUser");
		folder.setDateCreated(Calendar.getInstance().getTime());
		folder.save();
	}
	
	public static void populateDummyFolderData(FolderCycleMapping mapping, Folder folder){
		mapping.setCreatedBy("dummuyUser");
		mapping.setCycle(cycle);
		mapping.setDateCreated(Calendar.getInstance().getTime());
		mapping.setFolder(folder);
	}
	
	public static Map<String, Object> getFolderProperties(String folderName, String folderDescription){
		Map<String, Object> folderProperties = new HashMap<String, Object>(1);
		folderProperties.put("NAME", folderName);
		folderProperties.put("DESCRIPTION", folderDescription);
		folderProperties.put("DATE_CREATED", Calendar.getInstance().getTime());
		folderProperties.put("CREATED_BY", "dummyUser");
		return folderProperties;
	}
	
	
	public static Map<String, Object> getFolderCycleMappingProperties(Folder folder){
		Map<String, Object> folderCycleMappingProperties = new HashMap<String, Object>(1);
		folderCycleMappingProperties.put("CYCLE_ID", cycle.getID());
		folderCycleMappingProperties.put("FOLDER_ID", folder.getID());
		folderCycleMappingProperties.put("PROJECT_ID", PROJECT_ID);
		folderCycleMappingProperties.put("VERSION_ID", VERSION_ID);
		folderCycleMappingProperties.put("DATE_CREATED", Calendar.getInstance().getTime());
		folderCycleMappingProperties.put("CREATED_BY", "dummyUser");
		return folderCycleMappingProperties;
	}

}
