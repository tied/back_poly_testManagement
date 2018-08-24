/**
 * Zephyr Test Board Issues
 * IssuesBoardView
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }
if (typeof ZEPHYR.Agile.TestBoard.Issues == 'undefined') { ZEPHYR.Agile.TestBoard.Issues = {}; }

/**
 * - Extend from Testboard View
 * - Attach the issues board layout
 * - Attach the SprintsView
 */
ZEPHYR.Agile.TestBoard.IssuesBoardView = ZEPHYR.Agile.TestBoard.TestBoardView.extend({
	initialize: function() {
		_.bindAll(this, 'removeDOMListeners');
		ZEPHYR.Agile.TestBoard.TestBoardView.prototype.initialize.call(this);
		this.bind(ZEPHYR.Agile.TestBoard.Events.RENDER_LAYOUT_SUCCESS, this.attachSprintsView, this);
	},
	attachSprintsView: function() {
		// Sprints View
	    ZEPHYR.Agile.TestBoard.views.sprintsView = new ZEPHYR.Agile.TestBoard.Issues.SprintsView({
	        el: '#zephyr-tb-sprints-column',
	        model: ZEPHYR.Agile.TestBoard.data.sprints
	    });
	    ZEPHYR.Agile.TestBoard.views.sprintsView.render();
		// Displaying the defects on default position
	    ZEPHYR.defectHover.INLINE_DIALOG_OPTIONS.gravity = undefined;
	},
	removeDOMListeners: function() {
	    this.unbind();
	    // Check for older Backbone versions which does not have this.stopListening()
	    if (typeof this.stopListening == 'function') 
	    	this.stopListening();
	}
});