package com.thed.zephyr.je.service;

import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.je.vo.ImportFieldConfig;
import com.thed.zephyr.je.vo.ImportFieldMapping;
import com.thed.zephyr.je.vo.ImportFile.ColumnMetadata;
import com.thed.zephyr.je.vo.ImportJob;
import com.thed.zephyr.je.vo.TestCase;
import com.thed.zephyr.je.vo.TestCase.Response;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IssueImporterService {
	public boolean importFile(JiraAuthenticationContext authContext,
							  JobProgressService jobProgressService,
							  String jobProgressToken,
							  ImportJob importJob,
							  File file,
							  ApplicationUser user,
							  List<Sheet> sheets) throws Exception;
	public Project getProject (ApplicationUser user, String projectID);
	public IssueType getIssueType(Project project, String issueTypeName) ;	
	public Map<String, String> extractIssueMapping(ImportJob importJob, File file, List<Sheet> sheets) throws Exception;
	public void saveAttachment(String issueKey, File workbook);
	public Response createTestCase(TestCase testCase, ApplicationUser user) throws Exception;
	public void createTestSteps(ImportJob importJob, String issueKey, List<Map<String, Object>> testStepsProperties);
	public TestCase populateTestCase(ImportJob importJob,
			ApplicationUser user, Map<String, ColumnMetadata> columnMetadataMap, int lastRowIdForThisTC);
	public Map<String, ColumnMetadata> populateColumnMetadata(Map<String, ImportFieldConfig> fieldConfigs, 
			Set<ImportFieldMapping> mappingSet, Map<String, String> fileMapping);
	public Map<String, ColumnMetadata> populateColumnMetadata(Map<String, ImportFieldConfig> fieldConfigs, 
			Set<ImportFieldMapping> mappingSet);
}
