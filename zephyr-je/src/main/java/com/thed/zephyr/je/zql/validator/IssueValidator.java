package com.thed.zephyr.je.zql.validator;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.util.JqlIssueKeySupport;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.SupportedOperatorsValidator;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;
import com.atlassian.util.profiling.UtilTimerStack;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Clause validator for the &quot;ExecutionDefectKey&quot; clause.
 */
public class IssueValidator implements ClauseValidator {
    private static final Logger log = Logger.getLogger(IssueValidator.class);
    private static final int BATCH_MAX_SIZE = 1000;

    private final JqlOperandResolver operandResolver;
    private final SupportedOperatorsValidator supportedOperatorsValidator;
    private final JqlIssueKeySupport issueKeySupport;
    private final I18nHelper.BeanFactory i18nFactory;
    private final SearchProvider searchProvider;
    private final JiraAuthenticationContext authContext;

    @SuppressWarnings("unchecked")
    public IssueValidator(final JqlOperandResolver operandResolver, final JqlIssueKeySupport issueKeySupport,
                          final I18nHelper.BeanFactory i18nFactory,
                          final JiraAuthenticationContext authContext, final SearchProvider searchProvider) {
        this.issueKeySupport = notNull("issueKeySupport", issueKeySupport);
        this.i18nFactory = notNull("i18nFactory", i18nFactory);
        this.supportedOperatorsValidator = new SupportedOperatorsValidator(OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY);
        this.operandResolver = notNull("operandResolver", operandResolver);
        this.authContext = authContext;
        this.searchProvider = searchProvider;
    }


    @Nonnull
    public MessageSet validate(final ApplicationUser searcher, @Nonnull final TerminalClause terminalClause) {
        notNull("terminalClause", terminalClause);
        UtilTimerStack.push("IssueIdValidator.validate()");
        try {
            MessageSet messages = supportedOperatorsValidator.validate(searcher, terminalClause);
            if (!messages.hasAnyErrors()) {
                final Operand operand = terminalClause.getOperand();

                //Thus should not return null since the outside validation makes sure the operand is valid before
                //calling this method.
                final List<QueryLiteral> values = operandResolver.getValues(searcher, operand, terminalClause);

                UtilTimerStack.push("IssueIdValidator.validate() - looping");
                int batches = values == null ? 0 : values.size() / BATCH_MAX_SIZE + 1;
                for (int batchIndex = 0; batchIndex < batches; batchIndex++) {
                    List<QueryLiteral> valuesBatch = values.subList(batchIndex * BATCH_MAX_SIZE,
                            Math.min((batchIndex + 1) * BATCH_MAX_SIZE, values.size()));
                    validateBatch(searcher, terminalClause, valuesBatch, messages);
                }
                UtilTimerStack.pop("IssueIdValidator.validate() - looping");
            }

            return messages;
        } finally {
            UtilTimerStack.pop("IssueIdValidator.validate()");
        }
    }

    private void validateBatch(final ApplicationUser searcher, @Nonnull final TerminalClause terminalClause,
                               List<QueryLiteral> values, final MessageSet messages) {
        final Operand operand = terminalClause.getOperand();
        Set<Long> numericLiterals = new HashSet<Long>();
        Set<String> stringLiterals = new HashSet<String>();
        for (QueryLiteral value : values) {
            if (!value.isEmpty()) {
                if (value.getLongValue() != null) {
                    numericLiterals.add(value.getLongValue());
                } else if (value.getStringValue() != null) {
                    stringLiterals.add(value.getStringValue());
                } else {
                    log.debug("Unknown QueryLiteral: " + value.toString());
                }
            }
        }
        if (!numericLiterals.isEmpty() || !stringLiterals.isEmpty()) {
            if (numericLiterals.size() > 0)
                validateIssueIdsBatch(messages, numericLiterals, searcher, terminalClause, operand);
            if (stringLiterals.size() > 0)
                validateIssueKeysBatch(messages, stringLiterals, searcher, terminalClause, operand);
        }
    }


    private void validateIssueIdsBatch(final MessageSet messages, final Set<Long> issueIds, final ApplicationUser searcher,
                                       final TerminalClause clause, final Operand operand) {
        Set<Long> missingIssues = getIdsOfMissingIssues(issueIds);
        for (Long missingIssue : missingIssues) {
            addErrorIssueIdNotFound(messages, missingIssue, searcher, clause, operand);
        }
    }

    private void addErrorIssueIdNotFound(final MessageSet messages, final Long issueId, final ApplicationUser searcher,
                                         final TerminalClause clause, final Operand operand) {
        final I18nHelper i18n = i18nFactory.getInstance(searcher);
        if (!operandResolver.isFunctionOperand(operand)) {
            messages.addErrorMessage(
                    i18n.getText("jira.jql.clause.no.value.for.id", clause.getName(), issueId.toString()));
        } else {
            messages.addErrorMessage(i18n.getText("jira.jql.clause.no.value.for.name.from.function", operand.getName(),
                    clause.getName()));
        }
    }

    private void validateIssueKeysBatch(final MessageSet messages, final Set<String> issueKeys, final ApplicationUser searcher,
                                        final TerminalClause clause, final Operand operand) {
        Set<String> missingIssueKeys = getKeysOfMissingIssues(issueKeys);
        for (String missingIssueKey : missingIssueKeys) {
            addErrorIssueKeyNotFound(messages, missingIssueKey, searcher, clause, operand);
        }
        Set<String> validIssueKeys = new HashSet<String>(issueKeys);
        validIssueKeys.removeAll(missingIssueKeys);
    }

    private Set<String> getKeysOfMissingIssues(Set<String> issueKeys) {
        JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
        Query query = jqlClauseBuilder.issue().in(issueKeys.toArray(new String[issueKeys.size()])).buildQuery();
        try {
            SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), new PagerFilter());
            List<Issue> issues = searchResults.getIssues();
            for (Issue issue : issues) {
                issueKeys.remove(issue.getKey());
            }
        } catch (SearchException e) {
            log.warn("Error retrieving Issue", e);
        }
        return issueKeys;
    }

    private Set<Long> getIdsOfMissingIssues(Set<Long> issueIds) {
        JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
        Query query = jqlClauseBuilder.issue().in(issueIds.toArray(new Long[issueIds.size()])).buildQuery();
        try {
            SearchResults searchResults = searchProvider.search(query, authContext.getLoggedInUser(), new PagerFilter());
            List<Issue> issues = searchResults.getIssues();
            for (Issue issue : issues) {
                issueIds.remove(issue.getKey());
            }
        } catch (SearchException e) {
            log.warn("Error retrieving Issue", e);
        }
        return issueIds;
    }


    private void addErrorIssueKeyNotFound(final MessageSet messages, final String key, final ApplicationUser searcher,
                                          final TerminalClause clause, final Operand operand) {
        final I18nHelper i18n = i18nFactory.getInstance(searcher);
        final boolean validIssueKey = issueKeySupport.isValidIssueKey(key);
        if (!operandResolver.isFunctionOperand(operand)) {
            if (validIssueKey) {
                messages.addErrorMessage(i18n.getText("jira.jql.clause.issuekey.noissue", key, clause.getName()));
            } else {
                messages.addErrorMessage(
                        i18n.getText("jira.jql.clause.issuekey.invalidissuekey", key, clause.getName()));
            }
        } else {
            if (validIssueKey) {
                messages.addErrorMessage(i18n.getText("jira.jql.clause.issuekey.noissue.from.func", operand.getName(),
                        clause.getName()));
            } else {
                messages.addErrorMessage(
                        i18n.getText("jira.jql.clause.issuekey.invalidissuekey.from.func", operand.getName(),
                                clause.getName()));
            }
        }
    }
}
