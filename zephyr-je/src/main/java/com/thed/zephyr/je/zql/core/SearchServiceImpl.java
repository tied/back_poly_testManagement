package com.thed.zephyr.je.zql.core;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.lucene.search.Collector;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.parser.JqlParseErrorMessage;
import com.atlassian.jira.jql.parser.JqlParseErrorMessages;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.jql.validator.OrderByValidator;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.query.Query;
import com.atlassian.query.clause.Clause;
import com.atlassian.query.order.OrderBy;
import com.thed.zephyr.je.zql.helper.SearchResult;
import com.thed.zephyr.util.ZephyrComponentAccessor;

import java.util.Map;

public class SearchServiceImpl implements SearchService {
	
    private final JqlQueryParser jqlQueryParser;
    private OrderByValidator orderByValidator;
    private final LuceneSearchProvider searchProvider;
    private final I18nHelper.BeanFactory factory;
    private final ValidatorRegistry validatorRegistry;
    private final JqlOperandResolver operandResolver;

    public SearchServiceImpl(JqlQueryParser jqlQueryParser,LuceneSearchProvider searchProvider,
    		I18nHelper.BeanFactory factory,ValidatorRegistry validatorRegistry,JqlOperandResolver operandResolver) {
    	this.jqlQueryParser=jqlQueryParser;
    	this.searchProvider=searchProvider;
    	this.factory=factory;
    	this.validatorRegistry=validatorRegistry;
    	this.operandResolver=operandResolver;
    }
    
    @Override
    public MessageSet validateQuery(final ApplicationUser searcher, final Query query)
    {
        notNull("query", query);
        final Clause clause = query.getWhereClause();
        final MessageSet messageSet;

        if (clause != null) {
            //Validate clause.
            final ValidatorVisitor visitor = new ValidatorVisitor(validatorRegistry, operandResolver, searcher);
            messageSet = clause.accept(visitor);
        } else {
            messageSet = new MessageSetImpl();
        }

        final OrderBy orderBy = query.getOrderByClause();
        if (orderBy != null) {
            //Validate OrderBy.
            if(this.orderByValidator == null){
                this.orderByValidator= (OrderByValidator) ZephyrComponentAccessor.getInstance().getComponent("orderByValidator");
            }
            messageSet.addMessageSet(orderByValidator.validate(searcher, orderBy));
        }
        return messageSet;
    }

    @Override
	public SearchResult search(ApplicationUser searcher, Query query, Integer startIndex,boolean maxAllowedResult,Integer maxRecord,boolean overrideSecurity) throws Exception {
        return searchProvider.search(query, searcher,startIndex,maxAllowedResult,maxRecord,overrideSecurity);
	}
    
    @Override
	public SearchResult searchMax(ApplicationUser searcher, Query query, boolean overrideSecurity, boolean bypassPermissionFilter) throws Exception {
        return searchProvider.searchMax(query, searcher,overrideSecurity,bypassPermissionFilter);
	}
	
	@Override
	public long searchCount(ApplicationUser searcher, Query query) throws Exception {
        return searchProvider.searchCount(query, searcher);
	}

    @Override
    public long searchCountByPassSecurity(Query query, ApplicationUser user) throws Exception {
        return searchProvider.searchCountByPassSecurity(query,user);
    }

    @Override
    public ParseResult parseQuery(ApplicationUser searcher, final String query)
    {
        notNull("query", query);
        Query newQuery = null;
        MessageSet errors = new MessageSetImpl();
        try {
        	//ANTLR Query Parse
            newQuery = jqlQueryParser.parseQuery(query);
        } catch (JqlParseException exception) {

            JqlParseErrorMessage errorMessage = exception.getParseErrorMessage();
            if (errorMessage == null) {
                errorMessage = JqlParseErrorMessages.genericParseError();
            }
            errors.addErrorMessage(errorMessage.getLocalizedErrorMessage(getI18n(searcher)));
        }
        return new ParseResult(newQuery, errors);
    }
	
    @Override
    public Map<String, Object> search(final Query searchQuery, final Query executionQuery, final ApplicationUser user,
    		org.apache.lucene.search.Query andQuery, boolean overrideSecurity,Integer offset) throws SearchException {
         return searchProvider.searchAndSort(searchQuery, executionQuery, user, null, false, offset);
    }
    
    protected I18nHelper getI18n(ApplicationUser user) {
        return factory.getInstance(user);
    }
}
