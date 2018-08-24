package com.thed.zephyr.je.model;

import net.java.ao.Entity;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;

@Preload
public interface Teststep extends Entity{
	
	public Integer getOrderId();
	public void setOrderId(Integer orderId);

	@Indexed
	public Long getIssueId();
	public void setIssueId(Long issueId);

	@StringLength(StringLength.UNLIMITED)
	public String getStep();
	@StringLength(StringLength.UNLIMITED)
	public void setStep(String step);

	@StringLength(StringLength.UNLIMITED)
	public String getData();
	@StringLength(StringLength.UNLIMITED)
	public void setData(String data);

	@StringLength(StringLength.UNLIMITED)
	public String getResult();
	@StringLength(StringLength.UNLIMITED)
	public void setResult(String result);
	
	public String getCreatedBy();
	public void setCreatedBy(String createdBy);
	
	public String getModifiedBy();
	public void setModifiedBy(String modifiedBy);
}
