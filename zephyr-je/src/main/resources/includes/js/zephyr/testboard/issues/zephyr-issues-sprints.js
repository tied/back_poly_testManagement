/**
 * Zephyr Test Board Issues - Sprints
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }
if (typeof ZEPHYR.Agile.TestBoard.Issues == 'undefined') { ZEPHYR.Agile.TestBoard.Issues = {}; }

/**
 * - show/hide issues
 */
ZEPHYR.Agile.TestBoard.Issues.SprintView = Backbone.View.extend({
	initialize : function() {
		this.attachViews();
		this.sprintId = this.model.get('id');
		ZEPHYR.Agile.TestBoard.globalDispatcher.on(ZEPHYR.Agile.TestBoard.Events.ISSUES_COUNT_UPDATE + '_'+ this.sprintId, this.displayIssuesCount, this);
		this.selectedCycleIds = [];
	},
	displayIssuesCount : function(issueCount) {
		var issueCountHTML;

		if (issueCount < this.issueCount) {
			issueCountHTML = AJS.I18n.getText('zephyr-je.testboard.details.issue.count.visible', issueCount, this.issueCount);
		} else {
			issueCountHTML = AJS.I18n.getText('zephyr-je.testboard.details.issue.count', issueCount);
		}
		this.$el.find('#zephyr-tb-issues-count').html(issueCountHTML);
	},
	attachViews : function() {
		var sprintId = this.model.get('id');
		var instance = this;
		
		// Get issues 
		this.setIssuesByIds();
		// Issues View
		this.issueView = new ZEPHYR.Agile.TestBoard.Issues.SprintIssuesView({
			el: '#sprint-issues-container-' + sprintId,
			model: this.model.issues,
			sprintId: sprintId,
			sprintName: this.model.get('name')
		});
		this.issueCount = this.model.issues.length;
		if(this.issueCount) {
			this.fetchIssuesBySprintAndIds();
		}
		return this;
	},
	getIssueIds: function() {
		var issueIds = this.model.get('issuesIds');
		
		return issueIds.toString();
	},
	fetchIssuesBySprintAndIds: function() {
		var sprintId = this.model.get('id'),
			issueIds = this.getIssueIds(),
			instance = this;
		
		this.$el.find('.zephyr-tb-by-issues-sprint-wait').removeAttr('title');
		this.$el.find('.zephyr-tb-by-issues-sprint-wait').show();	
		// After 60 seconds on hover of the spinning wheel show message
		setTimeout(function() {
			instance.$el.find('.zephyr-tb-by-issues-sprint-wait').attr('title', AJS.I18n.getText('zephyr-je.testboard.issues.wait.title'));
		}, 6000);
		AJS.$.ajax({
			url: getRestURL() + "/execution/executionSummariesBySprintAndIssue",
			type : 'POST',
    		contentType: "application/json",
			data: JSON.stringify({
				sprintId: sprintId,
				issueIdOrKeys: issueIds
			}),
			timeout: 0,
			success: function(response) {
				instance.setIssueSatus(response);
				instance.issueView.render();
				instance.$el.find('.zephyr-tb-by-issues-sprint-wait').hide();
			},
			error: function(response) {
				instance.$el.find('.zephyr-tb-by-issues-sprint-wait').hide();
				var responseJSON = JSON.parse(response.responseText);
				if(response.status == 403 && responseJSON.PERM_DENIED) {
					AJS.$('.zfj-permission-test-view').addClass('active');
					showPermissionError(response);
					AJS.$('.zfj-permission-test-view').removeClass('active');
					instance.issueView.render();
					return;
				}
				if(!ZEPHYR.globalVars.unloaded) {
					AJS.$('.zephyr-tb-message-bar').html('');
					var responseText = responseJSON.message || responseJSON.errorDescHtml || responseJSON.PERM_DENIED;
					ZEPHYR.Agile.TestBoard.showErrorMessage(response, responseText, 3000);
				}
			}
		});
	},
	setIssueSatus: function(response) {
		if(!response.successful)
			return;
		// Parse the response and set it in the execution status in each issue model
		var statusList = response.successful;
		
		_.each(statusList, function(statusJSON, issueId) {
			var issue = this.model.issues.get(issueId); 
			if(issue) {
				issue.set(statusJSON);
			}
		}, this);
		return this;
	},
	setIssuesByIds: function() {
		var issueIds = this.model.get('issuesIds');
		
		this.model.issues.reset([]);
		_.each(issueIds, function(issueId) {
			var issue = ZEPHYR.Agile.TestBoard.data.issues.get(issueId); 
			this.model.issues.add(issue);
		}, this);
	}
});

/**
 * - SprintView - show Sprint details with list of Issues
 */
ZEPHYR.Agile.TestBoard.Issues.SprintsView = Backbone.View.extend({
	events : {
		'click .zephyr-tb-cycle-content' : 'displayCycleDetails'
	},
	initialize : function() {
		this.model.bind('reset', this.render, this);
		this.sprintRows = [];
	},
	attachSprintView : function() {
		var instance = this;
		// TODO: cleanup previous views
		// Attach Sprint View
		_.each(this.$el.find('.zephyr-tb-sprints-module'), function(sprintEl) {
			var sprintId = AJS.$(sprintEl).data('sprint-id');
			var sprint = instance.model.get(sprintId);
			if (sprint) {
				var sprintView = new ZEPHYR.Agile.TestBoard.Issues.SprintView({
					el : sprintEl,
					model : sprint
				});
				instance.sprintRows.push(sprintView);
			}
		});
	},
	render : function() {
		var sprints = this.model.toJSON();
		var sprintsHTML = ZEPHYR.Templates.TestBoard.Issues.sprintsHTML({
			sprints : sprints
		});
		this.$el.html(sprintsHTML);
		this.attachSprintView();
		ZEPHYR.Agile.TestBoard.Loading.hideLoadingIndicator();
		return this;
	}
});