package com.thed.zephyr.je.model;

import java.util.Date;

import net.java.ao.Preload;

import net.java.ao.Entity;
import net.java.ao.schema.Indexed;

@Preload
public interface Attachment extends Entity
{
	public Date getDateCreated();
    public void setDateCreated(Date dateCreated);

    public String getFileName() ;
    public void setFileName(String fileName);

    @Indexed
    public Long getEntityId();
    public void setEntityId(Long entityId);

    public String getType();
    public void setType(String type);

    public String getMimetype();
    public void setMimetype(String mimeType);
    
    public Long getFilesize();
    public void setFilesize(Long filesize);

    public String getAuthor();
    public void setAuthor(String author);

    public String getComment();
    public void setComment(String comment);

    public String getPreviewUrl();
    public void setPreviewUrl(String previewUrl);
}