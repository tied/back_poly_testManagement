package com.thed.zephyr.je.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.group.search.GroupPickerSearchService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.bc.issue.label.LabelService;
import com.atlassian.jira.bc.issue.label.LabelService.AddLabelValidationResult;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.LabelsCFType;
import com.atlassian.jira.issue.customfields.impl.MultiGroupCFType;
import com.atlassian.jira.issue.customfields.impl.MultiSelectCFType;
import com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
import com.atlassian.jira.issue.customfields.impl.NumberCFType;
import com.atlassian.jira.issue.customfields.impl.ProjectCFType;
import com.atlassian.jira.issue.customfields.impl.SelectCFType;
import com.atlassian.jira.issue.customfields.impl.UserCFType;
import com.atlassian.jira.issue.customfields.impl.VersionCFType;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.google.common.collect.Sets;
import com.thed.zephyr.je.service.TestcaseManager;
import com.thed.zephyr.je.vo.TestCase;
import com.thed.zephyr.je.vo.TestCase.Response;

public class TestcaseManagerImpl implements TestcaseManager {
	private JiraAuthenticationContext authContext;
	private ProjectComponentManager projectComponentManager;
	private ProjectService projectService;
	private LabelService labelService;
	private final IssueService issueService;
	private static final Logger log = LoggerFactory.getLogger(TestcaseManagerImpl.class);

	public TestcaseManagerImpl(JiraAuthenticationContext authContext, IssueService issueService, 
			LabelService labelService,
			ProjectComponentManager projectComponentManager,ProjectService projectService) {
		this.authContext = authContext;
		this.issueService = issueService;
		this.labelService = labelService;
		this.projectService = projectService;
		this.projectComponentManager = projectComponentManager;
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Response createTestCase(TestCase testCase, ApplicationUser user) throws Exception {
		IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
		issueInputParameters.setProjectId(Long.parseLong(testCase.getProjectID()));
		issueInputParameters.setIssueTypeId(testCase.getIssueTypeID());
		issueInputParameters.setSummary(testCase.getName().replace("\n", " "));
		issueInputParameters.setDescription(testCase.getDescription());
		// setting components
		if(StringUtils.isNotEmpty(testCase.getComponents())) {
			Collection<Long> targetProjectComponents = new HashSet<>();
			String[] comps = testCase.getComponents().split(",");
			for(String comp : comps){
				ProjectComponent component = (ProjectComponent)projectComponentManager.findByComponentName(Long.parseLong(testCase.getProjectID()), comp);
				if(component != null) {
					targetProjectComponents.add(component.getId());
				}else{
					throw new Exception(String.format("Component %s not valid", comp));
				}
			}
			issueInputParameters.setComponentIds(targetProjectComponents.toArray(new Long[targetProjectComponents.size()]));
		}
		Response response = testCase.new Response();
		String assigneeName = testCase.getAssignee();
		if(StringUtils.isNotEmpty(assigneeName)) {
			String assigneeKey = ComponentAccessor.getUserKeyService().getKeyForUsername(assigneeName);
			if(assigneeKey != null) {
				issueInputParameters.setAssigneeId(assigneeKey);
			}else{
				throw new Exception(String.format("Assignee %s not valid", assigneeKey));
			}
		}
		
		String userKey = ComponentAccessor.getUserKeyService().getKeyForUsername(user.getUsername());
		issueInputParameters.setReporterId(userKey);
		if(StringUtils.isNotEmpty(testCase.getPriority())) {
			issueInputParameters.setPriorityId(testCase.getPriority());
		}

		// setting versions
		if(testCase.getFixVersions() != null){
			String projectID = testCase.getProjectID();
			Map<String, Version> versionsMap = getProjectVersionsMap(user, projectID);
			String[] versionArr = testCase.getFixVersions().split(",");
			if(versionsMap != null && versionsMap.size() > 0 && versionArr != null && versionArr.length > 0) {
				List<Long> versionIDList = new ArrayList<Long>();
				for(String versionName:versionArr) {
					if(versionsMap.containsKey(versionName)) {
						Version ver = versionsMap.get(versionName);
						versionIDList.add(ver.getId());
					}else{
						throw new Exception(String.format("Version %s not valid", versionName));
					}
				}
				if(versionIDList != null && versionIDList.size() > 0) {
					issueInputParameters.setFixVersionIds(versionIDList.toArray(new Long[versionIDList.size()]));
				}
			}
		}
		// setting versions
		if(testCase.getAffectedVersions() != null){
			String projectID = testCase.getProjectID();
			Map<String, Version> versionsMap = getProjectVersionsMap(user, projectID);
			String[] versionArr = testCase.getAffectedVersions().split(",");
			if(versionsMap != null && versionsMap.size() > 0 && versionArr != null && versionArr.length > 0) {
				List<Long> versionIDList = new ArrayList<Long>();
				for(String versionName:versionArr) {
					if(versionsMap.containsKey(versionName)) {
						Version ver = versionsMap.get(versionName);
						versionIDList.add(ver.getId());
					}else{
						throw new Exception(String.format("Version %s not valid", versionName));
					}
				}
				if(versionIDList != null && versionIDList.size() > 0) {
					issueInputParameters.setAffectedVersionIds(versionIDList.toArray(new Long[versionIDList.size()]));
				}
			}
		}

		if(StringUtils.isNotEmpty(testCase.getDueDate())) {
			issueInputParameters.setDueDate(testCase.getDueDate());
		}
		issueInputParameters.setSkipScreenCheck(true);
		Map<String, Object> customPropertiesMap = testCase.getCustomProperties();
		if(customPropertiesMap != null && customPropertiesMap.size() > 0) {
			for(Map.Entry<String, Object> entry : customPropertiesMap.entrySet()) {
				try {
					if(entry.getValue() instanceof String){
						issueInputParameters.addCustomFieldValue(entry.getKey(), (String)entry.getValue());
					}
				} catch(Exception ex) {
					log.error("Exception while adding custom fields:"+entry.getKey(), ex);
					//throw new Exception(ex.getMessage());
				}
			}
		}

		authContext.setLoggedInUser(user);
		IssueService.CreateValidationResult result = issueService.validateCreate(user, issueInputParameters);
		if (!result.getErrorCollection().hasAnyErrors()) {
            //int i =0;
			IssueResult res =issueService.create(user, result);
			//MutableIssue issue = res.getIssue();
			String issueKey = res.getIssue().getKey();
			if(StringUtils.isNotEmpty(issueKey)) {
				response.setIssueKey(issueKey);
				StringBuilder sb = new StringBuilder();
				// set labels
				if(StringUtils.isNotEmpty(testCase.getTag())) {
					//LabelService labelService = componentManager.getComponentInstanceOfType(LabelService.class);
					AddLabelValidationResult labelResult = labelService.validateAddLabel(authContext.getLoggedInUser(), res.getIssue().getId(), testCase.getTag());
					if(!labelResult.getErrorCollection().hasAnyErrors()) {
						labelService.addLabel(user, labelResult, false);
					}else{
						sb.append("Label is not valid:").append(testCase.getTag()).append(System.lineSeparator());
					}
				}
				
				//Map<String, Object> customPropertiesMap = testCase.getCustomProperties();
				if(customPropertiesMap != null && customPropertiesMap.size() > 0) {
					for(Map.Entry<String, Object> entry : customPropertiesMap.entrySet()) {
						try {
							CustomField cf1 = ComponentAccessor.getFieldManager().getCustomField(entry.getKey());
							if(cf1 == null) continue;
							if(entry.getValue() instanceof IssueImporterServiceImpl.ArrayMap){
								FieldConfig fc = cf1.getRelevantConfig(res.getIssue());
								Options options = cf1.getOptions(entry.getKey(), fc, null);
								List<String> list = Arrays.asList(((IssueImporterServiceImpl.ArrayMap)entry.getValue()).getValues());
								List<Option> targetOptions = new ArrayList<>();
								Map<String, Option> existingOptions = new HashMap<>();
								if(options != null){
									for(Option option : options){
										existingOptions.put(option.getValue(), option);
									}
									for(String str : list){
										if(existingOptions.containsKey(str)){
											targetOptions.add(existingOptions.get(str));
										}else{
											sb.append(cf1.getNameKey()).append(" doesn't allow the value:").append(str).append(System.lineSeparator());
										}
									}
									cf1.createValue(res.getIssue(), targetOptions);
								}else{
									CustomFieldType cfType = cf1.getCustomFieldType();
									List<ApplicationUser> userList = new ArrayList<>();
									if(cfType instanceof MultiUserCFType){
										for(String value : list){
											ApplicationUser user1 = ApplicationUsers.byKey(value);
											if(user1 != null){
												userList.add(user1);
											}else{
												sb.append("User not found with:").append(value).append(System.lineSeparator());
											}
										}
										((MultiUserCFType)cfType).updateValue(cf1, res.getIssue(), userList);
									}else if(cfType instanceof MultiGroupCFType){
										GroupPickerSearchService groupSearch = ComponentAccessor.getComponent(GroupPickerSearchService.class);
										List<Group> groupList = new ArrayList<>();
										for(String value : list){
											List<Group> searchList = groupSearch.findGroups(value);
											if(searchList.size() > 0){
												groupList.addAll(searchList);
											}else{
												sb.append("Group not found with:").append(value).append(System.lineSeparator());
											}
										}
										((MultiGroupCFType) cfType).updateValue(cf1, res.getIssue(), groupList);
									}if(cfType instanceof MultiSelectCFType){
										for(String value : list){
											ApplicationUser user1 = ApplicationUsers.byKey(value);
											if(user1 != null){
												userList.add(user1);
											}else{
												sb.append("User not found with:").append(value).append(System.lineSeparator());
											}
										}
										((MultiUserCFType)cfType).updateValue(cf1, res.getIssue(), userList);
									}if(cfType instanceof VersionCFType){
										Map<String, Version> versionsMap = getProjectVersionsMap(user, String.valueOf(res.getIssue().getProjectId()));
										Collection<Version> versionList = new ArrayList<>();
										for(String value : list){
											if(versionsMap.containsKey(value)){
												versionList.add(versionsMap.get(value));
											}else{
												versionsMap.values().stream().forEach( version ->{
													if(value.equals(version.getId())){
														versionList.add(version);
													}
												});
											}
										}
										((VersionCFType)cfType).updateValue(cf1, res.getIssue(), versionList);
									}
								}

							} else if(entry.getValue() instanceof IssueImporterServiceImpl.SingleValueMap){
								String value = ((IssueImporterServiceImpl.SingleValueMap)entry.getValue()).getValue();
								CustomFieldType cfType = cf1.getCustomFieldType();
								if(cfType instanceof UserCFType){
									((UserCFType) cfType).updateValue(cf1, res.getIssue(), ApplicationUsers.byKey(value));
								}else if(cfType instanceof ProjectCFType){
									Project project = findProject(user, value);
									if(project == null){
										sb.append("Project not found with:").append(value).append(System.lineSeparator());
									}else{
										((ProjectCFType) cfType).updateValue(cf1, res.getIssue(), project);
									}
								}else if(cfType instanceof VersionCFType){
									Map<String, Version> versionsMap = getProjectVersionsMap(user, String.valueOf(res.getIssue().getProjectId()));
									Collection<Version> versions = new ArrayList<>();
									if(versionsMap.containsKey(value)){
										versions.add(versionsMap.get(value));
									}else{
										versionsMap.values().stream().forEach( version ->{
											if(value.equals(version.getId())){
												versions.add(version);
											}
										});
									}
									if(versions.size() > 0){
										((VersionCFType) cfType).updateValue(cf1, res.getIssue(), versions);
									}else{
										sb.append("Version not found with:").append(value).append(System.lineSeparator());
									}
								}else if(cfType instanceof SelectCFType){
									FieldConfig fc = cf1.getRelevantConfig(res.getIssue());
									Options options = cf1.getOptions(entry.getKey(), fc, null);
									List<Option> targetOptions = new ArrayList<>();
									Map<String, Option> existingOptions = new HashMap<>();
									if(options != null){
										for(Option option : options){
											existingOptions.put(option.getValue(), option);
										}
										if(existingOptions.containsKey(value)){
											targetOptions.add(existingOptions.get(value));
										}else{
											sb.append(cf1.getNameKey()).append(" doesn't allow the value:").append(value).append(System.lineSeparator());
										}
										((SelectCFType)cfType).updateValue(cf1, res.getIssue(), targetOptions.get(0));
									}else{
										sb.append("Option not found with:").append(value).append(System.lineSeparator());
									}
								}else if(cfType instanceof MultiGroupCFType){
									GroupPickerSearchService groupSearch = ComponentAccessor.getComponent(GroupPickerSearchService.class);
									//List<Group> groupList = new ArrayList<>();
									List<Group> searchList = groupSearch.findGroups(value);
									if(searchList.size() > 0){
										((MultiGroupCFType) cfType).updateValue(cf1, res.getIssue(), searchList);
									}else{
										sb.append("Group not found with:").append(value).append(System.lineSeparator());
									}
								}else {
									//For URLCFType, GenericTextCFType,  
									cfType.updateValue(cf1, res.getIssue(), value);
								}
								
							}else if(entry.getValue() instanceof String[]){
								cf1.createValue(res.getIssue(), Arrays.asList((String[])entry.getValue()));
							}else if(entry.getValue() instanceof Date){
								cf1.createValue(res.getIssue(),  (Date)entry.getValue());
							}else if(entry.getValue() instanceof String){//Ignore if String
								CustomFieldType cfType = cf1.getCustomFieldType();
								/*if(cfType instanceof JobCheckboxCFType){
									((JobCheckboxCFType)cfType).updateValue(customField1, res.getIssue(), value);
								}*/
								if(cfType instanceof LabelsCFType){
									String value = (String)entry.getValue();
									Set<String> lableSet = value != null ? Sets.newHashSet(value.split(",")) : new HashSet<>();
									LabelManager labelManager = ComponentAccessor.getComponent(LabelManager.class);
									labelManager.setLabels(user, res.getIssue().getId(), cf1.getIdAsLong(), lableSet, false, false);
								} if(cfType instanceof NumberCFType){
									((NumberCFType) cfType).createValue(cf1, res.getIssue(), Double.parseDouble((String)entry.getValue()));
									//cf1.createValue(res.getIssue(), Number.);
								}
							}else {
								cf1.createValue(res.getIssue(), (Object)entry.getValue());
							}
						} catch(Exception ex) {
							log.error("Exception while adding custom fields:"+entry.getKey(), ex);
							sb.append("Exception while adding custom fields:")
								.append(entry.getKey())
								.append(ex.getMessage())
								.append(System.lineSeparator());
						}
					}
				}
				// set comments
				if(StringUtils.isNotEmpty(testCase.getComments())){
					CommentManager commentManager = ComponentAccessor.getCommentManager();
					commentManager.create(result.getIssue(), authContext.getLoggedInUser(),testCase.getComments(), false);
				}

				// linking issues
				if(StringUtils.isNotEmpty(testCase.getLinkingIssueKey())) {
					IssueResult storyIssueResult = issueService.getIssue(authContext.getLoggedInUser(), testCase.getLinkingIssueKey());
					Issue storyIssue = storyIssueResult.getIssue();
					IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
					Collection<IssueLinkType> issueLinkTypes = ComponentAccessor.getComponent(IssueLinkTypeManager.class).getIssueLinkTypes();
					for(IssueLinkType linkType:issueLinkTypes){
						if(linkType.getId() == Long.parseLong(testCase.getLinkTypeId())) {
							try {
								issueLinkManager.createIssueLink(res.getIssue().getId(), storyIssue.getId(), linkType.getId(), null,  authContext.getLoggedInUser());
							} catch(Exception ex) {
								log.error("Exception while adding issue link:"+storyIssue.getId() + ex.getStackTrace());
								//throw new Exception(ex.getMessage());
								sb.append(ex.getMessage());
							}
						}
					}
				}
				if(sb != null){
					response.setResponseMsg(sb.toString());
				}
			}
		}else{
			Iterator<String> errorIterator = result.getErrorCollection().getErrors().values().iterator();
			StringBuilder sb = new StringBuilder();
			while(errorIterator.hasNext()){
				sb.append(errorIterator.next());
			}
			throw new Exception(sb.toString());
		}
		return response;
	}

	public Map<String, Version> getProjectVersionsMap (ApplicationUser user, String projectID) {
		Project project = null;
		Map<String, Version> versionMap = null;
		if(StringUtils.isNotEmpty(projectID)) {
			ProjectService.GetProjectResult projectResult = projectService.getProjectById(user, Long.parseLong(projectID));
			if (projectResult.isValid())
			{
				project = projectResult.getProject();
				Collection<Version> versionList = project.getVersions();
				if(versionList != null && versionList.size() > 0 ) {
					versionMap = new HashMap<String, Version>();
					for(Version version : versionList) {
						versionMap.put(version.getName(), version);
					}
				}
			}
		}
		return versionMap;
	}
	private Project findProject(ApplicationUser user, String idOrKey){
		if(StringUtils.isNotEmpty(idOrKey)) {
			Long projectId = null;
			try{
				projectId = Long.parseLong(idOrKey);
			}catch(Exception e){}
			ProjectService.GetProjectResult projectResult = null;
			if (projectId != null){
				projectResult = projectService.getProjectById(user, projectId);
			}else{
				projectResult = projectService.getProjectByKey(user, idOrKey);
			}
			return projectResult.isValid() ? projectResult.getProject() : null;
		}
		return null;
	}

}
