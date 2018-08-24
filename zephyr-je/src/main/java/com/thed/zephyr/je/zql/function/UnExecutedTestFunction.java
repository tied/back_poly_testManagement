package com.thed.zephyr.je.zql.function;


import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugin.jql.function.AbstractJqlFunction;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.index.ScheduleIndexerManager;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This function returns a list of tests which has no executions.
 * <p/>
 * This function expects zero or one argument.
 *
 */
public class UnExecutedTestFunction extends AbstractJqlFunction
{
	private final SearchService searchService;
	private final SearchProvider searchProvider;
	private final IssueTypeManager issueTypeManager;
	private final ProjectManager projectManager;
	private final PermissionManager permissionManager;
	private final ScheduleIndexerManager scheduleIndexerManager;

	protected final Logger log = Logger.getLogger(UnExecutedTestFunction.class);

	public UnExecutedTestFunction(SearchService searchService,
								  SearchProvider searchProvider, IssueTypeManager issueTypeManager,ProjectManager projectManager, PermissionManager permissionManager,
								  ScheduleIndexerManager scheduleIndexerManager) {
		this.searchService = searchService;
		this.searchProvider=searchProvider;
		this.issueTypeManager=issueTypeManager;
		this.projectManager=projectManager;
		this.permissionManager=permissionManager;
		this.scheduleIndexerManager=scheduleIndexerManager;
	}

	public List<QueryLiteral> getValues(@Nonnull QueryCreationContext queryCreationContext,
										@Nonnull FunctionOperand operand, @Nonnull TerminalClause terminalClause) {
		final List<QueryLiteral> literals = new ArrayList<>();
		ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		final List<String> args = operand.getArgs();
		if (args.isEmpty()) {
			return literals;
		}
		Stream<String> streamArgs = args.stream().filter(StringUtils::isNotBlank);
		List<String> projectKeys = new ArrayList<>();

		// check if the issue argument is actually an issue which the user can see
		streamArgs.forEach(projectKey -> {
			if (Pattern.matches(".*[a-zA-Z0-9]+.*", projectKey)) {
				Project project = projectManager.getProjectObjByKey(projectKey);
				if(project != null & permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS,
						project, ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser())){
					projectKeys.add(projectKey);
				}
			}
		});

		try {
			long[] allTestcaseExecutedArray = scheduleIndexerManager.getAllTestcaseDocuments();
			Arrays.sort(allTestcaseExecutedArray);
			//Get Issues from JQL and merge with the above.
			JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
			jqlClauseBuilder.project().in().strings(projectKeys);
			jqlClauseBuilder.and().issueType().eq().string(JiraUtil.getTestcaseIssueTypeId());
			Query query = jqlClauseBuilder.buildQuery();
			int totalTestcaseCount;
			int offset = 0;
			do {
				PagerFilter pagerFilter = new PagerFilter(2000);
				pagerFilter.setStart(offset);
				SearchResults jqlSearchResults = searchProvider.search(query, user,pagerFilter);
				int totalIssues = jqlSearchResults != null && jqlSearchResults.getIssues() != null ? jqlSearchResults.getIssues().size() : 0;
				totalTestcaseCount = jqlSearchResults.getTotal() < 500000 ? jqlSearchResults.getTotal() : 500000;
				if(jqlSearchResults != null) {
					jqlSearchResults.getIssues().stream().forEach(issue -> {
						int i = Arrays.binarySearch(allTestcaseExecutedArray,issue.getId());
						if(i < 0 && StringUtils.equals(issue.getIssueTypeId(), JiraUtil.getTestcaseIssueTypeId())) {
							literals.add(new QueryLiteral(operand, issue.getId()));
						}
					});
				}
				if(totalIssues == 0) {
					break;
				}
				offset = offset + totalIssues;
			} while (offset < totalTestcaseCount);
		} catch (SearchException e) {
			e.printStackTrace();
			log.error("SearchException retrieving Issues",e);
		} catch (IOException e) {
			e.printStackTrace();
			log.error("IOException retrieving Issues",e);
		} catch (ParseException e) {
			log.error("Error parsing JQL",e);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Error retrieving Data",e);
		}
		return literals;
	}

	/**
	 * This method validates the passed in args. In this case the function accepts no args, so let's validate that were none.
	 */
	public MessageSet validate(ApplicationUser searcher,
							   @Nonnull FunctionOperand operand,
							   @Nonnull TerminalClause terminalClause) {
		MessageSet messageSet = new MessageSetImpl();
		if(terminalClause.getOperator() != Operator.IN) {
			messageSet.addErrorMessage(getI18n().getText("jira.jql.function.testswithout.execution.operator.allowed.label", getFunctionName(),Operator.IN.name()));
			return messageSet;
		}

		final List<String> args = operand.getArgs();
		if (args.isEmpty()) {
			messageSet.addErrorMessage(getI18n().getText("jira.jql.function.testswithout.execution.incorrect.usage", getFunctionName()));
			return messageSet;
		}

		List<String> projectList = args.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());;
		if(projectList.size() > 2) {
			messageSet.addErrorMessage(getI18n().getText("jira.jql.function.testswithout.execution.maxlimit.label", getFunctionName()));
			return messageSet;
		}
		// check if the project argument is actually an project which the user can see
		if(projectList.size() > 0) {
			projectList.stream().forEach(projectIdOrKey -> {
				Project project = null;
				if (Pattern.matches(".*[a-zA-Z0-9]+.*", projectIdOrKey)) {
					project = projectManager.getProjectObjByKey(projectIdOrKey);
				} else {
					project = projectManager.getProjectObj(Long.valueOf(projectIdOrKey));
				}
				if (project == null) {
					messageSet.addErrorMessage(getI18n().getText("jira.jql.function.testswithout.execution.invalid.project.args", projectIdOrKey));
				}
			});
		} else {
			messageSet.addErrorMessage(getI18n().getText("jira.jql.function.testswithout.execution.incorrect.usage", getFunctionName()));
			return messageSet;
		}
		return messageSet;
	}

	/**
	 * This method returns the min number of args the function takes. In this case - 0
	 */
	public int getMinimumNumberOfExpectedArguments() {
		return 1;
	}

	/**
	 * This method needs to return the type of objects the function deals with. In this case - Issues
	 */
	public JiraDataType getDataType() {
		return JiraDataTypes.ISSUE;
	}

}
