package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.jira.issue.Issue;
import com.opensymphony.util.TextUtils;
import com.thed.zephyr.je.rest.CustomFieldValueResource;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ZephyrWikiParser;
import com.thed.zephyr.je.model.Teststep;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since v1.0
 */
@XmlRootElement(name = "Teststep")
public class TeststepBean {
    @XmlElement
    public Integer id;
    @XmlElement
    public Integer orderId;
    @XmlElement
    public String step;
    @XmlElement
    public String data;
    @XmlElement
    public String result;
    @XmlElement
    public String createdBy;
    @XmlElement
    public String modifiedBy;
    @XmlElement
    public String htmlStep;
    @XmlElement
    public String htmlData;
    @XmlElement
    public String htmlResult;
    @XmlElement
    public String stepComment;
    @XmlElement
    public String htmlStepComment;
    @XmlElement
    public String stepExecutionStatus;
    @XmlElement
    List<Map<String, String>> attachmentsMap;
    @XmlElement
    Map<String, CustomFieldValueResource.CustomFieldValueResponse> customFields;
    @XmlElement
    Map<String, CustomFieldValueResource.CustomFieldValueRequest> customFieldValues;
    @XmlElement
    public Integer totalStepCount;

    public Map<String, String> customFieldValuesMap = new HashMap<>();

    public TeststepBean() {
        super();
    }

    public Integer getId() {
        return id;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public String getStep() {
        return step;
    }

    public String getData() {
        return data;
    }

    public String getResult() {
        return result;
    }

    public String getHtmlStep() {
        return htmlStep;
    }

    public String getHtmlData() {
        return htmlData;
    }

    public String getHtmlResult() {
        return htmlResult;
    }

    public String getStepComment() {
        return stepComment;
    }

    public String getHtmlStepComment() {
        return htmlStepComment;
    }

    public String getStepExecutionStatus() {
        return stepExecutionStatus;
    }

    public List<Map<String, String>> getAttachmentsMap() {
        return attachmentsMap;
    }

    public Map<String, CustomFieldValueResource.CustomFieldValueResponse> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, CustomFieldValueResource.CustomFieldValueResponse> customFields) {
        this.customFields = customFields;
    }

    public Map<String, CustomFieldValueResource.CustomFieldValueRequest> getCustomFieldValues() {
        return customFieldValues;
    }

    public void setCustomFieldValues(Map<String, CustomFieldValueResource.CustomFieldValueRequest> customFieldValues) {
        this.customFieldValues = customFieldValues;
    }

    public Map<String, String> getCustomFieldValuesMap() {
  		return customFieldValuesMap;
  	}

    public Integer getTotalStepCount() {
        return totalStepCount;
    }

    public void setTotalStepCount(Integer totalStepCount) {
        this.totalStepCount = totalStepCount;
    }

    public TeststepBean(Integer id, Integer orderId, String step, String data, String result, Issue issue) {
        super();
        this.id = id;
        this.orderId = orderId;
        this.step = step != null ? step : "";
        this.data = data != null ? data : "";
        this.result = result != null ? result : "";

        /* Added a generic method - ZFJ-2994*/
        this.htmlStep = getHtmlMarkupValue(this.step, issue);
        this.htmlData = getHtmlMarkupValue(this.data, issue);
        this.htmlResult = getHtmlMarkupValue(this.result, issue);
    }

    public TeststepBean(Integer id, Integer orderId, String step, String data, String result, String stepComment, String stepExecutionStatus, Issue issue) {
        super();
        this.id = id;
        this.orderId = orderId;
        this.step = step != null ? step : "";
        this.data = data != null ? data : "";
        this.result = result != null ? result : "";
        this.stepComment = stepComment != null ? stepComment : "";
        this.stepExecutionStatus = stepExecutionStatus != null ? stepExecutionStatus : "";

        /* Added a generic method - ZFJ-2994*/
        this.htmlStep = getHtmlMarkupValue(this.step, issue);
        this.htmlData = getHtmlMarkupValue(this.data, issue);
        this.htmlResult = getHtmlMarkupValue(this.result, issue);
        this.htmlStepComment = getHtmlMarkupValue(this.stepComment, issue);
    }

    public TeststepBean(Integer id, Integer orderId, String step, String data, String result, String stepComment, String stepExecutionStatus, boolean isMasked) {
        super();
        this.id = id;
        this.orderId = orderId;
        this.step = step != null ? step : "";
        this.data = data != null ? data : "";
        this.result = result != null ? result : "";
        this.stepComment = stepComment != null ? stepComment : "";
        this.stepExecutionStatus = stepExecutionStatus != null ? stepExecutionStatus : "";
        if (isMasked) {
            this.htmlStep = ApplicationConstants.MASKED_DATA;
            this.htmlData = ApplicationConstants.MASKED_DATA;
            this.htmlResult = ApplicationConstants.MASKED_DATA;
            this.htmlStepComment = ApplicationConstants.MASKED_DATA;
        }
    }

    public TeststepBean(Teststep step, Issue issue, List<Map<String, String>> attachmentsMap, Map<String, CustomFieldValueResource.CustomFieldValueResponse> customFields) {
        super();
        this.id = step.getID();
        this.orderId = step.getOrderId();
        this.step = step.getStep() != null ? step.getStep() : "";
        this.data = step.getData() != null ? step.getData() : "";
        this.result = step.getResult() != null ? step.getResult() : "";

        this.createdBy = step.getCreatedBy();
        this.modifiedBy = step.getModifiedBy();

        /* Added a generic method - ZFJ-2994*/
        this.htmlStep = getHtmlMarkupValue(this.step, issue);
        this.htmlData = getHtmlMarkupValue(this.data, issue);
        this.htmlResult = getHtmlMarkupValue(this.result, issue);

        this.attachmentsMap = attachmentsMap;
        this.customFields = customFields;
    }

    public TeststepBean(Map<String, CustomFieldValueResource.CustomFieldValueResponse> customFields) {
        super();
        this.customFields = customFields;
    }

    public TeststepBean(Teststep step, Issue issue) {
        super();
        this.id = step.getID();
        this.orderId = step.getOrderId();
        this.step = step.getStep() != null ? step.getStep() : "";
        this.data = step.getData() != null ? step.getData() : "";
        this.result = step.getResult() != null ? step.getResult() : "";
        this.createdBy = step.getCreatedBy();
        this.modifiedBy = step.getModifiedBy();

        /* Added a generic method - ZFJ-2994*/
        this.htmlStep = getHtmlMarkupValue(this.step, issue);
        this.htmlData = getHtmlMarkupValue(this.data, issue);
        this.htmlResult = getHtmlMarkupValue(this.result, issue);
    }

    /**
     *
     * @param value
     * @param issue
     * @return
     */
    private String getHtmlMarkupValue(String value, Issue issue) {
        if(null != issue) {
            String htmlMarkupValue = ZephyrWikiParser.WIKIPARSER.convertWikiToHTML(value, issue);

            if(StringUtils.isNotBlank(htmlMarkupValue)) {
                return htmlMarkupValue.replaceAll("\\r?\\n\n", "<br/>");
            }else {
                return htmlMarkupValue;
            }
        } else {
            return StringUtils.EMPTY;
        }
    }
}
