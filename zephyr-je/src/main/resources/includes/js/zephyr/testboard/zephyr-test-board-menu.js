/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }

// Projects
ZEPHYR.Agile.TestBoard.Project = Backbone.Model.extend();
ZEPHYR.Agile.TestBoard.ProjectCollection = Backbone.Collection.extend({
    model: ZEPHYR.Agile.TestBoard.Project
});

// Versions
ZEPHYR.Agile.TestBoard.Version = Backbone.Model.extend();
ZEPHYR.Agile.TestBoard.VersionCollection = Backbone.Collection.extend({
    model: ZEPHYR.Agile.TestBoard.Version,
    url: function() {
    	return getRestURL() + "/util/versionBoard-list?projectId=" + ZEPHYR.Agile.TestBoard.data.projectId;
    },
    parse: function(resp, xhr) {
    	return resp;
    }
});

/**
 * Display versions
 */
ZEPHYR.Agile.TestBoard.VersionsView = Backbone.View.extend({
	events: {
		'click .zephyr-tb-version-close': 'hideVersionNav',
		'click .zephyr-tb-classification-item': 'selectVersion'
	},
	initialize: function(options) {
		// this.model.bind('reset', this.render, this);
		this.showProjectName = options.showProjectName;
	},
	selectVersion: function(ev) {
		var targetEl = AJS.$(ev.target).closest('.zephyr-tb-classification-item'),
			versionIds = targetEl.data('version-id'),
			projectId = targetEl.data('project-id');
		
		this.$el.find('.zephyr-tb-classification-item').removeClass('zephyr-tb-selected');
		targetEl.addClass('zephyr-tb-selected');
		
		// Clear the detail column view if expanded
		AJS.$('#zephyr-tb-detail-column').html('').width(0);
		AJS.$('.zephyr-tb-issue-content').removeClass('zephyr-tb-selected');
		ZEPHYR.Agile.TestBoard.data.versionId = versionIds;
		ZEPHYR.Agile.TestBoard.data['selectedSprint'] = null;
		ZEPHYR.Agile.TestBoard.data['selectedCycle'] = null;
		ZEPHYR.Agile.TestBoard.data['selectedStatus'] = null;
		ZEPHYR.Agile.TestBoard.data['projectId'] = projectId;
		if(targetEl.hasClass('zephyr-tb-classification-all')) {
			this.getAllVersions();			
		} else {
			this.getVersionById(versionIds);
		}
		/** Update URL **/
		ZEPHYR.Agile.TestBoard.Router.URL.setParams();
		ZEPHYR.Agile.TestBoard.Router.tbRouter.navigate(ZEPHYR.Agile.TestBoard.Router.URL.getURL(), {trigger: false}); 
		// Trigger call to sprint view 
		ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.VERSION_SELECT_SPRINTS_GET);		
	},
	hideVersionNav: function() {
		this.$el.hide();
		AJS.$('#' + ZEPHYR.Agile.TestBoard.data.testBoardElId).removeClass('zephyr-tb-version-expanded');
		/** Update URL **/
		ZEPHYR.Agile.TestBoard.data.isVersionsVisible = false;
		ZEPHYR.Agile.TestBoard.Router.URL.setParams();
		ZEPHYR.Agile.TestBoard.Router.tbRouter.navigate(ZEPHYR.Agile.TestBoard.Router.URL.getURL(), {trigger: false}); 
	},
	getAllVersionIds: function() {
		var versionIds = _.pluck(this.model.toJSON(), 'versionId');

		return _.unique(versionIds);
	},
	getAllProjectIds: function() {
		var projectIds = _.pluck(ZEPHYR.Agile.TestBoard.data.projects.toJSON(), 'id');

		return projectIds;
	},
	getAllVersions: function() {
		var versions = this.model.toJSON() || [];
		ZEPHYR.Agile.TestBoard.data.versions = versions;
	},
	getVersionById: function(versionId) {
		var versions = this.model.toJSON() || [];
		ZEPHYR.Agile.TestBoard.data.versions = [];
		
		if(versions.length) {
			_.each(versions, function(version) {
				if(version.versionId == versionId)
					ZEPHYR.Agile.TestBoard.data.versions.push(version);
			});
		}
	},
	render: function() {
		var versions = this.model.toJSON() || [];
		var allVersionIds = this.getAllVersionIds();
		var allProjectIds = this.getAllProjectIds();
		var versionsHTML = ZEPHYR.Templates.TestBoard.versionsHTML({
			versions: versions,
			allVersionIds: allVersionIds,
			allProjectIds: allProjectIds,
			showProjectName: this.showProjectName
		});
		this.$el.html(versionsHTML);
		if(!AJS.$('#' + ZEPHYR.Agile.TestBoard.data.testBoardElId + '.zephyr-tb-version-expanded').length) {
			this.$el.hide();
		}
		var selectedVersion = this.$el.find('.zephyr-tb-classification-item[data-version-id="' + ZEPHYR.Agile.TestBoard.data.versionId + '"]');
		// Select all Cycles option
		if(selectedVersion.length) {
			if(selectedVersion.hasClass('zephyr-tb-classification-all')) {
				/** Update URL **/
				selectedVersion.filter('.zephyr-tb-classification-all').addClass('zephyr-tb-selected'); // Select 'All Cycles' option
				this.getAllVersions();
			} else {
				/** Update URL **/
				selectedVersion.addClass('zephyr-tb-selected');
				this.getVersionById(ZEPHYR.Agile.TestBoard.data.versionId);
			}
			ZEPHYR.Agile.TestBoard.data.projectId = selectedVersion.data('project-id');
		} else {
			ZEPHYR.Agile.TestBoard.data.versionId = this.$el.find('.zephyr-tb-classification-all').data('version-id');
			ZEPHYR.Agile.TestBoard.data.projectId = this.$el.find('.zephyr-tb-classification-all').data('project-id');
			this.$el.find('.zephyr-tb-classification-all').addClass('zephyr-tb-selected');
			this.getAllVersions();
		}
		return this;
	}
});

ZEPHYR.Agile.TestBoard.Menu = {
	init: function() {
		this.attachClassificationMenuWrapper();
		this.attachViews();
	},
	attachClassificationMenuWrapper: function() {
		var projectCount = (ZEPHYR.Agile.TestBoard.data.projects)? ZEPHYR.Agile.TestBoard.data.projects.length: 0;
		var classificationHTML = ZEPHYR.Templates.TestBoard.classificationMenuHTML();
		AJS.$('#zephyr-tb-classification-menu-column').html(classificationHTML);
		
		if(ZEPHYR.Agile.TestBoard.data.isVersionsVisible) {
			AJS.$('#' + ZEPHYR.Agile.TestBoard.data.testBoardElId).addClass('zephyr-tb-version-expanded');
		} else {
			AJS.$('#' + ZEPHYR.Agile.TestBoard.data.testBoardElId).removeClass('zephyr-tb-version-expanded');
		}
		
		this.attachEvents();
	},
	attachEvents: function() {
		AJS.$('#zephyr-tb-classification-menu-column').find('#zephyr-tb-version-toggle').unbind('click')
			.bind('click', function(ev) {
				ev.preventDefault();
				var targetElId= AJS.$(ev.target).data('target-id');
				
				AJS.$('#' + ZEPHYR.Agile.TestBoard.data.testBoardElId).addClass('zephyr-tb-version-expanded');
				AJS.$('#' + targetElId).show();
				/** Update URL **/
				ZEPHYR.Agile.TestBoard.data.isVersionsVisible = true;
				ZEPHYR.Agile.TestBoard.Router.URL.setParams();
				ZEPHYR.Agile.TestBoard.Router.tbRouter.navigate(ZEPHYR.Agile.TestBoard.Router.URL.getURL(), {trigger: false}); 
			});
	},
	attachViews: function() {
		// Attach versions view
		this.attachVersionsView();
	},
	attachVersionsView: function(projectId) {	
		var showProjectName = (ZEPHYR.Agile.TestBoard.data.projects.length > 1) ? true: false;

		// Attach Versions view
		this.versions = new ZEPHYR.Agile.TestBoard.VersionCollection(versionsJSON);
		var versionsJSON = this.processVersions(this.versions);
		this.versionsView = new ZEPHYR.Agile.TestBoard.VersionsView({
			model: this.versions,
			el: '#zephyr-tb-version-column',
			showProjectName: showProjectName
		});
		this.versionsView.render();
	},
	processVersions: function(versions) {
		var versionsPerProject = ZEPHYR.Agile.TestBoard.data.versionList.versionsPerProject;

		if(ZEPHYR.Agile.TestBoard.data.projects.length) {
			_.each(ZEPHYR.Agile.TestBoard.data.projects.models, function(project) {
				// Add unscheduled version
				versions.add({
					id: -1  + '-' + project.get('id'),	// Setting to null
					versionId: -1,
					name: AJS.I18n.getText("zephyr.je.version.unscheduled"),
					released: false,
					projectId: project.get('id'),
					projectKey: project.get('key'),
					projectName: project.get('name')
				});
				var versionList = _(versionsPerProject[project.id]).map(function(version, i){  
						version.versionId = version.id; // Setting this because since for unscheduled ID is -1, duplicates won't be added to collection.
						version.projectId = project.get('id'); 
						version.projectKey = project.get('key');
						version.projectName = project.get('name');
						return version;
					});
				versions.add(versionList);				
			});
		}
	}
};
