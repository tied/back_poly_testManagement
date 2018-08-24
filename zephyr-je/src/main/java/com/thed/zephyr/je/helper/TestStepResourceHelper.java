package com.thed.zephyr.je.helper;


import com.atlassian.adapter.jackson.ObjectMapper;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.util.concurrent.atomic.AtomicInteger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.thed.zephyr.je.model.*;
import com.thed.zephyr.je.rest.CustomFieldValueResource.CustomFieldValueRequest;
import com.thed.zephyr.je.rest.CustomFieldValueResource.CustomFieldValueResponse;
import com.thed.zephyr.je.rest.delegate.CustomFieldResourceDelegate;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.vo.TeststepBean;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import com.thed.zephyr.util.ZephyrUtil;
import com.thed.zephyr.util.validator.CustomFieldValueValidationUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestStepResourceHelper {
    private final Logger log = Logger.getLogger(TestStepResourceHelper.class);
    private static final Integer STRING_VALUE_MAX_LENGTH = new Integer(255);
    
	private TeststepManager testStepManager;
	private JiraAuthenticationContext authContext;
	private final CustomFieldValueResourceDelegate customFieldValueResourceDelegate;

	public TestStepResourceHelper(TeststepManager testStepManager, JiraAuthenticationContext authContext, final CustomFieldValueResourceDelegate customFieldValueResourceDelegate) {
		this.testStepManager = testStepManager;
		this.authContext = authContext;
		this.customFieldValueResourceDelegate = customFieldValueResourceDelegate;
	}
	

	public List<Teststep> cloneTestStep(Long fromStepId, Long issueId, String step, Integer position) {
		//Following scenarios possible
		/**
		 * 1. Get All Test Steps foe the issueId passed in. If Steps is null, position does not matter, will be the first element
		 * 2. If steps exist, than we will check the position passed in and will update the orderId  
		 */
		//Teststep testStep = testStepManager.getTeststep(fromStepId.intValue());
		List<Teststep> testSteps = testStepManager.getTeststeps(issueId,Optional.empty(),Optional.empty());
		Teststep cloneTestStep = null;
		int maxTestStepId = 0;
		int matchedIndex = 0;
		int countIndex = 0;
		try {
			List<Map<String,Object>> stepProperties = new ArrayList<Map<String,Object>>();
			List<Teststep> newlyClonedTeststep = new ArrayList<>();
			for(Teststep testStep : testSteps) {
				if(testStep.getID() == fromStepId.intValue()) {
					cloneTestStep = testStep;
					matchedIndex = countIndex;
				} else {
					countIndex++;
				}
				if(testStep.getOrderId().intValue() > maxTestStepId) {
					maxTestStepId = testStep.getOrderId().intValue();
				}
			}
			
			if(cloneTestStep == null) {
				try {
					JSONObject errorJsonObject = new JSONObject();
					errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("schedule.update.ID.required", "id"));
					errorJsonObject.put("errors", new String(""));
                    throw new RESTException(Status.BAD_REQUEST, errorJsonObject);
				} catch (JSONException e) {
					log.error("Error constructing JSON",e);
				}			
			}

			//If Position = 2, than just take the fromStepId and add it at the end
			if(position == -2) {
				Map<String,Object> stepProperty = createTestStepProperties(cloneTestStep,maxTestStepId+1,step);
				stepProperties.add(stepProperty);
				newlyClonedTeststep = testStepManager.saveTeststepProperties(stepProperties);
			} 
			
			if(position == -1) {
				Map<String,Object> stepProperty = createTestStepProperties(cloneTestStep,cloneTestStep.getOrderId()+1,step);
				stepProperties.add(stepProperty);
				newlyClonedTeststep = testStepManager.saveTeststepProperties(stepProperties);
	
				List<Teststep>  steps = testSteps.subList(matchedIndex+1, testSteps.size());
				for(Teststep testStep : steps) {
					testStep.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
					testStep.setOrderId(testStep.getOrderId()+1);	
					testStep.save();
				}
			}
	
			if(position == 0 && matchedIndex >= 0) {
				Map<String,Object> stepProperty = createTestStepProperties(cloneTestStep,cloneTestStep.getOrderId(),step);
				stepProperties.add(stepProperty);
				newlyClonedTeststep = testStepManager.saveTeststepProperties(stepProperties);
	
				List<Teststep>  steps = testSteps.subList(matchedIndex, testSteps.size());
				for(Teststep testStep : steps) {
					testStep.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
					testStep.setOrderId(testStep.getOrderId()+1);	
					testStep.save();
				}
			}
			
			if(position > 0) {
				if(position > testSteps.size()+1) {
					JSONObject errorJsonObject = new JSONObject();
					errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("zephyr.common.error.invalid", "position",String.valueOf(position)));
					errorJsonObject.put("errors", new String(""));
                    throw new RESTException(Status.BAD_REQUEST, errorJsonObject);
				}
				
				List<Teststep>  steps = testSteps.subList(position.intValue()-1, testSteps.size());
				for(Teststep testStep : steps) {
					testStep.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
					testStep.setOrderId(testStep.getOrderId()+1);	
					testStep.save();
				}		
				Map<String,Object> stepProperty = createTestStepProperties(cloneTestStep,position,step);
				stepProperties.add(stepProperty);
				newlyClonedTeststep = testStepManager.saveTeststepProperties(stepProperties);

			}
			if(CollectionUtils.isNotEmpty(newlyClonedTeststep)) {
                IssueManager issueManager = ComponentAccessor.getIssueManager();
                Issue issue = issueManager.getIssueObject(issueId);
				cloneCustomFieldsData(fromStepId.intValue(), newlyClonedTeststep.get(0).getID(), issue.getProjectId(),issue.getProjectId());
			}
			List<Teststep> updatedTestSteps = testStepManager.getTeststeps(issueId,Optional.empty(),Optional.empty());
			return updatedTestSteps;
		} catch(Exception e) {
			log.error("Error Cloning Test Step",e);
			//return Response.status(Status.NOT_ACCEPTABLE).cacheControl(ZephyrCacheControl.never()).build();
		}
		return Collections.EMPTY_LIST;
	}

	/**
	 * Create Test Step Property for cloning
	 * @param testStep
	 * @param orderId
	 * @param step
	 * @return
	 */
	private Map<String, Object> createTestStepProperties(Teststep testStep, int orderId, String step) {
		Map<String, Object> steps = new HashMap<String, Object>();
		steps.put("ISSUE_ID", testStep.getIssueId());
		steps.put("ORDER_ID", orderId);
		steps.put("STEP", testStep.getStep());
		if(StringUtils.isNotBlank(step) && !StringUtils.equalsIgnoreCase(testStep.getStep(),step)) {
			steps.put("STEP", step);
		}else{
            String clonePrefix = authContext.getI18nHelper().getText("teststep.operation.clone.step.prefix");
            String existingStepValue = (org.apache.commons.lang.StringUtils.isNotBlank(testStep.getStep()) ? testStep.getStep() : "");
            Pattern stepMarkupPattern = Pattern.compile("^\\s*(#|h[1-6]\\.|\\*|\\|\\||bq.|\\-|\\{quote\\})");
            Matcher stepMatcher = stepMarkupPattern.matcher(existingStepValue);
            
            if(stepMatcher.find()) {
            	steps.put("STEP", clonePrefix + " - \n" + existingStepValue);
            } else {
            	steps.put("STEP", clonePrefix + " - " + existingStepValue);
            }
        }
		steps.put("DATA", testStep.getData());
		steps.put("RESULT", testStep.getResult());
		steps.put("CREATED_BY", UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
		steps.put("MODIFIED_BY",UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
		return steps;
	}

	public JSONObject verifyPermissions(Issue issue, ProjectPermissionKey permissionsId) {
		JSONObject errorJsonObject = null;
		try {
			if(!ComponentAccessor.getPermissionManager().hasPermission(permissionsId, issue, authContext.getLoggedInUser())) {
				errorJsonObject = new JSONObject();
				errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("schedule.project.permission.error", "Issue", String.valueOf(issue.getProjectObject().getKey())));
				errorJsonObject.put("errors", new String(""));
				return errorJsonObject;
			}
		} catch(JSONException e) {
			log.warn("Error creating JSONObject",e);
		} 
		return null;
	}
	
	public void copyTestStepsFromSrcToDst(JobProgressService jobProgressService, String isJql, Boolean copyCustomFields, Issue sourceIssue, String destinationIssueKeys,
			String jobProgressToken, Map<String, String> result, ApplicationUser loggedInUser) {
		try {
			result.put("notfoundissues", "");
			Collection<String> noJiraIssuePermission = new ArrayList<>();
			List<Teststep> srctestStepsList = testStepManager.getTeststeps(sourceIssue.getId(), Optional.empty(), Optional.empty());
			if(srctestStepsList != null && srctestStepsList.size() <= 0) {
				result.put("teststeps", 0+"");
				result.put("copiedIssues", 0+"");
				result.put("noIssuePermission", "-");
				jobProgressService.addCompletedSteps(jobProgressToken, 1);
				appendSummaryMessages(jobProgressService, jobProgressToken, result);
				return; //There is no steps to copy from source issue hence skipping the process.
			}
			result.put("teststeps", srctestStepsList.size()+"");
			if("false".equals(isJql)) {
				IssueManager issueManager = ComponentAccessor.getIssueManager();
				jobProgressService.addSteps(jobProgressToken, destinationIssueKeys.split(",").length);
				AtomicInteger copiedIssues = new AtomicInteger(0);
				StringBuffer sb = new StringBuffer("");
				Arrays.asList(destinationIssueKeys.split(",")).stream().forEach(issueKey -> {
					Issue issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
					boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,loggedInUser);
					if(!hasIssueViewPermission) {
						noJiraIssuePermission.add(issue.getKey());
					} else {
                        if(issue != null && !issue.getId().equals(sourceIssue.getId())) {
                            copyTestStepsFromSrcToDst(issue, srctestStepsList, issue.getProjectId(),copyCustomFields,sourceIssue.getProjectId());
                            copiedIssues.getAndIncrement();
                        } else {
                            if(issue == null) {
                                sb.append(issueKey).append(",");
                            } else {
                                result.put("self", "true");
                            }
                        }
					}
					jobProgressService.addCompletedSteps(jobProgressToken, 1);
				});
				String tempStr = sb.toString();
				result.put("notfoundissues",  tempStr.length() == 0 ? tempStr : tempStr.substring(0, tempStr.length()-1));
				result.put("copiedIssues", copiedIssues.toString());
				result.put("noIssuePermission", StringUtils.join(noJiraIssuePermission,","));
			} else {
				SearchProvider searchProvider = ComponentAccessor.getComponent(SearchProvider.class);
				SearchRequestService searchRequestService = ComponentAccessor.getComponentOfType(SearchRequestService.class);
				SearchRequest myFilter = searchRequestService.getFilter(new JiraServiceContextImpl(loggedInUser), Long.valueOf(destinationIssueKeys));
				if(myFilter == null) {
					log.warn("Saved filter not found, check filterId or permissions " + destinationIssueKeys);
					throw new RuntimeException("Saved filter not found, check filterId or permissions " + destinationIssueKeys);
				}
				final String testTypeId = JiraUtil.getTestcaseIssueTypeId();
				Query query = JqlQueryBuilder.newBuilder(myFilter.getQuery()).where().and().issueType(testTypeId).buildQuery();
				PagerFilter pageFilter = new PagerFilter(0, 10);
				SearchResults searchResults = searchProvider.search(query, loggedInUser,pageFilter);
				int totalCount = searchResults != null ? searchResults.getTotal() : 0;
				jobProgressService.addSteps(jobProgressToken, totalCount);
				int maxResult = 20, offset = 0; //Fetch only 20 records at a time for the jql result and process it.
				AtomicInteger copiedIssues = new AtomicInteger(0);
				while (totalCount > offset) {
					SearchResults results = searchProvider.search(query, loggedInUser, PagerFilter.newPageAlignedFilter(offset, maxResult));
					List<Issue> issueList = results.getIssues();
					issueList.stream().forEach(issue -> {
						boolean hasIssueViewPermission = JiraUtil.hasIssueViewPermission(null,issue,loggedInUser);
						if(!hasIssueViewPermission) {
							noJiraIssuePermission.add(issue.getKey());
						} else {
                            if(!issue.getId().equals(sourceIssue.getId())) {
                                copyTestStepsFromSrcToDst(issue, srctestStepsList,issue.getProjectId(),copyCustomFields,sourceIssue.getProjectId());
                                copiedIssues.getAndIncrement();
                            } else {
                                result.put("self", "true");
                            }
						}
						jobProgressService.addCompletedSteps(jobProgressToken, 1);
					});
					offset = offset + maxResult;
				}
				result.put("copiedIssues", copiedIssues.toString());
				result.put("noIssuePermission", StringUtils.join(noJiraIssuePermission,","));
			}
			appendSummaryMessages(jobProgressService, jobProgressToken, result);
			jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_COMPLETED);
		} catch(Exception e) {
			jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_FAILED);
			log.error("Error while copying Test Steps", e);
		}		
	}
	
	private void appendSummaryMessages(JobProgressService jobProgressService, String jobProgressToken, Map<String, String> result) {
		ObjectMapper mapper = new ObjectMapper();
        String resultStr = mapper.writeValueAsString(result);
		jobProgressService.setSummaryMessage(jobProgressToken, resultStr);
	}
	
	public void copyTestStepsFromSrcToDst(Issue destinationIssue, List<Teststep> srctestStepsList, Long destProjectId, Boolean copyCustomFields,Long sourceProjectId) {
		TeststepBean teststepBean = new TeststepBean();
		for(Teststep testStep : srctestStepsList) {
			teststepBean.data = testStep.getData();
			teststepBean.step = testStep.getStep();
			teststepBean.result = testStep.getResult();
			Teststep copiedTestStep = testStepManager.createTeststep(teststepBean, destinationIssue.getId());
			if(copyCustomFields) {
				cloneCustomFieldsData(testStep.getID(), copiedTestStep.getID(), destProjectId, sourceProjectId);
			}
		}	
	}

	/**
	 * Method to clone custom fields data.
	 * @param fromStepId
	 * @param clonedTeststepId
	 * @param destProjectId
	 * @param sourceProjectId
	 */
    public void cloneCustomFieldsData(Integer fromStepId, Integer clonedTeststepId, Long destProjectId, Long sourceProjectId) {
        ZephyrCustomFieldManager zephyrCustomFieldManager = (ZephyrCustomFieldManager) ZephyrComponentAccessor.getInstance().getComponent("zephyrcf-manager");
        Map<String, CustomFieldValueRequest> customFieldValues = new HashMap<>();
        CustomFieldValueRequest request;
        Map<String, CustomFieldValueResponse> response = customFieldValueResourceDelegate.getCustomFieldValuesByEntityId(fromStepId,ApplicationConstants.ENTITY_TYPE.TESTSTEP.name(), null);
        Map<Long, List<Long>> customFieldsProjectMapping = Maps.newHashMap();

        response.entrySet().forEach(customFieldRequest -> {
            Long customFieldId = Long.valueOf(customFieldRequest.getKey());
            CustomField customField = zephyrCustomFieldManager.getCustomFieldById(customFieldId);
            if(Objects.nonNull(customField)) {
                CustomFieldProject[] customFieldProjects = zephyrCustomFieldManager.getActiveCustomFieldProjects(customFieldId);
                List<Long> associatedProjectList;
                if(Objects.nonNull(customFieldProjects) && customFieldProjects.length > 0) {
                    associatedProjectList = Arrays.stream(customFieldProjects).map(CustomFieldProject::getProjectId).collect(Collectors.toList());
                    if(CollectionUtils.isNotEmpty(associatedProjectList)) {
                        customFieldsProjectMapping.put(customFieldId,associatedProjectList);
                    }
                }
            }
        });

        for(Map.Entry<String, CustomFieldValueResponse> entry : response.entrySet()) {
			request = new CustomFieldValueRequest();
			CustomFieldValueResponse res = entry.getValue();
        	if( null != customFieldsProjectMapping.get(res.getCustomFieldId())) {
        	    List<Long> associatedProjectList = customFieldsProjectMapping.get(res.getCustomFieldId());
                if(CollectionUtils.isNotEmpty(associatedProjectList) && associatedProjectList.contains(sourceProjectId) && associatedProjectList.contains(destProjectId)) {
                    request.setEntityId(res.getEntityId());
                    request.setCustomFieldId(res.getCustomFieldId());
                    request.setCustomFieldType(res.getCustomFieldType());
                    request.setEntityType(res.getEntityType());
                    if (StringUtils.isNotEmpty(res.getValue())) {
                        request.setValue(getCustomFieldValue(res.getValue(), res.getCustomFieldType()));
                    }
                    request.setSelectedOptions(res.getSelectedOptions());
                    customFieldValues.put(entry.getKey(), request);
                }
			}
        }
        customFieldValueResourceDelegate.createCustomFieldValues(customFieldValues, ApplicationConstants.ENTITY_TYPE.TESTSTEP.name(), clonedTeststepId);
    }

    /**
     *
     * @param customFieldName
     * @param projectId
     * @param zephyrCustomFieldManager
     * @return
     */
    private Long validateAndGetProjectSpecificCustomFieldId(String customFieldName, Long projectId, ZephyrCustomFieldManager zephyrCustomFieldManager) {
        CustomField customField = zephyrCustomFieldManager.getCustomFieldByProjectIdAndCustomFieldName(projectId,customFieldName,ApplicationConstants.ENTITY_TYPE.TESTSTEP.name());

        if(Objects.nonNull(customField)) {
            return Long.valueOf(customField.getID());
        }
        return null;
    }

    /**
	 *
	 * @param value
	 * @param customFieldType
	 * @return
	 */
	private String getCustomFieldValue(String value, String customFieldType) {
		if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)
				|| ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
			return value.substring(0, value.length() - 3);
		}
		return value;
	}

	public Response validateCustomFieldValueRequest(Map<String, CustomFieldValueRequest> customFieldValueRequests, CustomFieldResourceDelegate customFieldResourceDelegate, Issue issue, Integer entityId) {

        for(Map.Entry<String, CustomFieldValueRequest> customFieldValueRequest : customFieldValueRequests.entrySet()) {
            CustomFieldValueRequest request = customFieldValueRequest.getValue();

            if(CustomFieldValueValidationUtil.isObjectNull(request.getEntityId())) {
                request.setEntityId(entityId);
            }

            if(CustomFieldValueValidationUtil.isObjectNull(request.getEntityType())) {
                request.setEntityType(ApplicationConstants.ENTITY_TYPE.TESTSTEP.name());
            }

            Response response = validateCustomFieldValueRequestObject(request,customFieldResourceDelegate,issue);

            if(response != null)
                return response;
        }

		return null;
	}

    private Response validateCustomFieldValueRequestObject(CustomFieldValueRequest customFieldValueRequest, CustomFieldResourceDelegate customFieldResourceDelegate, Issue issue) {

	    JSONObject jsonObject;
        if (!CustomFieldValueValidationUtil.validateEntityType(customFieldValueRequest.getEntityType())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (CustomFieldValueValidationUtil.isObjectNull(customFieldValueRequest.getCustomFieldId())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Custom Field Id"),
                    Response.Status.BAD_REQUEST, null);
        }else {
            CustomField customField = customFieldResourceDelegate.getCustomFieldById(customFieldValueRequest.getCustomFieldId());
            if(CustomFieldValueValidationUtil.isObjectNull(customField)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom Field Id",customFieldValueRequest.getCustomFieldId()+ org.apache.commons.lang.StringUtils.EMPTY ), Response.Status.BAD_REQUEST, null);
            }

            if(CustomFieldValueValidationUtil.validateCustomFieldType(customFieldValueRequest.getCustomFieldType())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Custom Field Type"),
                        Response.Status.BAD_REQUEST, null);
            }

            if(CustomFieldValueValidationUtil.validateCustomFieldTypeWithGivenCustomField(customFieldValueRequest.getCustomFieldType(), customField.getCustomFieldType())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Given custom field type doesn't match with provided custom field ID.", customFieldValueRequest.getCustomFieldType()),
                        Response.Status.BAD_REQUEST, null);
            }

            if(CustomFieldValueValidationUtil.validateEntityTypeWithGivenCustomField(customFieldValueRequest.getEntityType(),customField.getZFJEntityType())) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Given custom field entity type doesn't match with provided custom field ID entity type.", customFieldValueRequest.getCustomFieldType()),
                        Response.Status.BAD_REQUEST, null);
            }
        }

        if(CustomFieldValueValidationUtil.isStringBlank(customFieldValueRequest.getCustomFieldType())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Custom Field Type"),
                    Response.Status.BAD_REQUEST, null);
        }

        if (CustomFieldValueValidationUtil.isObjectNull(customFieldValueRequest.getEntityId())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.invalid.parameter", "Entity Id"),
                    Response.Status.BAD_REQUEST, null);
        }


        if(customFieldValueRequest.getEntityType().equalsIgnoreCase(ApplicationConstants.ENTITY_TYPE.TESTSTEP.name())) {
            Teststep teststep = testStepManager.getTeststep(customFieldValueRequest.getEntityId());
            if(CustomFieldValueValidationUtil.isObjectNull(teststep)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("schedule.does.not.exist.title"), Response.Status.BAD_REQUEST, null);
            }

            if(!validateDisableCustomFieldForProjectAndCustomField(customFieldValueRequest.getCustomFieldId(),issue.getProjectId(),customFieldResourceDelegate)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Given custom field is disabled for the project to which given entity ID belongs.", customFieldValueRequest.getCustomFieldId()+ org.apache.commons.lang.StringUtils.EMPTY),
                        Response.Status.BAD_REQUEST, null);
            }
            if(Objects.nonNull(customFieldValueRequest.getCustomFieldValueId())) {
                TestStepCf testStepCf = customFieldValueResourceDelegate.getTeststepCustomFieldValue(customFieldValueRequest.getCustomFieldValueId());
                if(CustomFieldValueValidationUtil.isObjectNull(testStepCf)) {
                    jsonObject = new JSONObject();
                    return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom Field Value Id",customFieldValueRequest.getCustomFieldValueId()+ org.apache.commons.lang.StringUtils.EMPTY), Response.Status.BAD_REQUEST, null);
                }else {
                    if(Objects.nonNull(testStepCf.getID()) && CustomFieldValueValidationUtil.validateCustomFieldValueIdWithGivenId(Long.valueOf(testStepCf.getID()), customFieldValueRequest.getCustomFieldValueId())) {
                        jsonObject = new JSONObject();
                        return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field value ID doesn't belong to provided entity ID.", customFieldValueRequest.getCustomFieldValueId()+ org.apache.commons.lang.StringUtils.EMPTY),
                                Response.Status.BAD_REQUEST, null);
                    }

                    if(Objects.nonNull(testStepCf.getCustomField()) && CustomFieldValueValidationUtil.validateCustomFieldIdWithGivenId(Long.valueOf(testStepCf.getCustomField().getID()), customFieldValueRequest.getCustomFieldId())) {
                        jsonObject = new JSONObject();
                        return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field associated with given custom field value ID doesn't match with provided custom field ID.", customFieldValueRequest.getCustomFieldId()+ org.apache.commons.lang.StringUtils.EMPTY),
                                Response.Status.BAD_REQUEST, null);
                    }

                    if(Objects.nonNull(testStepCf.getTestStepId()) && CustomFieldValueValidationUtil.validateEntityIdWithGivenId(Integer.valueOf(testStepCf.getTestStepId()), customFieldValueRequest.getEntityId())) {
                        jsonObject = new JSONObject();
                        return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field value ID doesn't belong to provided entity ID.", customFieldValueRequest.getCustomFieldValueId()+ org.apache.commons.lang.StringUtils.EMPTY),
                                Response.Status.BAD_REQUEST, null);
                    }
                }
            }
        }

        if(org.apache.commons.lang.StringUtils.isNotBlank(customFieldValueRequest.getSelectedOptions())) {
            if(validateCustomFieldOptionIds(customFieldValueRequest.getSelectedOptions(), customFieldValueRequest.getCustomFieldId(),customFieldResourceDelegate)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Either Custom Field Options Value ID doesn't exist or doesn't belong to provided custom field ID.",customFieldValueRequest.getSelectedOptions()+ org.apache.commons.lang.StringUtils.EMPTY), Response.Status.BAD_REQUEST, null);
            }
        }

        if(Objects.isNull(customFieldValueRequest.getCustomFieldValueId())) {
            if(isCustomFieldAndEntityRecordExist(customFieldValueRequest)) {
                jsonObject = new JSONObject();
                return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom Field Value Id",customFieldValueRequest.getCustomFieldValueId()+ org.apache.commons.lang.StringUtils.EMPTY), Response.Status.BAD_REQUEST, null);
            }
        }

        if(CustomFieldValueValidationUtil.validateCustomFieldValueWithType(customFieldValueRequest.getCustomFieldType(),customFieldValueRequest.getValue())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom Field Value",customFieldValueRequest.getValue()), Response.Status.BAD_REQUEST, null);
        }

        if(CustomFieldValueValidationUtil.validateNumberValueMaxLength(customFieldValueRequest.getCustomFieldType(), customFieldValueRequest.getValue())) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("zephyr.common.error.invalid", "Custom field value is too large, max allowed is +/-10^14. Custom field value ",customFieldValueRequest.getValue()), Response.Status.BAD_REQUEST, null);
        }

        if(CustomFieldValueValidationUtil.validateTextValueMaxLength(customFieldValueRequest.getCustomFieldType(),customFieldValueRequest.getValue(), STRING_VALUE_MAX_LENGTH)) {
            jsonObject = new JSONObject();
            return ZephyrUtil.constructErrorResponse(jsonObject, authContext.getI18nHelper().getText("field.limit.exceed.validationError.description", "custom field value","255"),
                    Response.Status.BAD_REQUEST, null);
        }


	    return null;
    }

    private boolean validateDisableCustomFieldForProjectAndCustomField(Long customFieldId, Long projectId, CustomFieldResourceDelegate customFieldResourceDelegate) {
        return customFieldResourceDelegate.getDisableCustomFieldForProjectAndCustomField(customFieldId,projectId);
    }

    private boolean validateCustomFieldOptionIds(String selectedOptions, Long inputCustomFieldId, CustomFieldResourceDelegate customFieldResourceDelegate) {

        if(StringUtils.isNotBlank(selectedOptions)) {
            String[] options = StringUtils.split(selectedOptions, ",");

            for (String option : options) {
                try{
                    CustomFieldOption customFieldOption = customFieldResourceDelegate.getCustomFieldOptionById(Integer.valueOf(option));
                    if(Objects.isNull(customFieldOption)) {
                        return Boolean.TRUE;
                    }else {
                        // validate whether it belongs to provided custom field id.
                        Long customFieldId = Long.valueOf(customFieldOption.getCustomField().getID());
                        if( !customFieldId.equals(inputCustomFieldId)) {
                            return Boolean.TRUE;
                        }
                    }
                }catch (NumberFormatException exception) {
                    log.error("Error occurred while parsing the option id from selected options.");
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }

    private boolean isCustomFieldAndEntityRecordExist(CustomFieldValueRequest customFieldValueRequest) {
        return customFieldValueResourceDelegate.getCustomFieldAndEntityRecord(customFieldValueRequest);
    }
}
