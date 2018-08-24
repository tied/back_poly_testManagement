package com.thed.zephyr.je.zql.factory;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import org.apache.log4j.Logger;

public class CustomFieldLargeTextClauseQueryFactory extends TextClauseQueryFactory implements ClauseQueryFactory  {

	private static final Logger log = Logger.getLogger(CustomFieldLargeTextClauseQueryFactory.class);
	private JqlOperandResolver operandResolver;
	private CustomFieldValueManager customFieldValueManager;
    private ZephyrCustomFieldManager zephyrCustomFieldManager;

    public CustomFieldLargeTextClauseQueryFactory(JqlOperandResolver operandResolver, CustomFieldValueManager customFieldValueManager, ZephyrCustomFieldManager zephyrCustomFieldManager) {
        super(operandResolver,customFieldValueManager,zephyrCustomFieldManager);
    }
}
