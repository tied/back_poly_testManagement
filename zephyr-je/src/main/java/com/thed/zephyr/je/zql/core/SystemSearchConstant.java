package com.thed.zephyr.je.zql.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.constants.SimpleFieldSearchConstants;
import com.atlassian.jira.issue.search.constants.SimpleFieldSearchConstantsWithEmpty;
import com.atlassian.jira.jql.ClauseInformation;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.query.operator.Operator;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.thed.zephyr.je.zql.helper.ZephyrDataTypes;

public class SystemSearchConstant
{
    /**
     * The ID of the query searcher.
     */
    private static final String PRIORITY_ID = "PRIORITY_ID";
    private static final String PROJECT_ID = "PROJECT_ID";
    private static final String VERSION_ID = "VERSION_ID";
    private static final String ISSUE_ID = "ISSUE_ID";
    private static final String COMPONENT_ID = "COMPONENT_ID";
    private static final String SCHEDULE_ID = "schedule_id";
    private static final String EXECUTION_STATUS = "STATUS";
    private static final String EXECUTED_BY = "EXECUTED_BY";
    private static final String EXECUTION_DATE = "EXECUTED_ON";
    private static final String DATE_CREATED = "DATE_CREATED";
    private static final String CYCLE_ID = "cycle";
    private static final String CYCLE_ID_LONG = "CYCLE_ID";
    private static final String TEST_SUMMARY = "summary";
    private static final String TEST_LABEL = "LABEL";
    private static final String SCHEDULE_DEFECT_ID = "SCHEDULE_DEFECT_ID";
    private static final String ASSIGNED_TO = "ASSIGNED_TO";
    public static final String PERMISSIONS_FILTER_CACHE = "zephyr.permissions.filter.cache";
    public static final String FOLDER_ID = "folder";
    private static final String FOLDER_ID_LONG = "FOLDER_ID";
    private static final String ESTIMATION_TIME = "ESTIMATED_TIME";
    private static final String LOGGED_TIME = "LOGGED_TIME";


    //We don't want to create an instance of this class.
    private SystemSearchConstant()
    {}

    private static final SimpleFieldSearchConstants PRIORITY = new SimpleFieldSearchConstants(PRIORITY_ID,
    		new ClauseNames("priority"), PRIORITY_ID, PRIORITY_ID,PRIORITY_ID,
        OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.PRIORITY);
    

    public static SimpleFieldSearchConstants forPriority()
    {
        return PRIORITY;
    }

    private static final SimpleFieldSearchConstants PROJECT = new SimpleFieldSearchConstants(PROJECT_ID,
    		new ClauseNames("project"),PROJECT_ID,PROJECT_ID,PROJECT_ID,OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY,ZephyrDataTypes.PROJECT);

    public static SimpleFieldSearchConstants forProject()
    {
        return PROJECT;
    }

    private static final SimpleFieldSearchConstants ISSUE = new SimpleFieldSearchConstants(ISSUE_ID, new ClauseNames("issue"), ISSUE_ID, 
    		ISSUE_ID,ISSUE_ID,OperatorClasses.EQUALITY_AND_RELATIONAL, ZephyrDataTypes.ISSUE);

    public static SimpleFieldSearchConstants forIssue()
    {
        return ISSUE;
    }

    private static final SimpleFieldSearchConstants EXECUTION_DEFECT = new SimpleFieldSearchConstants(SCHEDULE_DEFECT_ID, new ClauseNames("executionDefectKey"), SCHEDULE_DEFECT_ID, 
    		SCHEDULE_DEFECT_ID,SCHEDULE_DEFECT_ID,OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.ISSUE);

    public static SimpleFieldSearchConstants forLinkedDefectKey()
    {
        return EXECUTION_DEFECT;
    }
    
    private static final SimpleFieldSearchConstantsWithEmpty COMPONENT = new SimpleFieldSearchConstantsWithEmpty(COMPONENT_ID, 
    		new ClauseNames("component"), COMPONENT_ID, COMPONENT_ID, "-1","-1",COMPONENT_ID,
    		OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, ZephyrDataTypes.COMPONENT);

    public static SimpleFieldSearchConstantsWithEmpty forComponent()
    {
        return COMPONENT;
    }
    
    

    private static final SimpleFieldSearchConstants STATUS = new SimpleFieldSearchConstants(EXECUTION_STATUS,
    		new ClauseNames("executionStatus"), EXECUTION_STATUS, EXECUTION_STATUS, EXECUTION_STATUS,
        OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, ZephyrDataTypes.STATUS);

    public static SimpleFieldSearchConstants forStatus()
    {
        return STATUS;
    }


    private static final SimpleFieldSearchConstants EXECUTEDBY = new SimpleFieldSearchConstants(EXECUTED_BY,
    		new ClauseNames("executedBy"), EXECUTED_BY, EXECUTED_BY, EXECUTED_BY,
        OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, ZephyrDataTypes.TEXT);

    public static SimpleFieldSearchConstants forExecutedBy()
    {
        return EXECUTEDBY;
    }
    
    private static final SimpleFieldSearchConstants SCHEDULE = new SimpleFieldSearchConstants(SCHEDULE_ID,
    		new ClauseNames("execution"), SCHEDULE_ID,SCHEDULE_ID,SCHEDULE_ID,
            OperatorClasses.EQUALITY_AND_RELATIONAL, ZephyrDataTypes.SCHEDULE);
    
    public static SimpleFieldSearchConstants forSchedule()
    {
        return SCHEDULE;
    }

    private static final SimpleFieldSearchConstants EXECUTIONDATE = new SimpleFieldSearchConstants(EXECUTION_DATE, new ClauseNames(
            "executionDate"), EXECUTION_DATE,EXECUTION_DATE, EXECUTION_DATE,
            OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.DATE);

    public static SimpleFieldSearchConstants forExecutionDate()
    {
        return EXECUTIONDATE;
    }

    private static final SimpleFieldSearchConstants DATECREATED = new SimpleFieldSearchConstants(DATE_CREATED, new ClauseNames(
    "creationDate"), DATE_CREATED,DATE_CREATED, DATE_CREATED,
    OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.DATE);

    public static SimpleFieldSearchConstants forDateCreated()
    {
    	return DATECREATED;
    }
    
    private static final SimpleFieldSearchConstants CYCLENAME = new SimpleFieldSearchConstants(CYCLE_ID,
    		new ClauseNames("cycleName"), CYCLE_ID, CYCLE_ID,CYCLE_ID,
    		new HashSet<Operator>(CollectionUtils.union(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY,OperatorClasses.TEXT_OPERATORS)), ZephyrDataTypes.CYCLE);

    public static SimpleFieldSearchConstants forCycleName()
    {
        return CYCLENAME;
    }
    
    private static final SimpleFieldSearchConstants CYCLEID = new SimpleFieldSearchConstants(CYCLE_ID_LONG,
    		new ClauseNames("cycleId"), CYCLE_ID_LONG, CYCLE_ID_LONG,CYCLE_ID_LONG,
    		new HashSet<Operator>(CollectionUtils.union(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY,OperatorClasses.TEXT_OPERATORS)), ZephyrDataTypes.CYCLE);

    public static SimpleFieldSearchConstants forCycleId()
    {
        return CYCLEID;
    }

    private static final SimpleFieldSearchConstants CYCLEBUILD = new SimpleFieldSearchConstants(CYCLE_ID,
    		new ClauseNames("cycleBuild"), CYCLE_ID, CYCLE_ID,CYCLE_ID,
        OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, ZephyrDataTypes.CYCLE);

    public static SimpleFieldSearchConstants forCycleBuild()
    {
        return CYCLEBUILD;
    }
    
    private static final SimpleFieldSearchConstants FOLDERNAME = new SimpleFieldSearchConstants(FOLDER_ID,
    		new ClauseNames("folderName"), FOLDER_ID, FOLDER_ID, FOLDER_ID,
    		new HashSet<Operator>(CollectionUtils.union(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, OperatorClasses.TEXT_OPERATORS)), ZephyrDataTypes.FOLDER);

    public static SimpleFieldSearchConstants forFolderName()
    {
        return FOLDERNAME;
    }
    
    private static final SimpleFieldSearchConstants FOLDERID = new SimpleFieldSearchConstants(FOLDER_ID_LONG,
    		new ClauseNames("folderId"), FOLDER_ID_LONG, FOLDER_ID_LONG, FOLDER_ID_LONG,
    		new HashSet<Operator>(CollectionUtils.union(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, OperatorClasses.TEXT_OPERATORS)), ZephyrDataTypes.FOLDER);

    public static SimpleFieldSearchConstants forFolderId()
    {
        return FOLDERID;
    }
    
	private static final SimpleFieldSearchConstants SUMMARY = new SimpleFieldSearchConstants(
			TEST_SUMMARY, new ClauseNames("summary"),TEST_SUMMARY,TEST_SUMMARY,TEST_SUMMARY, OperatorClasses.TEXT_OPERATORS,
			ZephyrDataTypes.TEXT);

	public static SimpleFieldSearchConstants forTestSummary() {
		return SUMMARY;
	}



    private static final SimpleFieldSearchConstantsWithEmpty LABEL = new SimpleFieldSearchConstantsWithEmpty(TEST_LABEL,
            new ClauseNames("labels"), TEST_LABEL, TEST_LABEL, "-1", "-1",TEST_LABEL,
            OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, ZephyrDataTypes.TEXT);

    public static SimpleFieldSearchConstantsWithEmpty forLabel() {
        return LABEL;
    }

    /**
     * The "SearcherId" for fixFor version comes from the DocumentConstants as per 3.13.
     */
    private static final SimpleFieldSearchConstantsWithEmpty FIXFOR_VERSION = new SimpleFieldSearchConstantsWithEmpty(
    		VERSION_ID, new ClauseNames("fixVersion"), VERSION_ID, VERSION_ID, "-1","-1", VERSION_ID,
    		OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.VERSION);


    public static SimpleFieldSearchConstantsWithEmpty forFixForVersion() {
        return FIXFOR_VERSION;
    }
    
    private static final SimpleFieldSearchConstants ASSIGNEE = new SimpleFieldSearchConstants(ASSIGNED_TO, new ClauseNames("assignee"), ASSIGNED_TO, 
    		ASSIGNED_TO,ASSIGNED_TO,OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, ZephyrDataTypes.TEXT);

    public static SimpleFieldSearchConstants forAssignee() {
        return ASSIGNEE;
    }


    private static final SimpleFieldSearchConstants ESTIMATIONTIME = new SimpleFieldSearchConstants(ESTIMATION_TIME,
            new ClauseNames("estimatedTime"), ESTIMATION_TIME,ESTIMATION_TIME,ESTIMATION_TIME,
            OperatorClasses.EQUALITY_AND_RELATIONAL, ZephyrDataTypes.DATE);

    public static SimpleFieldSearchConstants forEstimationTime()
    {
        return ESTIMATIONTIME;
    }


    private static final SimpleFieldSearchConstants LOGGEDTIME = new SimpleFieldSearchConstants(LOGGED_TIME,
            new ClauseNames("loggedTime"), LOGGED_TIME,LOGGED_TIME,LOGGED_TIME,
            OperatorClasses.EQUALITY_AND_RELATIONAL, ZephyrDataTypes.DATE);

    public static SimpleFieldSearchConstants forLoggedTime()
    {
        return LOGGEDTIME;
    }


    /*private static SimpleFieldSearchConstants forCustomFields() {
        /**
     * fetch the custom fields from DB and register here.
     *
     * Question :
     * The current implementation limits to single entry for SimpleFieldSearchConstants.
     * It will require code changes at multiple files.
     *
        CustomField[] customFields = zephyrCustomFieldManager.getCustomFieldsByEntityType(ApplicationConstants.ENTITY_TYPE.EXECUTION.name());

        if(null != customFields && customFields.length > 0) {

        }

        return null;
    }*/
    
    public static String dbFieldName(String fieldName) {
    	if(StringUtils.equalsIgnoreCase(fieldName, CYCLENAME.getJqlClauseNames().getPrimaryName())) {
    		return "NAME";
    	} else if(StringUtils.equalsIgnoreCase(fieldName,CYCLEBUILD.getJqlClauseNames().getPrimaryName())) {
    		return "BUILD";
    	} else if(StringUtils.equalsIgnoreCase(fieldName, FOLDERNAME.getJqlClauseNames().getPrimaryName())) {
    		return "NAME";
    	}
    	return null;
    }

    /**
     * Maintain Field Visibility in ENAV
     * @return
     */
    public static Map<String, Boolean> externallyInVisibleField() {
    	Map<String,Boolean> inVisibleMap = new HashMap<String, Boolean>();
    	inVisibleMap.put(forCycleId().getJqlClauseNames().getPrimaryName(), false);
    	inVisibleMap.put(forFolderId().getJqlClauseNames().getPrimaryName(), false);
        inVisibleMap.put(forEstimationTime().getJqlClauseNames().getPrimaryName(), false);
        inVisibleMap.put(forLoggedTime().getJqlClauseNames().getPrimaryName(), false);
    	return inVisibleMap;
    }
    
    private static final Map<String, ClauseInformation> CLAUSE_INFORMATION_MAP = Maps.uniqueIndex(
            ImmutableSet.of(
                forSchedule(),
                forCycleName(),
                forComponent(),
                forFixForVersion(),
                forIssue(),
                forExecutedBy(),
                forExecutionDate(),
                forDateCreated(),
                forPriority(),
                forProject(),
                forStatus(),
                forTestSummary(),
                forLabel(),
                forFolderName(),
                forLinkedDefectKey(),
                forEstimationTime(),
                forLoggedTime()
            ), new Function<ClauseInformation, String>()
            {
                @Override
                public String apply(ClauseInformation input)
                {
                    return input.getFieldId() != null ? input.getFieldId() : input.getJqlClauseNames().getPrimaryName();
                }
            }
        );

    public static ClauseInformation getClauseInformationById(String id)
    {
        return CLAUSE_INFORMATION_MAP.get(id);
    }
    private static final Set<String> SYSTEM_NAMES;

    public static Set<String> getSystemNames()
    {
        return SYSTEM_NAMES;
    }

    public static boolean isSystemName(final String name)
    {
        return SYSTEM_NAMES.contains(name);
    }

    //NOTE: This code must be after all the static variable declarations that we need to access. Basically, make this
    //the last code in the file.
    static
    {
        Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        try
        {
            for (final Method constantMethod : getConstantMethods())
            {
                names.addAll(getNames(constantMethod));
            }
        }
        catch (final RuntimeException e)
        {
            getLogger().error("Unable to calculate system ZQL names: Unexpected Error.", e);
            names = Collections.emptySet();
        }
        SYSTEM_NAMES = Collections.unmodifiableSet(names);
    }

    private static Collection<String> getNames(final Method constantMethod)
    {
        try
        {
            final ClauseInformation information = (ClauseInformation) constantMethod.invoke(null);
            if (information == null)
            {
                logConstantError(constantMethod, "Clause information was not available.", null);
                return Collections.emptySet();
            }

            final ClauseNames names = information.getJqlClauseNames();
            if (names == null)
            {
                logConstantError(constantMethod, "The ClauseName was not available.", null);
                return Collections.emptySet();
            }

            final Set<String> strings = names.getJqlFieldNames();
            if (strings == null)
            {
                logConstantError(constantMethod, "The ClauseName returned no values.", null);
                return Collections.emptySet();
            }

            return strings;
        }
        catch (final InvocationTargetException e)
        {
            Throwable exception;
            if (e.getTargetException() != null)
            {
                exception = e.getTargetException();
            }
            else
            {
                exception = e;
            }
            logConstantError(constantMethod, null, exception);
        }
        catch (final IllegalAccessException e)
        {
            logConstantError(constantMethod, null, e);
        }
        catch (final SecurityException e)
        {
            logConstantError(constantMethod, "Security Error.", e);
        }
        catch (final RuntimeException e)
        {
            logConstantError(constantMethod, "Unexpected Error.", e);
        }
        return Collections.emptySet();
    }

    private static Collection<Method> getConstantMethods()
    {
        final Method[] methods;
        try
        {
            methods = SystemSearchConstant.class.getMethods();
        }
        catch (final SecurityException e)
        {
            getLogger().error("Unable to calculate system ZQL names: " + e.getMessage(), e);
            return Collections.emptySet();
        }

        final List<Method> returnMethods = new ArrayList<Method>(methods.length);
        for (final Method method : methods)
        {
            final int modifiers = method.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers))
            {
                continue;
            }

            if (method.getParameterTypes().length != 0)
            {
                continue;
            }

            final Class<?> returnType = method.getReturnType();
            if (!ClauseInformation.class.isAssignableFrom(returnType))
            {
                continue;
            }

            returnMethods.add(method);
        }

        return returnMethods;
    }

    private static void logConstantError(final Method constantMethod, final String msg, final Throwable th)
    {
        String actualMessage = msg;
        if ((msg == null) && (th != null))
        {
            actualMessage = th.getMessage();
        }

        getLogger().error("Unable to calculate system ZQL names for '" + constantMethod.getName() + "': " + actualMessage, th);
    }

    private static Logger getLogger()
    {
        return Logger.getLogger(SystemSearchConstant.class);
    }
}
