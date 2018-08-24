package com.thed.zephyr.je.zql.core;

import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.issue.comparator.LocaleSensitiveStringComparator;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.ClauseInformation;
import com.atlassian.jira.jql.NoOpClauseHandler;
import com.atlassian.jira.jql.ValueGeneratingClauseHandler;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.util.JqlStringSupport;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.query.operator.Operator;
import com.google.common.collect.Maps;
import com.opensymphony.util.TextUtils;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.zql.helper.ZephyrDataTypes;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class AutoCompleteJsonGeneratorImpl implements AutoCompleteJsonGenerator {
    private static final String ZCF = " - ZCF";

    private SearchHandlerManager searchHandlerManager;
    private JqlStringSupport jqlStringSupport;
    private JiraAuthenticationContext authenticationContext;
    private ZephyrCustomFieldManager zephyrCustomFieldManager;

    public AutoCompleteJsonGeneratorImpl(SearchHandlerManager searchHandlerManager,
    		JqlStringSupport jqlStringSupport,JiraAuthenticationContext authenticationContext,
            ZephyrCustomFieldManager zephyrCustomFieldManager) {
    	this.jqlStringSupport=jqlStringSupport;
    	this.searchHandlerManager=searchHandlerManager;
    	this.authenticationContext=authenticationContext;
    	this.zephyrCustomFieldManager=zephyrCustomFieldManager;
    }
    
	@Override
	public String getVisibleFieldNamesJson(ApplicationUser user, Locale locale)
			throws JSONException {
    	Collection<ClauseHandler> clauseHandlers = searchHandlerManager.getVisibleClauseHandlers(authenticationContext.getLoggedInUser());
        Map<String, ClauseNameValue> visibleNames = new TreeMap<String, ClauseNameValue>(String.CASE_INSENSITIVE_ORDER);
        for (ClauseHandler clauseHandler : clauseHandlers)
        {
            final ClauseInformation information = clauseHandler.getInformation();
            final ClauseNames visibleClauseName = information.getJqlClauseNames();
            final Set<Operator> supportedOperators = information.getSupportedOperators();
            final JiraDataType supportedType = information.getDataType();
            final boolean isAutoCompleteable = clauseHandler instanceof ValueGeneratingClauseHandler;
            final boolean isOrderByable = true;
            // We do not want to include NoOpClauseHandlers since they are really just place-holders for fields
            // that are sortable but not searchable
            final boolean isSearchable = !(clauseHandler instanceof NoOpClauseHandler);

            for (String clauseName : visibleClauseName.getJqlFieldNames()) {
                CustomField[] allCustomFieldsByEntityType = zephyrCustomFieldManager.getAllCustomFieldsByEntityType(ApplicationConstants.ENTITY_TYPE.EXECUTION.name());
                boolean isAdded = false;
                if(allCustomFieldsByEntityType != null && allCustomFieldsByEntityType.length > 0) {
                    List<CustomField> customFields = Arrays.asList(allCustomFieldsByEntityType);
                    for (CustomField customField : customFields) {
                        if (StringUtils.equalsIgnoreCase(customField.getName(), clauseName)) {
                            visibleNames.put(getSuffixString(clauseName), new ClauseNameValue(clauseName, isAutoCompleteable, isOrderByable, isSearchable, false, supportedOperators, supportedType));
                            isAdded = true;
                        }
                    }
                }
                // Lets always add the system clause names
                if(!isAdded) {
                    visibleNames.put(clauseName, new ClauseNameValue(clauseName, isAutoCompleteable, isOrderByable, isSearchable, false, supportedOperators, supportedType));
                }
            }
        }

        // fetch the custom fields and add it to the visible name list
      // Map<String, ClauseNameValue> customFieldNames = getCustomFieldVisibleNames();

//        if(MapUtils.isNotEmpty(customFieldNames)) {
//            visibleNames.putAll(customFieldNames);
//        }

        List<String> visibleNamesList = new ArrayList<String>(visibleNames.keySet());
        // Lets sort all the names in a case-insensitive way
        Collections.sort(visibleNamesList, new LocaleSensitiveStringComparator(authenticationContext.getLocale()));


        // Now lets put it into a JSONArray
        JSONArray results = new JSONArray();

        for (String fieldName : visibleNamesList)
        {
            final JSONObject jsonObj = new JSONObject();
            final ClauseNameValue clauseNameValue = visibleNames.get(fieldName);
            try {
				jsonObj.put("value", jqlStringSupport.encodeFieldName(clauseNameValue.getClauseNameValue()));
	            jsonObj.put("displayName", TextUtils.htmlEncode(fieldName));
	            if (clauseNameValue.isAutoCompleteable())
	            {
	                jsonObj.put("auto", "true");
	            }
	            if (clauseNameValue.isOrderByable())
	            {
	                jsonObj.put("orderable", "true");
	            }
	            if (clauseNameValue.isSearchable())
	            {
	                jsonObj.put("searchable", "true");
	            }
	            if (clauseNameValue.getCustomFieldIdClauseName() != null)
	            {
	                jsonObj.put("cfid", clauseNameValue.getCustomFieldIdClauseName());
	            }
	            JSONArray supOpers = new JSONArray();
	            for (Operator operator : clauseNameValue.getSupportedOperators())
	            {
	                supOpers.put(operator.getDisplayString());
	            }
	            jsonObj.put("operators", supOpers);
	            // Include the clauseTypes type
	            JSONArray supportedTypes = new JSONArray();
	            for (String typeString : clauseNameValue.getSupportedType().asStrings())
	            {
	                supportedTypes.put(typeString);
	            }
	            jsonObj.put("types", supportedTypes);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			results.put(jsonObj);
        }
        return results.toString();
	}

    @Override
    public String getJqlReservedWordsJson() throws JSONException
    {
        final Set<String> reservedWords = jqlStringSupport.getJqlReservedWords();
        JSONArray results = new JSONArray();
        for (String reservedWord : reservedWords)
        {
            results.put(reservedWord);
        }
        return results.toString();
    }

	@Override
	public String getVisibleFunctionNamesJson(ApplicationUser user, Locale locale)
			throws JSONException {
      JSONArray results = new JSONArray();

		//        final List<String> functionNames = jqlFunctionHandlerRegistry.getAllFunctionNames();
//
//        // Lets sort all the names in a case-insensitive way
//        Collections.sort(functionNames, new LocaleSensitiveStringComparator(locale));
//
//        // Now lets put it into a JSONArray
//        JSONArray results = new JSONArray();
//
//        for (String functionName : functionNames)
//        {
//            // This is a hack to remove the currentUser function when users are not logged in.
//            boolean dontShowCurrentUserFunction = CurrentUserFunction.FUNCTION_CURRENT_USER.equals(functionName) && user == null;
//
//            // This is a hack to remove the echo function that is only around for testing.
//            if (!EchoFunction.ECHO_FUNCTION.equals(functionName) && !dontShowCurrentUserFunction)
//            {
//                final FunctionOperandHandler functionHandler = jqlFunctionHandlerRegistry.getOperandHandler(new FunctionOperand(functionName));
//                final JSONObject jsonObj = new JSONObject();
//                final int minArguments = functionHandler.getJqlFunction().getMinimumNumberOfExpectedArguments();
//                StringBuilder argPart = new StringBuilder("(");
//                for (int i = 0; i < minArguments; i++)
//                {
//                    if (i != 0)
//                    {
//                        argPart.append(", ");
//                    }
//                    argPart.append("\"\"");
//                }
//                argPart.append(")");
//                jsonObj.put("value", jqlStringSupport.encodeFunctionName(functionName) + argPart.toString());
//                jsonObj.put("displayName", htmlEncode(functionName) + argPart.toString());
//                if (functionHandler.isList())
//                {
//                    jsonObj.put("isList", "true");
//                }
//                // Include the functions type
//                JSONArray functionTypes = new JSONArray();
//                for (String typeString : functionHandler.getJqlFunction().getDataType().asStrings())
//                {
//                    functionTypes.put(typeString);
//                }
//                jsonObj.put("types", functionTypes);
//                results.put(jsonObj);
//            }
//
//        }

        return results.toString();
	}
	
    String htmlEncode(String string)
    {
        return TextUtils.htmlEncode(string);
    }

    private Map<String, ClauseNameValue> getCustomFieldVisibleNames() {

        CustomField[] customFields = zephyrCustomFieldManager.getCustomFieldsByEntityType(ApplicationConstants.ENTITY_TYPE.EXECUTION.name(), null, null);

        if(null != customFields && customFields.length > 0) {
            Map<String, ClauseNameValue> customFieldNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (CustomField customField : customFields) {
                String customFieldType = customField.getCustomFieldType();
                if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.STRING_VALUE)) {
                    customFieldNames.put(getSuffixString(customField.getName()), new ClauseNameValue(customField.getName(), true, true, true,
                            false, OperatorClasses.TEXT_OPERATORS, ZephyrDataTypes.TEXT));
                } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_VALUE)
                        || ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE)) {
                    customFieldNames.put(getSuffixString(customField.getName()), new ClauseNameValue(customField.getName(), false, true, true,
                            false, OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.DATE));
                } else if(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
                    customFieldNames.put(getSuffixString(customField.getName()), new ClauseNameValue(customField.getName(), true, true, true,
                            false,OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.NUMBER));
                } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.LARGE_VALUE)) {
                    customFieldNames.put(getSuffixString(customField.getName()), new ClauseNameValue(customField.getName(), true, true, true,
                            false, OperatorClasses.TEXT_OPERATORS, ZephyrDataTypes.TEXT));
                } else if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType).equalsIgnoreCase(ApplicationConstants.LIST_VALUE)) {
                    customFieldNames.put(getSuffixString(customField.getName()), new ClauseNameValue(customField.getName(), true, true, true,
                            false, OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, ZephyrDataTypes.TEXT));
                }
            }
           return customFieldNames;
        }

        return Maps.newHashMap();
    }

    private String getSuffixString(String customFieldName) {
        return customFieldName + ZCF;
    }
}
