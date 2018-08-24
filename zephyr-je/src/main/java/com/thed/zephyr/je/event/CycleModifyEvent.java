package com.thed.zephyr.je.event;

import java.util.Collection;
import java.util.Map;

import com.atlassian.event.api.AsynchronousPreferred;
import com.google.common.collect.Table;
import com.thed.zephyr.je.model.Cycle;

@AsynchronousPreferred
public class CycleModifyEvent extends ZephyrEvent {

	private Cycle cycle;
	private String userName;
	private Collection<Cycle> cycles;
	private Table<String, String, Object> changePropertyTable;
	
    public CycleModifyEvent(Collection<Cycle> cycles, Map<String, Object> params,
    		EventType eventType, String userName){
        super(params, eventType);
        this.cycles=cycles;
        this.userName = userName;
    }
	
	public CycleModifyEvent(Cycle cycle, Table<String, String, Object> changePropertyTable,
    		EventType eventType, String userName){
        super(null, eventType);
        this.cycle=cycle;
        this.changePropertyTable = changePropertyTable;
        this.userName = userName;
    }

    public int hashCode(){
        int result = super.hashCode();
        result = 29 * result + (cycle != null ? cycle.hashCode() : 0);
        result = 29 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 29 * result + (changePropertyTable != null ? changePropertyTable.hashCode() : 0);
        result = 29 * result + (userName != null ? userName.hashCode() : 0);
        result = 29 * result + (cycles != null ? cycles.hashCode() : 0);
        return result;
    }

	public Cycle getCycle() {
		return cycle;
	}

	public void setCycle(Cycle cycle) {
		this.cycle = cycle;
	}

	public Table<String, String, Object> getChangePropertyTable() {
		return changePropertyTable;
	}

	public void setChangePropertyTable(
			Table<String, String, Object> changePropertyTable) {
		this.changePropertyTable = changePropertyTable;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Collection<Cycle> getCycles() {
		return cycles;
	}

	public void setCycles(Collection<Cycle> cycles) {
		this.cycles = cycles;
	}	
	
}
