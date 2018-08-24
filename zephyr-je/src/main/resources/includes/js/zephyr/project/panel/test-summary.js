
var tId;
var isLoadedInIframe = function() {
    try {
        return (window !== window.parent);
    } catch(e) {
        return false;
    }
}
var testSummaryPagesUrlDetails = [];
var testSummaryTourXpathData = [];

AJS.$(document).ready(function (event) {
    let tab = window.location.href;
    if (tab.indexOf('test-cycles-tab') >=0 ) {
        setTimeout(function () {
            testCycleTabReady();
        }, 2000);
    }
});

function testCycleTabReady() {

    testSummaryTourXpathData = [
        {
            path: '//*[@id="breadcrumbs-wrapper"]',
            description: AJS.I18n.getText("walkthrough.testSummary.breadcrumb"),
            direction: 'down-right',
        }, {
            path: '//*[@id="tree-docker-container"]',
            description: AJS.I18n.getText("walkthrough.testSummary.docker"),
            direction: 'top-right',
        }, {
            path: '//*[@id="tree-tcr"]/div[2]/div[1]',
            description: AJS.I18n.getText("walkthrough.testSummary.filter"),
            direction: 'down-right',
        }, {
            path: '//*[@id="cycle-executions-wrapper"]/div[2]/div[1]',
            description: AJS.I18n.getText("walkthrough.testSummary.pagination"),
            direction: 'down-left',
        }, {
            path: '//*[@id="cycle-executions-wrapper"]/div[2]/div[2]/div/span[1]',
            description: AJS.I18n.getText("walkthrough.testSummary.pageWidth"),
            direction: 'down-left',
        }, {
            path: '//*[@id="cycle-info"]/div[1]/div[3]/div[3]',
            description: AJS.I18n.getText("walkthrough.testSummary.description"),
            direction: 'down-left',
        }, {
            path: '//*[@id="version--1_anchor"]/div/div[2]',
            description: AJS.I18n.getText("walkthrough.testSummary.contextMenu"),
            direction: 'down-left',
        }
    ];

    //  REFERENCE OBJECT FOR LINK IN DIFFERENT LOCATION AND SHOWING DIFFERENT LOCATION

    // {
    //     path: '//*[@id="cycle-details"]/div[2]/div[4]/div/div/div/div/header/div/h1/span',
    //     description: 'Go back to List view',
    //     link: true,
    //     linkName: 'List view',
    //     linkFunction: '//*[@id="backButton"]'
    // }

    jQuery.ajax({
        url: getRestURL() + "/zql/executeSearch/?zqlQuery=&offset=0&maxRecords=10",
        type: "GET",
        contentType: "application/json",
        dataType: "json",
        success: function success(response) {
            if (response.totalCount > 0 && response.executions[0].id) {
                testSummaryPagesUrlDetails = [
                    {
                        url: contextPath + '/secure/enav/#/' + response.executions[0].id + '?query=&',
                        title: AJS.I18n.getText('execute.test.execution.page.title'),
                        toPage: 'executionPage'
                    }
                ];
            } else {
                testSummaryPagesUrlDetails = [];
            }
            testSummaryCallWalkThrough('walkThrough', false);
        }
    });
    createCycleSummaryGrid(ZEPHYR.Cycle.executions, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder, true);
}

function testSummaryCallWalkThrough(knowledgeTour, isNotFirstTime) {
    if (knowledgeTour == 'walkThrough') {

        var tempXpathData = [];
        if (AJS.$('#listViewBtn').hasClass('active-view')) {

            tempXpathData = JSON.parse(JSON.stringify(testSummaryTourXpathData));

            if (ZEPHYR.Cycle.executions.length != 0) {
                tempXpathData.push(
                    {
                        path: '//*[@id="detailViewBtn"]',
                        description: AJS.I18n.getText("walkthrough.testSummary.switchView"),
                        direction: 'top-left',
                        link: true,
                        delayTime: 2000,
                        linkName: 'Detail View',
                    }, {
                        path: '//*[@id="execute-test-header-right"]',
                        description: AJS.I18n.getText("walkthrough.executionScroll"),
                        direction: 'top-left',
                    }, {
                        path: '//*[@id="custom-fidld-display-area"]/div/div/div/div[1]/h3',
                        description: AJS.I18n.getText("walkthrough.testSummary.customField"),
                        direction: 'down-right',
                    }, {
                        path: '//*[@id="cycle-details"]/div[2]/div[4]/div/div/div/div/header/div/h1/span',
                        description: AJS.I18n.getText("walkthrough.testSummary.listView"),
                        link: true,
                        linkName: 'List view',
                        linkFunction: '//*[@id="backButton"]',
                        direction: 'top-right',
                    }
                );
            // } else {
            //     tempXpathData.push(
            //         {
            //             path: '//*[@id="detailViewBtn"]',
            //             description: AJS.I18n.getText("walkthrough.testSummary.switchView"),
            //             direction: 'top-left',
            //             linkName: 'Detail View',
            //         }
            //     );
            }

        } else if (AJS.$('#detailViewBtn').hasClass('active-view')) {
            tempXpathData = [
                {
                    path: '//*[@id="execute-test-header-right"]',
                    description: AJS.I18n.getText("walkthrough.executionScroll"),
                    direction: 'top-left',
                }, {
                    path: '//*[@id="custom-fidld-display-area"]/div/div/div/div[1]/h3',
                    description: AJS.I18n.getText("walkthrough.testSummary.customField"),
                    direction: 'down-right',
                }, {
                    path: '//*[@id="cycle-details"]/div[2]/div[4]/div/div/div/div/header/div/h1/span',
                    description: AJS.I18n.getText("walkthrough.testSummary.listView"),
                    link: true,
                    linkName: 'List view',
                    linkFunction: '//*[@id="backButton"]',
                    direction: 'top-right',
                    delayTime: 250,
                }, {
                    path: '//*[@id="breadcrumbs-wrapper"]',
                    description: AJS.I18n.getText("walkthrough.testSummary.breadcrumb"),
                    direction: 'down-left',
                }, {
                    path: '//*[@id="tree-docker-container"]',
                    description: AJS.I18n.getText("walkthrough.testSummary.docker"),
                    direction: 'top-left',
                }, {
                    path: '//*[@id="tree-tcr"]/div[2]/div[1]',
                    description: AJS.I18n.getText("walkthrough.testSummary.filter"),
                    direction: 'down-right',
                }, {
                    path: '//*[@id="cycle-executions-wrapper"]/div[2]/div[1]',
                    description: AJS.I18n.getText("walkthrough.testSummary.pagination"),
                    direction: 'down-left',
                }, {
                    path: '//*[@id="cycle-executions-wrapper"]/div[2]/div[2]',
                    description: AJS.I18n.getText("walkthrough.testSummary.pageWidth"),
                    direction: 'down-left',
                }, {
                    path: '//*[@id="cycle-info"]/div[1]/div[3]/div[3]',
                    description: AJS.I18n.getText("walkthrough.testSummary.description"),
                    direction: 'down-left',
                }, {
                    path: '//*[@id="version--1_anchor"]/div/div[2]',
                    description: AJS.I18n.getText("walkthrough.testSummary.contextMenu"),
                    direction: 'down-right',
                }, {
                    path: '//*[@id="detailViewBtn"]',
                    description: AJS.I18n.getText("walkthrough.testSummary.switchView"),
                    direction: 'top-left',
                    link: true,
                    linkName: 'Detail View',
                }
            ];
        }

        AJS.$('#js-tree').scrollTop(0);

        walkThroughCall(tempXpathData, '#sidebar-page-container', 'cycleSummary', testSummaryPagesUrlDetails, isNotFirstTime);
    } else if (knowledgeTour == 'newPageLayout') {
        var tempXpathData = [];
        if (AJS.$('#listViewBtn').hasClass('active-view')) {
           tempXpathData = testSummaryTourXpathData;
        } else if (AJS.$('#detailViewBtn').hasClass('active-view')) {
            tempXpathData = [
                {
                    path: '//*[@id="custom-fidld-display-area"]/div/div/div/div[1]/h3',
                    description: AJS.I18n.getText("walkthrough.testSummary.customField"),
                    direction: 'down-right',
                }, {
                    path: '//*[@id="execute-test-header-right"]',
                    description: AJS.I18n.getText("walkthrough.executionScroll"),
                    direction: 'top-left',
                }, {
                    path: '//*[@id="cycle-details"]/div[2]/div[4]/div/div/div/div/header/div/h1/span',
                    description: AJS.I18n.getText("walkthrough.testSummary.listView"),
                    link: true,
                    linkName: 'List view',
                    linkFunction: '//*[@id="backButton"]',
                    direction: 'top-right',
                }
            ];
        }

        newPageLayout(tempXpathData, '#sidebar-page-container', 'cycleSummary');
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

var throttle = function(method) {
    clearTimeout(tId);
    tId = setTimeout(method, 800);
};

window.onbeforeunload = function (e) {
    return ;
}

AJS.$('.removeSearchInput').live('click', function(event){
  var parent = AJS.$(event.target).parent('.search-grid-wrapper');
  var searchInput = parent.find('.searchInput');
  searchInput.val('');
  if(searchInput.hasClass('active')) {
    searchInput.trigger('keyup');
    searchInput.removeClass('active');
  } else {
    searchInput.addClass('active');
  }
});

AJS.$('.search-grid-wrapper .searchInput').live('click', function(event){
    var searchInput = AJS.$(event.target);
    searchInput.addClass('active');
});

//Capture only blur event since keyup is too many calls for analytics.
AJS.$('.search-grid-wrapper .searchInput').live('blur', function(){
    //for analytics
    var el = AJS.$(this)
        .closest(".grid-container")
        .attr("id")
        .replace("-grid",""),event=null;
    if (el){
        switch (el) {
            case 'version':
                event=ZephyrEvents.SEARCH_TEST_SUMMARY_VERSIONS;
                break;
            case 'component':
                event=ZephyrEvents.SEARCH_TEST_SUMMARY_COMPONENTS;
                break;
            case 'label':
                event=ZephyrEvents.SEARCH_TEST_SUMMARY_LABELS;
                break;
            default:
                //don't pass
        }

        if (za != undefined && event) {
            za.track({'event':event,
                    'eventType':'Blur'
                },
                function (res) {
                    console.log('Analytics test: -> ',res);
                });
        }

    }

});



InitPageContent(function(){

    ZEPHYR.TestSummary = ZEPHYR.TestSummary || {};

    var projectId = AJS.$('#projId').val();

    if(!projectId) {
        return;
    }

    ZEPHYR.TestSummary.Version = Backbone.Model.extend({
        url:function(){
            return getRestURL() + "/test/summary/testsbyversion?projectId=" + this.projectId + '&versionName=' + this.name + '&offset=' + this.offset + '&maxRecords=' + this.maxRecords;
        }
    });

    ZEPHYR.TestSummary.Component = Backbone.Model.extend({
        url:function(){
            return getRestURL() + "/test/summary/testsbycomponent?projectId=" + this.projectId + '&componentName=' + this.name + '&offset=' + this.offset + '&maxRecords=' + this.maxRecords;
        }
    });

    ZEPHYR.TestSummary.Label = Backbone.Model.extend({
        url:function(){
            return getRestURL() + "/test/summary/testsbylabel?projectId=" + this.projectId + '&labelName=' + this.name + '&offset=' + this.offset + '&maxRecords=' + this.maxRecords;
        }
    });

    // AJS.$('.test-summary-grid-wrapper').on('click', '.search-grid-wrapper', function(ev) {
    //     var searchInput = AJS.$(ev.target).parents('.grid-container').find('.searchInput');
    //     if(searchInput.hasClass('collapse')) {
    //         searchInput.removeClass('collapse').addClass('expand');
    //     } else {
    //         searchInput.removeClass('expand').addClass('collapse');
    //     }
    // });

      ZEPHYR.TestSummary.GridWrapper = Backbone.View.extend({
        events: {
            'keyup .searchInput': 'searchHandler',
            'click .prev': 'loadPrevPage',
            'click .next': 'loadNextPage'
        },
        initialize: function(options) {
            this.options = options || {};
            this.render();
        },
        render: function() {
            var gridHtml = ZEPHYR.Templates.TestSummary.gridWrapper({
                items: this.model.attributes.values,
                title: this.options.title
            });
            AJS.$(this.el).html(gridHtml);
            var totalPages = Math.ceil(this.model.attributes.totalCount / this.model.maxRecords);
            AJS.$(this.el).find('.page-count').html('Page 1 of ' + totalPages);
            if(totalPages === 1) {
                AJS.$(this.el).find('.next').addClass('disabled').attr('disabled','true');
            }
            return this;
        },
        searchHandler: function(ev) {
            var that = this;
            that.model.name = ev.target.value;
            var removeElement = AJS.$(ev.target).parents('.search-grid-wrapper').find('.removeSearchInput');
            if(that.model.name) {
              removeElement.removeClass('aui-iconfont-search-small').addClass('aui-iconfont-remove');
            } else {
              removeElement.removeClass('aui-iconfont-remove').addClass('aui-iconfont-search-small');
            }
            that.model.fetch({
                success: function() {
                    var gridHtml = ZEPHYR.Templates.TestSummary.gridView({
                        items: that.model.attributes.values
                    });
                    AJS.$(that.el).find('.summary-grid-view').html(gridHtml);
                }
            });
        },
        loadPrevPage: function() {
            var that = this;
            that.model.offset = (that.model.pageNo - 1) * this.model.maxRecords;
            that.model.fetch({
                success: function(model, resp) {
                    var gridHtml = ZEPHYR.Templates.TestSummary.gridView({
                        items: that.model.attributes.values
                    });
                    AJS.$(that.el).find('.summary-grid-view').html(gridHtml);
                    AJS.$(that.el).find('.next').removeClass('disabled').removeAttr('disabled');
                    that.model.pageNo = that.model.pageNo - 1;
                    if(that.model.pageNo === 0) {
                        AJS.$(that.el).find('.prev').addClass('disabled').attr('disabled','true');
                    }
                    var currentPage = that.model.pageNo + 1;
                    AJS.$(that.el).find('.page-count').html('Page ' + currentPage + ' of ' + Math.ceil(resp.totalCount / that.model.maxRecords));
                }
            });
        },
        loadNextPage: function() {
            var that = this;
            that.model.offset = (that.model.pageNo + 1) * this.model.maxRecords;
            that.model.fetch({
                success: function(model, resp) {
                    var totalPages = Math.ceil(resp.totalCount / that.model.maxRecords);
                    var gridHtml = ZEPHYR.Templates.TestSummary.gridView({
                        items: that.model.attributes.values
                    });
                    AJS.$(that.el).find('.summary-grid-view').html(gridHtml);
                    AJS.$(that.el).find('.prev').removeClass('disabled').removeAttr('disabled');
                    that.model.pageNo = that.model.pageNo + 1;
                    if(that.model.pageNo === (totalPages - 1)) {
                        AJS.$(that.el).find('.next').addClass('disabled').attr('disabled','true');
                    }
                    var currentPage = that.model.pageNo + 1;
                    AJS.$(that.el).find('.page-count').html('Page ' + currentPage + ' of ' + Math.ceil(resp.totalCount / that.model.maxRecords));
                }
            });
        }
    });

    var versionModel = new ZEPHYR.TestSummary.Version();
    versionModel['projectId'] = projectId;
    versionModel['pageNo'] = 0;
    versionModel['name'] = '';
    versionModel['offset'] = 0;
    versionModel['maxRecords'] = 10;

    versionModel.fetch({
        success: function(model) {
            var gridWrapperView = new ZEPHYR.TestSummary.GridWrapper({
                el: '#version-grid',
                model: model,
                title: AJS.I18n.getText('zephyr.testSummary.byVersion')
            });
        }
    });

    var componentModel = new ZEPHYR.TestSummary.Component();
    componentModel['projectId'] = projectId;
    componentModel['pageNo'] = 0;
    componentModel['name'] = '';
    componentModel['offset'] = 0;
    componentModel['maxRecords'] = 10;

    componentModel.fetch({
        success: function(model) {
            var gridWrapperView = new ZEPHYR.TestSummary.GridWrapper({
                el: '#component-grid',
                model: model,
                title: AJS.I18n.getText('zephyr.testSummary.byComponent')
            });
        }
    });

    var labelModel = new ZEPHYR.TestSummary.Label();
    labelModel['projectId'] = projectId;
    labelModel['pageNo'] = 0;
    labelModel['name'] = '';
    labelModel['offset'] = 0;
    labelModel['maxRecords'] = 10;

    labelModel.fetch({
        success: function(model) {
            var gridWrapperView = new ZEPHYR.TestSummary.GridWrapper({
                el: '#label-grid',
                model: model,
                title: AJS.I18n.getText('zephyr.testSummary.byLabels')
            });
        }
    });

});

AJS.$.namespace("ZEPHYR.Templates.TestSummary");
