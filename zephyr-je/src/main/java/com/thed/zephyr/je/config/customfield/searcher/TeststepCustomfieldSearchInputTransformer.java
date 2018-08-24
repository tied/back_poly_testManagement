package com.thed.zephyr.je.config.customfield.searcher;

import java.util.Collection;
import java.util.Collections;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.customfields.searchers.transformer.AbstractCustomFieldSearchInputTransformer;
import com.atlassian.jira.issue.customfields.searchers.transformer.CustomFieldInputHelper;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.customfields.view.CustomFieldParamsImpl;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.SearchContext;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.searchers.transformer.SearchInputTransformer;
import com.atlassian.jira.issue.search.searchers.transformer.SimpleNavigatorCollectorVisitor;
import com.atlassian.query.Query;
import com.atlassian.query.clause.Clause;
import com.atlassian.query.clause.TerminalClauseImpl;
import com.atlassian.query.operand.SingleValueOperand;
import com.atlassian.query.operator.Operator;

/**
 * The {@link com.atlassian.jira.issue.search.searchers.transformer.SearchInputTransformer} for project custom fields.
 *
 * @since v4.0
 */
public class TeststepCustomfieldSearchInputTransformer extends AbstractCustomFieldSearchInputTransformer implements SearchInputTransformer
{
	protected static final Logger log = Logger.getLogger(TeststepCustomfieldSearchInputTransformer.class);

    private final ClauseNames clauseNames;

    public TeststepCustomfieldSearchInputTransformer(CustomField field, ClauseNames clauseNames, String urlParameterName, final CustomFieldInputHelper customFieldInputHelper)
    {
        super(field, urlParameterName, customFieldInputHelper);
        this.clauseNames = clauseNames;
    }

    @Override
	public boolean doRelevantClausesFitFilterForm(final ApplicationUser searcher, final Query query, final SearchContext searchContext)
    {
    	log.info("CoupleCustomfieldSearchInputTransformer - doRelevantClausesFitFilterForm() - Return TRUE for timing...");
        //return convertForNavigator(query).fitsNavigator();
    	return true;
    }
    
    @Override
	protected Clause getClauseFromParams(final ApplicationUser searcher, final CustomFieldParams customFieldParams)
    {
        Collection<String> searchValues = customFieldParams.getAllValues();
        
        if(searchValues == null){
        	return null;
        }
        
        return createSearchClause(searcher, searchValues.iterator().next());
    }

    @Override
	protected CustomFieldParams getParamsFromSearchRequest(final ApplicationUser searcher, final Query query, final SearchContext searchContext)
    {
        final NavigatorConversionResult result = convertForNavigator(query);
        if (result.fitsNavigator() && result.getValue() != null)
        {
            String stringValue = result.getValue().getStringValue() == null ? result.getValue().getLongValue().toString() : result.getValue().getStringValue();
            return new CustomFieldParamsImpl(getCustomField(), Collections.singleton(stringValue));
        }
        return null;
    }

    Clause createSearchClause(final ApplicationUser searcher, String value)
    {
    	//If value is single input string then Operator "=" ie Operator.EQUALS is used
    	//If value is multiples input strings then Operator "in" ie Operator.IN is used.
    	//TerminalClauseImpl will take care of which operator to be added.
    	
        return new TerminalClauseImpl(getClauseName(searcher, clauseNames), value);
    }

    /**
     * Checks if the {@link SearchRequest} fits the navigator for a single value custom field and
     * retrieves the single value for the clause from the {@link SearchRequest}.
     *
     * @param query defines the search criteria to convert.
     * @return returns a {@link NavigatorConversionResult}.
     */
    NavigatorConversionResult convertForNavigator(final Query query)
    {
        SimpleNavigatorCollectorVisitor collectorVisitor = createSimpleNavigatorCollectorVisitor();
        final NavigatorConversionResult result;
        if (query != null && query.getWhereClause() != null)
        {
            query.getWhereClause().accept(collectorVisitor);
            if (!collectorVisitor.isValid())
            {
                result = new NavigatorConversionResult(false, null);
            }
            else if (collectorVisitor.getClauses().isEmpty())
            {
                result = new NavigatorConversionResult(true, null);
            }
            else if (collectorVisitor.getClauses().size() == 1 &&
                    checkOperand(collectorVisitor.getClauses().get(0).getOperator()) &&
                    collectorVisitor.getClauses().get(0).getOperand() instanceof SingleValueOperand)
            {
                result = new NavigatorConversionResult(true, (SingleValueOperand)collectorVisitor.getClauses().get(0).getOperand());
            }
            else
            {
               result = new NavigatorConversionResult(false, null);
            }
        }
        else
        {
            result = new NavigatorConversionResult(true, null);
        }
        return result;
    }

    SimpleNavigatorCollectorVisitor createSimpleNavigatorCollectorVisitor()
    {
        return new SimpleNavigatorCollectorVisitor(clauseNames.getJqlFieldNames());
    }

    private boolean checkOperand(final Operator operator)
    {
        return operator == Operator.EQUALS || operator == Operator.IS || operator == Operator.LIKE || operator == Operator.IN;
    }
	

    
}