/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }

/**
 * Display the header,
 * Back to Boards option
 * Collapsible/Expandable option
 * Toggle between Sprint and Issue view
 */
ZEPHYR.Agile.TestBoard.HeaderView = Backbone.View.extend({
	initialize: function() {
		_.bindAll(this, 'displayBacklogUI');
		this.render();
		ZEPHYR.Agile.TestBoard.globalDispatcher.bind(ZEPHYR.Agile.TestBoard.Events.BOARD_NAME_UPDATE, this.updateHeader, this);
	},
	updateHeader: function(boardName) {
		AJS.$('#zephyr-tb-board-name').html(boardName);
	},
	displayBacklogUI: function(ev) {
		ev.preventDefault();
		var targetEl = AJS.$('.aui-nav-item[data-link-id="com.pyxis.greenhopper.jira:project-sidebar-' + ZEPHYR.Agile.TestBoard.data.mode + '-scrum"]');
		if(!targetEl.length)
			targetEl = AJS.$('.aui-nav-item[data-link-id="com.pyxis.greenhopper.jira:global-sidebar-' + ZEPHYR.Agile.TestBoard.data.mode + '-scrum"]');
		AJS.$('#ghx-view-pluggable, #ghx-controls, .ghx-sprint-meta').show();
		AJS.$('#zephyr-tb-back-to-boards-wrapper').remove();
		if(targetEl.length) {
			// If safari browser, since router navigate is not JIRA's router assigning to href
			// which will perform page reload
			if(navigator.userAgent.search("Safari") >= 0 && navigator.userAgent.search("Chrome") < 0) {
				window.location.href = targetEl.attr('href');
			} else {
				ZEPHYR.Agile.TestBoard.Router.tbRouter.navigate(targetEl.attr('href'), {trigger: false}); 
			}
		}
		targetEl.trigger('click');
	},
	filterFocus: function(ev) {
		AJS.$(ev.target).animate({
		    width: "150px"
		  }, 'fast', 'linear');
	},
	filterBlur:function(ev) {
		var targetEl = AJS.$(ev.target),
		rawQuery = AJS.$.trim(targetEl.val());
		if (_.isEmpty(rawQuery)) {
			targetEl.animate({
			    width: "48px"
			}, 'fast', 'linear');
		}
	},
	attachEvents: function() {
		AJS.$('#zephyr-tb-back-to-boards-button').unbind('click')
			.bind('click', this.displayBacklogUI);
		AJS.$('.zephyr-tb-compact-toggle').unbind('click')
			.bind('click', this.toggleCompactView);
		AJS.$('#zephyr-tb-sprint-search-input')
			.unbind('keydown')
			.bind('keydown', function(ev){ev.stopImmediatePropagation();}) // Fix for ZFJ-2154
			.unbind('keyup')
			.bind('keyup', this.filterCycles)
			.focus(this.filterFocus)
			.blur(this.filterBlur);
		
		AJS.$('#zephyr-tb-issue-search-input')
			.unbind('keydown')
			.bind('keydown', function(ev){ev.stopImmediatePropagation();}) // Fix for ZFJ-2154
			.unbind('keyup')
			.bind('keyup', this.filterIssues)
			.focus(this.filterFocus)
			.blur(this.filterBlur);
		
		AJS.$('#zephyr-tb-filter-cycle-icon').unbind('click')
			.bind('click', function(ev) {
				ev.preventDefault();
				if(AJS.$(ev.target).hasClass('aui-iconfont-remove')) {
					AJS.$('#zephyr-tb-sprint-search-input').val('').trigger('keyup').focus();
				}
			});
		AJS.$('#zephyr-tb-filter-issue-icon').unbind('click')
			.bind('click', function(ev) {
				ev.preventDefault();
				if(AJS.$(ev.target).hasClass('aui-iconfont-remove')) {
					AJS.$('#zephyr-tb-issue-search-input').val('').trigger('keyup').focus();
				}
			});
		
		// Attach Test view toggle button
		AJS.$('#zephyr-tb-testboard-toggle-button').unbind('click')
			.bind('click', function(ev) {
				ev.preventDefault();
				var pageType = AJS.$(ev.target).data('page');
				
				AJS.$(ev.target).attr('disabled', 'disabled');
				ZEPHYR.Agile.TestBoard.navigateToTestBoardView(pageType);
			});
	},
	filterCycles: function(ev) {
		var keyCode = ev.which;
		if(keyCode == 13) {
			ev.preventDefault();
			return false;
		}
		var input = AJS.$(ev.target).val();
		ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.CYCLES_FILTER,input);
		if(input) {
			AJS.$('#zephyr-tb-filter-cycle-icon').removeClass('aui-iconfont-search-small').addClass('aui-iconfont-remove');
		} else {
			AJS.$('#zephyr-tb-filter-cycle-icon').removeClass('aui-iconfont-remove').addClass('aui-iconfont-search-small');
		}
	},
	filterIssues: function(ev) {
		var keyCode = ev.which;
		if(keyCode == 13) {
			ev.preventDefault();
			return false;
		}
		var input = AJS.$(ev.target).val();
		ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.ISSUES_FILTER,input);
		if(input) {
			AJS.$('#zephyr-tb-filter-issue-icon').removeClass('aui-iconfont-search-small').addClass('aui-iconfont-remove');
		} else {
			AJS.$('#zephyr-tb-filter-issue-icon').removeClass('aui-iconfont-remove').addClass('aui-iconfont-search-small');
		}
	},
	toggleCompactView: function(ev) {
		ev.preventDefault();
		if(AJS.$('body').hasClass('zephyr-tb-header-compact')) {
			AJS.$('body').removeClass('zephyr-tb-header-compact');
		} else {
			AJS.$('body').addClass('zephyr-tb-header-compact');
		}
	},
	render: function() {	
		var boardName = '';
		if(GH.WorkControls && GH.WorkControls.rapidViewConfig && GH.WorkControls.rapidViewConfig.name) {
			boardName = GH.WorkControls.rapidViewConfig.name;
		} else {
			boardName = AJS.$('#ghx-board-name').text() || ZEPHYR.Agile.TestBoard.data.projectKey;
		}	
		// Hide the pluggable drop downs and agile controls
		AJS.$('#ghx-header, #ghx-operations').hide();
		AJS.$('#zephyr-tb-header, #zephyr-tb-operations').remove();
		
		var backToBoardsHTML = ZEPHYR.Templates.TestBoard.backToBoardsHTML({
			boardName: boardName,
			page: ZEPHYR.Agile.TestBoard.data.page
		});
		AJS.$(backToBoardsHTML).insertAfter('#ghx-header');
		
		var testboardOperationsHTML = ZEPHYR.Templates.TestBoard.testboardOperationsHTML();
		AJS.$(testboardOperationsHTML).insertAfter('#ghx-operations');
		this.attachEvents();
		return this;
	}
});