package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.util.JqlTimetrackingDurationSupport;
import com.atlassian.jira.jql.util.JqlTimetrackingDurationSupportImpl;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.SupportedOperatorsValidator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.dbc.Assertions;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.query.clause.TerminalClause;

import java.util.Collection;

/**
 * Created by niravshah on 4/15/18.
 */
public class EstimationTimeValidator implements ClauseValidator {
    private final SupportedOperatorsValidator supportedOperatorsValidator;
    private final DurationValueValidator durationValueValidator;

    public EstimationTimeValidator(JqlOperandResolver operandResolver) {
        Assertions.notNull("operandResolver", operandResolver);
        this.supportedOperatorsValidator = this.getSupportedOperatorsValidator();
        this.durationValueValidator = this.getDurationValueValidator(operandResolver);
    }

    public MessageSet validate(ApplicationUser searcher, TerminalClause terminalClause) {
        MessageSet errors = this.supportedOperatorsValidator.validate(searcher, terminalClause);
        if(!errors.hasAnyErrors()) {
            errors = this.durationValueValidator.validate(searcher, terminalClause);
        }
        return errors;
    }

    SupportedOperatorsValidator getSupportedOperatorsValidator() {
        return new SupportedOperatorsValidator(new Collection[]{OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, OperatorClasses.RELATIONAL_ONLY_OPERATORS});
    }

    DurationValueValidator getDurationValueValidator(JqlOperandResolver operandResolver) {
        return new DurationValueValidator(operandResolver);
    }

    I18nHelper getI18n(ApplicationUser user) {
        return new I18nBean(user);
    }
}
