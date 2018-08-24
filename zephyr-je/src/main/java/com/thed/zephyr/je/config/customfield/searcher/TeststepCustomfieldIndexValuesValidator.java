package com.thed.zephyr.je.config.customfield.searcher;

import java.util.List;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;

/**
 * A Clause Validator for validating the values of fields with raw index values (votes, numbers).
 *
 */
abstract class TeststepCustomfieldIndexValuesValidator implements ClauseValidator
{
	protected static final Logger log = Logger.getLogger(TeststepCustomfieldIndexValuesValidator.class);

	private final JqlOperandResolver jqlOperandResolver;
    private final boolean emptyValuesSupported;

    TeststepCustomfieldIndexValuesValidator(final JqlOperandResolver jqlOperandResolver)
    {
        this(jqlOperandResolver,true);
    }

    TeststepCustomfieldIndexValuesValidator(final JqlOperandResolver jqlOperandResolver, final boolean emptyValuesSupported)
    {
        this.jqlOperandResolver = jqlOperandResolver;
        this.emptyValuesSupported = emptyValuesSupported;
    }

    abstract void addError(final MessageSet messageSet, final ApplicationUser searcher, TerminalClause terminalClause, final QueryLiteral literal);

    @Override
	public MessageSet validate(final ApplicationUser searcher, final TerminalClause terminalClause)
    {
        Operand operand = terminalClause.getOperand();
        MessageSet messageSet = new MessageSetImpl();
        final List<QueryLiteral> literals = jqlOperandResolver.getValues(searcher, operand, terminalClause);
        if (literals != null)
        {
            for (QueryLiteral literal : literals)
            {
                if (literal.isEmpty())
                {
                	log.info("Entered literal is empty. Adding ErrorMessage for display.");
                    addError(messageSet, searcher, terminalClause, literal);
                }
            }
        }
        return messageSet;
    }
}
