/**
 * Traceability Report Page
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Traceability == 'undefined') { ZEPHYR.Traceability = {}; }
if (typeof ZEPHYR.Traceability.Report == 'undefined') { ZEPHYR.Traceability.Report = {}; }

ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED = 10;
ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT = 'defectToRequirement';
ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT = 'requirementToDefect';
ZEPHYR.Traceability.Report.REQUIREMENT_BY_ID = 'ID';
ZEPHYR.Traceability.Report.REQUIREMENT_BY_KEY = 'KEY';
// Events
ZEPHYR.Traceability.Report.PAGINATION_COUNT_UPDATE = 'PAGINATION_COUNT_UPDATE';
ZEPHYR.Traceability.Report.GET_REQUIREMENTS = 'GET_REQUIREMENTS';
ZEPHYR.Traceability.Report.REQUIREMENT_TEST_VIEW_ATTACH = 'REQUIREMENT_TEST_VIEW_ATTACH';

// Tests
ZEPHYR.Traceability.Report.Test = Backbone.Model.extend();
ZEPHYR.Traceability.Report.TestCollection = Backbone.Collection.extend({
    model: ZEPHYR.Traceability.Report.Test,
    url: function() {
    	return getRestURL() + "/traceability/testsByRequirement";
    },
    parse: function(resp, xhr) {
    	return resp;
    }
});

// Executions
ZEPHYR.Traceability.Report.Execution = Backbone.Model.extend();
ZEPHYR.Traceability.Report.ExecutionCollection = Backbone.Collection.extend({
    model: ZEPHYR.Traceability.Report.Execution,
    url: function() {
    	if(this.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
    		return getRestURL() + "/traceability/executionsByDefect";
    	} else if(this.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
    		return getRestURL() + "/traceability/executionsByTest";
    	}
    },
    initialize: function(options) {
    	(options && options.reportType) ? this.reportType = options.reportType: this.reportType = ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT;
    },
    parse: function(resp, xhr) {
    	this.totalCount = resp.totalCount;
    	this.PERM_DENIED = resp.PERM_DENIED || null;
    	return resp.executions;
    }
});

// Defect Statistics
ZEPHYR.Traceability.Report.DefectStats = Backbone.Model.extend();
ZEPHYR.Traceability.Report.DefectStatsCollection = Backbone.Collection.extend({
    model: ZEPHYR.Traceability.Report.DefectStats,
    url: function() {
    	return getRestURL() + "/traceability/defectStatistics";
    },
    parse: function(resp, xhr) {
    	return resp;
    }
});

/**
 * Traceability report requirement Pagination View
 */
ZEPHYR.Traceability.Report.RequirementPaginationView = Backbone.View.extend({
	tagName:'div',
    events:{
    	"click [id^='req-pagination-']" : 'executePaginatedRequirements',
    	"click [id^='refreshREQId']"	: "refreshREQView"
    },
    initialize:function (options) {
        this.model.bind("reset", this.render,this);
        this.currentOffset = ((ZEPHYR.Traceability.Report.offset) / ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED) + 1;
        this.totalCount = ZEPHYR.Traceability.selectedIssues.length;
        ZEPHYR.Traceability.Report.globalDispatcher.off(ZEPHYR.Traceability.Report.PAGINATION_COUNT_UPDATE);
        ZEPHYR.Traceability.Report.globalDispatcher.on(ZEPHYR.Traceability.Report.PAGINATION_COUNT_UPDATE, this.updateCurrentOffset, this);
    },

    updateCurrentOffset: function(offset) {
    	this.currentOffset = offset;
    },

    getLinks: function(totalCount, currentOffset, maxResultAllowed) {
    	var linksNew = [];
    	linksNew = ZEPHYR.generatePageNumbers(totalCount, currentOffset, maxResultAllowed);
    	return linksNew;
    },
    render:function () {
    	// this.totalCount = this.model.length;
    	this.maxResultAllowed = ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED;
    	this.linksNew = this.getLinks(this.totalCount, this.currentOffset, this.maxResultAllowed);
		this.$el.html(ZEPHYR.Templates.Project.Traceability.addRequirementPaginationFooter({
    		totalCount:		this.totalCount,
    		currentIndex:	this.currentOffset,
    		maxAllowed:		this.maxResultAllowed,
    		linksNew:		this.linksNew
    	}));
    	return this;
    },
    executePaginatedRequirements : function(ev) {
    	ev.preventDefault();
    	var offset = eval(ev.target.attributes['page-id'].value);
    	this.currentOffset = (offset/ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED) + 1;
    	ZEPHYR.Traceability.Report.offset = offset;
    	ZEPHYR.Traceability.Report.globalDispatcher.trigger(ZEPHYR.Traceability.Report.GET_REQUIREMENTS,offset);
    },

    refreshREQView: function(ev) {
    	ev.preventDefault();
    	this.currentOffset = 1;
    	ZEPHYR.Traceability.Report.offset = this.currentOffset;
    	ZEPHYR.Traceability.Report.globalDispatcher.trigger(ZEPHYR.Traceability.Report.GET_REQUIREMENTS, 0);
    }
});

/**
 * Tabular format row view
 * Takes care of show/hide chart summary and statistics
 * 1. Requirement to defects
 * 2. Defects to requirement
 */
ZEPHYR.Traceability.Report.TracebilityTabularReportRowView = Backbone.View.extend({
	events: {
		'click .aui-iconfont-list-collapse'		: '_getExecutionsByTestOnAdd',
		'click .aui-iconfont-list-expand'		: '_hideExecutionsSummary',
		'click #exec-show-more'					: '_getExecutionsOnShowMore',
		'click #execution-defects-show-more, #execution-requirements-show-more'	: '_triggerExpandOnDefectsShowMore'
	},

	initialize: function(options) {
		this.executionsList = new ZEPHYR.Traceability.Report.ExecutionCollection({
			reportType: options.reportType
		});
		this.reportType = options.reportType; // Passed from parent view, either can be 'defectToRequirement' or 'requirementToDefect'
		this.offset = 0;
		this.maxRecords = 10;
		this.testOrDefectId = options.testId || options.defectId; // Can be either testId or defectId
		this.executionsList.bind('reset', this.render, this);
		this.appendView = false;
	},

	_getExecutionsOnShowMore: function(ev) {
		ev.preventDefault();

		this.offset = this.offset + 10;
		this.appendView = true;
		if(this.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
			this._getExecutionsByDefectIds();
    	} else if(this.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
    		this._getExecutionsByTest();
    	}
	},

	render: function() {
		if(this.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
			this.renderExecutionByDefectId();
    	} else if(this.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
    		this.renderExecutionByTestId();
    	}
		return this;
	},
  /*htmlDecode: function(value){
    return AJS.$('<div/>').html(value).text();
  },*/
	renderExecutionByDefectId: function() {
    this.executionsList.toJSON().forEach(function(execution){
      if(execution.execution.testCycle)
        execution.execution.testCycle = execution.execution.testCycle;
      if(execution.execution.folderName)
        execution.execution.folderName = execution.execution.folderName;
    });
		var executionsSummaryByDefectHTML = ZEPHYR.Templates.Project.Traceability.executionsSummaryByDefect({
			executions: this.executionsList.toJSON(),
			appendView: this.appendView,
			totalCount: this.executionsList.totalCount,
			currentCount: (this.offset + this.maxRecords),
			PERM_DENIED: this.executionsList.PERM_DENIED
		});
		if(this.appendView) {
			this.$el.find('#execution-row-tbody').append(executionsSummaryByDefectHTML);
		} else
			this.$el.find('#defects-executions-wrapper').html(executionsSummaryByDefectHTML).addClass('defects-executions-wrapper');
		// Attach show more UI
		if(this.executionsList.totalCount > (this.offset + this.maxRecords)) {
			this.$el.find('#show-more-wrapper').html(ZEPHYR.Templates.Project.Traceability.executionShowMoreView());
		} else
			this.$el.find('tfoot').remove();

		var maxHeight = 0,
			defectExecutionRowHeight = this.$el.find('#defects-executions-wrapper #execution-row-table').height(),
			defectRowHeight = this.$el.find('#defect-cell').height();
		this.$el.find(".report-cell").each(function(){
		   if (AJS.$(this).height() > maxHeight) { maxHeight = AJS.$(this).height(); }
		});
		this.$el.find(".report-cell").height(maxHeight);
		if(defectExecutionRowHeight < defectRowHeight) {
			this.$el.find('#defects-executions-wrapper #execution-row-table').height(defectRowHeight);
		} else {
			this.$el.find('#defect-cell .report-cell').height((defectExecutionRowHeight - ZEPHYR.Traceability.Report.getRowPadding()));
		}
		return this;
	},

	renderExecutionByTestId: function() {
    this.executionsList.toJSON().forEach(function(execution){
      if(execution.execution.testCycle)
        execution.execution.testCycle = execution.execution.testCycle;
      if(execution.execution.folderName)
        execution.execution.folderName = execution.execution.folderName;
    });
		var currentCount = (this.offset + this.maxRecords),
			requirementId = this.$el.attr('data-reqId'),
			executionRowHTML = ZEPHYR.Templates.Project.Traceability.executionsSummary({
				executions: this.executionsList.toJSON(),
				appendView: this.appendView,
				totalCount: this.executionsList.totalCount,
				currentCount: currentCount,
				PERM_DENIED: this.executionsList.PERM_DENIED
			});
		if(this.appendView) {
			this.$el.find('#execution-row-tbody').append(executionRowHTML);
		} else
			this.$el.find('#tests-executions-wrapper').html(executionRowHTML).addClass('tests-executions-wrapper');
		// Attach show more UI
		if(this.executionsList.totalCount > currentCount) {
			this.$el.find('#show-more-wrapper').html(ZEPHYR.Templates.Project.Traceability.executionShowMoreView());
		} else
			this.$el.find('tfoot').remove();

		var elHeight = this.$el.find('#tests-executions-wrapper').height(),
			testExecutionRowHeight = this.$el.find('#tests-executions-wrapper #execution-row-table').height(),
			testRowHeight = this.$el.find('.report-test-cell').height();
		if(testExecutionRowHeight < testRowHeight) {
			this.$el.find('#tests-executions-wrapper #execution-row-table').height(testRowHeight);
			this.$el.find('.report-test-cell .report-cell').height(testRowHeight - ZEPHYR.Traceability.Report.getRowPadding());
		} else {
			this.$el.find('.report-test-cell .report-cell').height((elHeight - ZEPHYR.Traceability.Report.getRowPadding())); // Table height - (padding + border)
		}
		this.updateRequirementRowHeight(requirementId);
		return this;
	},
	updateRequirementRowHeight: function(requirementId) {
		var requirementRow = AJS.$('#requirement-tr-' + requirementId);
			$elHeightLeft = requirementRow.find('.report-cell-req').height(),
			totalHeight = 0;
		AJS.$('#rtod-tr-container-'+ requirementId).find(".report-tr-right").each(function() {
			totalHeight += AJS.$(this).height();
		});
		if(totalHeight > $elHeightLeft) {
			requirementRow.find('.report-cell-req').height((totalHeight - ZEPHYR.Traceability.Report.getRowPadding())); // height - (padding + border)
		} else {
			this.$el.find('.report-cell').height($elHeightLeft);
		}
	},
	_hideExecutionsSummary: function(ev) {
		ev.preventDefault();

		if(this.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
			var defectId = AJS.$(ev.target).attr('data-defectId');
			this._hideDefectSummary(defectId);
    	} else if(this.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
			var testId = AJS.$(ev.target).attr('data-testId');
    		this._hideTestSummary(testId);
    	}
		AJS.$(ev.target).removeClass('aui-iconfont-list-expand').addClass('aui-iconfont-list-collapse');
		this.appendView = false;
		this.offset = 0;
	},

	_triggerExpandOnDefectsShowMore: function(ev) {
		ev.preventDefault();

		this.$el.find('.aui-iconfont-list-collapse').trigger('click');
	},

	_hideDefectSummary: function(defectId) {
		// Attach executions statistics view
		var defectExecutionsStatisticsHTML = ZEPHYR.Templates.Project.Traceability.defectExecutionsWrapperView({
			defect: this.model.toJSON()
		});
		this.$el.find('#defects-executions-wrapper').html(defectExecutionsStatisticsHTML).removeClass('defects-executions-wrapper');
		this.$el.find('#defect-cell .report-cell').css('height', ''); // Resize the defect column
		var maxHeight = 0;
		this.$el.find(".report-cell").each(function(){
		   if (AJS.$(this).height() > maxHeight) { maxHeight = AJS.$(this).height(); }
		});
		this.$el.find(".report-cell").height(maxHeight);
	},

	_hideTestSummary: function(testId) {
		var test = [],
			requirementId = this.$el.attr('data-reqId');
		if(this.model) {
			var tests = this.model.get('tests');
			test = _.filter(tests, function(test){ return test.test.id == parseInt(testId); });
			if(test.length == 0)
				return false;
			test = test[0];
		}
		// Attach executions statistics view
		var testExecutionsStatisticsHTML = ZEPHYR.Templates.Project.Traceability.testExecutionsWrapperView({
			test: test
		});
		this.$el.find('#tests-executions-wrapper').html(testExecutionsStatisticsHTML).removeClass('tests-executions-wrapper');
		this.$el.find('.report-test-cell .report-cell').css('height', ''); // Resize the defect column
		AJS.$('#requirement-tr-' + requirementId).find('.report-requirement-cell .report-cell-req').css('height', '');
		var maxHeight = 0;
		this.$el.find(".report-cell").each(function(){
		   if (AJS.$(this).height() > maxHeight) { maxHeight = AJS.$(this).height(); }
		});
		this.$el.find(".report-cell").height(maxHeight);
		this.updateRequirementRowHeight(requirementId);
	},

	_getExecutionsByTestOnAdd: function(ev) {
		AJS.$(ev.target).removeClass('aui-iconfont-list-collapse').addClass('aui-iconfont-list-expand');
		if(this.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
			this._getExecutionsByDefectIds();
    	} else if(this.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
    		this._getExecutionsByTest();
    	}
	},

	_fetchExecutions: function(dataParams) {
		ZEPHYR.Loading.showLoadingIndicator();
		this.executionsList.fetch({
			reset: true,
			data: dataParams,
			contentType: "application/json",
			success: function() {
				ZEPHYR.Loading.hideLoadingIndicator();
			},
			error: function(tests, response) {
				ZEPHYR.Loading.hideLoadingIndicator();
				var responseText = JSON.parse(response.responseText);
				showErrorMessage(responseText.message);
			}
		});
	},

	_getExecutionsByDefectIds: function() {
		var dataParams = {
				defectIdOrKey: this.testOrDefectId,
				offset: this.offset,
				maxRecords: this.maxRecords
			};
		this._fetchExecutions(dataParams);
	},

	_getExecutionsByTest: function() {
		var dataParams = {
				testIdOrKey: this.testOrDefectId,
				offset: this.offset,
				maxRecords: this.maxRecords
			};
		this._fetchExecutions(dataParams);
	}
});

/**
 * Tabular Report Test Show more View
 */
ZEPHYR.Traceability.Report.TracebilityTestShowMoreView = Backbone.View.extend({
	events: {
		'click #test-show-more'					: '_displayTestsOnShowMore'
	},

	initialize: function(options) {
		this.offset = 0;
	},

	_displayTestsOnShowMore: function(ev) {
		ev.preventDefault();
		this.offset += ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED;
		var testList = this.model.get('tests'),
			minIndex = this.offset,
			maxIndex = this.offset + ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED,
			requirementId = this.model.get('requirement').id,
			instance = this;

		var tests = testList.slice(minIndex, maxIndex);
		if(tests.length){
			var testViewHTML = ZEPHYR.Templates.Project.Traceability.requirementTestView({
				tests: tests,
				requirementId: requirementId
			});
			AJS.$(testViewHTML).insertBefore(this.$el);
			ZEPHYR.Traceability.Report.globalDispatcher.trigger(ZEPHYR.Traceability.Report.REQUIREMENT_TEST_VIEW_ATTACH, function() {
				if(testList.length <= maxIndex)
					instance.remove();
			});
		}
	}
});

/**
 * Tabular Report Chart View
 * 1. Requirement to Defect
 */
ZEPHYR.Traceability.Report.TracebilityTabularRtoDReportView = Backbone.View.extend({
	initialize: function() {
		this.model.bind("reset", this.render,this);
		this.tracebilityTabularReportRows = [];
		this.offset = 0;
		ZEPHYR.Traceability.Report.globalDispatcher.off(ZEPHYR.Traceability.Report.REQUIREMENT_TEST_VIEW_ATTACH);
		ZEPHYR.Traceability.Report.globalDispatcher.on(ZEPHYR.Traceability.Report.REQUIREMENT_TEST_VIEW_ATTACH, this._attachReqTestRowViews, this);
	},

	_attachReqTestRowViews: function(callback) {
		var instance = this;

		if(callback)
			callback();
		_.each(this.$el.find('#requirement-defect-table .report-tbody .report-tr-right.test-on-show-more-row'), function(requirementRow) {
			var requirementId = AJS.$(requirementRow).attr('data-reqId');
			var showMore = AJS.$(requirementRow).attr('data-testShowMore');
			var model = _.filter(instance.model.models, function(req){ return req.get('requirement').id == requirementId; });

			if(model.length > 0) {
				var tracebilityTabularReportRowView = new ZEPHYR.Traceability.Report.TracebilityTabularReportRowView({
					el: requirementRow,
					testId: AJS.$(requirementRow).attr('data-testId'),
					model: model[0],
					reportType: ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT
				});
				instance.tracebilityTabularReportRows.push(tracebilityTabularReportRowView);
				instance._updateRowHeight(requirementRow, requirementId);
			}
			AJS.$(requirementRow).removeClass('test-on-show-more-row');
		});

		this.updateRequirementHeight();
	},

	_updateRowHeight: function(requirementRow) {
		// Set the height of the requirement container
		var maxHeight = 0;
		AJS.$(requirementRow).find(".report-cell").each(function() {
		   if (AJS.$(this).height() > maxHeight) { maxHeight = AJS.$(this).height(); }
		});
		AJS.$(requirementRow).find(".report-cell").height(maxHeight);
	},
	updateRequirementHeight: function() {
		_.each(this.$el.find('#requirement-defect-table .report-tbody .rtod-report-tr-container'), function(requirementRow) {
			var requirementId = AJS.$(requirementRow).attr('data-reqId'),
				totalHeight = 0,
				$elHeightLeft = AJS.$(requirementRow).find('.report-tr-left .report-cell-req').height();
			AJS.$(requirementRow).find(".report-tr-right").each(function() {
				totalHeight += AJS.$(this).height();
			});
			if(totalHeight > $elHeightLeft) {
				AJS.$(requirementRow).find('.report-tr-left .report-cell-req').height((totalHeight - ZEPHYR.Traceability.Report.getRowPadding())); // height - (padding + border)
			} else {
				AJS.$(requirementRow).find('.report-tr-right .report-cell').height($elHeightLeft);
			}
		});
	},
	render: function() {
		var instance = this;
		_.each(this.tracebilityTabularReportRows, function(tracebilityTabularReportRow) {
			tracebilityTabularReportRow.remove();
		});

		var requirementToDefectsViewHTML = ZEPHYR.Templates.Project.Traceability.requirementToDefectsView({
			testsByRequirement: this.model.toJSON(),
			maxTestAllowed: ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED
		});
		this.$el.html(requirementToDefectsViewHTML);
		this.tracebilityTabularReportRows = [];
		_.each(this.$el.find('#requirement-defect-table .report-tbody .report-tr-right'), function(requirementRow) {
			var requirementId = AJS.$(requirementRow).attr('data-reqId');
			var showMore = AJS.$(requirementRow).attr('data-testShowMore');
			var model = _.filter(instance.model.models, function(req){ return req.get('requirement').id == requirementId; });

			if(model.length > 0) {
				if(showMore) {
					var tracebilityTabularReportRowView = new ZEPHYR.Traceability.Report.TracebilityTestShowMoreView({
						el: requirementRow,
						model: model[0]
					});
				} else {
					var tracebilityTabularReportRowView = new ZEPHYR.Traceability.Report.TracebilityTabularReportRowView({
						el: requirementRow,
						testId: AJS.$(requirementRow).attr('data-testId'),
						model: model[0],
						reportType: ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT
					});
				}
				instance.tracebilityTabularReportRows.push(tracebilityTabularReportRowView);
				instance._updateRowHeight(requirementRow, requirementId);
			}
		});

		this.updateRequirementHeight();

		return this;
	}
});

/**
 * Tabular Report Chart View
 * 1. Defect to Requirement
 */
ZEPHYR.Traceability.Report.TracebilityTabularDtoRReportView = Backbone.View.extend({
	initialize: function() {
		this.model.bind("reset", this.render,this);
		this.tracebilityTabularReportRows = [];
	},

	_updateRowHeight: function(requirementRow) {
		// Set the height of the requirement container
		var maxHeight = 0;
		AJS.$(requirementRow).find(".report-cell").each(function(){
		   if (AJS.$(this).height() > maxHeight) { maxHeight = AJS.$(this).height(); }
		});
		AJS.$(requirementRow).find(".report-cell").height(maxHeight);
	},

	render: function() {
		var instance = this;

		_.each(this.tracebilityTabularReportRows, function(tracebilityTabularReportRow) {
			tracebilityTabularReportRow.remove();
		});
		var defectToRequirementsViewHTML = ZEPHYR.Templates.Project.Traceability.defectToRequirementsView({
			defectStatistics: this.model.toJSON()
		});
		this.$el.html(defectToRequirementsViewHTML);
		this.tracebilityTabularReportRows = [];
		_.each(this.$el.find('#defect-requirement-table .report-tbody .report-tr'), function(requirementRow) {
			var defectId = AJS.$(requirementRow).attr('data-defectId');
			var model = _.filter(instance.model.models, function(def){ return def.get('defect').id == defectId; });
			var tracebilityTabularReportRowView = new ZEPHYR.Traceability.Report.TracebilityTabularReportRowView({
				el: requirementRow,
				defectId: AJS.$(requirementRow).attr('data-defectId'),
				model: model[0],
				reportType: ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT
			});
			instance.tracebilityTabularReportRows.push(tracebilityTabularReportRowView);

			instance._updateRowHeight(requirementRow);
		});
		return this;
	}
});


ZEPHYR.Traceability.Report.TracebilityReportView = Backbone.View.extend({
	initialize: function() {
		this.offset = ZEPHYR.Traceability.Report.offset;
		// Can be requirement or defect keys
    var maxAllowed = this.offset + ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED;
		this.requirementIdOrKeyList = this._getRequirementIdOrKeyList(this.offset, maxAllowed);
		this.render();

		ZEPHYR.Traceability.Report.globalDispatcher.off(ZEPHYR.Traceability.Report.GET_REQUIREMENTS);
		ZEPHYR.Traceability.Report.globalDispatcher.on(ZEPHYR.Traceability.Report.GET_REQUIREMENTS, this._getRequirementsBasedOnOffset, this);
	},

	_getRequirementsBasedOnOffset: function(offset) {
		var maxAllowed = offset + ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED;
		this.requirementIdOrKeyList = this._getRequirementIdOrKeyList(offset, maxAllowed);
		if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
			this._fetchDefectStatistics();
		} else if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
			this._fetchTests();
		}
		this._updateReportPageURL();
	},

	_getRequirementIdOrKeyList: function(startIndex, maxAllowed) {
		var requirementIdorKeys = [];

		requirementIdorKeys = ZEPHYR.Traceability.selectedIssues.slice(startIndex, maxAllowed);

		return requirementIdorKeys.toString();
	},

	_getToggleReqDef: function() {
		if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
			return {
				id: ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT,
				label: AJS.I18n.getText('zephyr-je.pdb.traceability.report.defects.to.requirements.label')
			};
		} else if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
			return {
				id: ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT,
				label: AJS.I18n.getText('zephyr-je.pdb.traceability.report.requirements.to.defects.label')
			};
		}
	},

	_getReportBreadCrumbs: function() {
		var reportBreadCrumbs = [];
		if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
			reportBreadCrumbs = this._getDtoRReportBreadCrumbs();
		} else if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
			reportBreadCrumbs = this._getRtoDReportBreadCrumbs();
		}

		return reportBreadCrumbs;
	},

	_getDtoRReportBreadCrumbs: function() {
		var reportBreadCrumbs = [];

		reportBreadCrumbs.push({label: AJS.I18n.getText('execute.test.defect.label')}); // Defects
		reportBreadCrumbs.push({label: AJS.I18n.getText('enav.schedule.entity.name')});// Executions
		reportBreadCrumbs.push({label: AJS.I18n.getText('com.thed.zephyr.je.topnav.tests.label')}); // Tests
		reportBreadCrumbs.push({label: AJS.I18n.getText('zephyr-je.pdb.traceability.report.requirement.label')}); // Requirements
		return reportBreadCrumbs;
	},

	_getRtoDReportBreadCrumbs: function() {
		var reportBreadCrumbs = [];

		reportBreadCrumbs.push({label: AJS.I18n.getText('zephyr-je.pdb.traceability.report.requirement.label')}); // Requirement
		reportBreadCrumbs.push({label: AJS.I18n.getText('com.thed.zephyr.je.topnav.tests.label')}); // Tests
		reportBreadCrumbs.push({label: AJS.I18n.getText('enav.schedule.entity.name')}); // Executions
		reportBreadCrumbs.push({label: AJS.I18n.getText('execute.test.defect.label')}); // Defects
		return reportBreadCrumbs;
	},

	_attachEvents: function() {
		var instance = this;

		this.$el.find('#report-back').unbind('click');
		this.$el.find('#report-back').bind('click', function(ev) {
			ev.preventDefault();
			var isDefToReq = (ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT)? 'true': 'false';

			sessionStorage.setItem('zephyr-je-traceability-isReqRestore', "true");
			sessionStorage.setItem('zephyr-je-traceability-isDefToReq', isDefToReq);
			window.location.href = ZEPHYR.Traceability.Report.requirementPageURL;
			ZEPHYR.Traceability.Navigation.renderTraceabilityView();
		});
		this.$el.find('#requirementToDefect').unbind('click');
		this.$el.find('#requirementToDefect').bind('click', function(ev) {
			ev.preventDefault();

			instance.requirementIdOrKeyList = instance._getRequirementIdOrKeyList(0, ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED);
			ZEPHYR.Traceability.Report.reportType = ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT;
			sessionStorage.setItem('zephyr-je-traceability-report-type', ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT);
			instance.renderRequirementsToDefects();
		});

		this.$el.find('#defectToRequirement').unbind('click');
		this.$el.find('#defectToRequirement').bind('click', function(ev) {
			ev.preventDefault();

			instance.requirementIdOrKeyList = instance._getRequirementIdOrKeyList(0, ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED);
			ZEPHYR.Traceability.Report.reportType = ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT;
			sessionStorage.setItem('zephyr-je-traceability-report-type', ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT);
			instance.renderDefectsToRequirements();
		});

		this.$el.find('.export-report-html-li').unbind('click');
		this.$el.find('.export-report-html-li').bind('click', function(ev) {
			var dataParams = {
					exportType: 'html'
				},
				label = AJS.I18n.getText('enav.export.label') + ' ' + AJS.I18n.getText('zephyr.traceability.report.label') + ': ' +
						AJS.I18n.getText('enav.export.html.schedule.label');
			if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
				dataParams.defectIdList = ZEPHYR.Traceability.selectedIssues;
			} else if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
				dataParams.requirementIdList = ZEPHYR.Traceability.selectedIssues;
			}
			instance.exportReport(dataParams, label);
		});
		this.$el.find('.export-report-excel-li').unbind('click');
		this.$el.find('.export-report-excel-li').bind('click', function(ev) {
			var dataParams = {
					exportType: 'excel'
				},
				label = AJS.I18n.getText('enav.export.label') + ' ' + AJS.I18n.getText('zephyr.traceability.report.label') + ': ' +
						AJS.I18n.getText('enav.export.excel.schedule.label');
			if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
				dataParams.defectIdList = ZEPHYR.Traceability.selectedIssues;
			} else if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
				dataParams.requirementIdList = ZEPHYR.Traceability.selectedIssues;
			}
			instance.exportReport(dataParams, label);
		});

		/**
		 * Check if lower versions of JIRA support the aui right arro icons,
		 * if not add pseudo content element
		 */
		if(window.getComputedStyle) {
			var arrowRightContent = window.getComputedStyle(
				document.querySelector('.aui-iconfont-devtools-arrow-right.traceability-report-arrow-right'), '::before'
			).getPropertyValue('content');

			if(arrowRightContent == '' || arrowRightContent == 'none') {
				AJS.$('.aui-iconfont-devtools-arrow-right.traceability-report-arrow-right').addClass('traceability-arrow-right')
					.css('display', 'inline');
				if(!ZEPHYR.isIE(navigator.userAgent))
					AJS.$('.aui-iconfont-devtools-arrow-right.traceability-report-arrow-right').css('top', '-8px');
			}
		}
	},

	exportReport: function(dataParams, label) {
		var instance = this;
		dataParams.versionId = ZEPHYR.Traceability.fixVersionId;
		ZEPHYR.Loading.showLoadingIndicator();
		var exportXHR = AJS.$.ajax({
    		url: getRestURL() + '/traceability/export',
    		type : 'POST',
    		contentType: "application/json",
    		data: JSON.stringify(dataParams),
    		timeout: 0, // To remove timeout
    		success: function(response) {
    			ZEPHYR.Loading.hideLoadingIndicator();
    			instance._downloadExportedFile(response);
            },
			error: function (jqxhr) {
				ZEPHYR.Loading.hideLoadingIndicator();
				if (jqxhr.status === 403) { // this is to capture session timeout/ login expired error
					AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
						title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
						body: jqxhr.responseText,
						shadowed: true
					});
				}
				else if (jqxhr.status !== 401)
					showErrorMessage(jqxhr.responseText);
			}
    	});
		ZEPHYR.Utility.displayConfirmationOnTimeout(exportXHR);
	},

	_downloadExportedFile: function(response) {
		AJS.$('#traceability-report-export-iframe').remove(); // Clean up if previous iframes are present
		var iFrame = document.createElement("iframe");
		iFrame.id = 'traceability-report-export-iframe';
        iFrame.src = response.url;
        iFrame.style.display = "none";
        document.body.appendChild(iFrame);
	},

	_fetchTests: function() {
		ZEPHYR.Loading.showLoadingIndicator();
		this.testCollection.fetch({
			data: {
				requirementIdOrKeyList: this.requirementIdOrKeyList
			},
			reset: true,
			contentType: "application/json",
			success: function() {
				ZEPHYR.Loading.hideLoadingIndicator();
			},
			error: function(tests, response) {
				ZEPHYR.Loading.hideLoadingIndicator();
				var responseText = JSON.parse(response.responseText);
				showErrorMessage(responseText.message);
			}
		});
	},

	_fetchDefectStatistics: function() {
		ZEPHYR.Loading.showLoadingIndicator();
		this.defectStatistics.fetch({
			data: {
				defectIdOrKeyList: this.requirementIdOrKeyList
			},
			reset: true,
			contentType: "application/json",
			success: function() {
				ZEPHYR.Loading.hideLoadingIndicator();
			},
			error: function(defectStats, response) {
				ZEPHYR.Loading.hideLoadingIndicator();
				var responseText = JSON.parse(response.responseText);
				showErrorMessage(responseText.message);
			}
		});
	},

	// Return the report HTML
	_getReportHeaderHTML: function() {
		this.reportBreadcrumbs = this._getReportBreadCrumbs();
		this.toggleReqDef = this._getToggleReqDef();
		return ZEPHYR.Templates.Project.Traceability.attachReportHeader({
			reportBreadcrumbs: this.reportBreadcrumbs,
			toggleReqDef: this.toggleReqDef
		});
	},

	renderDefectsToRequirements: function() {
		// Attach defect -> requirement view
		if(typeof this.traceabilityDtoRTabularReport == 'undefined') {
			this.defectStatistics = new ZEPHYR.Traceability.Report.DefectStatsCollection();
			this.traceabilityDtoRTabularReport = new ZEPHYR.Traceability.Report.TracebilityTabularDtoRReportView({
				el: '#report-table-container',
				model: this.defectStatistics
			});

			// Attach the pagination
			this.requirementPaginationView = new ZEPHYR.Traceability.Report.RequirementPaginationView({
				el: '#report-pagination-container',
				model: this.defectStatistics
			});
		}
		var traceabilityHeaderHTML = this._getReportHeaderHTML();
		this.$el.find('#traceability-header').html(traceabilityHeaderHTML);
		var offset = (ZEPHYR.Traceability.Report.offset / ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED) + 1;
		ZEPHYR.Traceability.Report.globalDispatcher.trigger(ZEPHYR.Traceability.Report.PAGINATION_COUNT_UPDATE, offset);
		this._fetchDefectStatistics();
		this._attachEvents();
		this._updateReportPageURL();
	},

	_updateReportPageURL: function() {
		ZEPHYR.Traceability.Report.reportPageURL = ZEPHYR.Traceability.Report.requirementPageURL + this._getReportPageURL();

		window.location.href = ZEPHYR.Traceability.Report.reportPageURL;
	},

	_getReportPageURL: function() {
		var reportLoc = 'type=report&offset=' + ZEPHYR.Traceability.Report.offset,
			_location = window.location.hash;

		if(AJS.$('#isProjectCentricViewEnabled').val() || _location.indexOf('#selectedTab=') > -1)
			reportLoc = '&' + reportLoc;
		return reportLoc;
	},

	renderRequirementsToDefects: function() {
		// Attach the requirement -> defect view
		if(typeof this.traceabilityRtoDTabularReport == 'undefined') {
			this.testCollection = new ZEPHYR.Traceability.Report.TestCollection();
			this.traceabilityRtoDTabularReport = new ZEPHYR.Traceability.Report.TracebilityTabularRtoDReportView({
				el: '#report-table-container',
				model: this.testCollection
			});
			// Attach the pagination
			this.requirementPaginationView = new ZEPHYR.Traceability.Report.RequirementPaginationView({
				el: '#report-pagination-container',
				model: this.testCollection
			});
		}
		var traceabilityHeaderHTML = this._getReportHeaderHTML();
		this.$el.find('#traceability-header').html(traceabilityHeaderHTML);
		var offset = (ZEPHYR.Traceability.Report.offset / ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED) + 1;
		ZEPHYR.Traceability.Report.globalDispatcher.trigger(ZEPHYR.Traceability.Report.PAGINATION_COUNT_UPDATE, offset);
		this._fetchTests();
		this._attachEvents();
		this._updateReportPageURL();
	},

	render: function() {
		var traceabilityReportViewHTML = ZEPHYR.Templates.Project.Traceability.traceabilityReportView({
			reportBreadcrumbs: this.reportBreadcrumbs,
			toggleReqDef: this.toggleReqDef
		});
		this.$el.html(traceabilityReportViewHTML);

		if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.DEFECT_TO_REQUIREMENT) {
			this.renderDefectsToRequirements();
		} else if(ZEPHYR.Traceability.Report.reportType == ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT) {
			this.renderRequirementsToDefects();
		}
		return this;
	}
});

ZEPHYR.Traceability.Report.getRowPadding = function() {
	var _padding = 15.3; // padding + border

	return _padding;
}

ZEPHYR.Traceability.Report.getRequirementPageURL = function() {
	if(AJS.$('#isProjectCentricViewEnabled').val()) {
		reportLoc = '#traceability-tab';
	} else {
		var _location = window.location.hash;
		if(_location.indexOf('#selectedTab=') > -1)
			reportLoc = '#' + ZEPHYR.Traceability.nonProjectCentricViewTabURL;
		else
			reportLoc = '#';
	}

	ZEPHYR.Traceability.Report.requirementPageURL = reportLoc;
}

// Fetch the test level status
ZEPHYR.Traceability.Report.getTestLevelStatus = function() {
	ZEPHYR.Loading.showLoadingIndicator();
	AJS.$.ajax({
        url: getRestURL() + "/util/testExecutionStatus",
        type : "get",
        success : function(response){
        	ZEPHYR.Traceability.Report.executionStatus = response;
            ZEPHYR.Traceability.Report.traceabilityView = new ZEPHYR.Traceability.Report.TracebilityReportView({
				el: '#traceability-container'
			});
        },
        error: function() {
        	ZEPHYR.Loading.hideLoadingIndicator();
        }
    });
}

// Fetch JIRA status
ZEPHYR.Traceability.Report.getJIRAStatus = function() {
	ZEPHYR.Loading.showLoadingIndicator();
	jQuery.ajax({
        url: contextPath + "/rest/api/2/status",
        success: function(issueStatus) {
            ZEPHYR.Traceability.Report.issueStatus = issueStatus;
            ZEPHYR.Traceability.Report.getTestLevelStatus();
        },
        error: function() {
        	ZEPHYR.Loading.hideLoadingIndicator();
        }
    });
}

ZEPHYR.Traceability.Report.Status = {
    // {call ZEPHYR.Traceability.Report.Status.getExecutionStatus data="[$name, $id]" /}
    getExecutionStatus: function(executionStatus, output) {
        if(ZEPHYR.Traceability.Report.executionStatus && ZEPHYR.Traceability.Report.executionStatus.length > 0) {
        	var statusJSON = null;
        	if(!isNaN(executionStatus[1]))
        		executionStatus[1] = parseInt(executionStatus[1]); // Convert string to number
        	if(_.findWhere) {
        		statusJSON = _.findWhere(ZEPHYR.Traceability.Report.executionStatus, {id: executionStatus[1]});
        	} else {
        		_.each(ZEPHYR.Traceability.Report.executionStatus, function(status) {
        			if(status.id == executionStatus[1])
        				statusJSON = status;
        		});
        	}
        	if(statusJSON) {
        		return appendSoyOutputOnCall('<span class="aui-lozenge aui-lozenge-subtle status-right" style="border-color: ' + statusJSON.color + '">' + executionStatus[0] + '</span>', output);
        	}
        }
        return appendSoyOutputOnCall('<span class="aui-lozenge aui-lozenge-subtle status-right">' + executionStatus[0] + '</span>', output);
    },
    // {call ZEPHYR.Traceability.Report.Status.getIssueStatus data="[$name, $id]" /}
    getIssueStatus: function(issueStatus, output) {
        if(ZEPHYR.Traceability.Report.issueStatus && ZEPHYR.Traceability.Report.issueStatus.length > 0) {
        	var statusJSON = null;
        	if(_.findWhere) {
        		statusJSON = _.findWhere(ZEPHYR.Traceability.Report.issueStatus, {id: issueStatus[1].toString()});
        	} else {
        		_.each(ZEPHYR.Traceability.Report.issueStatus, function(status) {
        			if(status.id == issueStatus[1])
        				statusJSON = status;
        		});
        	}
        	if(statusJSON) {
        		var _colorName = (statusJSON.statusCategory && statusJSON.statusCategory.colorName) ? statusJSON.statusCategory.colorName : '#333333';
        		return appendSoyOutputOnCall('<span class="aui-lozenge aui-lozenge-subtle zfj-aui-lozenge-status jira-issue-status-lozenge-' + _colorName + '">' + issueStatus[0] + '</span>', output);
        		//return '<img class="issue-status-img"  width="16" height="16" src="' + statusJSON.iconUrl + '" title="' + statusJSON.name + ' - ' + statusJSON.description + '" alt="' + statusJSON.name + '">';
        	}
        }
        return appendSoyOutputOnCall('<span class="aui-lozenge aui-lozenge-subtle zfj-aui-lozenge-status">' + issueStatus[0] + '</span>', output);
    }
};

ZEPHYR.Traceability.Report.setOffset = function(offset) {
	var _offset = 0;
	if(offset <= ZEPHYR.Traceability.selectedIssues.length && ((offset % ZEPHYR.Traceability.Report.REQ_MAX_RESULT_ALLOWED) == 0)) {
		_offset = offset;
	}
	ZEPHYR.Traceability.Report.offset = _offset;
}

ZEPHYR.Traceability.Report.globalDispatcher = _.extend({}, Backbone.Events);
ZEPHYR.Traceability.Report.init = function(reportType, offset) {
	ZEPHYR.Traceability.Report.reportType = reportType || ZEPHYR.Traceability.Report.REQUIREMENT_TO_DEFECT; // Default report is requirement to defect

	if(!ZEPHYR.Traceability.fixVersionId)
		ZEPHYR.Traceability.fixVersionId = 0;
	ZEPHYR.Traceability.getSelectedIssueIdsFromSession(); // get the selected issue Ids from requirement page
	ZEPHYR.Traceability.Report.setOffset(offset);
	ZEPHYR.Traceability.Report.getRequirementPageURL(); // get the requirement page URL
	ZEPHYR.Traceability.Report.getJIRAStatus();
}
