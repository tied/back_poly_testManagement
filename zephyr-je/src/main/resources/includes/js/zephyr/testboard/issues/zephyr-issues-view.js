/**
 * Zephyr Test Board View
 * 
 * Issues view 
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }
if (typeof ZEPHYR.Agile.TestBoard.Issues == 'undefined') { ZEPHYR.Agile.TestBoard.Issues = {}; }

ZEPHYR.Agile.TestBoard.Issues.ACTION_EXPAND = 'expand';
ZEPHYR.Agile.TestBoard.Issues.ACTION_COLLAPSE = 'collapse';
ZEPHYR.Agile.TestBoard.Issues.ClassNames = {
	ACTION_ICON_EXPANDED: 'aui-iconfont-expanded',
	ACTION_ICON_COLLAPSED: 'aui-iconfont-collapsed',
	ACTION_ISSUES_EXPANDED: 'zephyr-tb-issues-expanded'
}

// Issues
ZEPHYR.Agile.TestBoard.Issue = Backbone.Model.extend();
ZEPHYR.Agile.TestBoard.IssueCollection = Backbone.Collection.extend({
	model: ZEPHYR.Agile.TestBoard.Issue
});

/**
 * - show/hide execution details
 * - Toggle between show/hide issues [remember in API]
 * - Show expand only if executions for that issue exists
 */
ZEPHYR.Agile.TestBoard.Issues.SprintIssueView = Backbone.View.extend({
	initialize: function(options) {
		_.bindAll(this, 'toggleExecutions');
		this.issueId = this.model.get('id');
		this.sprintId = options.sprintId;
		this.attachEvents();
		ZEPHYR.Agile.TestBoard.globalDispatcher.off(ZEPHYR.Agile.TestBoard.Events.ISSUE_PROGRESS_BAR_UPDATE + '_' + this.issueId);
		ZEPHYR.Agile.TestBoard.globalDispatcher.on(ZEPHYR.Agile.TestBoard.Events.ISSUE_PROGRESS_BAR_UPDATE + '_' + this.issueId, this.updateExecutionSummaryDetails, this);
	},
	updateExecutionSummaryDetails: function(executionId) {
		var instance = this,
			updatedExecutionId = parseInt(executionId);
		this.fetchIssuesBySprintAndIds(function(response) {
			if(!response.successful)
				return;
			var issueSummaries = response.successful[instance.issueId];
			if(issueSummaries) {
				instance.model.set(issueSummaries);
				var versionProgressHTML = ZEPHYR.Templates.TestBoard.versionProgressHTML({
					summaryList: instance.model.get('executionSummaries'),
					totalExecutions: instance.model.get('totalExecutions')
				}),
				versionProgressPercentageHTML = ((instance.model.get('totalExecuted')*100)/instance.model.get('totalExecutions')).toFixed(2);
				instance.$el.find('span.zephyr-tb-versions-table').html(versionProgressHTML)
					.append('<span class="zephyr-tb-version-progress-percentage">' + versionProgressPercentageHTML + '%</span>');
				/*
				 * If the issue is expanded and the issue contains the execution then re-fetch the executions
				 */
				if(instance.model.get('action')) {
					_.each(instance.$el.find('#zephyr-tb-executions-table .zephyr-tb-sprint-issue-execution'), function(sprintIssueEl) {
						var _executionId = AJS.$(sprintIssueEl).attr('data-execution-id');
						// Fetch the updated executions
						if(_executionId == updatedExecutionId) 
							instance.executionView.fetchExecutions();
					});	
				}
			}
		});
	},

	fetchIssuesBySprintAndIds: function(sucessCallback) {
		var instance = this;
			
		AJS.$.ajax({
			url: getRestURL() + "/execution/executionSummariesBySprintAndIssue",
			type : 'POST',
    		contentType: "application/json",
			data: JSON.stringify({
				sprintId: this.sprintId,
				issueIdOrKeys: this.issueId
			}),
			timeout: 0,
			success: function(response) {
				sucessCallback(response);
			},
			error: function(response) {
				AJS.$('.zephyr-tb-message-bar').html('');
				var responseJSON = JSON.parse(response.responseText);
				var responseText = responseJSON.message || responseJSON.errorDescHtml;
				ZEPHYR.Agile.TestBoard.showErrorMessage(response, responseText, 3000, notFadeMessage);
			}
		});
	},
	attachViews: function(action) {
		var issueId = this.model.get('id');
		// Attach Executions view
		if(!this.executions) {
			this.executions = new ZEPHYR.Agile.TestBoard.ExecutionCollection();
		}
		if(!this.executionView) {
			this.executionView = new ZEPHYR.Agile.TestBoard.Issues.ExecutionsView({
				el : '#zephyr-tb-issue-execution-container-' + issueId,
				model : this.executions,
				issueId: issueId
			});
		}
		this.executionView.action = action;
		return this;
	},
	attachEvents: function() {
		var action = this.model.get('action'),
			issueId = this.model.get('id'),
			totalExecutions = this.model.get('totalExecutions');
		// Attach header toggle event to show/hide executions
		this.$el.find('.zephyr-tb-toggle-header').unbind('click')
			.bind('click', this.toggleExecutions);
		
		// If action is expand, display the executions
		if(action == ZEPHYR.Agile.TestBoard.Issues.ACTION_EXPAND && totalExecutions > 0) {
			var targetEl = '#zephyr-tb-toggle-header-' + issueId;
			this.updateExecutionExpandAction(targetEl);
			this.executionView.fetchExecutions();
		}
	},
	updateExecutionExpandAction: function(targetEl) {
		AJS.$(targetEl).removeClass(ZEPHYR.Agile.TestBoard.Issues.ClassNames.ACTION_ICON_COLLAPSED)
			.addClass(ZEPHYR.Agile.TestBoard.Issues.ClassNames.ACTION_ICON_EXPANDED);
		this.$el.addClass(ZEPHYR.Agile.TestBoard.Issues.ClassNames.ACTION_ISSUES_EXPANDED);
		this.model.set('action', ZEPHYR.Agile.TestBoard.Issues.ACTION_EXPAND);
		this.attachViews(ZEPHYR.Agile.TestBoard.Issues.ACTION_EXPAND);
	},
	updateExecutionCollapseAction: function(targetEl) {
		var issueId = this.model.get('id');
		
		AJS.$(targetEl).removeClass(ZEPHYR.Agile.TestBoard.Issues.ClassNames.ACTION_ICON_EXPANDED)
			.addClass(ZEPHYR.Agile.TestBoard.Issues.ClassNames.ACTION_ICON_COLLAPSED);
		this.$el.removeClass(ZEPHYR.Agile.TestBoard.Issues.ClassNames.ACTION_ISSUES_EXPANDED);
		AJS.$('#issue-execution-pagination-container-' + issueId).html('');
		this.model.set('action', ZEPHYR.Agile.TestBoard.Issues.ACTION_COLLAPSE);
		this.attachViews(ZEPHYR.Agile.TestBoard.Issues.ACTION_COLLAPSE);
	},
	toggleExecutions: function(ev) {
		var isCollapsed = AJS.$(ev.target).hasClass('aui-iconfont-collapsed');
		
		if(isCollapsed) {
			this.updateExecutionExpandAction(ev.target);
		} else {
			this.updateExecutionCollapseAction(ev.target);
		}
		this.executionView.fetchExecutions();
	}
});

/**
 * - Display the issues per sprint
 * - Map the JIRA issues data to our execution data 
 */
ZEPHYR.Agile.TestBoard.Issues.SprintIssuesView = Backbone.View.extend({
	initialize : function(options) {
		this.model.bind('reset', this.render, this);
		this.sprintId = options.sprintId;
		ZEPHYR.Agile.TestBoard.globalDispatcher.on(ZEPHYR.Agile.TestBoard.Events.ISSUES_FILTER, this.filterIssues, this);
	},
	/**
	 * Filter issues by issue key / summary
	 */
	filterIssues: function(issueInput) {
		issueInput = (issueInput)? issueInput.toString().toLowerCase(): '';
		var issues = this.model.filter(function(issue) {
				var key = issue.get("key").toLowerCase();
				var summary = issue.get("summary").toLowerCase();
				return (key.indexOf(issueInput) > -1 || summary.indexOf(issueInput) > -1);
			}).map(function(issue) {
				return issue.attributes;
			});
		this.displayIssues(issues);
		ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.ISSUES_COUNT_UPDATE + '_' + this.sprintId, issues.length);
	},
	attachIssueView : function() {
		var instance = this;
		// Attach issue View
		_.each(this.$el.find('.zephyr-tb-issue-compact'), function(issueEl) {
			var issueId = AJS.$(issueEl).data('issue-id');
			var issue = instance.model.get(issueId);
			if (issue) {
				var sprintIssueView = new ZEPHYR.Agile.TestBoard.Issues.SprintIssueView({
					el : issueEl,
					model : issue,
					sprintId: instance.sprintId
				});
			}
		});
	},
	displayIssues: function(issues) {
		var issuesHTML = ZEPHYR.Templates.TestBoard.Issues.issuesHTML({
			issues: issues
		});
		this.$el.html(issuesHTML);
		this.attachIssueView();
	},
	render : function() {
		var issues = this.model.toJSON();
		this.displayIssues(issues);
		ZEPHYR.Agile.TestBoard.updateContentAreaHeight();
		ZEPHYR.Agile.TestBoard.Loading.hideLoadingIndicator();
		return this;
	}
});