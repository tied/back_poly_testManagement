package com.thed.zephyr.je.event;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Table;
import com.thed.zephyr.je.model.Teststep;

public class TeststepModifyEvent extends ZephyrEvent {

	private Teststep testStep;
	private String userName;
	private Collection<Teststep> testSteps;
	private Table<String, String, Object> changePropertyTable;
	
    public TeststepModifyEvent(Collection<Teststep> testSteps, Map<String, Object> params,
    		EventType eventType, String userName){
        super(params, eventType);
        this.testSteps=testSteps;
        this.userName = userName;
    }
	
	public TeststepModifyEvent(Teststep testStep, Table<String, String, Object> changePropertyTable,
    		EventType eventType, String userName){
        super(null, eventType);
        this.testStep = testStep;
        this.changePropertyTable = changePropertyTable;
        this.userName = userName;
    }

    public int hashCode(){
        int result = super.hashCode();
        result = 29 * result + (testStep != null ? testStep.hashCode() : 0);
        result = 29 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 29 * result + (changePropertyTable != null ? changePropertyTable.hashCode() : 0);
        result = 29 * result + (userName != null ? userName.hashCode() : 0);
        result = 29 * result + (testSteps != null ? testSteps.hashCode() : 0);
        return result;
    }

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Collection<Teststep> getTestSteps() {
		return testSteps;
	}

	public void setTestSteps(Collection<Teststep> testSteps) {
		this.testSteps = testSteps;
	}

	public Table<String, String, Object> getChangePropertyTable() {
		return changePropertyTable;
	}

	public void setChangePropertyTable(
			Table<String, String, Object> changePropertyTable) {
		this.changePropertyTable = changePropertyTable;
	}

	public Teststep getTestStep() {
		return testStep;
	}

	public void setTestStep(Teststep testStep) {
		this.testStep = testStep;
	}
	
}
