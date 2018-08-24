/**
 * Zephyr Test Board View
 * Execution views
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }
if (typeof ZEPHYR.Agile.TestBoard.Issues == 'undefined') { ZEPHYR.Agile.TestBoard.Issues = {}; }

/**
 * - Show hide executions
 */
ZEPHYR.Agile.TestBoard.Issues.ExecutionsView = Backbone.View.extend({
	events: {
		'click .zephyr-td-detail-execute-test': 'navigateToExecuteTest'
	},
	initialize: function(options) {
		_.bindAll(this, 'getExecutionsBasedOnStatusUpdate');
		this.offset = 0;
		this.issueId = options.issueId;
		this.attachViews();
		ZEPHYR.Agile.TestBoard.globalDispatcher.off(ZEPHYR.Agile.TestBoard.Events.EXECUTIONS_GET + '_' + this.issueId);
		ZEPHYR.Agile.TestBoard.globalDispatcher.on(ZEPHYR.Agile.TestBoard.Events.EXECUTIONS_GET + '_' + this.issueId, this.getExecutionsBasedOnOffset, this);
		jQuery(document).off("ScheduleUpdated");
		jQuery(document).on("ScheduleUpdated", this.getExecutionsBasedOnStatusUpdate);
	},
	getExecutionsBasedOnOffset: function(offset) {
		this.offset = offset;
		this.fetchExecutions();
	},
	getExecutionsBasedOnStatusUpdate: function(ev, executionId) {
		// Loop through all the issues that are containing the version progress bar
		_.each(AJS.$('.zephyr-tb-issue-compact'), function(sprintIssueEl) {
			if(AJS.$(sprintIssueEl).find('span.zephyr-tb-versions-table')) {
				var issueId = AJS.$(sprintIssueEl).attr('data-issue-id');
				// trigger call to update issue execution status progress bar and issue status
				ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.ISSUE_PROGRESS_BAR_UPDATE + '_' + issueId, executionId);
			}
		});
	},
	navigateToExecuteTest: function(ev) {
		ev.preventDefault();
		var targetEl = AJS.$(ev.target),
			index = targetEl.data('index'),
			elHref = targetEl.data('href');

		window.location.href = elHref;
	},
	attachViews: function() {
		// Attach Execution Pagination
		this.executionPaginationView = new ZEPHYR.Agile.TestBoard.ExecutionPaginationView({
			el: '#issue-execution-pagination-container-' + this.issueId,
			model: this.model,
			eventId: this.issueId
		});
	},
	getExecutionDataParams: function(offset) {
		var dataParams = {
			"issueIdOrKey": this.issueId,
			"offset": offset,
			"maxRecords": 10,
			"expand": "executionStatus",
			"action": this.action
		}
		return dataParams;
	},
	fetchExecutions: function() {
		var instance = this,
			dataParams = this.getExecutionDataParams(this.offset);

		ZEPHYR.Agile.TestBoard.Loading.showLoadingIndicator();
		this.model.fetch({
			reset: true,
			data: dataParams,
			timeout: 0, // Removing the timeout
			success: function(executions, response) {
				if(instance.action == 'expand') {
					instance.render();
					if(response.executionSummaries) {
						AJS.$('#issueBoard-defectCount-' + instance.issueId).html(ZEPHYR.Templates.TestBoard.Issues.defectCountHTML({
							totalDefectCount: response.totalDefectCount,
							totalOpenDefectCount: response.totalOpenDefectCount
						}));
						AJS.$('#issueBoard-progressBar-' + instance.issueId).html(ZEPHYR.Templates.TestBoard.versionProgressHTML({
							summaryList: JSON.parse(response.executionSummaries),
							totalExecutions: response.totalExecutions
						}));
						AJS.$('#issueBoard-progressBar-' + instance.issueId).append('<span totalExecuted='+ response.totalExecuted +' totalExecutions='+ response.totalExecutions +' class="zephyr-tb-version-progress-percentage">' + ((response.totalExecuted*100/response.totalExecutions).toFixed(2)) + '%</span>')
					} else {
						AJS.$('#issueBoard-defectCount-' + instance.issueId).html('');
						AJS.$('#issueBoard-progressBar-' + instance.issueId).html('');
					}

				}
				else {
					instance.$el.html('');
					AJS.$('#issue-execution-pagination-container-' + instance.issueId).html('');
				}
				ZEPHYR.Agile.TestBoard.Loading.hideLoadingIndicator();
			},
			error: function(executions, response) {
				ZEPHYR.Agile.TestBoard.Loading.hideLoadingIndicator();
				AJS.$('.zephyr-tb-message-bar').html('');
				var responseJSON = JSON.parse(response.responseText);
				var responseText = responseJSON.error[0] ||responseJSON.message || responseJSON.errorDescHtml;
				ZEPHYR.Agile.TestBoard.showErrorMessage(response, responseText, 3000);
			}
		});
	},
	render: function() {
		var executionStatus = this.model.executionStatus,
			executions = this.model.toJSON() || [];
			executions.forEach(function(cycle){
				cycle.cycleName = cycle.cycleName || '';
				cycle.folderName = cycle.folderName || '';
			});
		var	executionsHTML = ZEPHYR.Templates.TestBoard.Issues.executionsHTML({
				executions: executions,
				executionStatusList: executionStatus,
				issueId: this.issueId
			});
		this.$el.html(executionsHTML);
		// Create editable view for status field.
		var editableFields = AJS.$('#zephyr-tb-executions-table div.field-group.execution-status-container');
		AJS.$.each(editableFields, function(i, $container) {
			var executionStatusView = new ZEPHYR.Schedule.executionStatusView({
				el: 			$container,
				elBeforeEdit:	AJS.$($container).find('[id^=execution-status-value-schedule]'),
				elOnEdit:		AJS.$($container).find('[id^=execution-field-select-schedule]')
			});
		});
		return this;
	}
});
