/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }

/**
 * - Extend from Testboard View
 * - Attach the sprint board layout
 * - Attach the SprintsView
 */
ZEPHYR.Agile.TestBoard.SprintBoardView = ZEPHYR.Agile.TestBoard.TestBoardView.extend({
	initialize: function() {
		_.bindAll(this, 'removeDOMListeners');
		ZEPHYR.Agile.TestBoard.TestBoardView.prototype.initialize.call(this);
		this.bind(ZEPHYR.Agile.TestBoard.Events.RENDER_LAYOUT_SUCCESS, this.attachSprintsView, this);
	},
	fetchSprints: function() {
		ZEPHYR.Agile.TestBoard.Loading.showLoadingIndicator();
		ZEPHYR.Agile.TestBoard.views.sprintsView.render();
		ZEPHYR.Agile.TestBoard.Loading.hideLoadingIndicator();
	},
	attachEvents: function() {
		try {
			// Toggle list/ detail view on keypress of alphabet 'T' not 't' since JIRA Agile has already
			AJS.whenIType("T").execute(function () {
				if(ZEPHYR.Agile.TestBoard.data.isDetailView) {
					AJS.$('.zephyr-tb-detail-close').trigger('click');
				} else {
					if(ZEPHYR.Agile.TestBoard.data.selectedCycle) {
						AJS.$('.zephyr-tb-cycle-content[data-cycle-id="' + ZEPHYR.Agile.TestBoard.data.selectedCycle + '"]').click();
					}
				}
			});
		} catch(e) {
			console.log(e.message);
		}		
	},
	attachSprintsView: function() {
		this.attachEvents();
		//this.fetchSprints();
	},
	removeDOMListeners: function() {
	    this.unbind();
	    // Check for older Backbone versions which does not have this.stopListening()
	    if (typeof this.stopListening == 'function') 
	    	this.stopListening();
	}
});