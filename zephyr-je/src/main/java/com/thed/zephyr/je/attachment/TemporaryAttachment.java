package com.thed.zephyr.je.attachment;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.File;
import java.util.Date;

/**
 * Represents a temporary attachment, that is an attachment that's been uploaded to the server
 * but not yet attached to an issue. The entityId may be null for newly created issues!
 *
 * Temporary Attachments can be sorted by created date,
 *
 */
public class TemporaryAttachment implements Comparable<TemporaryAttachment>
{
    private final Long id;
    private final File tempAttachment;
    private final String filename;
    private final String contentType;
    private final Long entityId;
    private final String entityType;
    private final Date created;

    public TemporaryAttachment(final Long id, final Long entityId,final String entityType, final File tempAttachment, final String filename, final String contentType)
    {
        this.id = id;
        this.entityId = entityId;
        this.tempAttachment = tempAttachment;
        this.filename = filename;
        this.contentType = contentType;
        this.created = new Date();
        this.entityType=entityType;
    }

    public Long getId()
    {
        return id;
    }

    public Long getEntityId()
    {
        return entityId;
    }

    public File getFile()
    {
        return tempAttachment;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getFilename()
    {
        return filename;
    }

    public Date getCreated()
    {
        return created;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final TemporaryAttachment that = (TemporaryAttachment) o;

        if (!contentType.equals(that.contentType))
        {
            return false;
        }
        if (!filename.equals(that.filename))
        {
            return false;
        }
        if (!id.equals(that.id))
        {
            return false;
        }
        if (!entityId.equals(that.entityId))
        {
            return false;
        }
        if (!entityType.equals(that.entityType))
        {
            return false;
        }
        if (!tempAttachment.equals(that.tempAttachment))
        {
            return false;
        }
        if (!created.equals(that.created))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = id.hashCode();
        result = 31 * result + tempAttachment.hashCode();
        result = 31 * result + filename.hashCode();
        result = 31 * result + contentType.hashCode();
        result = 31 * result + entityId.hashCode();
        result = 31 * result + entityType.hashCode();
        result = 31 * result + created.hashCode();
        return result;
    }

    @Override
	public int compareTo(final TemporaryAttachment other)
    {
        return this.created.compareTo(other.getCreated());
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

