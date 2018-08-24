/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }

ZEPHYR.Agile.TestBoard.Router = {URL: {}};
ZEPHYR.Agile.TestBoard.Router.URL.prefix = contextPath + '/secure/RapidBoard.jspa';


/**
 * Set the URL parameters
 */
ZEPHYR.Agile.TestBoard.Router.URL.setParams = function() {
	var viewType = ZEPHYR.Agile.TestBoard.data.view || ZEPHYR.Agile.TestBoard.view,
		page = ZEPHYR.Agile.TestBoard.data.page,
		boardSuffix = '&view=' + viewType + '&page=' + page;
	
	if(ZEPHYR.Agile.TestBoard.data.selectedSprint) {
		boardSuffix += '&selectedSprint=' + ZEPHYR.Agile.TestBoard.data.selectedSprint;
	}
	if(page == ZEPHYR.Agile.TestBoard.page.sprint) {
		if(ZEPHYR.Agile.TestBoard.data.selectedCycle) {
			boardSuffix += '&selectedCycle=' + ZEPHYR.Agile.TestBoard.data.selectedCycle;
		}
		if(ZEPHYR.Agile.TestBoard.data.selectedStatus) {
			boardSuffix += '&selectedStatus=' + ZEPHYR.Agile.TestBoard.data.selectedStatus;
		}
	} else if(page == ZEPHYR.Agile.TestBoard.page.issues) {
		// Issues board routes
	}
	if(ZEPHYR.Agile.TestBoard.data.isVersionsVisible) {
		boardSuffix += '&versions=visible';
	}
	if(ZEPHYR.Agile.TestBoard.data.versionId && !isNaN(ZEPHYR.Agile.TestBoard.data.versionId)) {
		boardSuffix += '&selectedVersion=' + ZEPHYR.Agile.TestBoard.data.versionId;
	}
    if(ZEPHYR.Agile.TestBoard.data.projectKey){
        boardSuffix = '&projectKey=' + ZEPHYR.Agile.TestBoard.data.projectKey + boardSuffix;
    }
	ZEPHYR.Agile.TestBoard.Router.URL.href = ZEPHYR.Agile.TestBoard.Router.URL.prefix + '?rapidView=' + ZEPHYR.Agile.TestBoard.data.agileBoardId + boardSuffix;
}

ZEPHYR.Agile.TestBoard.Router.URL.getURL = function() {
	return ZEPHYR.Agile.TestBoard.Router.URL.href;
}

ZEPHYR.Agile.TestBoard.Router.TestBoardRouter = Backbone.Router.extend({
    initialize: function() {
    	this.testBoardURLRegex = /\?rapidView=(.*)(&projectKey=(.*))+&view=(.*)&page=(.*)/; // Regex to match testboard URL
    	this.route(/(.*)/, 'defaultAction');
    	this.route(this.testBoardURLRegex, "navigateToTestBoard");
    },
    navigateToTestBoard: function(agileId, projectKey, view, page) {
    	page = ZephyrURL('?page');
    	ZEPHYR.Agile.TestBoard.initTestBoard(page);    	
    },
    defaultAction: function(url) {
    	var location = window.location.href,
    		page = ZephyrURL('?page');
    		
    	if(this.testBoardURLRegex.test(location)) {
    		if(page) {
    			this.setParamaFromURL();
    			ZEPHYR.Agile.TestBoard.initTestBoard(page);
    		}
    	} else {
    		var view = ZephyrURL('?view');
    		if(view == ZEPHYR.Agile.TestBoard.view || view == ZEPHYR.Agile.TestBoard.viewDetail) {
    			this.setParamaFromURL();
    			ZEPHYR.Agile.TestBoard.initTestBoard(page);
    		}
    	}
    },
    setParamaFromURL: function() {
    	var versionId = ZephyrURL('?selectedVersion') || ZEPHYR.Agile.TestBoard.versionAllCycles;		// selected version id || 'all' 
    	var isVersionsVisible = (ZephyrURL('?versions') == 'visible')? true: false; 			// visible 
    	var selectedSprint = ZephyrURL('?selectedSprint');			// selected sprint id
    	var selectedCycle = ZephyrURL('?selectedCycle'); 			// selected cycle id
    	var selectedStatus = ZephyrURL('?selectedStatus'); 			// selected status id:offset
    	var view = ZephyrURL('?view'); 								// planning.test || planning.test.detail
    	var page = ZephyrURL('?page');								// sprint || issues
    	var isDetailView = (view == ZEPHYR.Agile.TestBoard.viewDetail)? true: false;
        var projectKey = ZephyrURL('?projectKey');  

    	ZEPHYR.Agile.TestBoard.data['versionId'] = versionId;
    	ZEPHYR.Agile.TestBoard.data['isVersionsVisible'] = isVersionsVisible;
    	ZEPHYR.Agile.TestBoard.data['selectedSprint'] = selectedSprint;
    	ZEPHYR.Agile.TestBoard.data['selectedCycle'] = selectedCycle;
    	ZEPHYR.Agile.TestBoard.data['selectedStatus'] = selectedStatus;
    	ZEPHYR.Agile.TestBoard.data['view'] = view;
    	ZEPHYR.Agile.TestBoard.data['page'] = page;
    	ZEPHYR.Agile.TestBoard.data['isDetailView'] = isDetailView;
        ZEPHYR.Agile.TestBoard.data['projectKey'] = projectKey;
    }
});