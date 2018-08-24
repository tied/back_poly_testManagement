package com.thed.zephyr.je.model.cluster;

import java.util.Date;

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Mutator;
import net.java.ao.Preload;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.StringLength;

@Preload
public interface ExecClusterMessage extends Entity {
	@Indexed
	public String getDestinationNode();
	public void setDestinationNode(String destinationNode);

	public String getSourceNode();
	public void setSourceNode(String sourceNode);
	
	@Indexed
	public String getClaimedNode();
	public void setClaimedNode(String claimedNode);

	public String getMessage();
	public void setMessage(String message);
	
    @Accessor("AFFECTED_EXECUTION_ID")
	@StringLength(StringLength.UNLIMITED)	
	public String getAffectedExecutionId();
	
    @StringLength(StringLength.UNLIMITED)
    @Mutator("AFFECTED_EXECUTION_ID")
	public void setAffectedExecutionId(String affectedExecutionId);

    @Indexed
	public Date getMessageTime();
	public void setMessageTime(Date messageTime);

	@Indexed
	public Date getCreationTime();
	public void setCreationTime(Date creationTime);

	@Indexed
	public String getStatus();
	public void setStatus(String status);
}
