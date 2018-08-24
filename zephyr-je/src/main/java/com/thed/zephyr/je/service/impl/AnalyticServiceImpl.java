package com.thed.zephyr.je.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.query.Query;
import com.thed.zephyr.je.model.Attachment;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.service.AnalyticService;
import com.thed.zephyr.je.zql.helper.SearchResult;
import com.thed.zephyr.util.JiraUtil;
import org.apache.log4j.Logger;

/**
 * Created by Masud on 3/15/18.
 */
public class AnalyticServiceImpl implements AnalyticService{

    private final Logger log = Logger.getLogger(AnalyticServiceImpl.class);

    private JiraAuthenticationContext authContext;
    private final SearchService searchService;
    public final ActiveObjects ao;

    public AnalyticServiceImpl(JiraAuthenticationContext authContext,
                               SearchService searchService,
                               ActiveObjects ao) {
        this.authContext = authContext;
         this.ao = ao;
         this.searchService = searchService;
    }

    @Override
    public Integer getTestCaseCount() {
        String typeId = JiraUtil.getTestcaseIssueTypeId();
        IssueType testType = JiraUtil.getConstantsManager().getIssueTypeObject(typeId);
        JqlClauseBuilder builder = JqlQueryBuilder.newClauseBuilder();
        builder.issueType(testType.getId());
        Query query = builder.buildQuery();
        try {

            Long searchResult = searchService.searchCount(authContext.getUser(),query);
            if(searchResult != null) {
                return searchResult.intValue();
            }
        } catch (Exception ex) {
            log.error("Error during getting testCases",ex);
        }
        return 0;
    }

    @Override
    public Integer getTestStepCount() {
        Integer result = 0;
        if (ao != null) {
            result = ao.count(Teststep.class);
        }
        return result;
    }

    @Override
    public Integer getAttachmentCount(String entityType) {
        Integer result = 0;
        net.java.ao.Query query =  net.java.ao.Query.select();
        if(entityType != null) {
            query.where("TYPE = ?", entityType);
        }
        if (ao != null) {
            result = ao.count(Attachment.class,query);
        }
        return result;
    }
}
