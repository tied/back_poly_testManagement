package com.thed.zephyr.je.index;

import com.google.common.collect.ComparisonChain;

import java.util.List;

public class DefectSummaryModel  implements Comparable<DefectSummaryModel>  {
	private Integer defectId;
	private String defectKey;
	private String defectSummary;
	private String defectStatus;
	private String defectResolution;
	private Integer testCount;
	private List<String> associatedTestIds;
    private String priority;
	
	public Integer getDefectId() {
		return defectId;
	}
	public void setDefectId(Integer defectId) {
		this.defectId = defectId;
	}
	public String getDefectSummary() {
		return defectSummary;
	}
	public void setDefectSummary(String defectSummary) {
		this.defectSummary = defectSummary;
	}
	
	public String getDefectStatus() {
		return defectStatus;
	}
	public void setDefectStatus(String defectStatus) {
		this.defectStatus = defectStatus;
	}

	public String getDefectResolution() {
		return defectResolution;
	}

	public void setDefectResolution(String defectResolution) {
		this.defectResolution = defectResolution;
	}


	public Integer getTestCount() {
		return testCount;
	}
	public void setTestCount(Integer testCount) {
		this.testCount = testCount;
	}

	public void setDefectKey(String defectKey) {
		this.defectKey = defectKey;
	}
	public String getDefectKey() {
		return defectKey;
	}

	public List<String> getAssociatedTestIds() {
		return associatedTestIds;
	}
	public void setAssociatedTestIds(List<String> associatedTestIds) {
		this.associatedTestIds = associatedTestIds;
	}

    public void setPriority(String priority) {
        this.priority = priority;
    }


	/** First compare by testCount and then count by priority and then by Key*/
    @Override
	public int compareTo(DefectSummaryModel that) {
        return ComparisonChain.start()
                .compare(this.testCount, that.testCount )
                .compare(this.priority, that.priority)
                .compare(this.defectKey, that.defectKey)
                .result();
    }


}
