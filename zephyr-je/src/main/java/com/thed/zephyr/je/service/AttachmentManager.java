package com.thed.zephyr.je.service;

import java.util.List;
import java.util.Map;

import com.atlassian.jira.project.Project;
import com.thed.zephyr.je.model.Attachment;

import javax.ws.rs.core.Response;


public interface AttachmentManager {
	static final Long ANY = -1l;

	 /**
     * Retrieves all of the attachments
     */
    List<Attachment> getAttachments(Attachment attachment);
    
    /**
     * Gets Attachment's information based on id.
     * @param id the attachment's id
     * @return attachment populated Attachment object
     */
    Attachment getAttachment(final Long id);
    

    /**
     * Gets List of Cycle information based on entity id and type.
     * @param id the entity
     * @param type the entity type
     * @return List of Attachments for given Entity ID and Type
     */
	 List<Attachment> getAttachmentsByEntityIdAndType(Integer scheduleId,String type);

	
    /**
     * Saves a cycle's information
     * @param cycle the object to be saved
     * @return 
     */
    Attachment saveAttachment(Map<String, Object> attachmentProperties);
	
    /**
     * Removes a attachment from the database by id
     * @param id the attachment id
     */
    Response removeAttachment(final Attachment attachment);
    
    /**
     * Removes all attachments for given entityIds
     * @param type
     * @param entityIds
     * @param project
     */
    void removeAttachmentsInBulk(String type, List<Integer> entityIds,Project project);
    
    /**
     * Gets All Attachments based on passed in criteria
     * @param searchExpression
     * @param maxAllowedRecord
     * @return
     */
    public List<Attachment> getAttachmentsByCriteria(String searchExpression,int maxAllowedRecord);

}
