package com.thed.zephyr.je.zql.core;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.index.indexers.FieldIndexer;
import com.atlassian.jira.issue.index.indexers.impl.IssueKeyIndexer;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.SearchHandler;
import com.atlassian.jira.issue.search.constants.SimpleFieldSearchConstants;
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.context.*;
import com.atlassian.jira.jql.context.ProjectClauseContextFactory;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.permission.*;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.resolver.ComponentResolver;
import com.atlassian.jira.jql.util.JqlDateSupport;
import com.atlassian.jira.jql.util.JqlIssueSupport;
import com.atlassian.jira.jql.validator.*;
import com.atlassian.jira.jql.values.ComponentClauseValuesGenerator;
import com.atlassian.jira.jql.values.LabelsClauseValuesGenerator;
import com.atlassian.jira.jql.values.PriorityClauseValuesGenerator;
import com.atlassian.jira.jql.values.ProjectClauseValuesGenerator;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.util.EmailFormatter;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.query.operator.Operator;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.zql.core.generator.*;
import com.thed.zephyr.je.zql.factory.*;
import com.thed.zephyr.je.zql.factory.IssueIdClauseContextFactory;
import com.thed.zephyr.je.zql.helper.ZephyrDataTypes;
import com.thed.zephyr.je.zql.validator.*;
import com.thed.zephyr.je.zql.validator.SummaryValidator;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import net.jcip.annotations.GuardedBy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * This class defines search handlers for all ZQL clauses. Any future clause that needs to be added in ZQL will need to have 3 interfaces implemented
 * 1. Need to add ClauseHandlerFactory 
 * 2. Validator is needed to validate the input values provided by the user in ZQL
 * 3. Value Generator is needed if we want to provide list of values for a given literal  
 * 4. Last, create a method like createExecutionStatusSearchHandler to use the ClauseHandlerFactory and add the call to getZQLClauseSearchHandlers 
 *
 */
public class ZephyrClauseHandlerFactoryImpl implements
		ZephyrClauseHandlerFactory {
	private final CycleManager cycleManager;
    private final LabelManager labelManager;
    private final IssueManager issueManager;
	private PermissionManager permissionManager;
	private final JqlOperandResolver jqlOperandResolver;
	private final JqlIssueSupport jqlIssueSupport;
	private final ScheduleManager scheduleManager;
	private final ExecutionStatusValidator executionStatusValidator;
    private final VersionManager versionManager;
    private I18nHelper.BeanFactory beanFactory;
	private final EmailFormatter emailFormatter;
	private final FolderManager folderManager;
	private final CustomFieldValueManager customFieldValueManager;
	private final JqlDateSupport jqlDateSupport;
	private final ZephyrCustomFieldManager zephyrCustomFieldManager;

	@GuardedBy("this")
	private Collection<SearchHandler> systemClauseSearchHandlers = null;

	@GuardedBy("this")
	private Collection<SearchHandler> customClauseSearchHandlers = null;

    public ZephyrClauseHandlerFactoryImpl(ProjectManager projectManager,
                                          PermissionManager permissionManager,
                                          JqlOperandResolver jqlOperandResolver,
                                          JqlIssueSupport jqlIssueSupport, CycleManager cycleManager,
                                          LabelManager labelManager, IssueManager issueManager,
                                          ExecutionStatusValidator executionStatusValidator, ScheduleManager scheduleManager,
                                          VersionManager versionManager,
                                          I18nHelper.BeanFactory beanFactory, EmailFormatter emailFormatter,
                                          FolderManager folderManager, CustomFieldValueManager customFieldValueManager,
                                          JqlDateSupport jqlDateSupport, ZephyrCustomFieldManager zephyrCustomFieldManager) {
        this.permissionManager = permissionManager;
        this.jqlIssueSupport = jqlIssueSupport;
        this.jqlOperandResolver = jqlOperandResolver;
        this.executionStatusValidator = executionStatusValidator;
        this.scheduleManager = scheduleManager;
        this.cycleManager = cycleManager;
        this.labelManager = labelManager;
        this.issueManager = issueManager;
        this.versionManager = versionManager;
        this.beanFactory = beanFactory;
        this.emailFormatter = emailFormatter;
        this.folderManager = folderManager;
        this.customFieldValueManager = customFieldValueManager;
        this.jqlDateSupport = jqlDateSupport;
        this.zephyrCustomFieldManager = zephyrCustomFieldManager;
    }

	/**
	 * Creates Execution Status Search Handler
	 * @return
	 */
	private SearchHandler createExecutionStatusSearchHandler() {
		final ClauseQueryFactory clauseQFactory = (ExecutionStatusClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("executionStatusClauseQueryFactory");
		final ClauseValidator clauseValidator = (ExecutionStatusValidator) executionStatusValidator;
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler scheduleClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
				SystemSearchConstant.forStatus(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),new ExecutionClauseValueGenerator());

		final SearchHandler.ClauseRegistration executionClauseRegistration = new SearchHandler.ClauseRegistration(
				scheduleClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(executionClauseRegistration));
	}
	
	/**
	 * Schedule Execution By Search Handler
	 * @return SearchHandler
	 */
	private SearchHandler createExecutedBySearchHandler() {
		ClauseQueryFactory clauseQFactory = (ExecutedByClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("executedByClauseQueryFactory");
		ClauseValidator clauseValidator = (ExecutedByValidator) ZephyrComponentAccessor.getInstance().getComponent("executedByValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler executedByClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
				SystemSearchConstant.forExecutedBy(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),new ExecutedByClauseValueGenerator(scheduleManager,emailFormatter));

		final SearchHandler.ClauseRegistration executedByClauseRegistration = new SearchHandler.ClauseRegistration(executedByClauseHandler);
		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(executedByClauseRegistration));
	}

	/**
	 * Execution Date Search Handler
	 * @return SearchHandler
	 */
	private SearchHandler createExecutionDateSearchHandler() {
		final ClauseQueryFactory clauseQFactory = (ExecutionDateClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("executionDateClauseQueryFactory");
		final ClauseValidator clauseValidator = (ExecutionDateValidator) ZephyrComponentAccessor.getInstance().getComponent("executionDateValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler executionDateClauseHandler = new ZephyrDefaultClauseHandler(
				SystemSearchConstant.forExecutionDate(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

		final SearchHandler.ClauseRegistration executedOnClauseRegistration = new SearchHandler.ClauseRegistration(executionDateClauseHandler);
		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(executedOnClauseRegistration));
	}
	
	/**
	 * Creation Date Search Handler
	 * @return SearchHandler
	 */
	private SearchHandler createDateCreatedSearchHandler() {
		final ClauseQueryFactory clauseQFactory = (DateCreatedClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("dateCreatedClauseQueryFactory");
		final ClauseValidator clauseValidator = (DateCreatedValidator) ZephyrComponentAccessor.getInstance().getComponent("dateCreatedValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler dateCreatedClauseHandler = new ZephyrDefaultClauseHandler(
				SystemSearchConstant.forDateCreated(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

		final SearchHandler.ClauseRegistration dateCreatedClauseRegistration = new SearchHandler.ClauseRegistration(dateCreatedClauseHandler);
		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(dateCreatedClauseRegistration));
	}

	/**
	 * Returns CycleName Clause Handler
	 * @return SearcHandler
	 */
	private SearchHandler createCycleNameSearchHandler() {
		final ClauseQueryFactory clauseQFactory = (CycleNameClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("cycleNameClauseQueryFactory");
		final ClauseValidator clauseValidator = (CycleValidator) ZephyrComponentAccessor.getInstance().getComponent("cycleValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler cycleNameClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
				SystemSearchConstant.forCycleName(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),new CycleNameClauseValueGenerator(cycleManager,permissionManager,beanFactory));

		final SearchHandler.ClauseRegistration cycleNameClauseRegistration = new SearchHandler.ClauseRegistration(
				cycleNameClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(cycleNameClauseRegistration));
	}

	/**
	 * Returns CycleId Clause Handler
	 * @return SearcHandler
	 */
	private SearchHandler createCycleIdSearchHandler() {
		final ClauseQueryFactory clauseQFactory = (CycleIdClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("cycleIdClauseQueryFactory");
		final ClauseValidator clauseValidator = (CycleValidator) ZephyrComponentAccessor.getInstance().getComponent("cycleValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler cycleIdClauseHandler = new ZephyrDefaultClauseHandler(
				SystemSearchConstant.forCycleId(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

		final SearchHandler.ClauseRegistration cycleIdClauseRegistration = new SearchHandler.ClauseRegistration(
				cycleIdClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(cycleIdClauseRegistration));
	}
	
	/**
	 * Returns Cycle Build Search Handler 
	 * @return SearcHandler
	 */
	private SearchHandler createCycleBuildSearchHandler() {
		final ClauseQueryFactory clauseQFactory = (CycleBuildClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("cycleBuildClauseQueryFactory");
		final ClauseValidator clauseValidator = (CycleValidator) ZephyrComponentAccessor.getInstance().getComponent("cycleValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler cycleBuildClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
				SystemSearchConstant.forCycleBuild(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),new CycleBuildClauseValueGenerator(cycleManager,permissionManager));

		final SearchHandler.ClauseRegistration cycleBuildClauseRegistration = new SearchHandler.ClauseRegistration(cycleBuildClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(cycleBuildClauseRegistration));
	}
	
	/**
	 * Returns FolderId Clause Handler
	 * 
	 * @return SearcHandler
	 */
	private SearchHandler createFolderIdSearchHandler() {
		final ClauseQueryFactory clauseQFactory = (FolderIdClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("folderIdClauseQueryFactory");
		final ClauseValidator clauseValidator = (FolderValidator) ZephyrComponentAccessor.getInstance().getComponent("folderValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler folderIdClauseHandler = new ZephyrDefaultClauseHandler(
				SystemSearchConstant.forFolderId(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

		final SearchHandler.ClauseRegistration folderIdClauseRegistration = new SearchHandler.ClauseRegistration(folderIdClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(folderIdClauseRegistration));
	}
	
	/**
	 * Returns FolderName Clause Handler
	 * 
	 * @return SearcHandler
	 */
	private SearchHandler createFolderNameSearchHandler() {
		final ClauseQueryFactory clauseQFactory = (FolderNameClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("folderNameClauseQueryFactory");
		final ClauseValidator clauseValidator = (FolderValidator) ZephyrComponentAccessor.getInstance().getComponent("folderValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler folderNameClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
				SystemSearchConstant.forFolderName(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),new FolderNameClauseValueGenerator(folderManager,permissionManager,beanFactory));

		final SearchHandler.ClauseRegistration folderNameClauseRegistration = new SearchHandler.ClauseRegistration(folderNameClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(folderNameClauseRegistration));
	}
	
	/**
	 * Returns Schedule ID Search Handler
	 * @return SearcHandler
	 */
	private SearchHandler createScheduleSearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (ScheduleClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("scheduleClauseQueryFactory");
        final ClauseValidator clauseValidator = (ScheduleValidator)ZephyrComponentAccessor.getInstance().getComponent("scheduleValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler scheduleClauseHandler = new ZephyrDefaultClauseHandler(
				SystemSearchConstant.forSchedule(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

		final SearchHandler.ClauseRegistration scheduleClauseRegistration = new SearchHandler.ClauseRegistration(
				scheduleClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(scheduleClauseRegistration));

	}
	
	/**
	 * Creates Project Search Handler
	 * @return SearchHandler
	 */
	private SearchHandler createProjectSearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (ProjectClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("projectClauseQueryFactory");
        final ClauseValidator clauseValidator = (ProjectValidator)ComponentAccessor.getComponentOfType(ProjectValidator.class);
        final ClauseContextFactory clauseCFactory = (ProjectClauseContextFactory) ZephyrComponentAccessor.getInstance().getComponent("projectClauseContextFactory");
        final ClauseHandler projectClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(SystemSearchConstant.forProject(),
        		clauseQFactory, clauseValidator, createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),
            new ProjectClauseValuesGenerator(permissionManager));
        final SearchHandler.ClauseRegistration projectCategoryClauseRegistration = new SearchHandler.ClauseRegistration(projectClauseHandler);

        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
        return new SearchHandler(builder.asList(), null, Collections.singletonList(projectCategoryClauseRegistration));
   }

	/**
	 * Creates Priority Search Handler
	 * @return SearchHandler
	 */
	private SearchHandler createPrioritySearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (PriorityClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("priorityClauseQueryFactory");
        final ClauseValidator clauseValidator = (PriorityValidator)ComponentManager.getComponentInstanceOfType(PriorityValidator.class);
        final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

        ConstantsManager constantsManager = (ConstantsManager)ComponentManager.getComponentInstanceOfType(ConstantsManager.class);

        final ClauseHandler priorityClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(SystemSearchConstant.forPriority(),
        		clauseQFactory, clauseValidator, createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),
                new PriorityClauseValuesGenerator(constantsManager));

        		
        final SearchHandler.ClauseRegistration proiorityCategoryClauseRegistration = new SearchHandler.ClauseRegistration(priorityClauseHandler);

        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
        return new SearchHandler(builder.asList(), null, Collections.singletonList(proiorityCategoryClauseRegistration));
   }

	
	
	/**
	 * Creates Version Search Handler
	 * @return SearchHandler
	 */
	private SearchHandler createFixVersionSearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (VersionClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("versionClauseQueryFactory");
		final ClauseValidator clauseValidator = (FixVersionValidator) ZephyrComponentAccessor.getInstance().getComponent("fixVersionValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler fixVersionClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
				SystemSearchConstant.forFixForVersion(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),
				new FixVersionClauseValueGenerator(versionManager,permissionManager,beanFactory));
        		
        final SearchHandler.ClauseRegistration versionCategoryClauseRegistration = new SearchHandler.ClauseRegistration(fixVersionClauseHandler);

        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
        return new SearchHandler(builder.asList(), null, Collections.singletonList(versionCategoryClauseRegistration));
   }
	
	/**
	 * Creates Component Search Handler
	 * @return
	 */
	private SearchHandler createComponentSearchHandler() {
        ProjectComponentManager projectComponentManager = ComponentAccessor.getProjectComponentManager();
    	ComponentResolver componentResolver = new ComponentResolver(projectComponentManager);
        ProjectManager projectManager = ComponentAccessor.getProjectManager();

		final ClauseQueryFactory clauseQFactory  = (ComponentClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("componentClauseQueryFactory");
        final ClauseValidator clauseValidator = (ComponentValidator)ComponentAccessor.getComponentOfType(ComponentValidator.class);
        final ClauseContextFactory clauseCFactory = new ComponentClauseContextFactory(jqlOperandResolver,componentResolver,projectManager,permissionManager);
        

        final ClauseHandler componentClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(SystemSearchConstant.forComponent(),
        		clauseQFactory, clauseValidator, createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),
        		new ComponentClauseValuesGenerator(projectComponentManager, projectManager, permissionManager));

        final SearchHandler.ClauseRegistration componentCategoryClauseRegistration = new SearchHandler.ClauseRegistration(componentClauseHandler);

        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
        return new SearchHandler(builder.asList(), null, Collections.singletonList(componentCategoryClauseRegistration));
   }


	/**
	 * Creates Issue Search Handler
	 * @return SearchHandler
	 */
	private SearchHandler createIssueSearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (IssueClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("issueClauseQueryFactory");
        final ClauseValidator clauseValidator = (IssueIdValidator)ComponentAccessor.getComponentOfType(IssueIdValidator.class);
        final IssueIdClauseContextFactory contextFactory =  new IssueIdClauseContextFactory(jqlIssueSupport, jqlOperandResolver, SystemSearchConstant.forIssue().getSupportedOperators());

        final ClausePermissionHandler clausePermissionHandler = createNoOpClausePermissionHandler(new IssueClauseValueSanitiser(permissionManager, jqlOperandResolver, jqlIssueSupport));
        final ClauseHandler issueKeySearchHandler = new ZephyrDefaultClauseHandler(SystemSearchConstant.forIssue(), clauseQFactory,
            clauseValidator, clausePermissionHandler, contextFactory);
        final SearchHandler.ClauseRegistration savedFilterClauseRegistration = new SearchHandler.ClauseRegistration(issueKeySearchHandler);

        final FieldVisibilityManager fieldVisibilityManager = (FieldVisibilityManager) ComponentManager.getComponentInstanceOfType(FieldVisibilityManager.class);
        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.<FieldIndexer> newBuilder(
				new IssueKeyIndexer(fieldVisibilityManager));
        return new SearchHandler(builder.asList(), null, Collections.singletonList(savedFilterClauseRegistration));

	}

	/**
	 * Creates Summary Search Handler
	 * @return SearchHandler
	 */
	private SearchHandler createSummarySearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (SummaryClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("summaryClauseQueryFactory");
		final ClauseValidator clauseValidator = (SummaryValidator) ZephyrComponentAccessor.getInstance().getComponent("summaryValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler summaryClauseHandler = new ZephyrDefaultClauseHandler(
				SystemSearchConstant.forTestSummary(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

        final SearchHandler.ClauseRegistration summaryClauseRegistration = new SearchHandler.ClauseRegistration(summaryClauseHandler);

        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
        return new SearchHandler(builder.asList(), null, Collections.singletonList(summaryClauseRegistration));
   }


	/**
	 * Creates Execution Defect Key Handler
	 * @return SearchHandler
	 */
	private SearchHandler createExecutionDefectKeySearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (ExecutionDefectKeyClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("executionDefectKeyClauseQueryFactory");
        final ClauseValidator clauseValidator = (IssueValidator)ZephyrComponentAccessor.getInstance().getComponent("issueValidator");
        final IssueIdClauseContextFactory contextFactory =  new IssueIdClauseContextFactory(jqlIssueSupport, jqlOperandResolver, SystemSearchConstant.forLinkedDefectKey().getSupportedOperators());

        final ClausePermissionHandler clausePermissionHandler = createNoOpClausePermissionHandler(new IssueClauseValueSanitiser(permissionManager, jqlOperandResolver, jqlIssueSupport));
        final ClauseHandler issueKeySearchHandler = new ZephyrDefaultClauseHandler(SystemSearchConstant.forLinkedDefectKey(), clauseQFactory,
            clauseValidator, clausePermissionHandler, contextFactory);
        final SearchHandler.ClauseRegistration savedFilterClauseRegistration = new SearchHandler.ClauseRegistration(issueKeySearchHandler);

        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
        return new SearchHandler(builder.asList(), null, Collections.singletonList(savedFilterClauseRegistration));
	}
	
	/**
	 * Creates Assignee Key Handler
	 * @return SearchHandler
	 */
	private SearchHandler createAssigneeSearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (AssigneeClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("assigneeClauseQueryFactory");
        final ClauseValidator clauseValidator = (ExecutedByValidator)ZephyrComponentAccessor.getInstance().getComponent("executedByValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler assigneeClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
				SystemSearchConstant.forAssignee(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),new AssigneeClauseValueGenerator(scheduleManager,emailFormatter));
		
        final SearchHandler.ClauseRegistration savedFilterClauseRegistration = new SearchHandler.ClauseRegistration(assigneeClauseHandler);

        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
        return new SearchHandler(builder.asList(), null, Collections.singletonList(savedFilterClauseRegistration));
	}


	/**
	 * Returns Schedule ID Search Handler
	 * @return SearcHandler
	 */
	private SearchHandler createEstimatedTimeSearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (EstimationTimeClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("estimationTimeClauseQueryFactory");
		final ClauseValidator clauseValidator = (EstimationTimeValidator)ZephyrComponentAccessor.getInstance().getComponent("estimationTimeValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler scheduleClauseHandler = new ZephyrDefaultClauseHandler(
				SystemSearchConstant.forEstimationTime(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

		final SearchHandler.ClauseRegistration scheduleClauseRegistration = new SearchHandler.ClauseRegistration(
				scheduleClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(scheduleClauseRegistration));

	}

	/**
	 * Returns Schedule ID Search Handler
	 * @return SearcHandler
	 */
	private SearchHandler createLoggedTimeSearchHandler() {
		final ClauseQueryFactory clauseQFactory  = (LoggedTimeClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("loggedTimeClauseQueryFactory");
		final ClauseValidator clauseValidator = (EstimationTimeValidator)ZephyrComponentAccessor.getInstance().getComponent("estimationTimeValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler scheduleClauseHandler = new ZephyrDefaultClauseHandler(
				SystemSearchConstant.forLoggedTime(), clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

		final SearchHandler.ClauseRegistration scheduleClauseRegistration = new SearchHandler.ClauseRegistration(
				scheduleClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(scheduleClauseRegistration));

	}

	private ClausePermissionHandler createNoOpClausePermissionHandler() {
		return new DefaultClausePermissionHandler(
				NoOpClausePermissionChecker.NOOP_CLAUSE_PERMISSION_CHECKER);
	}

	/**
	 * Support for Multi Clause
	 * @param factory
	 * @return
	 */
	private ClauseContextFactory decorateWithMultiContextFactory(
			final ClauseContextFactory factory) {
		final MultiClauseDecoratorContextFactory.Factory multiFactory = ComponentAccessor
				.getComponentOfType(MultiClauseDecoratorContextFactory.Factory.class);
		return multiFactory.create(factory);
	}

	
	private ClausePermissionHandler createNoOpClausePermissionHandler(
			final ClauseSanitiser sanitiser) {
		return new DefaultClausePermissionHandler(
				NoOpClausePermissionChecker.NOOP_CLAUSE_PERMISSION_CHECKER,
				sanitiser);
	}


    /**
     * Create Labels Search Handler
     * @return SearchHandler
     */
    private SearchHandler createLabelSearchHandler() {
        final ClauseQueryFactory clauseQFactory  = (LabelClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("labelClauseQueryFactory");
                //JIRA
                //new LabelsClauseQueryFactory(jqlOperandResolver, DocumentConstants.ISSUE_LABELS);
                //Custom
				//(LabelClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("labelClauseQueryFactory");
        final ClauseValidator clauseValidator = (LabelValidator)ZephyrComponentAccessor.getInstance().getComponent("labelValidator");
                //JIRA
                //new LabelsValidator(jqlOperandResolver);
                //Custom
                //(LabelValidator)ZephyrComponentAccessor.getInstance().getComponent("labelValidator");
        final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

        //JIRA
        LabelsClauseValuesGenerator lvg = new LabelsClauseValuesGenerator(labelManager);

        final ClauseHandler labelClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
                SystemSearchConstant.forLabel(), clauseQFactory, clauseValidator,
                createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory),
                new LabelClauseValueGenerator(labelManager, issueManager, permissionManager,beanFactory));
            //new LabelClauseValueGenerator(labelManager, issueManager, permissionManager,beanFactory)

        final SearchHandler.ClauseRegistration labelClauseRegistration = new SearchHandler.ClauseRegistration(
                labelClauseHandler);


        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
        return new SearchHandler(builder.asList(), null, Collections.singletonList(labelClauseRegistration));
    }
    
    @SuppressWarnings("unchecked")
	private SearchHandler createStringCustomFieldSearchHandler(Integer customFieldId, String clauseName) {
		final ClauseQueryFactory clauseQFactory = (CustomFieldStringClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("customFieldStringClauseQueryFactory");
		final ClauseValidator clauseValidator = (CustomFieldStringValidator) ZephyrComponentAccessor.getInstance().getComponent("customFieldStringValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();
		SimpleFieldSearchConstants customFieldClause = new SimpleFieldSearchConstants(String.valueOf(customFieldId),
	    		new ClauseNames(clauseName), clauseName,String.valueOf(customFieldId), String.valueOf(customFieldId),
	    		new HashSet<Operator>(OperatorClasses.TEXT_OPERATORS), ZephyrDataTypes.TEXT);
		final ClauseHandler customFieldClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
				customFieldClause, clauseQFactory, clauseValidator,
				new DefaultClausePermissionHandler(
						NoOpClausePermissionChecker.NOOP_CLAUSE_PERMISSION_CHECKER), ComponentAccessor
				.getComponentOfType(MultiClauseDecoratorContextFactory.Factory.class).create(clauseCFactory), 
				new CustomFieldStringClauseValueGenerator(permissionManager,zephyrCustomFieldManager,beanFactory));
		final SearchHandler.ClauseRegistration customFieldClauseRegistration = new SearchHandler.ClauseRegistration(customFieldClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(customFieldClauseRegistration));
	}

	/**
	 * Custom Field Number Field Handler
	 *
	 * @param customFieldId
	 * @param clauseName
	 * @return
	 */
    private SearchHandler createNumberCustomFieldSearchHandler(Integer customFieldId, String clauseName) {
		final ClauseQueryFactory clauseQFactory  = (CustomFieldNumberClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("customFieldNumberClauseQueryFactory");
        final ClauseValidator clauseValidator = (CustomFieldNumberValidator)ZephyrComponentAccessor.getInstance().getComponent("customFieldNumberValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();
		SimpleFieldSearchConstants customFieldClause = new SimpleFieldSearchConstants(String.valueOf(customFieldId),
	    		new ClauseNames(clauseName), clauseName,String.valueOf(customFieldId),String.valueOf(customFieldId),
				OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.NUMBER);

		final ClauseHandler customFieldClauseHandler = new ZephyrDefaultClauseHandler(
				customFieldClause, clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));
		final SearchHandler.ClauseRegistration customFieldClauseRegistration = new SearchHandler.ClauseRegistration(customFieldClauseHandler);
		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(customFieldClauseRegistration));

	}


	/**
	 * Custom Field Data Field Handler
	 *
	 * @param customFieldId
	 * @param clauseName
	 * @return
	 */
    private SearchHandler createDateCustomFieldSearchHandler(Integer customFieldId, String clauseName) {
		SimpleFieldSearchConstants customFieldDateClause = new SimpleFieldSearchConstants(String.valueOf(customFieldId),
			new ClauseNames(clauseName), clauseName,String.valueOf(customFieldId),String.valueOf(customFieldId),
			OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.DATE);
		final ClauseQueryFactory clauseQFactory  = (CustomFieldDateClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("customFieldDataClauseQueryFactory");
		final ClauseValidator clauseValidator = (CustomFieldDateValidator) ZephyrComponentAccessor.getInstance().getComponent("customFieldDateValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler customDateClauseHandler = new ZephyrDefaultClauseHandler(
				customFieldDateClause, clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

		final SearchHandler.ClauseRegistration customDateClauseRegistration = new SearchHandler.ClauseRegistration(customDateClauseHandler);
		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(customDateClauseRegistration));
    }


	/**
	 * Custom Field Data Field Handler
	 *
	 * @param customFieldId
	 * @param clauseName
	 * @return
	 */
	private SearchHandler createDateTimeCustomFieldSearchHandler(Integer customFieldId, String clauseName) {
		SimpleFieldSearchConstants customFieldDateTimeClause = new SimpleFieldSearchConstants(String.valueOf(customFieldId),
				new ClauseNames(clauseName), clauseName,String.valueOf(customFieldId),String.valueOf(customFieldId),
				OperatorClasses.EQUALITY_AND_RELATIONAL_WITH_EMPTY, ZephyrDataTypes.DATE);
		final ClauseQueryFactory clauseQFactory  = (CustomFieldDateTimeClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("customFieldDataTimeClauseQueryFactory");
		final ClauseValidator clauseValidator = (CustomFieldDateTimeValidator) ZephyrComponentAccessor.getInstance().getComponent("customFieldDateTimeValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();

		final ClauseHandler customDateClauseHandler = new ZephyrDefaultClauseHandler(
				customFieldDateTimeClause, clauseQFactory, clauseValidator,
				createNoOpClausePermissionHandler(), decorateWithMultiContextFactory(clauseCFactory));

		final SearchHandler.ClauseRegistration customDateClauseRegistration = new SearchHandler.ClauseRegistration(customDateClauseHandler);
		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(customDateClauseRegistration));
	}


    private SearchHandler createLargeTextCustomFieldSearchHandler(Integer customFieldId, String clauseName) {
        final ClauseQueryFactory clauseQFactory = (CustomFieldLargeTextClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("customFieldLargeTextClauseQueryFactory");
        final ClauseValidator clauseValidator = (CustomFieldLargeTextValidator) ZephyrComponentAccessor.getInstance().getComponent("customFieldLargeTextValidator");
        final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();
        SimpleFieldSearchConstants customFieldLargeTextClause = new SimpleFieldSearchConstants(String.valueOf(customFieldId),
                new ClauseNames(clauseName), clauseName,String.valueOf(customFieldId),String.valueOf(customFieldId),
                new HashSet<>(OperatorClasses.TEXT_OPERATORS), ZephyrDataTypes.TEXT);

        final ClauseHandler customFieldLargeTextClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
                customFieldLargeTextClause, clauseQFactory, clauseValidator,
				new DefaultClausePermissionHandler(
						NoOpClausePermissionChecker.NOOP_CLAUSE_PERMISSION_CHECKER), ComponentAccessor
				.getComponentOfType(MultiClauseDecoratorContextFactory.Factory.class).create(clauseCFactory),
				new CustomFieldLargeTextClauseValueGenerator(customFieldValueManager,zephyrCustomFieldManager,beanFactory));
        final SearchHandler.ClauseRegistration customFieldClauseRegistration = new SearchHandler.ClauseRegistration(customFieldLargeTextClauseHandler);

        final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
        return new SearchHandler(builder.asList(), null,Collections.singletonList(customFieldClauseRegistration));
    }


	private SearchHandler createOptionCustomFieldSearchHandler(Integer customFieldId, String clauseName) {
		final ClauseQueryFactory clauseQFactory = (CustomFieldOptionClauseQueryFactory) ZephyrComponentAccessor.getInstance().getComponent("customFieldOptionClauseQueryFactory");
		final ClauseValidator clauseValidator = (CustomFieldOptionTextValidator) ZephyrComponentAccessor.getInstance().getComponent("customFieldOptionValidator");
		final ClauseContextFactory clauseCFactory = new SimpleClauseContextFactory();
		SimpleFieldSearchConstants customFieldListClause = new SimpleFieldSearchConstants(String.valueOf(customFieldId),
				new ClauseNames(clauseName), clauseName,String.valueOf(customFieldId), String.valueOf(customFieldId),
				new HashSet<Operator>(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY), ZephyrDataTypes.TEXT);

		final ClauseHandler customFieldOptionClauseHandler = new ZephyrDefaultValuesGeneratingClauseHandler(
				customFieldListClause, clauseQFactory, clauseValidator,
				new DefaultClausePermissionHandler(
						NoOpClausePermissionChecker.NOOP_CLAUSE_PERMISSION_CHECKER), ComponentAccessor
				.getComponentOfType(MultiClauseDecoratorContextFactory.Factory.class).create(clauseCFactory),
				new CustomFieldStringClauseValueGenerator(permissionManager,zephyrCustomFieldManager,beanFactory));
		final SearchHandler.ClauseRegistration customFieldClauseRegistration = new SearchHandler.ClauseRegistration(customFieldOptionClauseHandler);

		final CollectionBuilder<FieldIndexer> builder = CollectionBuilder.newBuilder();
		return new SearchHandler(builder.asList(), null,Collections.singletonList(customFieldClauseRegistration));
	}

	/**
	 * 
	 * @return Collection of Search Handler that supports clauses
	 */
	@Override
	public Collection<SearchHandler> getZQLClauseSearchHandlers() {
		if (systemClauseSearchHandlers == null) {
			systemClauseSearchHandlers = Lists.newArrayList(createIssueSearchHandler(), createComponentSearchHandler(), 
					createPrioritySearchHandler(), createProjectSearchHandler(),createExecutionStatusSearchHandler(),
					createExecutedBySearchHandler(),createCycleNameSearchHandler(), createCycleIdSearchHandler(),
					createExecutionDateSearchHandler(),createDateCreatedSearchHandler(),createScheduleSearchHandler(),
					createFixVersionSearchHandler(),createSummarySearchHandler(),createExecutionDefectKeySearchHandler(),createLabelSearchHandler(),createAssigneeSearchHandler(),
					createFolderIdSearchHandler(), createFolderNameSearchHandler(), createEstimatedTimeSearchHandler(), createLoggedTimeSearchHandler());
		}
		return systemClauseSearchHandlers;
	}

	@Override
	public Collection<SearchHandler> getZQLCustomClauseSearchHandlers() {
		return customClauseSearchHandlers;
	}


	@Override
	public SearchHandler addClauseHandlerForCustomFieldType(String customFieldType, Integer customFieldId, String clauseName) {
		if(customClauseSearchHandlers == null) {
			customClauseSearchHandlers = Lists.newArrayList();
		}
		SearchHandler handler = null;
		switch(customFieldType) {
			case ApplicationConstants.STRING_VALUE:
				handler = createStringCustomFieldSearchHandler(customFieldId,clauseName);
				customClauseSearchHandlers.add(handler);
				return handler;
			case ApplicationConstants.NUMBER_VALUE:
				handler = createNumberCustomFieldSearchHandler(customFieldId,clauseName);
				customClauseSearchHandlers.add(handler);
				return handler;
			case ApplicationConstants.DATE_VALUE:
				handler = createDateCustomFieldSearchHandler(customFieldId,clauseName);
				customClauseSearchHandlers.add(handler);
				return handler;
			case ApplicationConstants.DATE_TIME_VALUE:
				handler = createDateTimeCustomFieldSearchHandler(customFieldId,clauseName);
				customClauseSearchHandlers.add(handler);
				return handler;
			case ApplicationConstants.LARGE_VALUE:
				handler = createLargeTextCustomFieldSearchHandler(customFieldId,clauseName);
				customClauseSearchHandlers.add(handler);
				return handler;
			case ApplicationConstants.LIST_VALUE:
				handler = createOptionCustomFieldSearchHandler(customFieldId,clauseName);
				customClauseSearchHandlers.add(handler);
				return handler;
		}
		return null;
	}
}
