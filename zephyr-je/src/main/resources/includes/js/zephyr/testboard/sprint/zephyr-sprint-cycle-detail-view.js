/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }
ZEPHYR.Agile.TestBoard.minWidth = 400;
ZEPHYR.Agile.TestBoard.containerSelector = '#zephyr-tb-sprints-wrapper';

ZEPHYR.Agile.TestBoard.resizeUI = {
	getContainer: function() {
	    if (!ZEPHYR.Agile.TestBoard.container) {
	        ZEPHYR.Agile.TestBoard.container = AJS.$(ZEPHYR.Agile.TestBoard.containerSelector);
	    }
	    return ZEPHYR.Agile.TestBoard.container;
	},
	/**
	 * 'Element query' for backlog column - used to resize/change elements
	 */
	checkBacklogWidth: function() {
	    var currentWidth = ZEPHYR.Agile.TestBoard.resizeUI.getContainer().width();

	    // These classes must be placed on the body or they won't apply to the elements created when dragging an issue
	    if (currentWidth < 600) {
	        AJS.$('body').addClass('ghx-plan-band-1').removeClass('ghx-plan-band-2');
	    } else {
	        AJS.$('body').addClass('ghx-plan-band-2').removeClass('ghx-plan-band-1');
	    }
		ZEPHYR.Agile.TestBoard.resizeUI.setExecutionDetailWidth();
	    GH.planOnboarding.refresh();
	    // TODO: Refresh elements (usually due to change in layout).
	},
	attachResizable: function() {
		// TODO: Fix the resizable trigger
	    AJS.$("#zephyr-tb-detail-column").resizable({
	        handles: {
	            w: AJS.$("#zephyr-tb-js-sizer")
	        },
	        resize: ZEPHYR.Agile.TestBoard.resizeUI.checkBacklogWidth,
	        stop: ZEPHYR.Agile.TestBoard.resizeUI.storeWidth,
	        maxWidth: ZEPHYR.Agile.TestBoard.resizeUI.getMaxWidth(),
	        minWidth: ZEPHYR.Agile.TestBoard.resizeUI.minWidth
	    });
		ZEPHYR.Agile.TestBoard.resizeUI.setExecutionDetailWidth();
	},
	setExecutionDetailWidth: function() {
		var detailExecutionWidth = AJS.$('#zephyr-tb-detail-column').width();

		AJS.$('#zephyr-tb-detail-execution-section').width((detailExecutionWidth - 40));
		AJS.$('#zephyr-tb-detail-section').width((detailExecutionWidth - 60));
	},
	getMaxWidth: function() {
	    return Math.max(ZEPHYR.Agile.TestBoard.minWidth, (AJS.$("#zephyr-tb-sprints-column").width() + AJS.$("#zephyr-tb-detail-column").width()) / 2);
	},
	setDetailViewWidth: function(width) {
	    localStorage.setItem('zephyr.tb.detailViewWidth', width);
	},
	getDetailViewWidth: function() {
		var detailViewWidth = localStorage.getItem('zephyr.tb.detailViewWidth') || 400;

		return detailViewWidth;
	},
	storeWidth: function() {
	    var detailViewWidth = AJS.$("#zephyr-tb-detail-column").width();
	    // var backlogWidth = AJS.$("#zephyr-tb-sprints-column").width();

	    //var detailViewPercentage = detailViewWidth <= ZEPHYR.Agile.TestBoard.minWidth ? 0 : (detailViewWidth / (detailViewWidth + backlogWidth));
	    var detailViewColumnWidth = detailViewWidth <= ZEPHYR.Agile.TestBoard.minWidth ? ZEPHYR.Agile.TestBoard.minWidth : detailViewWidth;
	    ZEPHYR.Agile.TestBoard.resizeUI.setDetailViewWidth(detailViewColumnWidth);

	    var isSmallWidth = function(detailViewWidth) {
	        return detailViewWidth >= ZEPHYR.Agile.TestBoard.minWidth && detailViewWidth <= 500;
	    };
	    var isMediumWidth = function(detailViewWidth) {
	        return detailViewWidth > 500 && detailViewWidth <= 600;
	    };
	    var isLargeWidth = function(detailViewWidth) {
	        return detailViewWidth > 600;
	    };
	}
};


/**
 * - Show Cycle details
 * - Next
 * - ExecutionDetailsView
 */
ZEPHYR.Agile.TestBoard.SprintCycleDetailView = Backbone.View.extend({
	events: {
		'click .zephyr-tb-detail-close': 	'hideCycleDetails',
		'click .zephyr-td-detail-execute-test': 'navigateToExecuteTest'
	},
	initialize: function() {
		_.bindAll(this, 'getExecutionsBasedOnStatusUpdate', 'getZQLQuery', 'getExecutionDataParams');
		var instance = this
		this.cycleName = null;
		this.cycleId = null;
		this.clearStatus = false;
		this.model.bind('reset', this.renderExecutionUI, this);
		ZEPHYR.Agile.TestBoard.globalDispatcher.off(ZEPHYR.Agile.TestBoard.Events.EXECUTIONS_GET);
		ZEPHYR.Agile.TestBoard.globalDispatcher.on(ZEPHYR.Agile.TestBoard.Events.EXECUTIONS_GET, this.getExecutionsBasedOnOffset, this);
		jQuery(document).off("ScheduleUpdated");
		jQuery(document).on("ScheduleUpdated", instance.getExecutionsBasedOnStatusUpdate);
	},
	attachViews: function() {
		var instance = this;
		var resizeTimeout;
		// Attach Single select for Filter By
		this.attachExecutionStatusSelect();
		// Attach Execution Pagination
		this.executionPaginationView = new ZEPHYR.Agile.TestBoard.ExecutionPaginationView({
			el: '#execution-pagination-container',
			model: this.model
		});
		// Attach ExecutionDetailsView
		var dataParams = this.getExecutionDataParams(this.offset);
		this.fetchExecutions(dataParams);
		AJS.$(window).resize(function() {
			clearTimeout(resizeTimeout);

            resizeTimeout = setTimeout(function () {
				instance.resizeUIHeight();
            }, 250);
		});
		// Attach View Resize
		ZEPHYR.Agile.TestBoard.resizeUI.attachResizable();
	},
	attachExecutionStatusSelect: function() {
		var instance = this;
		AJS.$('#zephyr-tb-status-dd').val(instance.statusJSON.id);
		AJS.$('#zephyr-tb-status-dd').auiSelect2({
			allowClear: true
		})
		.on("change", function (ev) {
			instance.statusId = AJS.$(ev.target).val();
			instance.offset = 0;
			var dataParams = instance.getExecutionDataParams(instance.offset);
			instance.fetchExecutions(dataParams);
			instance.setSelectedStatusParams();
		});
		AJS.$('.select2-container').css({'width': '80%'});

	},
	getStatusName: function() {
		var statusIds = this.statusId,
			statusName = [];
		_.each(statusIds, function(statusId) {
			var statusJSON = ZEPHYR.Agile.TestBoard.executionStatus.filter(function(status) {if(status.id == statusId) {return true;}});
			if(statusJSON && statusJSON[0]) {
				statusName.push('"' + ZEPHYR.Agile.TestBoard.escapeString(statusJSON[0].name) + '"');
			}
		});
		return statusName.toString();
	},

	getZQLQuery: function(useCycleId) {
		var fixVersion = this.versionName;
		var statusName = this.getStatusName();
		var statusZQLClause = (statusName == "") ? "" : 'AND executionStatus in (' + statusName + ') ';
		var folderId = this.folderId;
		var cycleZQLClause = (useCycleId) ? ' AND cycleId=' + this.cycleId : ' AND cycleName in ("' + ZEPHYR.Agile.TestBoard.escapeString(this.cycleName) +'")';
		var zql ;
		if (folderId ){
			zql = 'project = "' + ZEPHYR.Agile.TestBoard.escapeString(this.projectKey) + '" AND fixVersion = "' + ZEPHYR.Agile.TestBoard.escapeString(fixVersion) + '"' +
				cycleZQLClause  +' ' + statusZQLClause +  ' AND folderId = ' + folderId + ' ORDER BY Execution ASC';
		} else {
			zql = 'project = "' + ZEPHYR.Agile.TestBoard.escapeString(this.projectKey) + '" AND fixVersion = "' + ZEPHYR.Agile.TestBoard.escapeString(fixVersion) + '"' +
				cycleZQLClause  +' ' + statusZQLClause + ' ORDER BY Execution ASC';
		}
		return zql;
	},
	getExecutionDataParams: function(offset) {
		var zqlQuery = this.getZQLQuery(true),
			dataParams = {
				zqlQuery: zqlQuery,
				offset: offset,
				maxRecords:	10,
				expand: 'executionStatus'
			};
		this.offset = offset;
		return dataParams;
	},
	getExecutionsBasedOnOffset: function(offset) {
		var dataParams = this.getExecutionDataParams(offset);
		this.fetchExecutions(dataParams);
		this.setSelectedStatusParams();
	},
	getExecutionsBasedOnStatusUpdate: function() {
		var dataParams = this.getExecutionDataParams(this.offset);
		this.fetchExecutions(dataParams);
		// trigger call to update cycle progress bar
		ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.CYCLE_PROGRESS_BAR_UPDATE + '_' + this.sprintId, this.cycleId);
	},
	fetchExecutions: function(dataParams, successCallback) {
		var instance = this,
			detailViewWidth = ZEPHYR.Agile.TestBoard.resizeUI.getDetailViewWidth();
		AJS.$('#zephyr-tb-detail-column').width(detailViewWidth);
		ZEPHYR.Agile.TestBoard.Loading.showLoadingIndicator();
		this.model.fetch({
			reset: true,
			data: dataParams,
			success: function(executions, response) {
				if(successCallback)
					successCallback();
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
	navigateToExecuteTest: function(ev) {
		ev.preventDefault();
		var targetEl = AJS.$(ev.target),
			index = targetEl.data('index'),
			elHref = targetEl.data('href'),
			zqlQuery = this.getZQLQuery(false);

		window.location.href = elHref + encodeURIComponent(zqlQuery) + '&view=detail&offset='+ (this.offset + index + 1);
	},
	hideCycleDetails: function(ev) {
		this.$el.html('').width(0);
		AJS.$('.zephyr-tb-cycle-content').removeClass('zephyr-tb-selected');
		/** Update URL **/
		//ZEPHYR.Agile.TestBoard.data['selectedSprint'] = null;
		//ZEPHYR.Agile.TestBoard.data['selectedCycle'] = null;
		ZEPHYR.Agile.TestBoard.data['view'] = ZEPHYR.Agile.TestBoard.view;
		ZEPHYR.Agile.TestBoard.data['isDetailView'] = false;
		ZEPHYR.Agile.TestBoard.data['selectedStatus'] = null;
		ZEPHYR.Agile.TestBoard.Router.URL.setParams();
		ZEPHYR.Agile.TestBoard.Router.tbRouter.navigate(ZEPHYR.Agile.TestBoard.Router.URL.getURL(), {trigger: false});
	},
	renderExecutionUI: function() {
		var executionStatus = this.model.executionStatus,
			executions = this.model.toJSON() || [];
			executions.forEach(function(cycle){
				cycle.cycleName = cycle.cycleName || '';
				cycle.folderName = cycle.folderName || '';
			});

		var	executionsHTML = ZEPHYR.Templates.TestBoard.Sprint.executionsHTML({
				executions: executions,
				executionStatusList: executionStatus
			});

		ZEPHYR.Agile.TestBoard.Loading.hideLoadingIndicator();
		this.$el.find('#zephyr-tb-detail-section').html(executionsHTML);

		// Attach execution nav menu
		var executionDetailNavHTML = ZEPHYR.Templates.TestBoard.Sprint.executionDetailNavHTML({
			executionCount: (this.model.totalCount || 0)
		});
		AJS.$('#zephyr-tb-detail-execution-nav-menu').html(executionDetailNavHTML);

		// Create editable view for status field.
		var editableFields = AJS.$('#zephyr-tb-executions-table div.field-group.execution-status-container');
		AJS.$.each(editableFields, function(i, $container) {
			var executionStatusView = new ZEPHYR.Schedule.executionStatusView({
				el: 			$container,
				elBeforeEdit:	AJS.$($container).find('[id^=execution-status-value-schedule-]'),
				elOnEdit:		AJS.$($container).find('[id^=execution-field-select-schedule-]')
			});
		});
		/**
		 * Adjust the height dependant on the headers
		 */
		 this.resizeUIHeight();
	},
	resizeUIHeight: function() {
		var containerHeight = AJS.$('#' + ZEPHYR.Agile.TestBoard.data.testBoardElId).height();
		var detailHeaderHeight = AJS.$('#zephyr-tb-detail-head').height() + AJS.$('.zephyr-tb-statistic-group').height() + 20 + 88;		// height +  margin + padding
		AJS.$('#zephyr-tb-detail-execution-section').height(containerHeight - detailHeaderHeight);
		AJS.$('.zephyr-tb-container').height(containerHeight - detailHeaderHeight);
	},
	setSelectedStatusParams: function() {
		var selectedStatus = null;
		var status = AJS.$('#zephyr-tb-status-dd').val();
		if(status) {
			selectedStatus = status + ':' + this.offset;
		}
		ZEPHYR.Agile.TestBoard.data.selectedStatus = selectedStatus;
		/** Update URL */
		ZEPHYR.Agile.TestBoard.Router.URL.setParams();
		ZEPHYR.Agile.TestBoard.Router.tbRouter.navigate(ZEPHYR.Agile.TestBoard.Router.URL.getURL(), {trigger: false});
	},
	getPrevSelectedStatusParams: function() {
		var statusJSON = {};
		if(ZEPHYR.Agile.TestBoard.data.selectedStatus) {
			var statusAttr = ZEPHYR.Agile.TestBoard.data.selectedStatus;
			statusAttr = statusAttr.split(':');
			statusJSON.id = (statusAttr[0]) ? statusAttr[0].split(',') : null;
			statusJSON.offset = statusAttr[1] || 0;
		} else {
			statusJSON.id = null;
			statusJSON.offset = 0;
		}

		return statusJSON;
	},
	render: function() {
	    AJS.$("#zephyr-tb-detail-column").resizable("destroy"); // Destroy the previous resizable
	    this.statusJSON = this.getPrevSelectedStatusParams();
	    this.offset = this.statusJSON.offset;
		var cycleDetailsHTML = ZEPHYR.Templates.TestBoard.Sprint.cycleDetailsHTML({
			executionStatus: ZEPHYR.Agile.TestBoard.executionStatus,
			cycleName: this.cycleName,
			versionName: this.versionName,
			projectKey: this.projectKey,
			folderId : this.folderId,
			folderName : this.folderName
		});
		this.$el.html(cycleDetailsHTML);
		this.statusId = this.statusJSON.id;
		this.attachViews();
		this.clearStatus = true; // To clear the status, if other cycle is selected
		return this;
	}
});