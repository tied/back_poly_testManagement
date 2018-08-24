package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.util.JqlTimetrackingDurationSupport;
import com.atlassian.jira.jql.util.JqlTimetrackingDurationSupportImpl;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;

import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Created by niravshah on 4/15/18.
 */
public class DurationValueValidator {
    private final JqlOperandResolver operandResolver;
    private final JqlTimetrackingDurationSupport jqlTimetrackingDurationSupport;

    public DurationValueValidator(final JqlOperandResolver operandResolver) {
        this.operandResolver = operandResolver;
        this.jqlTimetrackingDurationSupport = new JqlTimetrackingDurationSupportImpl(ComponentAccessor.getJiraDurationUtils());
    }

    public MessageSet validate(final ApplicationUser searcher, final TerminalClause terminalClause) {
        notNull("terminalClause", terminalClause);

        final Operand operand = terminalClause.getOperand();
        final MessageSet messages = new MessageSetImpl();

        if (operandResolver.isValidOperand(operand)) {
            final I18nHelper i18n = getI18n(searcher);
            final List<QueryLiteral> values = operandResolver.getValues(searcher, operand, terminalClause);
            final String fieldName = terminalClause.getName();

            for (QueryLiteral value : values)
            {
                // we are ok with positive longValues -- "minutes" is the implied scale
                boolean isValid = true;

                final Long longValue = value.getLongValue();
                if (longValue != null)
                {
                    isValid = longValue >= 0;
                }
                else
                {
                    final String str = value.getStringValue();
                    if (str != null)
                    {
                        isValid = jqlTimetrackingDurationSupport.validate(str);
                    }
                }

                if (!isValid)
                {
                    String msg;
                    if (operandResolver.isFunctionOperand(value.getSourceOperand())) {
                        msg = i18n.getText("jira.jql.clause.positive.duration.format.invalid.from.func", value.getSourceOperand().getName(), fieldName);
                    } else {
                        msg = i18n.getText("jira.jql.clause.positive.duration.format.invalid", value.toString(), fieldName);
                    }
                    messages.addErrorMessage(msg);
                }
            }
        }
        return messages;
    }

    I18nHelper getI18n(ApplicationUser user) {
        return new I18nBean(user);
    }
}
