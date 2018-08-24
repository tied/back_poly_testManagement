package com.thed.zephyr.je.zql.factory;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.*;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.helper.OperatorHelper;
import com.thed.zephyr.je.zql.resolver.CustomFieldIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.CustomFieldResolver;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

public class CustomFieldStringClauseQueryFactory extends TextClauseQueryFactory implements ClauseQueryFactory  {
	
	private static final Logger log = Logger.getLogger(CustomFieldStringClauseQueryFactory.class);
    private JqlOperandResolver operandResolver;
    private CustomFieldValueManager customFieldValueManager;
    private ZephyrCustomFieldManager zephyrCustomFieldManager;

    
    public CustomFieldStringClauseQueryFactory(JqlOperandResolver operandResolver,
            CustomFieldValueManager customFieldValueManager,ZephyrCustomFieldManager zephyrCustomFieldManager) {
        super(operandResolver,customFieldValueManager,zephyrCustomFieldManager);
    }
}
