package com.thed.zephyr.je.zql.helper;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.JiraDataTypeImpl;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CommentField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.types.Duration;
import com.atlassian.jira.util.collect.MapBuilder;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.Schedule;

/**
 * Defines the known {@link com.thed.zephyr.je.zql.helper.ZiraDataType data types}.
 *
 */
public final class ZephyrDataTypes
{
    /**
     * Defines the core ZFJ data types
     */
    public static final JiraDataType SCHEDULE = new JiraDataTypeImpl(Schedule.class);
    public static final JiraDataType PROJECT = new JiraDataTypeImpl(Project.class);
    public static final JiraDataType COMPONENT = new JiraDataTypeImpl(ProjectComponent.class);
    public static final JiraDataType VERSION = new JiraDataTypeImpl(Version.class);
    public static final JiraDataType PRIORITY = new JiraDataTypeImpl(Priority.class);
    public static final JiraDataType CYCLE = new JiraDataTypeImpl(Cycle.class);
    public static final JiraDataType ISSUE = new JiraDataTypeImpl(Issue.class);
    public static final JiraDataType STATUS = new JiraDataTypeImpl(ExecutionStatus.class);
    public static final JiraDataType FOLDER = new JiraDataTypeImpl(Folder.class);

    public static final JiraDataType DATE = new JiraDataTypeImpl(Date.class);
    public static final JiraDataType TEXT = new JiraDataTypeImpl(String.class);
    public static final JiraDataType NUMBER = new JiraDataTypeImpl(Number.class);
    public static final JiraDataType DURATION = new JiraDataTypeImpl(Duration.class);
    public static final JiraDataType URL = new JiraDataTypeImpl(java.net.URL.class);

    public static final JiraDataType ALL = new JiraDataTypeImpl(Object.class);

    public static String getType(final Field field)
    {
        if (field instanceof CommentField)
        {
            return field.getClass().getCanonicalName();
        }

        final JiraDataType dataType = getFieldType(field.getId());
        if (dataType == null)
        {
            return field.getClass().getCanonicalName();
        }

        final Collection<String> stringCollection = dataType.asStrings();
        if (stringCollection.size() == 1)
        {
            return stringCollection.iterator().next();
        }
        else
        {
            return stringCollection.toString();
        }
    }

    // This is primarily for generating REST documentation and other such things where you
    // can't get a Field easily. In real production code you should probably be using the other version
    public static String getType(final String fieldId)
    {
        final JiraDataType dataType = getFieldType(fieldId);
        if (dataType == null)
        {
            return fieldId;
        }
        final Collection<String> stringCollection = dataType.asStrings();
        if (stringCollection.size() == 1)
        {
            return stringCollection.iterator().next();
        }
        else
        {
            return stringCollection.toString();
        }
    }

    public static JiraDataType getFieldType(final String fieldId)
    {
        final Map<String,JiraDataType> map = MapBuilder.<String, JiraDataType>newBuilder()
                .add("PROJECT_ID", ZephyrDataTypes.PROJECT)
                .add("schedule_id", ZephyrDataTypes.SCHEDULE)
                .toFastMap();
        return map.get(fieldId);
    }
}
