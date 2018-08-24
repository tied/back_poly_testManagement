package com.thed.zephyr.je.vo;

import com.thed.zephyr.je.config.model.ExecutionStatus;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Map;


@XmlRootElement(name = "ZQLSearchResult")
public class ZQLSearchResultBean implements Serializable {
    @XmlElement
    private List<ZQLScheduleBean> executions;
    @XmlElement
    private List<ExecutionStatus> executionStatuses;
    @XmlElement
    private List<ExecutionStatus> stepExecutionStatuses;
    @XmlElement
    private String shouldWeUpdateStepStatus;
    @XmlElement
    private Boolean shouldWeClearDefectMapping;
    @XmlElement
    private String zqlQuery;
    @XmlElement
    private Integer offset;
    @XmlElement
    private Integer currentIndex;
    @XmlElement
    private Integer maxResultAllowed;
    @XmlElement
    private List<Integer> linksNew;
    @XmlElement
    private Integer totalCount;
    @XmlElement
    private String expand;
    @XmlElement
    private Integer selectedExecution;
    @XmlElement
    private List<Integer> executionIds;
    @XmlElement
    private Integer totalExecutions;
    @XmlElement
    private Integer totalExecuted;
    @XmlElement
    private Integer totalDefectCount;
    @XmlElement
    private Integer totalOpenDefectCount;

    @XmlElement
    private String executionSummaries;
    
    @XmlElement
	private Map<String, Map<String, String>> totalExecutionWorkflowTimePerCycleMap;


	public ZQLSearchResultBean() {
    }

    public ZQLSearchResultBean(List<ZQLScheduleBean> schedules, String zqlQuery,
                               Integer offset, Integer currentIndex, Integer maxResultAllowed, String expand,
                               List<Integer> linksNew, Integer totalCount, Boolean shouldWeClearDefectMapping, String shouldWeUpdateStepStatus) {
        this.executions = schedules;
        this.zqlQuery = zqlQuery;
        this.offset = offset;
        this.currentIndex = currentIndex;
        this.maxResultAllowed = maxResultAllowed;
        this.expand = expand;
        this.linksNew = linksNew;
        this.totalCount = totalCount;
        this.shouldWeClearDefectMapping = shouldWeClearDefectMapping;
        this.shouldWeUpdateStepStatus = shouldWeUpdateStepStatus;
    }

    public String getShouldWeUpdateStepStatus() {
        return shouldWeUpdateStepStatus;
    }

    public void setShouldWeUpdateStepStatus(String shouldWeUpdateStepStatus) {
        this.shouldWeUpdateStepStatus = shouldWeUpdateStepStatus;
    }

    public Boolean getShouldWeClearDefectMapping() {
        return shouldWeClearDefectMapping;
    }

    public void setShouldWeClearDefectMapping(Boolean shouldWeClearDefectMapping) {
        this.shouldWeClearDefectMapping = shouldWeClearDefectMapping;
    }

    public String getZqlQuery() {
        return zqlQuery;
    }

    public void setZqlQuery(String zqlQuery) {
        this.zqlQuery = zqlQuery;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(Integer currentIndex) {
        this.currentIndex = currentIndex;
    }

    public Integer getMaxResultAllowed() {
        return maxResultAllowed;
    }

    public void setMaxResultAllowed(Integer maxResultAllowed) {
        this.maxResultAllowed = maxResultAllowed;
    }

    public List<Integer> getLinksNew() {
        return linksNew;
    }

    public void setLinksNew(List<Integer> linksNew) {
        this.linksNew = linksNew;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public String getExpand() {
        return expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
    }


    public List<Integer> getExecutionIds() {
        return executionIds;
    }

    public void setExecutionIds(List<Integer> executionIds) {
        this.executionIds = executionIds;
    }

    public List<ZQLScheduleBean> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ZQLScheduleBean> executions) {
        this.executions = executions;
    }

    public Integer getSelectedExecution() {
        return selectedExecution;
    }

    public void setSelectedExecution(Integer selectedExecution) {
        this.selectedExecution = selectedExecution;
    }

    public List<ExecutionStatus> getExecutionStatuses() {
        return executionStatuses;
    }

    public void setExecutionStatuses(List<ExecutionStatus> executionStatuses) {
        this.executionStatuses = executionStatuses;
    }

    public List<ExecutionStatus> getStepExecutionStatuses() {
        return stepExecutionStatuses;
    }

    public void setStepExecutionStatuses(List<ExecutionStatus> stepExecutionStatuses) {
        this.stepExecutionStatuses = stepExecutionStatuses;
    }

    public Integer getTotalExecutions() {
        return totalExecutions;
    }

    public void setTotalExecutions(Integer totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public Integer getTotalExecuted() {
        return totalExecuted;
    }

    public void setTotalExecuted(Integer totalExecuted) {
        this.totalExecuted = totalExecuted;
    }

    public Integer getTotalDefectCount() {
        return totalDefectCount;
    }

    public void setTotalDefectCount(Integer totalDefectCount) {
        this.totalDefectCount = totalDefectCount;
    }

    public Integer getTotalOpenDefectCount() {
        return totalOpenDefectCount;
    }

    public void setTotalOpenDefectCount(Integer totalOpenDefectCount) {
        this.totalOpenDefectCount = totalOpenDefectCount;
    }

    public String getExecutionSummaries() {
        return executionSummaries;
    }

    public void setExecutionSummaries(String executionSummaries) {
        this.executionSummaries = executionSummaries;
    }

    @Override
    public String toString() {
        return "ZQLSearchResultBean{" +
                "executions=" + executions +
                ", executionStatuses=" + executionStatuses +
                ", stepExecutionStatuses=" + stepExecutionStatuses +
                ", shouldWeUpdateStepStatus='" + shouldWeUpdateStepStatus + '\'' +
                ", shouldWeClearDefectMapping=" + shouldWeClearDefectMapping +
                ", zqlQuery='" + zqlQuery + '\'' +
                ", offset=" + offset +
                ", currentIndex=" + currentIndex +
                ", maxResultAllowed=" + maxResultAllowed +
                ", linksNew=" + linksNew +
                ", totalCount=" + totalCount +
                ", expand='" + expand + '\'' +
                ", selectedExecution=" + selectedExecution +
                ", executionIds=" + executionIds +
                ", totalExecutions=" + totalExecutions +
                ", totalExecuted=" + totalExecuted +
                ", totalDefectCount=" + totalDefectCount +
                ", totalOpenDefectCount=" + totalOpenDefectCount +
                ", executionSummaries='" + executionSummaries + '\'' +
                ", totalExecutionWorkflowTimePerCycleMap=" + totalExecutionWorkflowTimePerCycleMap +
                '}';
    }
    
    public  Map<String, Map<String, String>> getTotalExecutionWorkflowTimePerCycleMap() {
		return totalExecutionWorkflowTimePerCycleMap;
	}

	public void setTotalExecutionWorkflowTimePerCycleMap(Map<String, Map<String, String>> totalExecutionWorkflowTimePerCycleMap) {
		this.totalExecutionWorkflowTimePerCycleMap = totalExecutionWorkflowTimePerCycleMap;		
	}
}
