package com.thed.zephyr.je.config.customfield;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.customfields.impl.AbstractCustomFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.JiraAuthenticationContextImpl;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.util.velocity.VelocityRequestContext;
import com.atlassian.jira.web.util.FileIconUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.TeststepModifyEvent;
import com.thed.zephyr.je.helper.TestStepResourceHelper;
import com.thed.zephyr.je.model.Attachment;
import com.thed.zephyr.je.model.TestStepCf;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.rest.TeststepResource;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.service.AttachmentManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.je.vo.TeststepBean;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrWikiParser;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.NotNullPredicate;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.*;

public class TeststepCFType extends AbstractCustomFieldType {

    private final CustomFieldValuePersister persister;
    private final GenericConfigManager genericConfigManager;
    private final TeststepManager teststepManager;
    private final ActiveObjects ao;
    private final EventPublisher eventPublisher;
    private final JiraAuthenticationContext authContext;
    private final AttachmentManager attachmentManager;
    private final FileIconUtil fileIconUtil;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final CustomFieldValueResourceDelegate customFieldValueResourceDelegate;

    /**
     * Used in the database representation of a singular value. Treated as a
     * regex when checking text input.
     */
    public static final String DB_SEP = "###";

    // The type of data in the database, one entry per value in this field
    private static final PersistenceFieldType DB_TYPE = PersistenceFieldType.TYPE_UNLIMITED_TEXT;
    protected static final Logger log = Logger.getLogger(TeststepCFType.class);

    public TeststepCFType(
            final CustomFieldValuePersister customFieldValuePersister,
            final GenericConfigManager genericConfigManager,
            final TeststepManager teststepManager,
            final ActiveObjects ao,
            final EventPublisher eventPublisher,
            JiraAuthenticationContext authContext,
            AttachmentManager attachmentManager,
            final FileIconUtil fileIconUtil,final DateTimeFormatterFactory dateTimeFormatterFactory,
            final CustomFieldValueResourceDelegate customFieldValueResourceDelegate) {
        this.persister = customFieldValuePersister;
        this.genericConfigManager = genericConfigManager;
        this.teststepManager = teststepManager;
        this.ao = ao;
        this.eventPublisher = eventPublisher;
        this.authContext = authContext;
        this.attachmentManager = attachmentManager;
        this.fileIconUtil = fileIconUtil;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.customFieldValueResourceDelegate = customFieldValueResourceDelegate;
    }

    @Override
    public String getChangelogValue(CustomField field, Object value) {

        if (value == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        Collection<StepWrapper> teststepWrappers = (Collection<StepWrapper>) value;
        for (StepWrapper teststep : teststepWrappers) {
            sb.append(teststep.toString());
            // Newlines are text not HTML here
            sb.append(", ");

        }

        log.debug("TeststepCFType - getChangelogValue entered with " + sb);
        return null;
    }

    @Override
    public Object getValueFromIssue(CustomField field, Issue issue) {
        log.debug("TeststepCFType - Inside getValueFromIssue - ");

        Object velocityCache = JiraAuthenticationContextImpl.getRequestCache().get("jira.velocity.request.context");
        if(velocityCache != null){
            String url = ((VelocityRequestContext)velocityCache).getRequestParameters().getRequestURL();
            if(!(url.endsWith(".html")
                    || url.endsWith(".xml")
                    || url.endsWith(".doc")
                    || url.endsWith(".xls")
                    || url.contains("CloneIssueDetails.jspa"))){
                return null;
            }
        }

        // This is also called to display a default value in view.vm
        // in which case the issue is a dummy one with no key
        if (issue == null || issue.getKey() == null) {
            return null;
        }

        // These are the database representation of the singular objects
        log.debug("Getting Steps for Issue with ID" + issue.getId());
        final List<Teststep> values = teststepManager.getTeststeps(issue.getId(), Optional.empty(), Optional.empty());

        if ((values != null) && !values.isEmpty()) {

            List<TeststepBean> result = new ArrayList<TeststepBean>();
            for (Iterator it = values.iterator(); it.hasNext();) {
                Teststep teststepObj = (Teststep) it.next();
                if (teststepObj == null) {
                    continue;
                }
                //TeststepImpl schedule = (TeststepImpl) getSingularObjectFromString(dbValue);
               // TeststepBean stepBean = new TeststepBean(teststepObj, issue);
                TeststepBean teststepBean = new TeststepBean(teststepObj, issue,getAttachmentsMap(teststepObj.getID()), null);
                //TeststepImpl teststep = new TeststepImpl(issue.getId(),teststepObj.getOrderId(),teststepObj.getStep(),teststepObj.getData(), teststepObj.getResult());
                /**
                 * Convert the wiki markup to text for export.
                 * Checking that action is not clone test steps from URL
                 */
                if(velocityCache != null){
                    String url = ((VelocityRequestContext)velocityCache).getRequestParameters().getRequestURL();
                    if(!(url.contains("CloneIssueDetails.jspa"))){
                        teststepBean.step = ZephyrWikiParser.WIKIPARSER.convertWikiToText(teststepBean.step);
                        teststepBean.data = ZephyrWikiParser.WIKIPARSER.convertWikiToText(teststepBean.data);
                        teststepBean.result = ZephyrWikiParser.WIKIPARSER.convertWikiToText(teststepBean.result);
                        teststepBean.stepComment = ZephyrWikiParser.WIKIPARSER.convertWikiToText(teststepBean.stepComment);
                    }
                }

                List<TestStepCf> testStepCfList = teststepManager.getCustomFieldValuesForTeststep(teststepObj.getID());
                if(CollectionUtils.isNotEmpty(testStepCfList)) {
                    testStepCfList.forEach(testStepCf -> {
                        teststepBean.getCustomFieldValuesMap().put(testStepCf.getCustomField().getName().replaceAll("\\s+",StringUtils.EMPTY), getValue(testStepCf));
                    });
                }
                result.add(teststepBean);
            }

            return result;

        } else {
            return null;
        }
    }

    @Override
    public void createValue(CustomField field, Issue issue, Object value) {

        if (value == null) {
            return;
        }

        log.debug("TeststepCFType - inside createValue with value - " + value);
        Collection<Object> inputStepsCollection = (Collection<Object>) value;

        List<Teststep> stepList = new ArrayList<Teststep>();
        List<Integer> incomingStepIds = new ArrayList<>();
        List<Integer> stepsToBeDeleted = new ArrayList<Integer>();
        for(Object testStepObj : inputStepsCollection){
            try{
                TeststepBean teststepPO = null;
                if(testStepObj instanceof StepWrapper) {
                    StepWrapper teststepWrapper = (StepWrapper) testStepObj;
                    if (teststepWrapper == null)        //Simple null check
                        continue;

                    if (teststepWrapper.stepaction == StepWrapper.StepAction.noChange)    //No change in client, nothing to persist, return. Validation already should have caught required errors
                        return;

                    if (teststepWrapper.data == null)        //For some reason a particular step is null, should never happen
                        continue;

                    if (teststepWrapper.stepaction == StepWrapper.StepAction.deleteAll)    //all steps deleted, this will be taken care of outside this loop.
                        break;

                    teststepPO = teststepWrapper.data;
                }else{
                    teststepPO = (TeststepBean) testStepObj;
                }
                log.debug("Teststep : Save Teststep -> " + teststepPO);

                //Convert POJO Teststep to ActiveObject Teststep
                boolean clonedIssue = false;
                Teststep teststepAO = null;
                if(teststepPO.getId() == null || teststepPO.getId() == 0){
                    teststepAO = ao.create(Teststep.class);
                    teststepAO.setCreatedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));
                }else {
                    teststepAO = ao.get(Teststep.class, teststepPO.getId());
                    if(teststepAO.getIssueId().intValue() != issue.getId().intValue()) {
                        clonedIssue = true;
                    } else {
                        incomingStepIds.add(teststepPO.getId());
                    }
                    // publishing TeststepModifyEvent
                    Table<String, String, Object> changePropertyTable = TeststepResource.TeststepUtils.changePropertyTable(teststepAO, teststepPO);
                    eventPublisher.publish(new TeststepModifyEvent(teststepAO, changePropertyTable, EventType.TESTSTEP_UPDATED,
                            UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
                }
                if(teststepAO != null && !clonedIssue) {
                    populateTestStepObject(issue, stepList, teststepPO,
                            teststepAO);
                } else {
                    teststepAO = ao.create(Teststep.class);

                    populateTestStepObject(issue, stepList, teststepPO, teststepAO);
                    populateTestStepCustomFields(teststepAO,teststepPO, issue.getProjectId());
                }
            }
            catch(Exception e){
                log.error("Error in populating Steps data in createStep, " + e.getMessage());
            }
        }
        List<Teststep> steps = teststepManager.getTeststeps(issue.getId(), Optional.empty(), Optional.empty());
        steps.forEach(step -> stepsToBeDeleted.add(step.getID()));
        stepsToBeDeleted.removeAll(incomingStepIds);
        //save test steps
        teststepManager.saveTeststeps(stepList);
        //remove test steps
        stepsToBeDeleted.forEach(stepId -> {
            // publishing TeststepModifyEvent
            eventPublisher.publish(new TeststepModifyEvent(Lists.newArrayList(teststepManager.getTeststep(stepId)), null, EventType.TESTSTEP_DELETED,
                    UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
            teststepManager.removeTeststep(stepId);
        });

    }

    private void populateTestStepObject(Issue issue, List<Teststep> stepList,
                                        TeststepBean teststepPO, Teststep teststepAO) {
        teststepAO.setIssueId(issue.getId());
        teststepAO.setOrderId(teststepPO.getOrderId());
        teststepAO.setData(teststepPO.getData());
        teststepAO.setStep(teststepPO.getStep());
        teststepAO.setResult(teststepPO.getResult());
        teststepAO.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(ComponentAccessor.getJiraAuthenticationContext())));
        stepList.add(teststepAO);
    }

    @Override
    public void updateValue(CustomField field, Issue issue, Object value) {
        //Teststep updates are done from Teststepdetails block. So don't do any updates here.
        log.debug("TeststepCFType - inside updateValue() - this method should not get called. Anyways do nothing");
    }

    /**
     * For removing the field, not for removing one value
     * Need to find out how to implement it with AO backend
     */
    @Override
    public Set<Long> remove(CustomField field) {
        //Below is default implementation to remove all values associated given Field.
        //But it will not work for our custom field as we store Teststep customfield into active objects.

        //To make this work with our AO storage, first we need to store field id into TestStep AO table for each row.
        //Then here make a call something like teststepManager.deleteTeststepCustomfield()
        //Well this is still a TODO

        return persister.removeAllValues(field.getId());
    }

    /**
     *
     * @param relevantParams
     * @param errorCollectionToAddTo
     * @param config
     * Sample params = ArrayList of "{"type":"req","issueaction":"edit","stepaction":"addUpdate","data":{"step":"sf","data":"asdf","result":"sadf","orderId":1}}"

     */
    @Override
    public void validateFromParams(CustomFieldParams relevantParams,
                                   ErrorCollection errorCollectionToAddTo, FieldConfig config) {

        log.debug("TeststepCFType - validateFromParams: " + relevantParams.getKeysAndValues());

        try {
            String issueIdKey = "com.atlassian.jira.internal.issue_id";
            String issueId = String.valueOf(relevantParams.getFirstValueForKey(issueIdKey));
            Collection<StepWrapper> stepWrapperCollection = (Collection<StepWrapper>) getValueFromCustomFieldParams(relevantParams);
            if(stepWrapperCollection != null && stepWrapperCollection.size() > 0){
                StepWrapper stepWrapper = stepWrapperCollection.iterator().next();
                if(stepWrapper.type == StepWrapper.CFType.req && stepWrapper.stepaction == StepWrapper.StepAction.noChange && stepWrapper.data == null){
                    if(stepWrapper.issueaction == StepWrapper.IssueAction.NEW){
                        errorCollectionToAddTo.addError(getName(), getI18nBean().getText("issue.field.required", this.getName()), ErrorCollection.Reason.VALIDATION_FAILED);
                    }else if(stepWrapper.issueaction == StepWrapper.IssueAction.EDIT){
                        List<Teststep> steps = teststepManager.getTeststeps(Long.valueOf(issueId), Optional.empty(), Optional.empty());
                        if(steps.size() < 1){
                            errorCollectionToAddTo.addError(getName(), getI18nBean().getText("issue.field.required", this.getName()), ErrorCollection.Reason.VALIDATION_FAILED);
                        }
                    }
                }
            }
        } catch (FieldValidationException fve) {
            errorCollectionToAddTo.addError(config.getCustomField().getId(), fve.getMessage());
        } catch (Exception ex){
            log.error("Field Validation failed for TeststepCFType " + ex.getMessage());
        }

    }

    @Override
    public Object getValueFromCustomFieldParams(CustomFieldParams parameters)
            throws FieldValidationException {

        log.debug("TeststepCFType - getValueFromCustomFieldParams: " + parameters.getKeysAndValues());

        // Strings in the order they appeared in the web page
        final Collection values = parameters.getValuesForNullKey();
        Collection<StepWrapper> value = null;

        if ((values != null) && !values.isEmpty()) {
            value = new ArrayList();
            for (Iterator it = values.iterator(); it.hasNext(); ) {
                Object obj = it.next();
                if(obj instanceof String) {
                    StepWrapper stepsWrapper = StepWrapper.parse((String) obj);
                    log.debug("TeststepCFType - Teststep Object to be added - " + stepsWrapper);
                    value.add(stepsWrapper);
                }
            }
            return value;
        } else {
            return value;
        }
    }

    /**
     * This method is used to create the $value object in Velocity templates.
     */
    @Override
    public Object getStringValueFromCustomFieldParams(
            CustomFieldParams parameters) {

        log.debug("TeststepCFType - getStringValueFromCustomFieldParams: " + parameters.getKeysAndValues());
        return parameters.getAllValues();
    }

    /**
     * Convert a transport object (a Collection of Carrier objects) to its
     * database representation and store it in the database.
     */
    @Override
    public void setDefaultValue(FieldConfig fieldConfig, Object value) {
        log.debug("TeststepCFType - setDefaultValue with object " + value);

        Collection teststepStrings = getDbValueFromCollection(value);

        if (teststepStrings != null) {
            teststepStrings = new ArrayList(teststepStrings);
            genericConfigManager.update(CustomFieldType.DEFAULT_VALUE_TYPE, fieldConfig.getId().toString(), teststepStrings);

        }
    }

    /**
     * Retrieve the stored default value (if any) from the database
     * and convert it to a transport object (a Collection of schedule objects).
     */
    @Override
    public Object getDefaultValue(FieldConfig fieldConfig) {

        final Object o = genericConfigManager.retrieve(CustomFieldType.DEFAULT_VALUE_TYPE, fieldConfig.getId().toString());
        log.debug("TeststepCFType - getDefaultValue with database value " + o);

        Collection<TeststepBean> collectionOfteststeps = null;

        if (o instanceof Collection) {
            collectionOfteststeps = (Collection) o;
        } else if (o instanceof TeststepBean) {
            log.debug("TeststepCFType - Backwards compatible default value");
            collectionOfteststeps = ImmutableList.of((TeststepBean)o);
        }

        if (collectionOfteststeps == null) {
            log.debug("TeststepCFType - No default value exists. Returning null.");
            return null; // No default value exists
        }

        final Collection collection = CollectionUtils.collect(collectionOfteststeps, new Transformer() {
            // Convert a database value (String) to a singular Object (teststepImpl)
            @Override
            public Object transform(final Object input) {
                if (input == null) {
                    return null;
                }

                String dbValue = (String)input;
                return getSingularObjectFromString(dbValue);
            }
        });

        CollectionUtils.filter(collection, NotNullPredicate.getInstance());

        log.debug("TeststepCFType - getDefaultValue returning " + collection);
        return collection;
    }

    /**
     * Convert a database representation of a FieldType object into
     * a Field object. This method is also used for bulk moves and imports.
     *
     * But in our case, data stored into Active Objects. Hence there will not be any dbValue available to us.
     * For the time, return back dummy Teststep POJO.
     */
    @Override
    public Object getSingularObjectFromString(String dbValue)
            throws FieldValidationException {

        log.debug("TeststepCFType -*- getSingularObjectFromString: " + dbValue);
        return new TeststepBean(null,null,"Step - getSOFS", "Data - getSFOS", "Result - getSFOS", null);
    }

    /**
     * returns JSON representation of the object. Used during cloning procedure.
     */
    @Override
    public String getStringFromSingularObject(Object singularObject) {
        assertObjectImplementsType(TeststepBean.class, singularObject);
        TeststepBean teststepPO = (TeststepBean) singularObject;
        JSONObject jo = new JSONObject();
        try {
            jo.put("id", teststepPO.getId());
            jo.put("orderId", teststepPO.getOrderId());
            jo.put("step", teststepPO.getStep());
            jo.put("data", teststepPO.getData());
            jo.put("result", teststepPO.getResult());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jo.toString();
    }

    // Helper Methods

    /**
     * Convert the Transport object to a collection of the
     * representation used in the database.
     */
    private Collection getDbValueFromCollection(final Object value)
    {
        log.debug("TeststepCFType - getDbValueFromCollection: " + value);

        if (value == null) {
            return Collections.EMPTY_LIST;
        }

        Collection<TeststepBean> teststepsCollection = (Collection<TeststepBean>) value;
        List<String> result = new ArrayList<String>();
        for (TeststepBean teststepPO : teststepsCollection) {
            if (teststepPO == null) {
                continue;
            }

            StringBuffer sb = new StringBuffer();
            sb.append(teststepPO.getOrderId());
            sb.append(DB_SEP);
            sb.append(teststepPO.getStep());
            sb.append(DB_SEP);
            sb.append(teststepPO.getData());
            sb.append(DB_SEP);
            sb.append(teststepPO.getResult());
            result.add(sb.toString());
        }
        return result;
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StepWrapper{
        static enum CFType{req, opt};
        static enum IssueAction{NEW, EDIT};
        static enum StepAction{deleteAll, noChange, addUpdate};

        public CFType type;
        public IssueAction issueaction;
        public StepAction stepaction;
        public TeststepBean data;

        @Override
        public String toString() {
            return "StepWrapper{" +
                    "data=" + data +
                    ", type=" + type +
                    ", issueaction=" + issueaction +
                    ", stepaction=" + stepaction +
                    '}';
        }

        public static StepWrapper parse(String jsonString){
            StepWrapper wrapper = null;
            try {
                wrapper = new StepWrapper();
                JSONObject obj = new JSONObject(jsonString);
                wrapper.type = CFType.valueOf(obj.optString("type"));
                wrapper.issueaction = IssueAction.valueOf(StringUtils.upperCase(obj.optString("issueaction")));
                wrapper.stepaction = StepAction.valueOf(obj.optString("stepaction"));
                String dataString = obj.optString("data");
                wrapper.data = parseStepBean(obj.optString("data"));
            } catch (JSONException e) {
                log.debug("Error in parsing steps" + e.getMessage());
            } catch (IllegalArgumentException iae){
				/* Backward compatibility */
                if(wrapper.type == null || wrapper.issueaction == null || wrapper.stepaction == null){
                    wrapper.data = parseStepBean(jsonString);
                }
            }
            return wrapper;
        }

        public static TeststepBean parseStepBean(String stepJson){
            if(StringUtils.isEmpty(stepJson) || StringUtils.equals("{}", stepJson))
                return null;
            Integer orderId = 0;
            Integer id = 0;
            String step = "";
            String data = "";
            String result = "";
            JSONObject stepObject = null;
            try {
                stepObject = new JSONObject(stepJson);
                id = stepObject.optInt("id");
                orderId = stepObject.optInt("orderId");
                step = stepObject.optString("step");
                data = stepObject.optString("data");
                result = stepObject.optString("result");
            } catch (JSONException e) {
                log.debug("Error in parsing steps" + e.getMessage());
            }
            TeststepBean teststepPO = new TeststepBean(id, orderId, step, data, result, null);
            return teststepPO;
        }
    }

    private List<Map<String,String>> getAttachmentsMap(Integer stepId) {
        List<Attachment> attachmentList = attachmentManager.getAttachmentsByEntityIdAndType(stepId, ApplicationConstants.TEST_STEP_TYPE);
        return convertAttachmentListDataToMap(attachmentList);
    }

    /**
     *
     * @param attachmentList
     * @return
     */
    private List<Map<String,String>> convertAttachmentListDataToMap(List<Attachment> attachmentList) {
        List<Map<String,String>> responseMap = new ArrayList<Map<String,String>>();
        if(CollectionUtils.isEmpty(attachmentList)) return responseMap;
        attachmentList.forEach(attachment -> responseMap.add(attachmentObjectToMap(attachment)));
        return responseMap;
    }


    /**
     *
     * @param attachment
     * @return
     */
    private Map<String, String> attachmentObjectToMap(Attachment attachment) {
        Map<String,String> attachmentMap = new HashMap<String,String>();
        attachmentMap.put("fileId", String.valueOf(attachment.getID()));
        attachmentMap.put("fileName", attachment.getFileName());
        attachmentMap.put("dateCreated", dateTimeFormatterFactory.formatter().forLoggedInUser().format(attachment.getDateCreated()));
        return attachmentMap;
    }

    /**
     *
     * @param object
     * @return
     */
    private String getValue(Object object) {
        Object obj = null;
        if (object instanceof TestStepCf) {
            TestStepCf testStepCf = (TestStepCf) object;
            if (StringUtils.isNotBlank(testStepCf.getStringValue())) {
                obj = testStepCf.getStringValue();
            } else if (null != testStepCf.getDateValue()) {
                obj = testStepCf.getDateValue();
            } else if (null != testStepCf.getNumberValue()) {
                obj = testStepCf.getNumberValue();
            } else if (StringUtils.isNotBlank(testStepCf.getLargeValue())) {
                obj = testStepCf.getLargeValue();
            }
        }
        return null != obj ? String.valueOf(obj) : StringUtils.EMPTY;
    }

    private void populateTestStepCustomFields(Teststep teststepAO, TeststepBean teststepPO, Long projectId) {
        TestStepResourceHelper testStepResourceHelper = new TestStepResourceHelper(teststepManager,authContext,customFieldValueResourceDelegate);
        testStepResourceHelper.cloneCustomFieldsData(teststepPO.getId(),teststepAO.getID(), projectId,projectId);
    }
}