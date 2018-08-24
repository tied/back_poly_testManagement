/**
 * @namespace ZEPHYR.PROJECT.SIDEBAR
 */
if (typeof ZEPHYR == 'undefined') {
    var ZEPHYR = {};
}

if (typeof ZEPHYR.PROJECT == 'undefined') {
    ZEPHYR.PROJECT = {};
}

if (typeof ZEPHYR.PROJECT.SIDEBAR == 'undefined') {
    ZEPHYR.PROJECT.SIDEBAR = {};
}

ZEPHYR.PROJECT.SIDEBAR = (function () {
    var _showTabByURLParam = function (tabType) {
        AJS.params.zdateformat = AJS.$("#zdateformat").val(); // Added for ZFJ-2899
    	if(tabType.indexOf('traceability-tab') > -1) {
    		tabType = 'traceability-tab'; // Fix for traceability report page with '&type=report' in the location
        }
        tabType = tabType.indexOf('&') ? tabType.split('&')[0] : tabType;
        AJS.tabs.change(AJS.$("a[href=#" + tabType + "]"));
    };

    return {
        showTabByURLParam: _showTabByURLParam
    }
});

var isLoadedInIframe = function() {
    try {
        return (window !== window.parent);
    } catch(e) {
        return false;
    }
}

var InitPageContent = function(initCallback) {
    if(isLoadedInIframe()) {
        AJS.$(window).load(function(){
            initCallback();
        });
    } else {
        AJS.$(document).ready(function(){
            initCallback();
        });
    }
}

InitPageContent(function(){
    var tabName = window.location.hash.substr(1);
    var projectSideBar = new ZEPHYR.PROJECT.SIDEBAR();
    if (tabName && tabName.length > 0) {
        projectSideBar.showTabByURLParam(tabName);
    } else {
      if(window.location.href.indexOf('zephyr-tests-page') > -1) {
        projectSideBar.showTabByURLParam("test-summary-tab");
      }
    }


    window.addEventListener("hashchange", function(e){
        var tabType = window.location.hash;
        var selector;

        var newTab = e.newURL.substring(e.newURL.lastIndexOf("#")+1, e.newURL.indexOf("&") === -1 ? e.newURL.lastIndexOf("") : e.newURL.indexOf("&"));
        var oldTab = e.oldURL.substring(e.oldURL.lastIndexOf("#")+1, e.oldURL.indexOf("&") === -1 ? e.oldURL.lastIndexOf("") : e.oldURL.indexOf("&"));
        if(newTab === oldTab) return;
        if(tabType.indexOf('traceability-tab') > -1){
            selector = "#aui-traceability-tab";
        } else if(tabType.indexOf('test-summary-tab') > -1){
            selector = "#aui-test-summary-tab";
        } else if(tabType.indexOf('test-cycles-tab') > -1){
            selector = "#aui-test-cycles-tab";
        }
        if (selector) {
            AJS.$(selector).trigger("click");
        }
    }, false);


    AJS.$("#zephyr-project-view-tabs > ul > li > a").bind("tabSelect", function (e, o) {
        var newHref,
            hashIndex = window.location.href.indexOf('#');

        var tabType = o.pane.selector.slice(1);
        if (hashIndex && hashIndex > 0)
            newHref = window.location.href.slice(0, hashIndex);

        window.location.href = newHref ? (newHref + o.pane.selector) : (window.location.href + o.pane.selector);
        if(tabType.indexOf('traceability-tab') > -1) {
          // Fix for traceability report page with '&type=report' in the location
          if(!AJS.$('#traceability-container').find('#report-section').length && !AJS.$('#traceability-container').find('#requirement-section').length) {
            ZEPHYR.Traceability.init();
          }
          AJS.$("#selected-tab").text('Traceability');
          AJS.$("#project-key").hide();
          AJS.$("#walk-through-initializer").hide();
          // AJS.$("#selected-tab").text(AJS.I18n.getText('zephyr-je.pdb.traceability.label'));
        }

        if(tabType.indexOf('test-summary-tab') > -1) {
          AJS.$(o.pane.selector+'-content').hide();
          ZEPHYR.Loading.showLoadingIndicator();
          AJS.$('#zfj-aui-blanket').css('visibility', 'visible');
          AJS.$('#zfj-aui-blanket').css('opacity', '0.2');
          window.location.reload();
        }

        if(tabType.indexOf('test-cycles-tab') > -1) {
            ZEPHYR.Cycle.versionList = [];
            AJS.$("#select-version2 > optgroup > option").each(function() {
                ZEPHYR.Cycle.versionList.push({id:this.value, name:this.text});
            });
            AJS.$("#selected-tab").text('Cycle Summary');
            AJS.$("#walk-through-initializer").show();
        }

        setTimeout(function() {
            AJS.$(document).scrollTop(0);
        }, 0);
        if (e.currentTarget.id == "aui-test-cycles-tab") {
            if (testCycleTabReady()) {
                testCycleTabReady();
            }
        }
    });
});
