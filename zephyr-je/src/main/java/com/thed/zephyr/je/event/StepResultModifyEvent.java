package com.thed.zephyr.je.event;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Table;
import com.thed.zephyr.je.model.StepResult;

public class StepResultModifyEvent  extends ZephyrEvent {

	private StepResult stepResult;
	private String userName;
	private Collection<StepResult> stepResults;
	private Table<String, String, Object> changePropertyTable;
	
    public StepResultModifyEvent(Collection<StepResult> stepResults, Map<String, Object> params,
    		EventType eventType, String userName){
        super(params, eventType);
        this.stepResults=stepResults;
        this.userName = userName;
    }
	
	public StepResultModifyEvent(StepResult stepResult, Table<String, String, Object> changePropertyTable,
    		EventType eventType, String userName){
        super(null, eventType);
        this.stepResult=stepResult;
        this.changePropertyTable = changePropertyTable;
        this.userName = userName;
    }

    public int hashCode(){
        int result = super.hashCode();
        result = 29 * result + (stepResult != null ? stepResult.hashCode() : 0);
        result = 29 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 29 * result + (changePropertyTable != null ? changePropertyTable.hashCode() : 0);
        result = 29 * result + (userName != null ? userName.hashCode() : 0);
        result = 29 * result + (stepResults != null ? stepResults.hashCode() : 0);
        return result;
    }

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Collection<StepResult> getStepResults() {
		return stepResults;
	}

	public void setStepResults(Collection<StepResult> stepResults) {
		this.stepResults = stepResults;
	}

	public Table<String, String, Object> getChangePropertyTable() {
		return changePropertyTable;
	}

	public void setChangePropertyTable(
			Table<String, String, Object> changePropertyTable) {
		this.changePropertyTable = changePropertyTable;
	}

	public StepResult getStepResult() {
		return stepResult;
	}

	public void setStepResult(StepResult stepResult) {
		this.stepResult = stepResult;
	}
	
}
