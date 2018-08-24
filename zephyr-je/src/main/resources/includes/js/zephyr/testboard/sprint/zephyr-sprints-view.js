/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }

// Sprints
ZEPHYR.Agile.TestBoard.Sprint = Backbone.Model.extend({
	initialize: function() {
		/**
		 * Nested collection
		 * Sprint view: Cycle collection
		 * Issues view: Issue collection
		 */
		if(ZEPHYR.Agile.TestBoard.data.page == ZEPHYR.Agile.TestBoard.page.sprint) {
			this.cycles = new ZEPHYR.Agile.TestBoard.CycleCollection();
		} else if(ZEPHYR.Agile.TestBoard.data.page == ZEPHYR.Agile.TestBoard.page.issues) {
			this.issues = new ZEPHYR.Agile.TestBoard.IssueCollection();
		}
	},
	parse: function(resp) {
		return resp;
	}
});
ZEPHYR.Agile.TestBoard.SprintCollection = Backbone.Collection.extend({
    model: ZEPHYR.Agile.TestBoard.Sprint,
    url: function() {
    	return  contextPath + "/rest/agile/1.0/board/" + ZEPHYR.Agile.TestBoard.data.agileBoardId + "/sprint";
    },
    parse: function(resp, xhr) {
    	return resp.values;
    }
});

/**
 * - SprintView - show Sprint details with list of Cycles
 * - Ability to add cycle, link test cycles. 
 * - SprintCycleDetailView
 */
ZEPHYR.Agile.TestBoard.SprintsView = Backbone.View.extend({
	events: {
		'click .zephyr-tb-cycle-content': 'displayCycleDetails'
	},
	initialize: function() {
		this.model.bind('reset', this.render, this);
		this.sprintRows = [];
	},	
	attachSprintView: function() {
		var instance = this;
		// TODO: cleanup previous views
		// Attach Sprint View
		_.each(this.$el.find('.zephyr-tb-sprints-module'), function(sprintEl) {
			var sprintId = AJS.$(sprintEl).data('sprint-id');
			var sprint = instance.model.get(sprintId);
			if(sprint) {
				var sprintView = new ZEPHYR.Agile.TestBoard.SprintView({
					el: sprintEl,
					model: sprint
				});
				instance.sprintRows.push(sprintView);
			}
		});
		this.executions = new ZEPHYR.Agile.TestBoard.ExecutionCollection();
		// Attach SprintCycleDetailView
		this.sprintCycleDetailView = new ZEPHYR.Agile.TestBoard.SprintCycleDetailView({
			el: '#zephyr-tb-detail-column',
			model: this.executions
		});
		// Fix for ZFJ-1743: Displaying the defects on left
		ZEPHYR.defectHover.INLINE_DIALOG_OPTIONS.gravity =  'w';
	},
	displayCycleDetails: function(ev) {
		ev.stopImmediatePropagation();
		var targetElement = AJS.$(ev.target).closest('.zephyr-tb-cycle-content') || AJS.$(ev.target);
			cycleId = targetElement.data('cycle-id'),
			cycleName = targetElement.attr('data-cycle-name'),
			versionName = targetElement.attr('data-version-name'),
			sprintId = targetElement.data('sprint-id'),
			projectKey = targetElement.attr('data-project-key'),
			folderId = targetElement.attr('data-folder-id'),
			folderName = targetElement.attr('data-folder-name');
			
		AJS.$('.zephyr-tb-cycle-content').removeClass('zephyr-tb-selected');
		targetElement.addClass('zephyr-tb-selected');
		this.sprintCycleDetailView.cycleId = cycleId;
		this.sprintCycleDetailView.cycleName = cycleName;
		this.sprintCycleDetailView.versionName = versionName;
		this.sprintCycleDetailView.sprintId = sprintId;
		this.sprintCycleDetailView.projectKey = projectKey;
		this.sprintCycleDetailView.folderId = folderId;
		this.sprintCycleDetailView.folderName = folderName;
		
		// On page refresh this is false, so that the selectedStatus value is not cleared
		if(this.sprintCycleDetailView.clearStatus)
			ZEPHYR.Agile.TestBoard.data.selectedStatus = null;
		this.sprintCycleDetailView.render();
		/** Update URL **/
    	ZEPHYR.Agile.TestBoard.data['selectedSprint'] = sprintId;
    	ZEPHYR.Agile.TestBoard.data['selectedCycle'] = cycleId;
    	ZEPHYR.Agile.TestBoard.data['view'] = ZEPHYR.Agile.TestBoard.viewDetail;
    	ZEPHYR.Agile.TestBoard.data['isDetailView'] = true;
		ZEPHYR.Agile.TestBoard.Router.URL.setParams();
		ZEPHYR.Agile.TestBoard.Router.tbRouter.navigate(ZEPHYR.Agile.TestBoard.Router.URL.getURL(), {trigger: false}); 
	},
	render: function() {
		var sprints = this.model.toJSON();
		var sprintsHTML = ZEPHYR.Templates.TestBoard.Sprint.sprintsHTML({
			sprints: sprints
		});
		this.$el.html(sprintsHTML);
		this.attachSprintView();
		ZEPHYR.Agile.TestBoard.Loading.hideLoadingIndicator();
		return this;
	}
});