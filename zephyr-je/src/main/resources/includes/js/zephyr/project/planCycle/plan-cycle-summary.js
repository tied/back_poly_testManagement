/*************************************/
/*** Backbone Models Declaration ****/
/************************************/

var zephyrjQuery3 = window.zephyrjQuery3;
var editSameName = false;
var statusMap = {};
var cycleNodeDuplicate = {};
var folderNodeDuplicate = {};
var paginationWidth = 10;
var allPagesPaginationPageWidth = {};
var refreshGrid = true;
var currentPaginationExecutionTable;
var isDetailedViewClicked = false;
var isOffsetReset = false;
jQuery.noConflict( true );
var isButtonDisabled = false;
var selectedPage, totalPages;
var clearData = true;
var updatedGridDataCycleSummary = {};
var onlyUpdateGridValueCycleSummary = false;
var initialCountCycleSummary = 10;
var cycleSummaryGridSelectedExecutions = [];
ZEPHYR.Cycle.clickedExecutionId = null;
var currentMoveExecutionPageNumber = 0;
var initialTreeLoad = false;
var isCloneCycleFail = false;
var isCloneFolderFail = false;
var isDeleteSchedulesFail = false;

AJS.$(document).ready(function (event) {
    //  NEED TO HIT THE GET API HERE TO GET THE PAGINATION WIDTH

    var xhr = {
        url: '/preference/paginationWidthPreference',
        method: 'GET'
    }
    xhrCall(xhr, function (response) {
        //console.log(response);
        if (!response.planCycleSummary) {
            response.planCycleSummary = 10;
        }
        paginationWidth = response.planCycleSummary;
        allPagesPaginationPageWidth = response;
    });

    //  GETTING THE PAGINATION WIDTH FROM THE PREFERENCE API IN getExecutionCustomizationPlanCycle FUNCTION
});

function setZephyrjQuery3Header(options, originalOptions, jqXHR) {
    if(window.zEncKeyFld) {
        jqXHR.setRequestHeader(window.zEncKeyFld, window.zEncKeyVal);
    } else {
        if(parent) {
            jqXHR.setRequestHeader(parent.zEncKeyFld, parent.zEncKeyVal);
        }
    }
}

zephyrjQuery3.ajaxPrefilter(function( options, originalOptions, jqXHR ) {
    setZephyrjQuery3Header(options, originalOptions, jqXHR);
});

ZEPHYR.Cycle.cycle = Backbone.Model.extend({});
ZEPHYR.Cycle.version = Backbone.Model.extend({});
ZEPHYR.Cycle.execution = Backbone.Model.extend({});
ZEPHYR.Cycle.info = Backbone.Model.extend({});
ZEPHYR.Cycle.scheduleList = [];
ZEPHYR.Cycle.executions = [];
var stepLevelDefects = [];
var stepLevelDefectsExecutionId;
var InlineDialog;
var stepAttachmentInlineDialog;
var selectedVersionFromTree;
var sortKey;
var sortOrder;
var moveExecutionSelectedId = [];
var moveExecutionNodeParam = {};
var resetSelectAll = false;

ZEPHYR.Cycle.executionColumns = {};
ZEPHYR.Cycle.planCycleCFOrder = [];

var selectedExecutionId = '';
var allCustomFieldsPC;
var refreshExecutionFlag = true;
var cycleSelected = {};
var queryParamsAllCycles = [];
var executionsTableModelNew = new ZEPHYR.Cycle.execution({
    'executions' : [],
    'allStatusList' : {},
    'totalCount' : 0,
    'currentIndex' : 1,
    'selectedId': null
});
var executionsMoveGrid = new ZEPHYR.Cycle.execution({
    'executions' : [],
    'allStatusList' : {},
    'totalCount' : 0,
    'currentIndex' : 1
});
var cycleInfoModelNew = new ZEPHYR.Cycle.info({
    cycle : {
        'build' : '',
        'environment' : '',
        'createdByDisplay' : '',
        'startDate' : '',
        'description' : ''
    }
})
var showDefectsPopup = false;
var getExecutionUrl = function(params) {
    var execution = params.execution;
    var cycleName = (execution.cycleId == -1) ? "Ad hoc" : execution.cycleName;
    var folderName = (execution.folderID)? false : execution.folderName;
    var queryParam = '';
    var sortQuery = executionsTableModelNew.get('sortQuery');
    var orderBy = 'ORDER BY Execution ASC'
    if(sortQuery) {
        var field = sortQuery.split(':')[0];
        if(field != 'OrderId' && field != 'ExecutionStatus') {
            orderBy = 'ORDER BY ' + (field == 'ID' ? 'Issue' : field) + ' ' + sortQuery.split(':')[1];
        } else if (field == 'ExecutionStatus') {
            orderBy = 'ORDER BY ' + (field == 'ID' ? 'Issue' : field) + ' ' + sortQuery.split(':')[1] + ', Execution' + ' ' + sortQuery.split(':')[1];
        }
    }
    if(folderName) {
      queryParam = 'project = "' + addSlashes(execution.projectKey) + '" AND fixVersion = "' +  addSlashes(execution.versionName) + '" AND cycleName in ("' + addSlashes(cycleName) + '")  AND folderName in ("' + addSlashes(folderName) + '") ' + orderBy;
    } else {
      queryParam = 'project = "' + addSlashes(execution.projectKey) + '" AND fixVersion = "' +  addSlashes(execution.versionName) + '" AND cycleName in ("' + addSlashes(cycleName) + '") AND folderName is EMPTY ' + orderBy;
    }
    var zqlQuery = 'query=' + encodeURIComponent(queryParam) + '&offset=' + (parseInt(params.index) + 1) + '&pageWidth=' + params.paginationWidth;
    return params.url + '/secure/enav/#/' + execution.id + '?' + zqlQuery;
}
var executionsTableView;
var cycleInfoView;
var customMenu =function(node) {
    var nodeAlias = JSON.parse(JSON.stringify(node));
    var items = {};
    switch(node.a_attr.nodeType) {
        case 'version':
            items = {
                createCycle: {
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.createCycle'),
                    action: function(e) {
                        createCycle(nodeAlias);
                    }
                }
            };
            break;

        case 'cycle':
            items = {
                addTests: { // Add Tests to either cycle or folder based on nodeType
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.addTest'),
                    action: function (e) {
                        addTestsToCycle(e, nodeAlias)
                    }
                },
                createNode: { // The "add phase folder" menu item
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.addFolder'),
                    action: function (e) {
                        createNode(nodeAlias);
                    }
                },
                editCycle: { // The "edit cycle" menu item
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.editCycle'),
                    action: function () {
                        editCycle(nodeAlias);
                    }
                },
                deleteCycle : { // The "delete cycle" menu item
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.deleteCycle'),
                    action: function(e) {
                        deleteConfirmationDialog(nodeAlias);
                    }
                },
                cloneCycle : { // The "clone cycle" menu item
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.cloneCycle'),
                    action: function (e) {
                        cloneCycle(e, nodeAlias);
                    }
                },
                exportCycle: { // The "export cycle" menu item
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.exportCycle'),
                    action: function() {
                        exportNode(nodeAlias);
                    }
                },
                executionsByOrder: { // The "executions by order" menu item
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.executionsByOrder'),
                    separator_before: true,
                    action: function() {
                        ZEPHYR.Cycle.updateCycleExecutionsOnReorder(nodeAlias);
                    }
                },
                reorderExecutions: { // The "reorder executions" menu item
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.reorderExecutions'),
                    action: function(e) {
                        reorderExecutions(nodeAlias);
                    }
                },
                moveExecutionsToFolder: { // The "reorder executions" menu item
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.moveExecutions'),
                    action: function(e) {
                        moveExecutionsToFolderDialgoue(nodeAlias);
                    }
                }
            };
            break;

        case 'folder':
            items = {
                addTests: {
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.addTest'),
                    action: function (e) {
                        addTestsToCycle(e, nodeAlias);
                    }
                },
                editFolder: {
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.editFolder'),
                    action: function(e) {
                        editNode(nodeAlias);
                    }
                },
                deleteFolder: {
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.deleteFolder'),
                    action: function() {
                        deleteConfirmationDialog(nodeAlias);
                    }
                },
                cloneFolder: {
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.cloneFolder'),
                    action: function() {
                        cloneNode(nodeAlias);
                    }
                },
                exportFolder: {
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.exportFolder'),
                    action: function() {
                        exportNode(nodeAlias);
                    }
                },
                executionsByOrder: {
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.executionsByOrder'),
                    separator_before: true,
                    action: function() {
                        ZEPHYR.Cycle.updateCycleExecutionsOnReorder(nodeAlias);
                    }
                },
                reorderExecutions: {
                    label: AJS.I18n.getText('project.cycle.summary.contextMenu.reorderExecutions'),
                    action: function(e) {
                        reorderExecutions(nodeAlias);
                    }
                }
            };
            break;

        default:
            items = {};
            break;
    }

    if(node.a_attr.nodeType === 'cycle') {
        if(!node.a_attr.totalFolders || !node.a_attr.totalCycleExecutions) {
            delete items.moveExecutionsToFolder;

        }
        if(node.a_attr.totalCycleExecutions < 2) {
            delete items.executionsByOrder;
            delete items.reorderExecutions;
        }

        if(node.a_attr.cycleId == -1){
            delete items.createNode;
            delete items.editCycle;
            delete items.cloneCycle;
            delete items.deleteCycle;
            delete items.executionsByOrder;
            delete items.reorderExecutions;
            delete items.moveExecutionsToFolder;
        }

    }

    if(node.a_attr.nodeType === 'folder') {
        if(node.a_attr.totalExecutions < 2) {
            delete items.executionsByOrder;
            delete items.reorderExecutions;
        }

        if(node.a_attr.folderId == -2) {
            delete items.editFolder;
            delete items.cloneFolder;
            delete items.deleteFolder;
            delete items.executionsByOrder;
            delete items.reorderExecutions;
        }

    }

    return items;
}

var timewithyears = function(time){
    if (time=='') {
        return time;
    }
    var index = time.indexOf("w");
    if(index>-1){
        var weeks = time.slice(0, index);
        var year;
        if(weeks >= 52){
            time = time.slice(index+2, time.length);
            year = parseInt(weeks/52);
            weeks = weeks % 52;
            if(weeks){
                time = year+"y "+ weeks+"w "+ time;
            } else{
                time = year+"y "+ time;
            }
        }
    }
    return time;
}

var setCustomFieldsObjectsPlanCycle = function(allCustomFields) {
  ZEPHYR.Cycle.planCycleCFOrder = [];
  allCustomFieldsPC.forEach(function(field) {
    var orderObj = {
      "customfieldId": field.id,
      "customFieldType": field.fieldType,
      "customFieldName": field.name,
      "customDefaultValue": field.defaultValue
    }
    ZEPHYR.Cycle.planCycleCFOrder.push(orderObj);
  });
}

var getExecutionCustomizationPlanCycle = function(completed) {
  var xhr = {
      url: '/preference/getcyclesummarycustomization',
      method: 'GET'
  }
  xhrCall(xhr, function(response) {
      //console.log(response)
      Object.keys(response.preferences).forEach(function(key) {
        var obj = {
            "displayName": response.preferences[key].displayName,
            "isVisible": response.preferences[key].isVisible
        }
          ZEPHYR.Cycle.executionColumns[key] = obj;
      });
      if(Object.keys(response.preferences).length ===  8) {
        allCustomFieldsPC.forEach(function(field) {
          var obj = {
            "displayName": field.name,
            "isVisible": "false"
          }
          ZEPHYR.Cycle.executionColumns[field.id] = obj;
        });
      }

      if(completed)
       completed.call();

      //console.log(ZEPHYR.Cycle.executionColumns);
  });
}

ZEPHYR.Cycle.updateCycleExecutionsOnReorder = function(node) {
    var nodeObj = node.a_attr,
        cycleId, cycleObj, folderId;

    if(nodeObj.nodeType == 'cycle') {
        cycleObj = nodeObj;
        cycleId = nodeObj.cycleId;
        folderId = null;
    } else {
        cycleObj = zephyrjQuery3('#js-tree').jstree().get_node(node.parent).a_attr;
        cycleId = cycleObj.cycleId;
        folderId = nodeObj.folderId;
    }


    var limit   = 10,
        CONST   = ZEPHYR.Cycle.expandos(),
        action  = 'expand',
        pid     = '&projectId=' + AJS.$("#zprojectId")[0].value,
        vid     = '&versionId=' + AJS.$("#select-version2").val(),
        folderSuffix = folderId ? '&folderId=' + folderId : '',
        url;

        url     = getRestURL() + "/execution?cycleId=" + cycleId + folderSuffix + "&action=" + action + "&offset=0&sorter=OrderId:ASC";

    jQuery.ajax({
        url: url,
        data: CONST.requestParams + pid + vid,
        dataType: "json",
        error: function(xhr) {
            ZEPHYR.Cycle.permissionsCheck(xhr, null, url);
        },
        success: function (response, textStatus, xhr) {
            if (ZEPHYR.Cycle.permissionsCheck(xhr, response, url)) {
                AJS.$('li#cycle-' + cycleId + ' .versionBanner-content').find("tbody>tr").remove();
                for(var key in response.executions) {
                    var schedule        = response.executions[key],
                        allStatusesMap  = response.status,
                        recordsCount    = response.recordsCount;

                }
                //var labelCount =  1 + "-" + limit;
                delegateExecuteStatus(cycleId);
                executionsTableModelNew.set('currentOffset' , 0);
                executionsTableModelNew.set('executions' , response.executions);
            }
        }
    });
}

var xhrCall = function(xhr, successCallback, errorCallback) {
    jQuery.ajax({
        url: getRestURL() + xhr.url,
        type : xhr.method,
        accept: "application/json",
        contentType :"application/json",
        dataType: "json",
        data: JSON.stringify(xhr.data),
        success : function(response) {
            successCallback(response);
            if(xhr.method != 'GET' && !xhr.noSuccessMsg){
                showExecStatusSuccessMessage();
            }
            //console.log('saved successfully');
        },
        error : function(xhr, status, error) {
            if(xhr.status !== 403)
                errorCallback && errorCallback(xhr);
            //console.log('error', xhr, error);
        },
        statusCode: {
            403: function(xhr, status, error) {
                errorCallback(xhr);
            }

        }
    });
}

var returnNodeParams = function(node) {
    var nodeObj = node.a_attr,
        cycleObj, cycleId;
    if(nodeObj.nodeType == 'cycle') {
        cycleObj = nodeObj;
        cycleId = nodeObj.cycleId;
        folderId = null;
        folderObj = null;

    } else {
        cycleObj = zephyrjQuery3('#js-tree').jstree().get_node(node.parent).a_attr;
        cycleId = cycleObj.cycleId;
        folderId = nodeObj.folderId;
        folderObj = nodeObj;
    }

    return  {
        nodeObj: nodeObj,
        cycleObj: cycleObj,
        cycleId: cycleId,
        folderId: folderId,
        folderObj: folderObj,
        projectId: cycleObj.projectId,
        versionId: cycleObj.versionId,
        sortQuery : cycleObj.sortQuery,
        soffset : cycleObj.soffset
    }
}

var exportNode = function(node) {
    var nodeParams = returnNodeParams(node);

    //for analytics
    var zaObj = {
        'event': ZephyrEvents.EXPORT_FOLDER,
        'eventType': 'Click',
        'cycleId': nodeParams.cycleId,
        'projectId': nodeParams.projectId,
        'versionId': nodeParams.versionId,
        'folderId': nodeParams.folderId,
        'sortQuery' : nodeParams.sortQuery
    };
    if (za != undefined){
        za.track(zaObj, function(){
            //res
        })
    }

    var elID = ('-projectId-' + nodeParams.projectId);
    var url = "/cycle/" + nodeParams.cycleId + "/export?projectId=" + nodeParams.projectId + "&versionId="+nodeParams.versionId;
    if(nodeParams.folderId) {
        url += "&folderId=" + nodeParams.folderId;
    }
    if(nodeParams.sortQuery){
    	url += "&sorter=" + nodeParams.sortQuery;
    }
    AJS.$('#cycle-wrapper-' + nodeParams.cycleId).show();
    ZEPHYR.Cycle.attachPermissionActiveClass(elID);
    AJS.$('.aui-blanket').css({'visibility': 'hidden'});
    var xhr = {
        url: url,
        method: "GET"
    };

    xhrCall(xhr, function(response) {
        AJS.$('#csvDownloadFrame').attr('src',response.url);
        AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
    }, function(response,jqXHR) {
        showZQLError(jqXHR);
        AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
    });
}

var editNode = function(node){
    var nodeParams = returnNodeParams(node);

    zephyrjQuery3('#js-tree').jstree(true).edit(nodeParams.nodeObj, nodeParams.nodeObj.folderName, function(node){

        //for analytics
        var zaObj = {
            'event': ZephyrEvents.UPDATE_FOLDER,
            'eventType': 'Blur',
            'cycleId': nodeParams.cycleId,
            'projectId': nodeParams.projectId,
            'versionId': nodeParams.versionId,
            'folderId': nodeParams.folderId
        };
        if (za != undefined){
            za.track(zaObj, function(){
                //res
            })
        }

        var xhr = {
            url: "/folder/" + nodeParams.folderId,
            method: "PUT",
            data: {
                  'name' : node.text.trim(),
                  'cycleId': nodeParams.cycleId,
                  'projectId': nodeParams.projectId,
                  'versionId': nodeParams.versionId
            },
            noSuccessMsg: true
        };
        xhrCall(xhr, function(response) {
            var parentNode = zephyrjQuery3('#js-tree').jstree().get_node(node.parent);
            zephyrjQuery3("#js-tree").jstree(true).refresh_node(parentNode);
            if(!editSameName)
              showExecStatusSuccessMessage(htmlEncode(response.responseMessage));
            else
              editSameName = false;
        }, function(xhrError) {
            if(xhrError.status !== 403){
                editSameName = true;
                editNode(node);
            } else {
                refreshTree();
            }
            //var message = xhrError.responseJSON && xhrError.responseJSON.error;
            var message;
            try {
                message = xhrError.responseText;
                message = message && JSON.parse(message) && JSON.parse(message).error;
            } catch(err) {
                message = xhrError.responseText;
            }
            showErrorMessage(message, 5000);
        });
    })
};

var cloneNode = function(node) {

    var nodeParams = returnNodeParams(node);

    var clonedFolderId = nodeParams.nodeObj.folderId;
    var cycleId = nodeParams.cycleId;
    var projectId = nodeParams.projectId;
    var versionId = nodeParams.versionId;

    ZEPHYR.Cycle.CycleFunction.cloneFolderDialog(clonedFolderId, cycleId, projectId, versionId, nodeParams.nodeObj.folderName, function (response) {
        triggerTreeRefresh();
    });
}

var createNode = function(cycleNode){
    var cycleObj = cycleNode.a_attr
        cycleId = cycleNode.a_attr.cycleId;

    zephyrjQuery3('#js-tree').jstree("create_node", cycleObj.id, null, "first", function (node) {
        this.edit(node, null, function() {

            //for analytics
            var zaObj = {
                'event': ZephyrEvents.ADD_FOLDER,
                'eventType': 'Blur',
                'cycleId': cycleObj.cycleId,
                'projectId': cycleObj.projectId,
                'versionId': cycleObj.versionId
            };
            if (za != undefined){
                za.track(zaObj, function(){
                    //res
                })
            }

            var xhr = {
                url: "/folder/create",
                method: "POST",
                data: {
                      'name' : node.text.trim(),
                      'cycleId': cycleId,
                      'projectId': cycleObj.projectId,
                      'versionId': cycleObj.versionId
                },
                noSuccessMsg: true
            }

            xhrCall(xhr, function(response) {
                triggerTreeRefresh();
                showExecStatusSuccessMessage(htmlEncode(response.responseMessage));
            }, function(xhrError){
                var message;
                try {
                    message = xhrError.responseText;
                    message = message && JSON.parse(message) && JSON.parse(message).error;
                } catch(err) {
                    message = xhrError.responseText;
                }
                showErrorMessage(message, 5000);
                refreshTree();
            })
        });
    });
}
/*In lack of model layer, persisting data by passing thro' functions*/
var deleteConfirmationDialog = function (node) {

    var nodeParams = returnNodeParams(node);
        nodeParams.nodeObj.name =  !nodeParams.folderId ? nodeParams.nodeObj.name : nodeParams.nodeObj.folderName;


    var instance = this;
    var dialog = new JIRA.FormDialog({
        id: (!nodeParams.folderId ? "cycle-" + nodeParams.cycleId : "folder-" + nodeParams.folderId) + "-delete-dialog",
        content: function (callback) {
            /*Short cut of creating view, move it to Backbone View and do it in render() */
            var innerHtmlStr = ZEPHYR.Project.Cycle.deleteCycleConfirmationDialog({node:nodeParams.nodeObj});
            callback(innerHtmlStr);
        },

        submitHandler: function (e) {
            deleteNode(nodeParams.cycleId, nodeParams.folderId, nodeParams.projectId, nodeParams.versionId, dialog, function () {
                dialog.hide();
                triggerTreeRefresh(null, 'delete');
            });
            e.preventDefault();
        }
    });

    dialog.show();
}

var deleteNode = function(cycleId, folderId, projectId, versionId, deletionDialog, completed){
    var elID = ('-project-' + projectId);
    AJS.$('#cycle-wrapper-' + cycleId).show();

    var zaObj = {
        'event': ZephyrEvents.DELETE_CYCLE,
        'eventType': 'Click',
        'projectId': projectId,
        'versionId': versionId,
        'cycleId': cycleId,
        'folderId': folderId
    };

    //analytics
    if (za != undefined){
        za.track(zaObj, function(){
            //res
        })
    }

    ZEPHYR.Cycle.attachPermissionActiveClass(elID);
    AJS.$('.aui-blanket').css({'visibility': 'hidden'});
    var urlSuffix = "/cycle/" + cycleId;
    urlSuffix = folderId ? "/folder/" + folderId + "?cycleId=" + cycleId + "&projectId=" + projectId + "&versionId=" + versionId: urlSuffix;
    jQuery.ajax({
        url: getRestURL() + urlSuffix,
        type : "delete",
        contentType :"application/json",
        dataType: "json",
        success : function(response) {
            var msg = !folderId ? AJS.I18n.getText('zephyr.je.cycle.delete.in.progress') : AJS.I18n.getText('zephyr.je.folder.delete.in.progress');
            var auiclass = 'info';
            if (response != null && response.PERM_DENIED){
                msg = response.PERM_DENIED;
                auiclass = 'error';
            }
            /*Full server refresh - or shall we just remove the corresponding cycle div?*/
            var jobProgressToken = response.jobProgressToken;
            if(response != null) {
                var msgDlg = new JIRA.FormDialog({
                    id: "warning-message-dialog",
                    content: function (callback) {
                        var innerHtmlStr = ZEPHYR.Project.Cycle.deleteCycleConfirmatedDialog({
                            warningMsg:msg,
                            progress:0,
                            percent:0,
                            auiclass:auiclass,
                            timeTaken:0,
                            isFolder: folderId
                        });
                        callback(innerHtmlStr);
                    },
                });
                msgDlg.show();
                AJS.$("#cycle-delete-form-submit, #unresolved-ignore-label").remove();
                AJS.$(".cycle-delete").html(AJS.I18n.getText('zephyr-je.close.link.title'));
                if (response.PERM_DENIED){
                    AJS.$(".timeTaken, .aui-progress-indicator ").remove();
                    return;
                }
            }
            var intervalId = setInterval(function(){
                jQuery.ajax({url: contextPath + "/rest/zephyr/latest/execution/jobProgress/"+jobProgressToken,
                    data: {'type':"cycle_delete_job_progress"}, complete:function(jqXHR, textStatus){
                        if (jqXHR != undefined && jqXHR != null) {
                            var data = jQuery.parseJSON(jqXHR.responseText);
                            AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
                            AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
                            AJS.$(".timeTaken").html(AJS.I18n.getText('zephyr.je.cycle.timeTaken') + ": " + data.timeTaken);
                            var errMsg = ((data.errorMessage != undefined && data.errorMessage.length > 0) ? data.errorMessage: null);
                            if (errMsg != null){
                                AJS.$("#cycle-delete-aui-message-bar .aui-message").html(errMsg);
                                AJS.$(".timeTaken, .aui-progress-indicator ").remove();
                                clearInterval(intervalId);
                            }

                            if(data.progress == 1 && errMsg == null) {
                                if (data.message) {
                                    var message = JSON.parse(data.message);
                                    var msg = message.success ? message.success: message.error;
                                    AJS.$("#cycle-delete-aui-message-bar .aui-message").html(msg);
                                }
                                clearInterval(intervalId);
                            }
                            if(completed)
                                completed.call();
                            AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
                        }
                    }
                })
            }, 1000);

        },
        error : function(response) {
            if(folderId && response.status === 403)
                return;
            var cxt = AJS.$("#cycle-aui-message-bar");
            cxt.empty();
            AJS.messages.error(cxt, {
                title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
                body: response.responseText && JSON.parse(response.responseText).error,
                closeable: false
            });
            AJS.$(':submit', deletionDialog.$form).removeAttr('disabled');
            AJS.$(".loading, .aui-progress-indicator ").remove();
            deletionDialog.$form.removeClass("submitting");
            AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
        },
        statusCode: {
            403: function(xhrError){
                var message = xhrError.responseJSON && xhrError.responseJSON.error;
                showErrorMessage(message, 5000);
                deletionDialog.hide();
            }
        }
    });
}

var editCycle = function(node){
    var nodeParams = returnNodeParams(node);

    editCycleDialog(nodeParams.projectId, ZEPHYR.Cycle.versionList, nodeParams.cycleId, nodeParams.cycleObj, ZEPHYR.Cycle.sprintList, function(response) {
        //console.log('after cycle Edit',nodeParams, response);
        triggerTreeRefresh('version-'+response.versionId);
    });
    return;
}
var submitButtonId = 'submitExecutionColumns';
var closeButtonId = 'closeInlineDialog';

AJS.$('#inline-dialog-execution-column-picker').live("click", function(e) {
   e.stopPropagation();
});

AJS.$('body').live("click", function(e){
    InlineDialog && InlineDialog.hide();
});

AJS.$('#closeInlineDialog').live('click', function(){
    InlineDialog.hide();
});

AJS.$('#' + submitButtonId).live('click', function() {
    var params = {
        url: '/preference/setcyclesummarycustomization',
        method: 'POST',
        data: {}
    }
    AJS.$('#inline-dialog-execution-column-picker li :checkbox').each(function() {
        if(this.checked) {
          ZEPHYR.Cycle.executionColumns[this.id].isVisible = "true";
  			}	else {
  				ZEPHYR.Cycle.executionColumns[this.id].isVisible = "false"
  			}
    });
    params.data['preferences'] = ZEPHYR.Cycle.executionColumns;

    executionsTableModelNew.set('colums', ZEPHYR.Cycle.executionColumns);
    executionsTableModelNew.set('emptyExecutions' , !executionsTableModelNew.get('emptyExecutions'));

    xhrCall(params, function(response){
        InlineDialog.hide();
    })
});

var checkMoveFormValidity = function(executionSelected) {
    if(AJS.$('#allExecutions')[0].checked) {
        AJS.$('#cycle-move-executions-form-submit').removeAttr('disabled');
    } else {
        if (executionSelected) {
            AJS.$('#cycle-move-executions-form-submit').removeAttr('disabled');
        } else {
            AJS.$('#cycle-move-executions-form-submit').attr('disabled', true);
        }
    }
}

AJS.$('#selectAllExecutions').live('click',function(event) {
    if(this.checked) {
        AJS.$('#moveExecutionsTable tbody :checkbox').each(function() {
            this.checked = true;
            if(ZEPHYR.Cycle.scheduleList.indexOf(this.name) == -1){
                ZEPHYR.Cycle.scheduleList.push(this.name);
            }

        });
    } else {
        AJS.$('#moveExecutionsTable tbody :checkbox').each(function() {
            this.checked = false;
            var index = ZEPHYR.Cycle.scheduleList.indexOf(this.name);
            if(index > -1) {
                ZEPHYR.Cycle.scheduleList.splice(index, 1);
            }
        });

    }
    checkMoveFormValidity();
    //console.log('scheduleList', ZEPHYR.Cycle.scheduleList);
});
AJS.$('#moveExecutionsTable tbody :checkbox').live('click', function(){
    if(this.checked) {
        ZEPHYR.Cycle.scheduleList.push(this.name);
    } else {
        var index = ZEPHYR.Cycle.scheduleList.indexOf(this.name);
        if(index > -1) {
            ZEPHYR.Cycle.scheduleList.splice(index, 1);
        }
    }
    //console.log('scheduleList', ZEPHYR.Cycle.scheduleList);
    checkMoveFormValidity();
});

AJS.$('#allExecutions').live('click', function(){
    if(this.checked) {
        AJS.$('#moveExecutionsTables').addClass('disabled');
    } else {
        AJS.$('#moveExecutionsTables').removeClass('disabled');
    }
    checkMoveFormValidity();
});

AJS.$('#backButton').live('click', function(){
    if(AJS.$('.tree-tcr').hasClass('collapse')) {
        AJS.$('#tree-docker').trigger('click');
    }
    AJS.$('#tree-docker').show();
    AJS.$('#cycle-executions-wrapper').removeClass('hide');
    AJS.$('#cycle-info-wrapper').removeClass('hide');
    // AJS.$('.executionSummaryBar').removeClass('hide');
    AJS.$('.execution-details').addClass('hide');
    AJS.$('#detailViewBtn').removeClass('active-view');
    AJS.$('#listViewBtn').addClass('active-view');
    var breadcrumbTemplate = ZEPHYR.Templates.PlanCycle.breadCrumbsView();
    AJS.$('#breadcrumbs-wrapper').html(breadcrumbTemplate);
    triggerTreeRefresh();
    var query = updateLocationHash('list');
    updateRouter(query);
    initialTreeLoad = true;
    //highlightRow();
    setTimeout(function() {
        highlightRow();
    }, 500)

});

AJS.$('#listViewBtn').live('click', function(){
    if (!AJS.$('#listViewBtn').hasClass('active-view') && isButtonDisabled != true) {
        if(AJS.$('.tree-tcr').hasClass('collapse')) {
            AJS.$('#tree-docker').trigger('click');
        }
        AJS.$('#tree-docker').show();
        AJS.$('#cycle-executions-wrapper').removeClass('hide');
        AJS.$('#cycle-info-wrapper').removeClass('hide');
        // AJS.$('.executionSummaryBar').removeClass('hide');
        AJS.$('.execution-details').addClass('hide');
        AJS.$('#detailViewBtn').removeClass('active-view');
        AJS.$('#listViewBtn').addClass('active-view');
        var breadcrumbTemplate = ZEPHYR.Templates.PlanCycle.breadCrumbsView();
        AJS.$('#breadcrumbs-wrapper').html(breadcrumbTemplate);
        triggerTreeRefresh();

        isButtonDisabled = true;
        setTimeout(function () {
            isButtonDisabled = false;
            highlightRow();
        }, 1000);
        var query = updateLocationHash('list');

        updateRouter(query);
        initialTreeLoad = true;


    }
});

AJS.$('#detailViewBtn').live('click', function(){
    if (!AJS.$('#detailViewBtn').hasClass('active-view') && isButtonDisabled!= true) {
        if(!(AJS.$('.tree-tcr').hasClass('collapse'))) {
            AJS.$('#tree-docker').trigger('click');
        }
        isDetailedViewClicked = true;
        detailView(selectedExecutionId);

        isButtonDisabled = true;
        setTimeout(function () {
            isButtonDisabled = false;
        }, 1000);
        var query = updateLocationHash('detail', selectedExecutionId);
        updateRouter(query);
    }
});

var fetchGridToMove = function(nodeParams) {
    var nodeDetails = new ZEPHYR.Cycle.cycleDetails({
        cycleId: nodeParams.cycleId,
        projectId : nodeParams.projectId,
        versionId : nodeParams.versionId,
        sortQuery : nodeParams.sortQuery || 'OrderId:ASC',
        soffset : parseInt(nodeParams.soffset) || 0,
        folderId : nodeParams.folderId,
        limit: parseInt(nodeParams.pageWidth),
    });
    var offset = parseInt(nodeParams.soffset) || 0;
    var pageWidth = parseInt(nodeParams.pageWidth);
    var statusColName = AJS.I18n.getText('project.cycle.schedule.table.column.status');
    nodeDetails.fetch({
        success: function(resp) {
            // var executionsMoveTableView = new ZEPHYR.Cycle.ExecutionTable({
            //     el : '#moveExecutionsTable',
            //     model : executionsMoveGrid
            // });
            // resp.forEach(function(model) {
            //     executionsMoveGrid.set('allStatusList' , model.get('status'));
            //     executionsMoveGrid.set('totalCount' , model.get('recordsCount'));
            //     executionsMoveGrid.set('currentOffset' , nodeParams.soffset || 0);
            //     executionsMoveGrid.set('executions' , model.get('executions'));
            //     executionsMoveGrid.set('isCheckbox' , true);
            //     executionsMoveGrid.set('totalChecked', false);
            //     executionsMoveGrid.set('compareList', ZEPHYR.Cycle.scheduleList);
            //     executionsMoveGrid.set('emptyExecutions' , !executionsMoveGrid.get('emptyExecutions'));
            // });

            if (!(allPagesPaginationPageWidth.moveExecution >= 0)) {
                allPagesPaginationPageWidth.moveExecution = 10;
            }
            totalPages = Math.ceil((resp.models[0].attributes.totalExecutions) / allPagesPaginationPageWidth.moveExecution);
            selectedPage = parseInt(offset / allPagesPaginationPageWidth.moveExecution) + 1;
            currentMoveExecutionPageNumber = selectedPage - 1;
            var innerHtmlStr = ZEPHYR.Project.Cycle.moveExecutionsPagination({
                totalPages: totalPages,
                selectedPage: selectedPage,
                entriesCount: allPagesPaginationPageWidth.moveExecution,
                totalCount: resp.models[0].attributes.totalExecutions,
            });
            createMoveExecutionGrid(resp.models[0].attributes.executions, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder);
            AJS.$('#moveExecutionPagination').html(innerHtmlStr);
            // executionsMoveTableView.render();
        }
    });

}

var moveExecutionsToFolderDialgoue = function (node) {

    var nodeParams = returnNodeParams(node);
    nodeParams.pageWidth = allPagesPaginationPageWidth.moveExecution;
    moveExecutionNodeParam = nodeParams;
    moveExecutionSelectedId = [];
    vanillaGrid.utils.selectedExecutionIds['moveExecutionGrid'] = [];
    dialog = new JIRA.FormDialog({
        id: "move-executions-to-folder-dialog",
        widthClass: "large",
        content: function (callback) {
                var req = jQuery.ajax({
                    url:  getRestURL() + '/cycle/'+ nodeParams.cycleId +'/folders?projectId='+nodeParams.projectId+'&versionId='+nodeParams.versionId+'&limit=1000&offset=0',
                    type : "GET",
                    contentType :"application/json",
                    dataType: "json",
                    success : function(data) {
                        ZEPHYR.Cycle.scheduleList = [];
                        var folders = data.map(function(folder) {
                            folder.folderName = folder.folderName;
                            return folder;
                        });
                        //console.log('folders data', folders);
                        nodeParams.nodeObj.name = nodeParams.nodeObj.name;
                        var innerHtmlStr = ZEPHYR.Project.Cycle.moveExecutionsToFolderDialgoue({
                            node:nodeParams,
                            folders : folders,
                        });
                        callback(innerHtmlStr);
                    },
                    error: function(response) {
                            // var dialog = new AJS.Dialog({
                            //     width:540,
                            //     height: 200,
                            //     id: "dialog-error"
                            // });
                            // dialog.addHeader(AJS.I18n.getText('zephyr.je.submit.form.error.title'));

                            // dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
                            // AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
                            //     body: AJS.I18n.getText('zapi.login.error'),
                            //     closeable: false
                            // });

                            // dialog.addLink("Close", function (dialog) {
                            //     dialog.hide();
                            // }, "#");
                            // dialog.show();
                            var message = response.error;
                            showErrorMessage(message, 5000);
                    }
                    // statusCode: {
                    //     403: function(xhr, status, error) {
                    //         dialog.hide();
                    //     }
                    // }
                });
        },

        submitHandler: function (e) {
            moveExecutionsToFolderAPI(this.$form.serializeArray(), nodeParams.nodeObj , node ,function () {
                refreshTree();
            });
            e.preventDefault();
        }
    });
    dialog.show();
}

var refreshTreeAfterMoveExecutions = function (node , nodeObject) {
    var url  = getRestURL() + '/cycle/'+ nodeObject.cycleId +'/folders?limit=1000&offset=0';
}

var moveExecutionsToFolderAPI = function (formValues , nodeObj , node, callback) {
    var formToSubmit = {};
    if(!formValues.length) {
        return;
    }
    formValues.forEach(function(entry) {
        if(entry.name === 'select-folder' || entry.name === 'allExecutions') {
            formToSubmit[entry.name] = entry.value === 'on' ? true : entry.value === 'off' ? false : parseInt(entry.value);
        }
    })
    jQuery.ajax({
        url: getRestURL() + '/cycle/' + nodeObj.cycleId + '/move/executions/folder/' + formToSubmit['select-folder'],
        type : "PUT",
        contentType :"application/json",
        dataType: "json",
        data: JSON.stringify( {
              'projectId' : nodeObj.projectId,
              'versionId' : nodeObj.versionId,
              'schedulesList': formToSubmit['allExecutions'] ? [] : moveExecutionSelectedId
        }),
        success : function(response) {
                var jobProgressToken = response.jobProgressToken;
                if(response != null) {
                    var msgDlg = new JIRA.FormDialog({
                        id: "warning-message-dialog",
                        content: function (callback) {
                            var innerHtmlStr = ZEPHYR.Project.Cycle.warningDialogContent({
                                warningMsg:AJS.I18n.getText('zephyr.move.executions.cycle.folder.in.progress'),
                                progress:0,
                                percent:0,
                                timeTaken:0,
                                title: AJS.I18n.getText('folder.operation.move.execuitons.heading')
                            });
                            callback(innerHtmlStr);
                        },
                    });
                    msgDlg.show();
                }

                var intervalId = setInterval(function(){
                    jQuery.ajax({url: contextPath + "/rest/zephyr/latest/execution/jobProgress/"+jobProgressToken,
                        data: {'type':"add_tests_to_cycle_job_progress"}, complete:function(jqXHR, textStatus){
                            var data = jQuery.parseJSON(jqXHR.responseText);
                            AJS.$(".aui-progress-indicator").attr("data-value",data.progress);
                            AJS.$(".aui-progress-indicator-value").css("width",data.progress*100+"%");
                            AJS.$(".timeTaken").html("Time Taken: "+data.timeTaken);
                            var errMsg = ((data.errorMessage != undefined && data.errorMessage.length > 0) ? data.errorMessage: null);
                            if (errMsg != null){
                                AJS.$("#cycle-aui-message-bar .aui-message").html(errMsg);
                                AJS.$(".timeTaken, .aui-progress-indicator ").remove();
                                clearInterval(intervalId);
                            }
                            if(data.progress == 1 && errMsg == null) {
                                AJS.$("#cycle-aui-message-bar .aui-message").html(data.message);
                                clearInterval(intervalId);
                                //refreshTreeAfterMoveExecutions(node , nodeObj);
                                if(callback){
                                    callback.call();
                                }
                            }
                        }
                    })
                }, 1000);

        },
        error : function(response) {
            /*var dialog = new AJS.Dialog({
                width:540,
                height: 200,
                id: "dialog-error"
            });
            dialog.addHeader(AJS.I18n.getText('zephyr.je.submit.form.error.title'));

            dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
            AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
                body: AJS.I18n.getText('zapi.login.error'),
                closeable: false
            });

            dialog.addLink("Close", function (dialog) {
                dialog.hide();
            }, "#");
            dialog.show();*/
            var message = JSON.parse(response.responseText).error;
            showErrorMessage(message, 5000);
        }
    });
}

/*In lack of model layer, persisting data by passing thro' functions*/
var editCycleDialog = function (projectId,versionList,cycleId,cycleObj,sprintList, callback) {

    dialog = new JIRA.FormDialog({
        id: "edit-cycle-dialog",
        content: function (callback) {
            /*Short cut of creating view, move it to Backbone View and do it in render() */
            cycleObj.name = cleanGarb(cycleObj.name);
            cycleObj.build = cycleObj.build;
            cycleObj.environment = cycleObj.environment;
            cycleObj.description = cycleObj.description;
            var innerHtmlStr = ZEPHYR.Project.Cycle.editCycleDialog({projectId:projectId,versions:versionList,cycle:cycleObj, sprints: []});
            callback(innerHtmlStr);
            new AJS.SingleSelect({
                element: AJS.$('#cycle_sprint'),
                maxInlineResultsDisplayed: 15,
                maxWidth: 250
            });
        },

        submitHandler: function (e) {
            // Attach Loading icon
            dialog.$popupContent.find('.buttons-container').append('<span class="icon throbber loading dialog-icon-wait dialog-icon-wait-top">' + AJS.I18n.getText('zephyr.gridData.loading') + '</span>');
            saveEditCycle(projectId, cycleId, cycleObj.sprintId, function (response) {
                dialog.hide();
                if(callback) {
                    callback.call(null, response);
                }
            });
            e.preventDefault();
        }
    });
    dialog.show();
}

var saveEditCycle = function(projectId, cycleId, sprintId, completed) {
    var elID = ('-project-' + projectId);
    AJS.$('#cycle-wrapper-' + cycleId).show();

    //for analytics
    var zaObj = {
        'event': ZephyrEvents.UPDATE_CYCLE,
        'eventType': 'Click',
        'cycleId': cycleId,
        'projectId': projectId,
        'versionId': AJS.$('#cycle_version').val()
    };
    if (za != undefined){
        za.track(zaObj, function(){
            //res
        })
    }

    AJS.$('.aui-blanket').css({'visibility': 'hidden'});
    ZEPHYR.Cycle.attachPermissionActiveClass(elID);
    var payload = {
            id : cycleId,
            name : AJS.$('#cycle_name').val(),
            build : AJS.$('#cycle_build').val(),
            environment : AJS.$('#cycle_environment').val(),
            description : AJS.$('#cycle_description').val(),
            startDate : AJS.$('#cycle_startDate').val(),
            endDate : AJS.$('#cycle_endDate').val(),
            projectId : projectId,
            versionId: AJS.$('#cycle_version').val(),
            sprintId: sprintId,
            issueId: AJS.$("#issueId").val()
        };
    jQuery.ajax({
        url: getRestURL() + "/cycle",
        type : "PUT",
        accept: "PUT",
        contentType :"application/json",
        dataType: "json",
        data: JSON.stringify( {
              'id' : payload.id,
              'name' : payload.name,
              'build' :  payload.build,
              'environment': payload.environment,
              'description' : payload.description,
              'startDate': payload.startDate,
              'endDate': payload.endDate,
              'projectId': payload.projectId,
              'versionId': payload.versionId,
              'sprintId': payload.sprintId,
              'issueId': payload.issueId
        }),
        success : function(response) {
            selectedVersionChanged();
            if(completed)
                completed.call(null, payload);
            AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
            showExecStatusSuccessMessage(htmlEncode(response.responseMessage));

        },
        error : function(response) {
            validateCycleInput(response);
            AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
        }
    });
}

var addTestsToCycle = function(e, node){
    var nodeParams = returnNodeParams(node);

    ZEPHYR.Dialogs.createAddTestsDialog(e, nodeParams.projectId, nodeParams.cycleId, nodeParams.cycleObj, nodeParams.folderObj, function() {
        triggerTreeRefresh();
    });

    //e.preventDefault();
    return;
}

var cloneCycle = function(event, cycleNode){

    var projectId = AJS.$("#zprojectId")[0].value,
        cycleObj = cycleNode.a_attr,
        cycleId = cycleNode.a_attr.cycleId;

    //for analytics
    var zaObj = {
        'event': ZephyrEvents.CLONE_CYCLE,
        'eventType': 'Click',
        'cycleId': cycleId,
        'projectId': projectId,
        'versionId': cycleObj.versionId,
        'folderId': cycleObj.folderId
    };
    if (za != undefined){
        za.track(zaObj, function(){
            //res
        })
    }

    AJS.$("#select-version2").val(cycleObj.versionId);
    var lastSelectedVersion = AJS.$("#select-version2 :selected").val();
    var cycleObjClone = jQuery.extend({}, cycleObj);
    cycleObjClone.id = cycleId;
    cycleObjClone.name = AJS.I18n.getText('teststep.operation.clone.step.prefix').toUpperCase() + " - " + cycleObjClone.name;
    cycleObjClone.build = cycleObjClone.build;
    cycleObjClone.environment = cycleObjClone.environment;
    cycleObjClone.description = cycleObjClone.description;
    ZEPHYR.Cycle.CycleFunction.createCycleDialog(projectId, ZEPHYR.Cycle.versionList, lastSelectedVersion.toString(), cycleObjClone, cycleNode, function(response) {
        var parentVersion = cycleNode.parent;
        if(response.versionId) {
          var parentVersion = 'version-' + response.versionId;
        }
        var parentNode = zephyrjQuery3('#js-tree').jstree().get_node(parentVersion);
        zephyrjQuery3("#js-tree").jstree(true).refresh_node(parentNode);
    });
    //event.preventDefault();
    return;
}

/*Caches and creates Cycle Dialog*/
var createCycle = function (node) {
    var nodeObj = node.a_attr;
    AJS.$("#select-version2").val(nodeObj.versionId);
    var projectId = AJS.$("#zprojectId").val();
    var lastSelectedVersion = AJS.$("#select-version2 :selected").val();
    ZEPHYR.Cycle.CycleFunction.createCycleDialog(projectId, ZEPHYR.Cycle.versionList,lastSelectedVersion.toString(), null, nodeObj, function(response) {
        if(!zephyrjQuery3('#js-tree').jstree(true).is_open(nodeObj)) {
            zephyrjQuery3('#js-tree').jstree(true).toggle_node(nodeObj)
        } else {
            zephyrjQuery3('#js-tree').jstree("create_node", nodeObj.id, null, "first", function (node) {
                //console.log('successfully created node');
                var parentNode = zephyrjQuery3('#js-tree').jstree().get_node(node.parent);
                zephyrjQuery3("#js-tree").jstree(true).refresh_node(parentNode);

            });
        }
        if(!(response.status === 403)) {
          showExecStatusSuccessMessage(htmlEncode(response.responseMessage));
        }
    });
    //window.event.preventDefault();
    return;
}

ZEPHYR.Cycle.CycleFunction = {
    /*In lack of model layer, persisting data by passing thro' functions*/
    cloneFolderDialog: function (clonedFolderId, cycleId, projectId, versionId, folderName, callback) {
        dialog = new JIRA.FormDialog({
            id: "clone-folder-dialog",
            content: function (callback) {
                /*Short cut of creating view, move it to Backbone View and do it in render() */
                var innerHtmlStr = ZEPHYR.Project.Cycle.cloneFolderDialog({ folderName: folderName });
                callback(innerHtmlStr);
            },

            submitHandler: function (e) {

                // Attach Loading icon
                var newFolderName = AJS.$('#folder_name')[0].value;
                var cloneCustomFields = AJS.$('#clone_customFields')[0].checked;
                dialog.$popupContent.find('.buttons-container').append('<span class="icon throbber loading dialog-icon-wait dialog-icon-wait-top">' + AJS.I18n.getText('zephyr.gridData.loading') + '</span>');
                ZEPHYR.Cycle.CycleFunction.createFolder(clonedFolderId, cycleId, projectId, versionId, newFolderName, cloneCustomFields, function (response) {
                    dialog.hide();
                    if (callback) {
                        callback.call(null, response);
                    }
                });
                e.preventDefault();
            }
        });
        dialog.show();
    },
    createFolder: function (clonedFolderId, cycleId, projectId, versionId, folderName, cloneCustomFields, completed) {

        var intervalId;
        AJS.$(document).off('click', '#aui-dialog-close');
        AJS.$(document).on('click', '#aui-dialog-close', function () {
            if (!isCloneFolderFail) {
                selectedVersionChanged();
                clearInterval(intervalId);
            } else {
                isCloneFolderFail = false;
            }
        })

        jQuery.ajax({
            url: getRestURL() + "/folder/create",
            type: "post",
            contentType: "application/json",
            data: JSON.stringify({
                'clonedFolderId': clonedFolderId,
                'cycleId': cycleId,
                'name': folderName,
                'projectId': projectId,
                'versionId': versionId,
                'cloneCustomFields': cloneCustomFields,
            }),
            success: function (response) {

                var jobProgressToken = response.jobProgressToken;
                if (response != null) {
                    var msgDlg = new JIRA.FormDialog({
                        id: "warning-message-dialog",
                        content: function (callback) {
                            var innerHtmlStr = ZEPHYR.Project.Cycle.warningDialogContent({
                                title: AJS.I18n.getText('project.cycle.summary.contextMenu.cloneFolder'),
                                warningMsg: AJS.I18n.getText('zephyr.je.clone.folder.in.progress'),
                                progress: 0,
                                percent: 0,
                                timeTaken: 0
                            });
                            callback(innerHtmlStr);
                        },
                    });
                    msgDlg.show();
                }

                intervalId = setInterval(function () {
                    jQuery.ajax({
                        url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + jobProgressToken,
                        data: { 'type': "add_tests_to_cycle_job_progress" }, complete: function (jqXHR, textStatus) {
                            var data = jQuery.parseJSON(jqXHR.responseText);
                            if (data.progress == 0 && data.message != '') {
                                isCloneFolderFail = true;
                                clearInterval(intervalId);
                                AJS.$('#aui-dialog-close').click();
                                showErrorMessage(data.message);
                            } else {
                                AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
                                AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
                                AJS.$(".timeTaken").html(AJS.I18n.getText('zephyr.je.cycle.timeTaken') + ": " + data.timeTaken);
                                var errMsg = ((data.errorMessage != undefined && data.errorMessage.length > 0) ? data.errorMessage : null);
                                if (errMsg != null) {
                                    AJS.$("#cycle-aui-message-bar .aui-message").html(errMsg);
                                    AJS.$(".timeTaken, .aui-progress-indicator ").remove();
                                    clearInterval(intervalId);
                                    selectedVersionChanged();
                                }
                                if (data.progress == 1 && errMsg == null) {
                                    AJS.$("#cycle-aui-message-bar .aui-message").html(htmlEncode(data.message));
                                    clearInterval(intervalId);
                                    selectedVersionChanged();
                                }
                                if (completed)
                                completed.call(null, JSON.parse(jqXHR.responseText).message);
                            }
                        }
                    })
                }, 1000);
            },
            error: function (response) {
                validateCycleInput(response);
                AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');

            },
            statusCode: {
                403: function (xhr, status, error) {
                    if (completed) {
                        completed.call(null, xhr);
                    }
                }
            }
        });
    },
    createCycleDialog : function (projectId,versionList,lastvisitedVersion, cycleBeingCloned, cycleNode, callback) {
        dialog = new JIRA.FormDialog({
            id: "create-cycle-dialog",
            content: function (callback) {
                /*Short cut of creating view, move it to Backbone View and do it in render() */
                var innerHtmlStr = ZEPHYR.Project.Cycle.createCycleDialog({projectId:projectId,versions:versionList,lastvisitedVersion:lastvisitedVersion, cycle:cycleBeingCloned, sprints: []});
                callback(innerHtmlStr);
                new AJS.SingleSelect({
                    element: AJS.$('#cycle_sprint'),
                    maxInlineResultsDisplayed: 15,
                    maxWidth: 250
                });
            },

            submitHandler: function (e) {

                //for analytics
                if (za != undefined) {
                    za.track({'event':ZephyrEvents.CREATE_CYCLE,
                            'eventType':'Click',
                            'buildNumber': AJS.$('#cycle_build').val(),
                            'projectId': projectId,
                            'versionId': AJS.$('#cycle_version').val()
                        },
                        function (res) {
                            //console.log('Analytics test: -> ',res);
                        });
                }

                // Attach Loading icon
                dialog.$popupContent.find('.buttons-container').append('<span class="icon throbber loading dialog-icon-wait dialog-icon-wait-top">' + AJS.I18n.getText('zephyr.gridData.loading') + '</span>');
                ZEPHYR.Cycle.CycleFunction.createCycle(projectId, cycleBeingCloned, function (response) {
                    // dialog.hide();
                    if (response.progress == 0 && response.message != '') {
                        showErrorMessage(response.message);
                    } else {
                        if(callback){
                            callback.call(null, response);
                        }
                    }
                });
                e.preventDefault();
            }
        });
        dialog.show();
    },
    createCycle : function(projectId, cycleBeingCloned, completed) {

        var intervalId;
        AJS.$(document).off('click', '#aui-dialog-close');
        AJS.$(document).on('click', '#aui-dialog-close', function(event, value) {
            if (!isCloneCycleFail) {
                payload.responseMessage = '';
                completed.call(null, payload);
                clearInterval(intervalId);
                selectedVersionChanged();
            } else {
                isCloneCycleFail = false;
            }
        })

        var elID = ('-project-' + projectId);
        var payload = {
            clonedCycleId: cycleBeingCloned ? cycleBeingCloned.id :"",
            name : AJS.$('#cycle_name').val(),
            build : AJS.$('#cycle_build').val(),
            environment : AJS.$('#cycle_environment').val(),
            description : AJS.$('#cycle_description').val(),
            startDate : AJS.$('#cycle_startDate').val(),
            endDate : AJS.$('#cycle_endDate').val(),
            projectId : projectId,
            versionId: AJS.$('#cycle_version').val(),
            sprintId: AJS.$('#cycle_sprint :selected').val(),
            issueId: AJS.$("#issueId").val(),
            cloneCustomFields : cycleBeingCloned? AJS.$("#clone_customFields")[0].checked : false,
        };
        ZEPHYR.Cycle.attachPermissionActiveClass(elID);
        AJS.$('#create-cycle-tree-trigger').removeClass('no-hover');
        AJS.$('.aui-blanket').css({'visibility': 'hidden'});
        dialog.hide();
        jQuery.ajax({
            url: getRestURL() + "/cycle",
            type : "post",
            contentType :"application/json",
            data: JSON.stringify( {
                  'clonedCycleId': payload.clonedCycleId,
                  'name' : payload.name,
                  'build' :  payload.build,
                  'environment': payload.environment,
                  'description' : payload.description,
                  'startDate': payload.startDate,
                  'endDate': payload.endDate,
                  'projectId': payload.projectId,
                  'versionId': payload.versionId,
                  'sprintId': payload.sprintId,
                  'issueId': payload.issueId,
                  'cloneCustomFields' : payload.cloneCustomFields
            }),
            success : function(response) {

                if (cycleBeingCloned){
                    var jobProgressToken = response.jobProgressToken;
                    if(response != null) {
                        var msgDlg = new JIRA.FormDialog({
                            id: "warning-message-dialog",
                            content: function (callback) {
                                var innerHtmlStr = ZEPHYR.Project.Cycle.warningDialogContent({
                                    title: AJS.I18n.getText('project.cycle.summary.clone.dialog.heading'),
                                    warningMsg:AJS.I18n.getText('zephyr.je.clone.cycle.in.progress'),
                                    progress:0,
                                    percent:0,
                                    timeTaken:0
                                });
                                callback(innerHtmlStr);
                            },
                        });
                        msgDlg.show();
                    }

                    intervalId = setInterval(function(){
                        jQuery.ajax({url: contextPath + "/rest/zephyr/latest/execution/jobProgress/"+jobProgressToken,
                            data: {'type':"add_tests_to_cycle_job_progress"}, complete:function(jqXHR, textStatus){
                                var data = jQuery.parseJSON(jqXHR.responseText);
                                if (data.progress == 0 && data.message != '') {
                                    isCloneCycleFail = true;
                                    AJS.$('#aui-dialog-close').click();
                                    clearInterval(intervalId);
                                    if (completed) {
                                        completed.call(null, data);
                                    }
                                } else {
                                    AJS.$(".aui-progress-indicator").attr("data-value",data.progress);
                                    AJS.$(".aui-progress-indicator-value").css("width",data.progress*100+"%");
                                    AJS.$(".timeTaken").html(AJS.I18n.getText('zephyr.je.cycle.timeTaken')+ ": " + data.timeTaken);
                                    var errMsg = ((data.errorMessage != undefined && data.errorMessage.length > 0) ? data.errorMessage: null);
                                    if (errMsg != null){
                                        AJS.$("#cycle-aui-message-bar .aui-message").html(errMsg);
                                        AJS.$(".timeTaken, .aui-progress-indicator ").remove();
                                        clearInterval(intervalId);
                                        selectedVersionChanged();
                                    }
                                    if(data.progress == 1 && errMsg == null) {
                                        AJS.$("#cycle-aui-message-bar .aui-message").html(htmlEncode(data.message));
                                        clearInterval(intervalId);
                                        selectedVersionChanged();

                                        if(completed) {
                                            payload.responseMessage = JSON.parse(jqXHR.responseText).message;
                                            completed.call(null, payload);
                                        }
                                    }
                                }
                            }
                        })
                    }, 1000);

                }else{

                if(completed)
                    var cycleResponse = AJS.$.extend({},payload, response);
                    completed.call(null, cycleResponse);
                AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
                }
            },
            error : function(response) {
                validateCycleInput(response);
                AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');

            },
            statusCode: {
                403: function(xhr, status, error) {
                    if(completed)
                        completed.call(null, xhr);
                }
            }
        });
    }
}

var reorderExecutions = function(node) {
    var projectId = AJS.$("#zprojectId")[0].value,
        nodeObj = node.a_attr;

    if(nodeObj.nodeType == 'cycle') {
        cycleObj = nodeObj;
        cycleId = nodeObj.cycleId;
        folderId = null;

    } else {
        cycleObj = zephyrjQuery3('#js-tree').jstree().get_node(node.parent).a_attr;
        cycleId = cycleObj.cycleId;
        folderId = nodeObj.folderId;
    }



    var action = 'expand';
    var lastSortQuery = 'OrderId:ASC';
    var CONST = ZEPHYR.Cycle.expandos();

    JIRA.Loading.showLoadingIndicator();

    ZEPHYR.Cycle.reorderExecutions.url = getRestURL() + "/execution?cycleId=" + cycleId + "&action=" + action +
        "&offset=0&sorter=" + lastSortQuery + "&" + CONST.requestParams + "&projectId=" + projectId + "&versionId=" + cycleObj.versionId + '&expand=reorderId';
    if(folderId) {
        ZEPHYR.Cycle.reorderExecutions.url += '&folderId=' + folderId;
    }
    var elID = '-project-' + projectId;
    AJS.$('#cycle-wrapper-' + cycleId).show();
    ZEPHYR.Cycle.attachPermissionActiveClass(elID);
    AJS.$('.aui-blanket').css({'visibility': 'hidden'});
    ZEPHYR.Cycle.reorderExecutions.fetch({
        success: function(collection, response) {
            sortKey = '';
            cycleSelected.sortQuery = ''
            collection.sort();
            AJS.$.each(collection.models, function(i, model) {
                model.set('position', i);
                model.set('prevOrderId', model.get('orderId'));
            });
            var reorderExecutionsView = new ZEPHYR.Cycle.ReorderExecutionsView({
                collection:     collection,
                cycleId:        cycleId,
                versionId:      cycleObj.versionId,
                soffset:        cycleObj.soffset,
                status:         response.status

            });
            AJS.$('body').append(reorderExecutionsView.render().el);
            reorderExecutionsView.displayReorderExecutionsDND(function() {
                refreshTree();
            });
            AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
        },
        error: function(collection, response) {
            JIRA.Loading.hideLoadingIndicator();
            var auiMessageBar = AJS.$("<div class='zephyr-aui-message-bar' id='aui-message-bar'/>").prependTo("#project-tab");
            AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
                title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
                body: response.responseText
            });
            setTimeout(function(){
                auiMessageBar.fadeOut(1000, function(){
                    auiMessageBar.remove();
                });
            }, 2000);
            AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
        }
    });
}


var fetchNodeExecutions =  function(cycleNode, folderNode) {

    cycleNodeDuplicate = cycleNode;
    folderNodeDuplicate = folderNode;

    var isFolder = folderNode ? Object.keys(folderNode).length : false;
    var folderId = isFolder ? folderNode.folderId : null;
    var currentCycleState = queryParamsAllCycles.filter(function(cycle) {
        return cycle.cycleId == cycleNode.a_attr.cycleId && cycle.projectId == cycleNode.a_attr.projectId && cycle.versionId == cycleNode.a_attr.versionId && cycle.folderId == folderId;
    });
    var cycleId = cycleNode.a_attr.cycleId;
    var projectId = cycleNode.a_attr.projectId;
    var versionId = cycleNode.a_attr.versionId;
    var sortQuery = isFolder ? folderNode.sortQuery :  null;
    var soffset = isFolder ? folderNode.soffset : null;


    if (currentCycleState && currentCycleState.length > 0) {
        currentCycleState[0].sortQuery = currentCycleState[0].sortQuery  || 'OrderId:ASC';
        currentCycleState[0].soffset = currentCycleState[0].soffset  ||  0;
        sortQuery = currentCycleState[0].sortQuery;
        soffset = currentCycleState[0].soffset;
        cycleSelected = currentCycleState[0];
        cycleSelected.folderId = folderId;
    } else {
        cycleNode.a_attr.sortQuery  = isFolder ? (sortQuery || 'OrderId:ASC') : (cycleNode.a_attr.sortQuery || 'OrderId:ASC');
        cycleNode.a_attr.soffset = isFolder ? ( soffset || 0 ) : (cycleNode.a_attr.soffset || 0);
        sortQuery = cycleNode.a_attr.sortQuery;
        soffset = cycleNode.a_attr.soffset;
        cycleSelected = cycleNode.a_attr;
        cycleSelected.folderId = folderId;
        queryParamsAllCycles.push( JSON.parse(JSON.stringify(cycleSelected)));
    }

    //if(soffset > 0 && ZEPHYR.Cycle.executions.length)

    if (isOffsetReset) {
        soffset = 0;
        isOffsetReset = false;
    }

    var nodeDetails = new ZEPHYR.Cycle.cycleDetails({
        cycleId: cycleId,
        projectId : projectId,
        versionId : versionId,
        sortQuery : sortQuery,
        soffset : soffset,
        folderId : folderId,
        limit: paginationWidth
    });

    nodeDetails.fetch({
        success: function(resp) {
            vanillaGrid.utils.selectedExecutionIds['cycleSummaryGrid'] = [];
            ZEPHYR.Cycle.executions = resp.models[0].attributes.executions;
            if(!ZEPHYR.Cycle.executions.length && nodeDetails.options.soffset > 0) {
                var obj = nodeDetails.options;
                soffset = obj.soffset - 1 *(parseInt(obj.limit));
                sortQuery = obj.sortQuery;
                    obj.soffset = soffset;
                var nodeObjDetails = new ZEPHYR.Cycle.cycleDetails(obj);
                var result = nodeObjDetails.fetch({async: false});
                 //console.log('need to fetch with this obj', obj, result);
                resp.models[0].attributes = JSON.parse(result.responseText);
            }
            if (!ZEPHYR.Cycle.executions.length) {
                AJS.$('#listViewBtn').prop('disabled', true);
                AJS.$('#detailViewBtn').prop('disabled', true);
            } else{
                AJS.$('#listViewBtn').prop('disabled', false);
                AJS.$('#detailViewBtn').prop('disabled', false);
            }
            resp.forEach(function(model) {
                executionsTableModelNew.set('cycle', cycleNode.a_attr);
                executionsTableModelNew.set('context', 'detail-grid');
                executionsTableModelNew.set('allStatusList' , model.get('status'));
                executionsTableModelNew.set('totalCount' , model.get('recordsCount'));
                executionsTableModelNew.set('currentOffset' , soffset);
                executionsTableModelNew.set('sortQuery' , sortQuery);
                executionsTableModelNew.set('executions' , model.get('executions'));
                executionsTableModelNew.set('isColumns',true);
                //executionsTableModelNew.set('columns', ZEPHYR.Cycle.executionColumns);
                var tempCycleInfoModel = cycleInfoModelNew.get('cycle');
                    tempCycleInfoModel.totalExecutionEstimatedTime =  model.get('totalExecutionEstimatedTime');
                    tempCycleInfoModel.totalExecutionEstimatedTimeTitle =  timewithyears(model.get('totalExecutionEstimatedTime'));

                    tempCycleInfoModel.totalExecutionLoggedTime = model.get('totalExecutionLoggedTime');
                    tempCycleInfoModel.totalExecutionLoggedTimeTitle = timewithyears(model.get('totalExecutionLoggedTime'));

                    tempCycleInfoModel.executionsAwaitingLog = model.get('executionsToBeLogged');
                    tempCycleInfoModel.isExecutionWorkflowEnabledForProject = model.get('isExecutionWorkflowEnabledForProject');

                cycleInfoModelNew.set('cycle' , tempCycleInfoModel);
                cycleInfoModelNew.set('redraw' , !cycleInfoModelNew.get('redraw'));

                if(!model.get('executions').length) {
                  var html = ZEPHYR.Templates.PlanCycle.executionNewTable();
                  var navigationHtml = ZEPHYR.Templates.PlanCycle.addPaginationNewUI({
                    totalPages : 1,
                    selectedPage : 1,
                    entriesCount: paginationWidth,
                    totalCount : 0,
                  });
                    AJS.$('#cycle-executions-wrapper').html(html.concat(navigationHtml));
                    createCycleSummaryGrid([], ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder);
                }
            });
        }
    });

    if(!refreshExecutionFlag) {
        refreshExecutionFlag = true;
        return;
    }
    AJS.$(document).trigger( "triggerExecutionDetails", [ null ] );
}

var setSelectedVersionFromTree = function(versionId) {
    selectedVersionFromTree = versionId;
};


/*************************************/
/****Backbone Views Declaration ******/
/************************************/

ZEPHYR.Cycle.TreeView = Backbone.View.extend({
    render: function() {
        this.initData();
    },
    fetchNodeDetails : function (e, data) {
        // console.log('----------', data.selected, data.node);
        e.stopImmediatePropagation();

        if (!isDetailedViewClicked) {
            ZEPHYR.Cycle.clickedExecutionId = null;

            if (clearData) {
                cycleSummaryGridSelectedExecutions = [];
            }
        }
        isDetailedViewClicked = false;
        if(!data.node) {
            return;
        }
        // if(!data.node || (data.node.a_attr.nodeType != 'cycle' && data.node.a_attr.nodeType != 'folder')) {
        //     return;
        // }
        if(data.action === 'delete_node') {
            cycleInfoModelNew.set('cycle' , {});
            AJS.$('#cycle-executions-wrapper').html('');
            AJS.$('#view-btn-container').addClass('hide');
            AJS.$('#cycle-name').html(ZEPHYR.Templates.PlanCycle.nodeNameBar({
                nodeName: null,
                nodeType: null
            }));
            return;
        }
        // if(data.event && data.event.target.nodeName === 'A') {
        //     window.open(data.event.target.href,"_self");
        //     return;
        // }

        var folderId = null;
        var sortQuery;
        var soffset;
        var cycleInfoModel = JSON.parse(JSON.stringify(data.node.a_attr));
        cycleInfoModel.totalExecutedZql = nodeHref(data.node.a_attr, 'totalExecuted');
        if(data.node.a_attr.nodeType === 'cycle') {
            cycleInfoModel.totalExecutionsZql = nodeHref(data.node.a_attr);
            cycleInfoModel.build = cycleInfoModel.build;
            cycleInfoModel.environment = cycleInfoModel.environment;
            cycleInfoModel.description = cycleInfoModel.description;
            cycleInfoModel.totalCycleExecutionsZql = nodeHref(data.node.a_attr, 'totalCycleExecutions');
            setSelectedVersionFromTree(cycleInfoModel.versionId);
        }

        if(data.node.a_attr.nodeType === 'folder') {
            cycleInfoModel.totalExecutionsZql = nodeHref(data.node.a_attr,'totalExecutions');
        }
        cycleInfoModelNew.set('cycle' , cycleInfoModel);

        if(data.node.a_attr.nodeType !== 'folder' && data.node.a_attr.nodeType !== 'cycle') {
            // AJS.$('#cycle-executions-wrapper').html('');
            AJS.$('#view-btn-container').addClass('hide');
            AJS.$('#cycle-executions-wrapper').addClass('hide');
            AJS.$('#cycle-name').html(ZEPHYR.Templates.PlanCycle.nodeNameBar({
                nodeName: data.node.a_attr.name,
                nodeType: data.node.a_attr.nodeType
            }));
            if(data.node.a_attr && data.node.a_attr.versionId) {
                setSelectedVersionFromTree(data.node.a_attr.versionId);
            } else {
                setSelectedVersionFromTree(null);
            }
            return;
        }
        if(data.node.a_attr.nodeType === 'folder') {
            data.node.a_attr.isFolder = true;
            AJS.$('#cycle-name').html(ZEPHYR.Templates.PlanCycle.nodeNameBar({
                nodeName: data.node.a_attr.folderName,
                nodeType: 'folder',
                href: nodeHref(data.node.a_attr)
            }));

            AJS.$('#view-btn-container').removeClass('hide');

            var folderObj = {
                folderId : data.node.a_attr.folderId,
                sortQuery : data.node.a_attr.sortQuery,
                soffset : data.node.a_attr.soffset
            }
            setSelectedVersionFromTree(data.node && data.node.a_attr.versionId);
            fetchNodeExecutions(zephyrjQuery3('#js-tree').jstree().get_node(data.node.parent), folderObj);
        }

        if (data.node.a_attr.nodeType === 'cycle') {
            data.node.a_attr.isFolder = false;

            AJS.$('#cycle-name').html(ZEPHYR.Templates.PlanCycle.nodeNameBar({
                nodeName: data.node.a_attr.name,
                nodeType: 'cycle',
                href: nodeHref(data.node.a_attr)
            }));
            AJS.$('#view-btn-container').removeClass('hide');
            if (!AJS.$('#detailViewBtn').hasClass('active-view')) {
                AJS.$('#cycle-executions-wrapper').removeClass('hide');
            }
            fetchNodeExecutions(data.node, null);
        }
        if(initialTreeLoad) {
            initialTreeLoad =  false;
            return;
        }
        selectedExecutionId = null;
        var query = updateLocationHash('list', null, true);
        updateRouter(query);
    },
    // onNodeHover: function(e, data) {
    //     var parent = zephyrjQuery3('#' + data.node.id);
    //     //zephyrjQuery3(parent).find('.contextMenuIcon').addClass('visibilityHidden');
    //     parent.children('.jstree-anchor').find('.contextMenuIcon').removeClass('visibilityHidden');
    // },
    // onNodeDehover: function(e, data) {
    //     var parent = zephyrjQuery3('#' + data.node.id);
    //     parent.children('.jstree-anchor').find('.contextMenuIcon').addClass('visibilityHidden');
    // },
    showContextMenu: function(e, data){
        var treeView = e.target;
        setTimeout(function() {
            var childrenId = zephyrjQuery3(e.target).closest('a').prop('id');
            zephyrjQuery3(treeView).jstree().show_contextmenu('#' + childrenId);
            zephyrjQuery3(treeView).find('.jstree-container-ul.jstree-contextmenu').removeClass('jstree-contextmenu');

            contextMenuEl = zephyrjQuery3('.jstree-default-contextmenu');
            contextMenuWidth = contextMenuEl.width();
            contextMenuHeight = contextMenuEl.height();
            var pageX = e.pageX,
                pageY = e.pageY;

            if(pageX + contextMenuWidth >= zephyrjQuery3(window).width()) {
                pageX = zephyrjQuery3(window).width() - contextMenuWidth;
            } else if(pageY + contextMenuHeight >= zephyrjQuery3(window).height()) {
                pageY = pageY - contextMenuHeight;
            }
            contextMenuEl.css({left: pageX, top: pageY});
        }, 201);
    },
    initData: function() {
        var that = this;
        var handleAjaxError = function(ev, xhr, ajaxSettings) {
            try {
                if(ajaxSettings && ajaxSettings.url && ajaxSettings.url.indexOf('/cycle?projectId') > -1 && xhr.status === 403) {
                    var errorMsg = JSON.parse(xhr.responseText) ? JSON.parse(xhr.responseText)['PERM_DENIED'] : '';
                    require(['aui/flag'], function(flag) {
                        if(AJS.$('#zephyr-aui-message-bar').children().length) {
                            return;
                        }
                        var permissionErrorFlag = flag({
                            type: 'error',
                            title: 'Permission denied',
                            body: errorMsg
                        });
                        setTimeout(function() {
                            permissionErrorFlag.close();
                        }, 4000);
                    });
                }
            } catch(err) {
                //console.log('Error');
            }
        };
        zephyrjQuery3(document).ajaxError(handleAjaxError);
        zephyrjQuery3(this.$el)
            .on("select_node.jstree", this.fetchNodeDetails)
            .on('click.context', '.contextMenuIcon', this.showContextMenu)
            .on("hover_node.jstree", this.onNodeHover)
            .on("dehover_node.jstree", this.onNodeDehover)
            .on('after_close.jstree', function (e, data) {
                var node = data.node;
                if(node.a_attr.nodeType === 'release' || node.a_attr.nodeType === 'root') {
                    return;
                }
                var tree = zephyrjQuery3('#js-tree').jstree(true);
                var isChildren = data.node.children && data.node.children.length;
                //tree.delete_node(data.node.children);
                data.node.children = isChildren ? true : false;
                data.node.children_d = isChildren ? true : false;
                tree._model.data[data.node.id].state.loaded = false;
            })
            .on("ready.jstree", function(e){
                zephyrjQuery3('#js-tree').off('keydown.jstree', '.jstree-anchor');
                initialTreeLoad = true;
            })
            .on('contextmenu.jstree', function(ev, node) {
                ev.preventDefault();
                return false;
            })
            .jstree({
                'core': {
                    'check_callback': true,
                    'data' : {
                        'cache': false,
                        'url': function(node) {
                            var nodeType = node.a_attr && node.a_attr.nodeType,
                                projectId = AJS.$("#projId").val(),
                                versionId = node.a_attr && node.a_attr.versionId,
                                cycleId = node.a_attr && node.a_attr.cycleId,
                                urlSuffix;

                            if(node.id === '#') {
                                urlSuffix = '/util/allversionstext';
                            } else if (nodeType === 'root'){
                                urlSuffix = '/util/versionBoard-list?projectId='+projectId;
                            }else if (nodeType === 'version'){
                                urlSuffix = '/cycle?projectId='+projectId+'&versionId='+versionId+'&offset=0&expand=executionSummaries';
                            } else if (nodeType === 'cycle') {
                                urlSuffix = '/cycle/'+ cycleId +'/folders?projectId='+projectId+'&versionId='+versionId+'&limit=1000&offset=0';
                            }
                            return getRestURL() + urlSuffix;
                        },
                        'contentType': 'application/json',
                        'dataFilter' : function(ops) {
                            try {
                                if(JSON.parse(ops)) {
                                    var data = JSON.parse(ops),
                                        keys = Object.keys(data);
                                    if(data instanceof Array) {
                                        var tempFolders = [];
                                        data.forEach(function(folder) {
                                            var folderObj = {
                                                'a_attr': folder,
                                                'text': nodeHTML(folder),
                                                'children' : false
                                            };
                                            folderObj['a_attr'].nodeType = 'folder';
                                            folderObj.id = 'folder-'+folder.folderId;

                                            tempFolders.push(folderObj);
                                        });
                                        return JSON.stringify(tempFolders);
                                    } else {
                                        if(Object.keys(data).length === 1 && Object.keys(data).indexOf('text') > -1){
                                            var tempData = [{
                                                'text': data.text,
                                                'a_attr': {
                                                    'nodeType': 'root',
                                                    'name': data.text
                                                },
                                                'state': {
                                                    'opened': true,
                                                    'disabled': true
                                                },
                                                'id': 'root',
                                                'children': true
                                            }];
                                            return JSON.stringify(tempData);
                                        } else if(Object.keys(data).indexOf("unreleasedVersions") > -1) {
                                            var versionId = window.location.href.split('versionId=')[1] && window.location.href.split('versionId=')[1].split('&')[0];
                                            var keysToIterate = ['releasedVersions', 'unreleasedVersions'],
                                                tempData = [];
                                                var treeData = treeData || [];
                                            keysToIterate.forEach(function(key) {
                                                var obj = formatVersionData(data[key], key === 'releasedVersions' ? 'Released' : 'Unreleased', versionId);
                                                tempData = tempData.concat(obj);
                                            });
                                            var groupedByRelease = _.groupBy(tempData, function(version) {
                                                return version.released;
                                            });
                                            Object.keys(groupedByRelease).forEach(function(key) {
                                                treeData.push({
                                                    text: '<div class="js-node-data-custom">' +key + '</div>',
                                                    a_attr: {
                                                        nodeType: 'release',
                                                        name: key
                                                    },
                                                    'state' : {
                                                        'opened' : key == 'Released' ? false : true,
                                                        'disabled': true
                                                    },
                                                    children: groupedByRelease[key]
                                                });
                                            });
                                            return JSON.stringify(treeData);

                                        } else {
                                            var tempCycles = [],
                                                cycles = data,
                                                cycleId = window.location.href.split('cycleId=')[1] && window.location.href.split('cycleId=')[1].split('&')[0];
                                            Object.keys(cycles).forEach(function(key) {
                                                if (key === 'recordsCount') {
                                                    return;
                                                }
                                                var cycleObj = {
                                                    'a_attr': cycles[key],
                                                    'text': nodeHTML(cycles[key], key),
                                                    'children' : (parseInt(key) === -1) || !cycles[key].totalFolders ? false : true,
                                                    'state' : {
                                                        'opened' : cycleId == key ? true : false
                                                    },
                                                };
                                                cycleObj['a_attr'].nodeType = 'cycle';
                                                cycleObj['a_attr'].cycleId = parseInt(key);
                                                cycleObj.id = 'version-'+cycles[key].versionId +'-cycle-'+parseInt(key);
                                                tempCycles.push(cycleObj);
                                                //console.log('cycleObj', cycleObj);
                                            });
                                            return JSON.stringify(tempCycles);
                                        }

                                    }

                                }
                            }
                            catch(err) {
                                return that.model.data;
                                returnState = false;
                            }

                        }
                    }
                },
                'search' : {
                    'show_only_matches' : true,
                    'case_insensitive' : true,
                    'search_callback': function(str, node){
                        var attrs = node.a_attr;
                        str = str && str.toLowerCase();
                        return  attrs.name && attrs.name.toLowerCase().indexOf(str) > -1
                             || attrs.folderName && attrs.folderName.toLowerCase().indexOf(str) > -1
                             || attrs.build && attrs.build.toLowerCase().indexOf(str) > -1
                             || attrs.description && attrs.description.toLowerCase().indexOf(str) > -1
                             || attrs.environment && attrs.environment.toLowerCase().indexOf(str) > -1
                    }
                },
                'state': {
                    'key': 'jstree'
                },
                'plugins' : [ "contextmenu", "json_data", "search", "state", "unique", "wholerow" ],
                'contextmenu': {items: customMenu}
            });

        var to = false;
        var that = this;




        zephyrjQuery3('#search-tree').blur(function () {
            //for analytics
            if (za != undefined) {
                za.track({
                        'event':ZephyrEvents.SEARCH_CYCLE,
                        'eventType':"Keyup"
                    },
                    function (res) {
                        // executionsTableModelNew
                        //console.log('Analytics test: -> ',res);
                    });
            }
        });
        zephyrjQuery3('#search-tree').keyup(function () {
            if(to) { clearTimeout(to); }
            to = setTimeout(function () {
              var v = zephyrjQuery3('#search-tree').val();
              zephyrjQuery3(that.$el).jstree(true).search(v);
            }, 250);
        });
        executionsTableView = new ZEPHYR.Cycle.ExecutionTable({
            el : '#cycle-executions-wrapper',
            model : executionsTableModelNew
        });
        cycleInfoView = new ZEPHYR.Cycle.InfoView({
            el : '#cycle-info-wrapper',
            model : cycleInfoModelNew
        });
        cycleInfoView.render();
    }
});

ZEPHYR.Cycle.InfoView = Backbone.View.extend({
    model: ZEPHYR.Cycle.info,

    initialize: function(options){
        if (this.model) {
            this.model.on('change:cycle', this.render, this);
            this.model.on('change:redraw', this.render, this);
        }
    },

    render: function() {
        var html = ZEPHYR.Templates.PlanCycle.cycleInfoView({
            cycle: this.model.get('cycle')
        });
        zephyrjQuery3(this.$el).html(html);
    }
});

ZEPHYR.Cycle.ExecutionTable = Backbone.View.extend({
    model: ZEPHYR.Cycle.execution,
    executionIdDetails : null,
    initialize: function(options){
        if (this.model) {
            this.model.on('change:executions', this.render, this);
            this.model.on('change:emptyExecutions', this.render, this);
        }
        AJS.$('#cyclemodule_heading').find('.toggle-title').on('click', function(ev, data) {
            if(!AJS.$('#cyclesummarymodule').hasClass('collapsed')) {
                AJS.$('.execution-detail-pagination-wrapper').show();

                if(!data) {
                    //check for selected execution id
                    var model = executionsTableModelNew.attributes;
                    if(model.selectedId) {
                        var selectedRow = AJS.$('#execution-table').find('tr.selected');
                        var elementIndex = selectedRow[0] && selectedRow[0].rowIndex - 1;
                        var executionIds = [];
                        model.executions && model.executions.forEach(function(execution) {
                              executionIds.push(execution.id);
                        });
                        AJS.$(document).trigger( "triggerExecutionDetails", [ executionsTableModelNew.attributes.selectedId, false, executionsTableModelNew.attributes.allStatusList, {total:parseInt(model.totalCount), dbIndex: parseInt(cycleSelected.soffset) + parseInt(elementIndex), localIndex: parseInt(elementIndex), executionIds:executionIds} ] );
                    }
                }
            } else {
                AJS.$('.execution-detail-pagination-wrapper').hide();
                AJS.$('.execution-details-wrapper').empty();
            }
        });
    },
    events:{
        "click #execution-table thead th.sortable" : 'sortExecutionsTable',
        "click #prev-page-execution" : 'prevPageExecutions',
        "click #next-page-execution" : 'nextPageExecutions',
        "click .goToPage" : "fetchExecutions",
        "dblclick #execution-table tbody tr" : 'showExecutionDetails',
        "click #execution-table tbody td .issue-schedule-operations" : 'deleteExecution',
        "click #execution-table tbody td .execution-status-lozenges .zfj-editable-field" : 'showExecutionStatusSelect',
        "click #execution-table td .execution-status-container button.exec_status_update-schedule" : 'updateExecutionStatus',
        "click #execution-table td .execution-status-container button.exec_status_cancel-schedule" : 'cancelExecutionStatus',
        "change #execution-table td .execution-status-container select.exec_status-schedule-select" : 'onchangeExecutionSelect',
        "click .dropDown-Trigger" : 'selectNumberOfEntries',
        "click .chooseEntries" : 'setNumberOfEntries'
    },

    onchangeExecutionSelect : function(ev) {
        AJS.$(ev.target).closest('.execution-status-container').data('newstatusid', ev.target.value);
    },

    cancelExecutionStatus : function(ev) {
        ev.stopImmediatePropagation();
        var executionId = AJS.$(ev.target).closest('tr').data('exeuctionid');
        var executionContainer =  AJS.$('#execution-status-container-' + executionId);
        executionContainer[0].children[1].getElementsByTagName('select')[0].value = executionContainer.data('statusid');
        executionContainer[0].children[1].className = 'displaynone';
        executionContainer[0].children[0].className = '';
    },
    deleteExecution: function(e) {
        var executionIdDetails = this.executionIdDetails;
        ZEPHYR.Schedule.addDeleteTrigger(function(){
            triggerTreeRefresh();
        });
        if (za != undefined) {
            za.track({
                        'event':ZephyrEvents.DELETE_EXECUTION,
                        'eventType':"Click"
                    },
                function (res) {
                    //console.log('Analytics test: -> ',res);
                });
        }
    },
    updateExecutionStatus : function(ev) {
        ev.stopImmediatePropagation();
        var executionContainer = AJS.$(ev.target).closest('.execution-status-container');
        var oldExecutionStatus = executionContainer.data('statusid');
        var newExecutionStatus = executionContainer.data('newstatusid');
        var executionIdStatus = executionContainer.data('id');
        var executionIdDetails = this.executionIdDetails;
        var projectId = this.model.attributes.executions[0].projectId;
        var elID = ('-project-' + projectId);
        ZEPHYR.Cycle.attachPermissionActiveClass(elID);
        AJS.$('.aui-blanket').css({'visibility': 'hidden'});
        if (executionIdStatus && newExecutionStatus != oldExecutionStatus) {
            var xhr = {
                url: '/execution/'+ executionIdStatus + '/execute',
                method: 'PUT',
                data:{
                    'status': newExecutionStatus,
                    'changeAssignee': false
                }
            }
            xhrCall(xhr, _.bind(function(schedule) {
                if (this.model && this.model.attributes.executions) {
                    var prevExecutions = JSON.parse(JSON.stringify(this.model.attributes.executions));
                    prevExecutions.forEach(_.bind(function(execution, index) {
                        if (execution.id == schedule.id) {
                            schedule.defects = execution.defects;
                            prevExecutions[index] = schedule;
                            this.model.set('executions' , prevExecutions);
                            return -1;
                        }
                    }, this))
                }
                /*if (schedule.id == executionIdDetails) {
                    AJS.$(document).trigger( "triggerExecutionDetails", [executionIdDetails , true, this.model.attributes.allStatusList ] );
                }*/
                showExecStatusSuccessMessage();
                refreshExecutionFlag = false;
                triggerTreeRefresh();

                if (za != undefined) {
                    za.track({'event':ZephyrEvents.EXECUTE,
                            'eventType':'Change',
                            'executionId': schedule.id,
                            'executionStatus': newExecutionStatus
                        },
                        function (res) {
                            //console.log('Analytics test: -> ',res);
                        });
                }

            },this), function(response) {
                // Hide the loading indicator on error
                ZEPHYR.Loading.hideLoadingIndicator();
                if(response && response.status == 403) {
                    var _responseJSON = {};
                    try {
                        _responseJSON = jQuery.parseJSON(response.responseText);
                    } catch(e) {
                        //console.log(e);
                    }
                    if(_responseJSON.PERM_DENIED) {
                        showPermissionError(response);
                        return;
                    }
                }
                if(response && typeof buildExecutionError == 'function')
                    buildExecutionError(response);
                else if(response.status == 401){
                    var dialog = new AJS.Dialog({
                        width:540,
                        height: 200,
                        id: "dialog-error"
                    });
                    dialog.addHeader(AJS.I18n.getText('zephyr.je.submit.form.error.title'));

                    dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
                    AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
                        body: AJS.I18n.getText('zapi.login.error'),
                        closeable: false
                    });

                    dialog.addLink("Close", function (dialog) {
                        dialog.hide();
                    }, "#");
                    dialog.show();
                }
            })
        }
        this.cancelExecutionStatus(ev);
    },

    showExecutionStatusSelect : function(ev) {
        var currentElement = AJS.$(ev.target);
        parentElement = currentElement.closest('div.execution-status-container')[0];

        parentElement.children[0].className = 'displaynone';
        parentElement.children[1].className = '';
    },

    showExecutionDetails : function(ev) {
        ev.stopPropagation();
        ev.stopImmediatePropagation();
        var currentElement = AJS.$(ev.target);
        if(currentElement.closest('table').hasClass('cycle-aggregated-summary') || currentElement.hasClass('zExecute')) {
            return;
        }
        executionStatusContainer = currentElement.closest('.execution-status-container').length;
        deleteContainer = currentElement.closest('td .execution-delete-container')[0];
        executionSummaryContainer = currentElement.closest('td .executionSummaryBar')[0];

        if(!executionSummaryContainer) {
          var executionId = ev.currentTarget.dataset.exeuctionid || $(ev.currentTarget).closest('tr')[0].dataset.exeuctionid;
          var elementIndex = (ev.currentTarget.rowIndex || $(ev.currentTarget).closest('tr')[0].rowIndex) - 1;
          var executionIds = [];
          this.model.get('executions').forEach(function(execution) {
              executionIds.push(execution.id);
          });

          if (!executionStatusContainer && (!currentElement.closest('.zfj-editable-field').length) && !deleteContainer && !this.model.get('isCheckbox')) {
              this.executionIdDetails = executionId;
              // if(!AJS.$('#cyclesummarymodule').hasClass('collapsed')) {
              //   AJS.$('#cyclemodule_heading').find('.toggle-title').trigger('click', [{'isShowExecutionDetails': true}]);
              // }
                AJS.$('#cycle-executions-wrapper').addClass('hide');
                AJS.$('#cycle-info-wrapper').addClass('hide');
                AJS.$('.execution-details').removeClass('hide');
                AJS.$('#detailViewBtn').addClass('active-view');
                AJS.$('#listViewBtn').removeClass('active-view');

                AJS.$(document).trigger( "triggerExecutionDetails", [ executionId, false, executionsTableModelNew.attributes.allStatusList, {total:parseInt(this.model.get('totalCount')), dbIndex: parseInt(cycleSelected.soffset) + parseInt(elementIndex), localIndex: parseInt(elementIndex), executionIds:executionIds} ] );
          }
          if(!executionStatusContainer && (!currentElement.closest('.zfj-editable-field').length) && !this.model.get('isCheckbox')) {
              this.model.set('selectedId' , executionId);
              this.model.set('emptyExecutions' , !this.model.get('emptyExecutions'));
          }

          //console.log('selectedId', executionId, this.model.get('selectedId'));
      }
    },

    selectNumberOfEntries : function(ev) {
      ev.stopPropagation();
      ev.preventDefault();
      ev.stopImmediatePropagation();
      if(AJS.$('.entries-per-page .dropDown-container').hasClass('active')) {
        AJS.$('.entries-per-page .dropDown-container').removeClass('active');
      } else {
        AJS.$('.entries-per-page .dropDown-container').addClass('active');
      }
    },

    setNumberOfEntries : function(ev) {
      ev.stopPropagation();
      ev.preventDefault();
      ev.stopImmediatePropagation();
      var numberOfEntries = ev.target.dataset.entries;
      AJS.$('.entries-per-page .dropDown-container').removeClass('active');
    },

    fetchExecutions: function(ev) {
        ev.preventDefault();
        ev.stopPropagation();
        ev.stopImmediatePropagation();

      var soffset = (ev.target.dataset.offset === "0" ) ? 0 : (parseInt(ev.target.dataset.offset)-1) * parseInt(paginationWidth);
      if(AJS.$(this.el).attr('id') !== 'moveExecutionsTable') {
          cycleSelected.soffset = soffset;
      }
      queryParamsAllCycles.forEach(function(cycle, index) {
               if (cycle.cycleId ==cycleSelected.cycleId && cycle.projectId == cycleSelected.projectId && cycle.versionId == cycleSelected.versionId && cycle.folderId == cycleSelected.folderId) {
                   queryParamsAllCycles[index] = JSON.parse(JSON.stringify(cycleSelected));
               }
       });
      var cycleDetails = new ZEPHYR.Cycle.cycleDetails({
                cycleId: cycleSelected.cycleId,
                projectId : cycleSelected.projectId,
                versionId : cycleSelected.versionId,
                soffset : soffset,
                sortQuery : cycleSelected.sortQuery,
                folderId : cycleSelected.folderId,
                limit: parseInt(paginationWidth),
          });
          cycleDetails.fetch({
              success: _.bind(function(data) {
                  var executions = data.models[0].get('executions'),
                      totalChecked = data.models[0].get('totalChecked'),
                      modifiedExecutions;
                  if(this.model.get('isCheckbox')) {
                      modifiedExecutions = this.fetchCheckboxState(executions, this.model.get('compareList'));
                      executions = modifiedExecutions.executions;
                      totalChecked = executions.length === modifiedExecutions.count;
                  }
                  this.model.set('totalChecked' , totalChecked);
                  this.model.set('currentOffset' , soffset);
                  this.model.set('executions' , executions);

              }, this)
          });
    },

    prevPageExecutions : function(ev) {
      if(ev) {
        ev.preventDefault();
        ev.stopPropagation();
        ev.stopImmediatePropagation();
      }

        var soffset = parseInt(this.model.get('currentOffset')) - parseInt(paginationWidth);
        if(AJS.$(this.el).attr('id') !== 'moveExecutionsTable') {
            cycleSelected.soffset = soffset;
        }
        queryParamsAllCycles.forEach(function(cycle, index) {
                 if (cycle.cycleId ==cycleSelected.cycleId && cycle.projectId == cycleSelected.projectId && cycle.versionId == cycleSelected.versionId && cycle.folderId == cycleSelected.folderId) {
                     queryParamsAllCycles[index] = JSON.parse(JSON.stringify(cycleSelected));
                 }
         });
        var cycleDetails = new ZEPHYR.Cycle.cycleDetails({
            cycleId: cycleSelected.cycleId,
            projectId : cycleSelected.projectId,
            versionId : cycleSelected.versionId,
            soffset : soffset,
            sortQuery : cycleSelected.sortQuery,
            folderId : cycleSelected.folderId,
            limit: parseInt(paginationWidth),
        });
        var that = this;
        cycleDetails.fetch({
            success: function(data) {

                var executions = data.models[0].get('executions'),
                    totalChecked = data.models[0].get('totalChecked'),
                    modifiedExecutions;
                ZEPHYR.Cycle.clickedExecutionId = executions[0].id;
                if(that.model.get('isCheckbox')) {
                    modifiedExecutions = this.fetchCheckboxState(executions, this.model.get('compareList'));
                    executions = modifiedExecutions.executions;
                    totalChecked = executions.length === modifiedExecutions.count;
                }
                that.model.set('totalChecked' , totalChecked);
                that.model.set('currentOffset' , soffset);
                that.model.set('executions' , executions);
                if(!ev) {
                  detailView(executions[executions.length - 1].id);
                }
            }
        });

    },

    nextPageExecutions : function(ev) {
      if(ev) {
        ev.preventDefault();
        ev.stopPropagation();
        ev.stopImmediatePropagation();
      }

        var soffset = parseInt(this.model.get('currentOffset') || 0) + parseInt(paginationWidth);
        if(AJS.$(this.el).attr('id') !== 'moveExecutionsTable') {
            cycleSelected.soffset = soffset;
        }
        queryParamsAllCycles.forEach(function(cycle, index) {
                 if (cycle.cycleId ==cycleSelected.cycleId && cycle.projectId == cycleSelected.projectId && cycle.versionId == cycleSelected.versionId && cycle.folderId == cycleSelected.folderId) {
                     queryParamsAllCycles[index] = JSON.parse(JSON.stringify(cycleSelected));
                 }
         });
        var cycleDetails = new ZEPHYR.Cycle.cycleDetails({
                cycleId: cycleSelected.cycleId,
                projectId : cycleSelected.projectId,
                versionId : cycleSelected.versionId,
                soffset : soffset,
                sortQuery : cycleSelected.sortQuery,
                folderId : cycleSelected.folderId,
                limit: paginationWidth
            });
            var that = this;
            cycleDetails.fetch({
                success: function(data) {
                    var executions = data.models[0].get('executions'),
                        totalChecked = data.models[0].get('totalChecked'),
                        modifiedExecutions;
                    ZEPHYR.Cycle.clickedExecutionId = executions[0].id;
                    if(that.model.get('isCheckbox')) {
                        modifiedExecutions = that.fetchCheckboxState(executions, this.model.get('compareList'));
                        executions = modifiedExecutions.executions;
                        totalChecked = executions.length === modifiedExecutions.count;
                    }
                    that.model.set('totalChecked' , totalChecked);
                    that.model.set('currentOffset' , soffset);
                    that.model.set('executions' , executions);
                    if(!ev) {
                      detailView(selectedExecutionId);
                    }
                }
            });
    },

    fetchCheckboxState: function(executions, arr) {
        //console.log('arrayList', arr, this.model.get('executions'));
        var count = 0
        executions.forEach(function(execution) {
             if(arr.indexOf(execution.id.toString()) > - 1) {
                execution.checked = true;
                count++;
             }

        });
        return {executions: executions, count: count};
    },

    sortExecutionsTable: function(ev) {
      if(ev.target.localName === "span") {
        ev.target = ev.target.parentElement;
      }
      AJS.$("#execution-table th.sortable span").remove();
       if(ev.target.type == 'checkbox') {
        return;
       }
       ev.preventDefault();

        var relValue = ev.target.getAttribute('rel');
       var order =  cycleSelected.sortQuery.split(':')[1];
       var column = ev.target.getAttribute('rel').split(':')[0];
       var sortQuery = cycleSelected.sortQuery.indexOf(column) > -1 ? (order == 'ASC' ? column + ':DESC' : column + ':ASC') : relValue;
       cycleSelected.sortQuery = sortQuery;
       cycleSelected.soffset = 0;
       queryParamsAllCycles.forEach(function(cycle, index) {
                if (cycle.cycleId ==cycleSelected.cycleId && cycle.projectId == cycleSelected.projectId && cycle.versionId == cycleSelected.versionId && cycle.folderId == cycleSelected.folderId ) {
                    queryParamsAllCycles[index] = JSON.parse(JSON.stringify(cycleSelected));
                }
        });
       var cycleDetails = new ZEPHYR.Cycle.cycleDetails({
                cycleId: cycleSelected.cycleId,
                projectId : cycleSelected.projectId,
                versionId : cycleSelected.versionId,
                soffset : 0,
                sortQuery : cycleSelected.sortQuery,
                folderId : cycleSelected.folderId
            });
            cycleDetails.fetch({
                success: _.bind(function(data) {
                    this.model.set('currentOffset' , 0);
                    this.model.set('executions' , data.models[0].get('executions'));
                    var order =  cycleSelected.sortQuery.split(':');
                    if(order[1] === 'ASC') {
                        AJS.$("#execution-table th.sortable[rel='"+ order[0] +":ASC']").append('<span class="aui-icon aui-icon-small aui-iconfont-arrow-up"></span>')
                    }
                    else {
                        AJS.$("#execution-table th.sortable[rel='"+ order[0] +":ASC']").append('<span class="aui-icon aui-icon-small aui-iconfont-arrow-down"></span>')
                    }
                }, this)
            });
    },
    render: function() {
        var statusIdArray   = [];
        for(i in this.options.allStatusList){
            statusIdArray.push(i);
        }
        var statusColName = AJS.I18n.getText('project.cycle.schedule.table.column.status');
        var executionsList = this.model.get('executions');
        executionsList.forEach(function(execution){
          if(!execution.customFields) {
            execution.customFields = {};
          }
          if(typeof execution.customFields === 'string')
            execution.customFields = JSON.parse(execution.customFields);
        })

         //pagination level for execution table in global variable
        currentPaginationExecutionTable = parseInt(this.model.get('currentOffset') / paginationWidth);
        if(executionsList && executionsList.length) {
            var html = ZEPHYR.Templates.PlanCycle.executionNewTable();
            totalPages = Math.ceil((this.model.get('totalCount')) / paginationWidth);
            selectedPage = parseInt(this.model.get('currentOffset') / paginationWidth) + 1;
            var navigationHtml = ZEPHYR.Templates.PlanCycle.addPaginationNewUI({
                totalPages: Math.ceil((this.model.get('totalCount')) / paginationWidth),
                selectedPage: parseInt(this.model.get('currentOffset') / paginationWidth) + 1,
                entriesCount: paginationWidth,
                totalCount : this.model.get('totalCount'),
            });

            //  html = html +  ZEPHYR.Templates.PlanCycle.executionTable({
            //     executions: executionsList,
            //     url: contextPath,
            //     deleteUrl: getRestURL() + "/execution/",
            //     allStatusMap: this.model.get('allStatusList'),
            //     dtStatusName: statusColName,
            //     isCheckbox: this.model.get('isCheckbox'),
            //     isColumns: this.model.get('isColumns'),
            //     columns: ZEPHYR.Cycle.executionColumns,
            //     selectedId: this.model.get('selectedId'),
            //     customFieldsOrder: ZEPHYR.Cycle.planCycleCFOrder
            // });// render using closure template
            //
            // var navigationHtml = ZEPHYR.Templates.PlanCycle.addPrevNextNavigator({
            //      totalCount : this.model.get('totalCount'),
            //      currentIndex : parseInt(this.model.get('currentOffset')) + 1
            // });// render using closure template
            statusMap = this.model.get('allStatusList');
            ZEPHYR.Cycle.executions = executionsList;

            if(!clearData) {
                createCycleSummaryGrid(executionsList, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder);
                AJS.$('#cycle-executions-wrapper')[0].removeChild(AJS.$('.pagination-wrapper-newUI')[0]);
                AJS.$('#cycle-executions-wrapper .grid-componentWrapper').after(navigationHtml);
                clearData = true;
            } else {
                zephyrjQuery3(this.$el).html(html.concat(navigationHtml));
                createCycleSummaryGrid(executionsList, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder,true);
            }
            if(AJS.$("#inline-dialog-execution-column-picker").length > 0) {
              AJS.$("#inline-dialog-execution-column-picker").remove();
            }
            //vanillaGrid.templates.selectRow( null, 'cycleSummaryGrid', selectedExecutionId);
            highlightRow();
            if(!this.model.get('isColumns')) {
                return;
            }
            var executionColumnCustomization = ZEPHYR.Templates.PlanCycle.columnCustomisation({columns: ZEPHYR.Cycle.executionColumns, submitButtonId: submitButtonId, closeButtonId: closeButtonId});
            InlineDialog = AJS.InlineDialog(AJS.$("#columnCustomisation-inlineDialog"), "execution-column-picker",
              function(content, trigger, showPopup) {
                  content.css({"padding":"10px 0 0", "max-height":"none"}).html(executionColumnCustomization);
                  showPopup();
                  return false;
              },
              {
                width: 250,
                closeOnTriggerClick: true,
                persistent: true
              }
            );
        } else {
            var cycle = this.model.get('cycle');
            var inlineTriggerId = "inlineTrigger-" + cycle.versionId + "-cycleId-1-" + cycle.cycleId;
            var summaryHTML = ZEPHYR.Templates.PlanCycle.cycleAggregatedSummary({
                cycle: cycle, inlineTriggerId: inlineTriggerId
            });
            zephyrjQuery3(this.$el).html(summaryHTML);
        }
    }
});
var refreshTree = function() {
    zephyrjQuery3('#js-tree').jstree(true).refresh();
}


AJS.$(document).on("triggerGridClick", function(event, action) {
    switch(action) {
        case 'prev':
            // AJS.$('#prev-page-execution').trigger('click');
            executionsTableView.prevPageExecutions();
            break;

        case 'next':
            // AJS.$('#next-page-execution').trigger('click');
            executionsTableView.nextPageExecutions();
            break;
    }

    setTimeout(function(){
        //console.log(executionsTableModelNew.get('executions'));
        if(!executionsTableModelNew.get('executions').length)
            return;
        var allExecutions = executionsTableModelNew.get('executions');
        if(action === 'next'){
            AJS.$('#execution-table tbody tr:first-child').trigger('click');
        } else {
            AJS.$('#execution-table tbody tr:last-child').trigger('click');
        }

        //AJS.$(document).trigger( "triggerExecutionDetails", [ allExecutions[0].id, false, executionsTableModelNew.attributes.allStatusList, {total:parseInt(this.model.get('totalCount')), dbIndex: parseInt(cycleSelected.soffset) + parseInt(elementIndex), localIndex: parseInt(elementIndex), executionIds:executionIds} ] );
    },500);

});

AJS.$(document).on("updateSelectionId", function(event, executionId) {
    executionsTableModelNew.set('selectedId', executionId);
    selectedExecutionId = executionId;
    var query = updateLocationHash('detail', selectedExecutionId);
    updateRouter(query);
    executionsTableModelNew.set('emptyExecutions' , !executionsTableModelNew.get('emptyExecutions'));
});

AJS.$( document ).on( "updateExecutionModel", function(event, schedule) {
    //console.log(schedule);
    // this.executionIdDetails = null;
    if (executionsTableModelNew && executionsTableModelNew.attributes.executions) {
        var prevExecutions = JSON.parse(JSON.stringify(executionsTableModelNew.attributes.executions));
        prevExecutions.forEach(function(execution, index) {
            if (execution.id == schedule.id) {
                //schedule.defects = execution.defects;
                prevExecutions[index] = schedule;
                executionsTableModelNew.set('executions' , prevExecutions);
                //return -1;
            }
        })
    }
})

AJS.$( document ).on( "updateExecutionModelWithSteps", function(event, stepResult) {
    if (executionsTableModelNew && executionsTableModelNew.attributes.executions) {
        var prevExecutions = JSON.parse(JSON.stringify(executionsTableModelNew.attributes.executions));
        prevExecutions.forEach(function(execution, index) {
            if (execution.id == stepResult.executionId) {
                prevExecutions[index].stepDefectCount = stepResult.stepDefectCount;
                prevExecutions[index].totalDefectCount = stepResult.totalDefectCount;
                executionsTableModelNew.set('executions' , prevExecutions);
            }
        })
    }
})

var triggerTreeRefresh = function(node, action) {
    var selectedNodeId = zephyrjQuery3('#js-tree').jstree().get_state().core.selected[0];
    if(!selectedNodeId) {
        zephyrjQuery3("#js-tree").jstree(true).refresh();
        return;
    }
    var selectedNode = zephyrjQuery3('#js-tree').jstree().get_node(selectedNodeId);
    var cycleNode, selectDefaultNodeId;
    if(selectedNode && selectedNode.a_attr.nodeType === 'cycle') {
        cycleNode = selectedNode;
        selectDefaultNodeId = cycleNode && cycleNode.id.split('cycle')[0] + 'cycle--1';
    } else {
        cycleNode = zephyrjQuery3('#js-tree').jstree().get_node(selectedNode.parent);
        selectDefaultNodeId = cycleNode.id;
    }

    var versionNode = zephyrjQuery3('#js-tree').jstree().get_node(selectedNode.parents[1]);
    zephyrjQuery3("#js-tree").jstree(true).refresh_node(cycleNode.parent);
    if(node) {
        zephyrjQuery3("#js-tree").jstree(true).refresh_node(node);
    }
    if(selectDefaultNodeId && action === 'delete') {
        setTimeout(function () {
            zephyrjQuery3('#js-tree').jstree('select_node', selectDefaultNodeId);
        }, 1000);
    }
}

AJS.$( document ).on( "triggerTreeRefresh", function(event) {
    triggerTreeRefresh();
});

AJS.$(document).on("updateGridAndTree", function(event){
    refreshExecutionFlag = false;
    triggerTreeRefresh();
})

function changePagenationCount(event) {
    //  NEED TO HIT THE PUT/POST API HERE TO SET THE PAGINATION WIDTH

    var sendingData = allPagesPaginationPageWidth;
    sendingData.planCycleSummary = event.getAttribute("data-entries");
    AJS.$(document).scrollTop(AJS.$('#cycle-executions-wrapper').offset().top);
    var params = {
        url: '/preference/paginationWidthPreference',
        data: sendingData,
        method: 'PUT',
        noSuccessMsg: true,
    };
    xhrCall(params, function (response) {
        //  NEED TO UNCOMMENT THIS AS SOON AS THE API WORKS AND COMMENT OFF THE NEXT PART WRITTEN BELOW
        // paginationWidth = response.paginationWidth;
        // isOffsetReset = true;
        // fetchNodeExecutions(cycleNodeDuplicate,folderNodeDuplicate);
    })

    //  NEED TO REMOVE THIS WHEN THE API WORKS AND UN-COMMENT THE SUCCESS FUNCTION INSIDE THE API SUCCESS CALLBACK
    paginationWidth = event.getAttribute("data-entries");
    isOffsetReset = true;

    ZEPHYR.Cycle.clickedExecutionId = '';
    selectedExecutionId = null;

    cycleSelected.soffset = 0;
    queryParamsAllCycles.forEach(function (cycle, index) {
        if (cycle.cycleId == cycleSelected.cycleId && cycle.projectId == cycleSelected.projectId && cycle.versionId == cycleSelected.versionId && cycle.folderId == cycleSelected.folderId) {
            queryParamsAllCycles[index] = JSON.parse(JSON.stringify(cycleSelected));
        }
    });
    fetchNodeExecutions(cycleNodeDuplicate, folderNodeDuplicate);

}

ZEPHYR.Cycle.SelectView = Backbone.View.extend({
    tagName: 'select',
    className: 'select-dropdown',
    render: function() {
        this.initData();
    },
    initData: function() {
        if(!(this.model.attributes.items && this.model.attributes.items.length)) {
            this.model.fetch(function() {
                this.initData();
            });
        } else {
            var selectDropDown, options;
            this.model.items.forEach(function(item) {
            var option = '<option value="' +item.value + '">'+ item.content +'</option>';
                options += option;
            });
            selectDropDown = '<select>'+ options +'</select>';
            zephyrjQuery3(this.el).append(selectDropDown);
        }
        //console.log('inside selectdropdown');
    }
});


/*************************************/
/** Backbone Collection Declaration **/
/************************************/

ZEPHYR.Cycle.allCycles = Backbone.Collection.extend({
    model : ZEPHYR.Cycle.cycle,
    url : function() {
        var projectId = AJS.$("#projId").val(),
            versionId = AJS.$("#versionId").val() || -1;
        return getRestURL() + '/cycle?projectId='+projectId+'&versionId='+versionId+'&offset=0&expand=executionSummaries';
    }
});

ZEPHYR.Cycle.allVersions = Backbone.Collection.extend({
    model : ZEPHYR.Cycle.version,
    url : function() {
        var projectId = AJS.$("#projId").val();
        return getRestURL() + '/util/versionBoard-list?projectId='+projectId;
    }
});

ZEPHYR.Cycle.cycleDetails = Backbone.Collection.extend({
    model : ZEPHYR.Cycle.execution,
    initialize: function(options){
        this.options = options || {};
    },
    url : function() {
        var action = 'expand';
        var isFolder = this.options.folderId ? "&folderId=" + this.options.folderId : "";
        var isLimit = this.options.limit ? "&limit=" + this.options.limit : "";
        return getRestURL() + "/execution?cycleId=" + this.options.cycleId + "&action=" + action + "&projectId=" + this.options.projectId +  "&versionId="+
            this.options.versionId + isFolder + isLimit +"&offset=" + this.options.soffset + "&sorter=" + this.options.sortQuery ;
    }
});

var formatVersionData = function (versions, type, versionId) {
    var tempData = [];
    var projectId = AJS.$("#projId").val();
    versions.forEach(function(version) {
        var versionTitle = version.label;
        if(versionTitle.indexOf('"') > -1) {
          versionTitle = versionTitle.replace(/"/gi, '&quot;')
        }
        tempData.push({
            text : '<div class="js-node-data-custom"><div title="' + versionTitle + '">' + htmlEncode(version.label) + '</div>' +  '<div><div class="contextMenuIcon aui-icon aui-icon-small aui-iconfont-handle-horizontal"></div></div></div>',
            a_attr: {
                name: version.label,
                versionId: version.value,
                projectId: projectId,
                nodeType: 'version'
            },
            released: type,
            id: 'version-'+ version.value,
            children : true,
            state : {
                'opened' : versionId && version.value == versionId ? true : false,
                'disabled' : true
            }
        });
    });
    return tempData;
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

var executionSummaryHTMLSoy = function(params) {
    return executionSummaryHTML(params[0], params[1]);
}

var executionSummaryInlineDialogHTMLSoy = function(params) {
    return executionSummaryInlineDialogHTML(params[0], params[1]);
}

var executionSummaryHTML = function (executionSummary , totalCount) {
    //var executionSummaryString = $('<div> </div>');
    var executionSummaryString = '';
    executionSummary.forEach(function (executionObject) {
    if (executionObject.count) {
        executionSummaryString += '<span class="showSummary" style="width : ' + executionObject.count*100/totalCount + '%; background-color: ' + executionObject.statusColor + '"' +
                        '"> </span>';
    }
    });
    return executionSummaryString;
}

var executionSummaryInlineDialogHTML = function (executionSummary , totalCount) {
  var inlineContent = zephyrjQuery3('<div> </div>');
  executionSummary.forEach(function (executionObject) {
    if(executionObject.statusName .indexOf('<') > -1) {
            executionObject.statusName  = executionObject.statusName .replace(/</gi, '&lt;');
        }
        if(executionObject.statusName .indexOf('>') > -1) {
            executionObject.statusName  = executionObject.statusName .replace(/>/gi, '&gt;');
        }
    if (executionObject.count) {
        inlineContent.append('<div class="inlineExecutionSummary"><span>' + executionObject.statusName + '</span><span>'+ executionObject.count + ' / ' + Math.round(executionObject.count/totalCount*100) + '% </span></div>');
    }
  });
  return inlineContent.html();
}

var nodeHref = function(nodeObj, key) {

    var isFolder = !nodeObj.name ? true : false,
        fixVersion = nodeObj.fixVersion || nodeObj.versionName,
        cycleName = nodeObj.cycleName || nodeObj.name,
        folderName = nodeObj.folderName || nodeObj.folderName;


    var queryParam  = 'project = "' + addSlashes(nodeObj.projectKey) + '" AND fixVersion = "' + addSlashes(fixVersion) + '" AND cycleName in ("' + addSlashes(cycleName) + '")';
    if(nodeObj.isFolder) {
        queryParam += ' AND folderName in ("' + addSlashes(folderName) + '")';
    }
    if(folderName && key === 'totalExecuted') {
        queryParam += ' AND folderName in ("' + addSlashes(folderName) + '") AND executionStatus != ' + calculateUnexecutedLabel(nodeObj);
    } else if(key === 'totalExecuted') {
        queryParam += ' AND executionStatus != ' + calculateUnexecutedLabel(nodeObj);
    }  else if (key === 'totalCycleExecutions') {
        queryParam += ' AND folderName is EMPTY';
    } else if (key === 'totalExecutions') {
        queryParam += ' AND folderName in ("' + addSlashes(folderName) + '")';
    }
    var zqlQuery    = 'query=' + encodeURIComponent(queryParam);
    //console.log('query', queryParam, zqlQuery)
    return contextPath + '/secure/enav/#?' + zqlQuery;
}

function calculateUnexecutedLabel(nodeObj) {
    var label = 'UNEXECUTED';
    var executionStatusSummary = (nodeObj && nodeObj.executionSummaries && nodeObj.executionSummaries.executionSummary) || [];
    for (var i=0;i<executionStatusSummary.length; i++) {
        var status = executionStatusSummary[i];
        if (status.statusKey == '-1') {
            label =  status.statusName;
            label = '"' + label + '"';
            break;
        }
    }
    return label;
}

function htmlEncode(value){
    return zephyrjQuery3('<div/>').text(value).html();
}
function htmlDecode(value){
    return zephyrjQuery3('<div/>').html(value).text();
}
var cleanGarb = function (el) {
    el = el.replace('&amp;','&');
    el = el.replace('&lt;','<');
    el = el.replace('&gt;','>');
    return el;
}

var nodeHTML = function (nodeObj, key) {
    var fixVersion, projectKey;
    var isFolder = !nodeObj.name ? true : false;
    var name = isFolder ? nodeObj.folderName : nodeObj.name;
    var title = cleanGarb(name.replace(/"/g, '\''));
    //console.log('nodeObj in nodeHTML', nodeObj);
    // var href = nodeHref({
    //     fixVersion: nodeObj.versionName,
    //     isFolder: isFolder,
    //     cycleName: nodeObj.name || nodeObj.cycleName,
    //     folderName: nodeObj.folderName,
    //     projectKey: nodeObj.projectKey
    // });
    var nodeName = nodeName = name,
        //nodeName = isFolder ? name : '<a href='+ href +' title="'+title+'">'+name+'</a>',
        executionSummary = nodeObj.executionSummaries ? nodeObj.executionSummaries.executionSummary : [],
        totalCount = nodeObj.totalExecutions;
    var inlineDialogTriggerId = '';
    var hasInlineDialog = false;
    var inlineDialogHtml = '>';
    nodeName = cleanGarb(nodeName);
    if(nodeObj.totalExecutions && key) {
      inlineDialogTriggerId = 'inlineTrigger-' + nodeObj.versionId + '-cycleId-' + key ;
      hasInlineDialog = true;
    } else if(nodeObj.folderId) {
      inlineDialogTriggerId = 'inlineTrigger-' + nodeObj.versionId + '-folderId-' + nodeObj.folderId;
      hasInlineDialog = true;
    }
    if(hasInlineDialog) {
      inlineDialogHtml = " id=" + inlineDialogTriggerId + ' onmouseout=closeSummaryInlineDialog(event)' + ' onmouseover=summaryInlineDialog(event,"' + inlineDialogTriggerId + '")>' + '<div style="display:none"><div class="'+ inlineDialogTriggerId +'">' + executionSummaryInlineDialogHTML(executionSummary, totalCount) + '</div></div>';
    }

    //<a id="version-expando-{$cycleId}" class="versionBanner-link" href="{$restUrl}?cycleId={$cycleId}&action={$action}&offset={$offset}&sorter={$sorter}"></a>
    return '<div class="js-node-data-custom"><div title="'+title+'">' + htmlEncode(nodeName) + '</div>' +  '<div><div class="executionSummaryBar showSummary"' + inlineDialogHtml + executionSummaryHTML(executionSummary , totalCount) + '</div>  <div class="contextMenuIcon aui-icon aui-icon-small aui-iconfont-handle-horizontal"></div></div> </div>';

    // return '<div class="js-node-data-custom"><div title="'+title+'">' + htmlEncode(nodeName) + '</div>' + '<div class="contextMenuIcon aui-icon aui-icon-small aui-iconfont-handle-horizontal"></div></div> </div>';
}

var closeSummaryInlineDialog = function(event) {
  if(event.relatedTarget && event.relatedTarget.className.indexOf('showSummary') === -1 ) {
    stepAttachmentInlineDialog && stepAttachmentInlineDialog.hide();
  }
}

AJS.$('.aui-inline-dialog-contents').live('mouseover', function(event) {
  if(event.relatedTarget.className.indexOf('showSummary') > -1) {
    stepAttachmentInlineDialog && stepAttachmentInlineDialog.hide();
  }
});

AJS.$('#inlineDialog').live('click', function(event) {
    var htmlContent = AJS.$(this).next('.cycle-description').html();
    var descriptionInlineDialog = AJS.InlineDialog(AJS.$("#inlineDialog"), "description",
        function(content, trigger, showPopup) {
            content.css({"padding":"20px"}).html(htmlContent);
            showPopup();
            return false;
        }
    );
    descriptionInlineDialog.show(event);
});



var summaryInlineDialog = function(event, triggerContent) {
  if((event.relatedTarget && event.relatedTarget.className.indexOf('showSummary') === -1 && event.target.className.indexOf('showSummary') > -1)) {
    var html = AJS.$("." + triggerContent)[0].innerHTML;
    if(AJS.$("#inline-dialog-" + triggerContent).length > 0) {
        AJS.$("#inline-dialog-" + triggerContent).remove();
    }
    stepAttachmentInlineDialog = AJS.InlineDialog(AJS.$("#" + triggerContent), triggerContent,
        function(content, trigger, showPopup) {
                content.css({"padding":"10px"}).html(html);
                showPopup();
                return false;
        },
        {
            width: 180,
            closeOnTriggerClick: true,
            persistent: true,
            noBind: true
        }
    );
      stepAttachmentInlineDialog.show(event);
    }
}


var addSlashes = function(str) {
    if(str) {
        return str.replace(/\\/g, '\\\\').
            replace(/\u0008/g, '\\b').
            replace(/\t/g, '\\t').
            replace(/\n/g, '\\n').
            replace(/\f/g, '\\f').
            replace(/\r/g, '\\r').
            replace(/'/g, '\\\'').
            replace(/"/g, '\\"');
    }
}

var returnMappedNode = function(node, key) {
    var allNodes, match;
    switch(key) {
        case 'release':
            toCompare = 'name';
            break;

        case 'version':
            toCompare = 'versionId';
            break;

        case 'cycle':
            toCompare = 'cycleId';
            break;
        case 'folder':
            toCompare = 'folderId';
            break;
    }
    allNodes = zephyrjQuery3('#js-tree').jstree(true).get_json('#', {flat:true});
    var match = allNodes.filter(function(entry){
        return (entry.a_attr.nodeType === key) && (entry.a_attr[toCompare] === node.a_attr[toCompare])
    });
    if(match && match.length){
        return match[0];
    } else {
        return undefined;
    }
}
var selectNode = function(node) {
    var nodeToSelect = returnMappedNode(node, node.a_attr.nodeType);
    if(!nodeToSelect) {
        return;
    }
    zephyrjQuery3('#js-tree').jstree('select_node', nodeToSelect.id);
}

var toggleNode = function(node, key) {
    var nodeToToggle = returnMappedNode(node, key);
    if(!zephyrjQuery3('#js-tree').jstree(true).is_open(nodeToToggle)) {
        zephyrjQuery3('#js-tree').jstree(true).toggle_node(nodeToToggle)
    }

}

window.onbeforeunload = function (e) {
    return ;
}

/*var getInAppMessage = function() {
    var options = {'className': 'cycle_summary_in_app', 'containerId': 'content'};
    var inAppMessage = new InAppMessage(options);
    inAppMessage.createShowHideButton();
    //inAppMessage.fetchInAppMessageData(options);
};*/

var bulkDeleteConfirmationDialog = function (selectedSchedules) {
    var instance = this,
    dialog = new JIRA.FormDialog({
        id: "execution-delete-dialog",
        content: function (callback) {
            var innerHtmlStr = ZEPHYR.Bulk.deleteExecutionConfirmationDialog();
            callback(innerHtmlStr);
        },

        submitHandler: function (e) {
            deleteSchedules(selectedSchedules, dialog, function () {
                dialog.hide();
                triggerTreeRefresh();
            });
            e.preventDefault();
         }
    });

    dialog.show();
}

var deleteSchedules = function(selectedScheduleItems, deletionDialog, completed) {

    var intervalId;
    AJS.$(document).off('click', '#aui-dialog-close');
    AJS.$(document).on('click', '#aui-dialog-close', function (event, value) {
        if (!isDeleteSchedulesFail) {
            clearInterval(intervalId);
            completed.call();
        } else {
            isDeleteSchedulesFail = false;
        }
    })
    
    var xhr = {
        url: '/execution/deleteExecutions',
        method: 'DELETE',
        data: {
            'executions' : selectedScheduleItems
        },
        noSuccessMsg: true
    }
    xhrCall(xhr, function(response) {
        if (response != null || response != undefined) {
            var jobProgressToken = response.jobProgressToken;
            var msgDlg = new JIRA.FormDialog({
                id: "warning-message-dialog",
                content: function (callback) {
                    var innerHtmlStr = ZEPHYR.Bulk.warningBulkDeleteDialogContent({
                        warningMsg: AJS.I18n.getText('zephyr.je.bulk.execution.delete.in.progress'),
                        progress: 0,
                        percent: 0,
                        timeTaken: 0
                    });
                    callback(innerHtmlStr);
                }
            });
            msgDlg.show();
            intervalId = setInterval(function () {
                jQuery.ajax({
                    url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + jobProgressToken,
                    data: {'type': "bulk_executions_delete_job_progress"},
                    complete: function (jqXHR, textStatus) {
                        var data = jQuery.parseJSON(jqXHR.responseText);
                        if (data.progress == 0 && data.errorMessage && data.errorMessage != '') {
                            isDeleteSchedulesFail = true;
                            showErrorMessage(data.errorMessage, 5000);
                            AJS.$('#aui-dialog-close').click();
                            clearInterval(intervalId);
                        } else {
                            AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
                            AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
                            AJS.$(".timeTaken").html("Time Taken: " + data.timeTaken);
                            if (data.progress == 1) {
                                var dataContent = jQuery.parseJSON(data.message);
                                var errZPermission = ((dataContent.noIssuePermission != undefined && dataContent.noIssuePermission != "-")?dataContent.noIssuePermission:"");
                                var errPermission = ((dataContent.noPermission != undefined && dataContent.noPermission != "-")?dataContent.noPermission:"");
                                AJS.$("#execs-delete-aui-message-bar .aui-message").html(AJS.I18n.getText('enav.bulk.delete.operation.warn', dataContent.success, dataContent.error, errZPermission + " "+ errPermission));
                                clearInterval(intervalId);
                                if (completed)
                                    completed.call();
                            }
                        }
                    }
                })
            }, 1000);
        }
    });
}



var attachResizable = function() {
    AJS.$("#tree-tcr").resizable({
        handles: 'e',
        minWidth: 0,
        maxWidth: 1200,
        zIndex: 100
    });
    AJS.$('#tree-tcr').find('.ui-resizable-e').addClass('aui-icon aui-icon-small').append(AJS.$('<span></span>').attr('id', 'tree-docker-container'));
    AJS.$('#tree-docker-container').append(AJS.$('<span></span>').attr('id', 'tree-docker'));
    // AJS.$('#tree-tcr').find('.ui-resizable-e').addClass('aui-icon aui-icon-small').append(AJS.$('<span></span>').attr('id', 'tree-docker'));
    // AJS.$('#tree-docker').attr('data-title', 'Collapse tree');
};

var highlightRow = function() {
    //hacking it for chrome with a timeout
    setTimeout(function() {
        vanillaGrid.templates.selectRow( null, 'cycleSummaryGrid', selectedExecutionId);
    }, 1500);
}

InitPageContent(function(){

    if(zephyrjQuery3.jstree) {
        zephyrjQuery3.browser = AJS.$.browser;
    }

    if(!zephyrjQuery3.jstree) {
        return;
    }

    var planCycleTemplate = ZEPHYR.Templates.PlanCycle.planCycleView();
    var breadcrumbTemplate = ZEPHYR.Templates.PlanCycle.breadCrumbsView();
    AJS.$('#cycle-view-wrap').append(planCycleTemplate);
    AJS.$('#breadcrumbs-wrapper').append(breadcrumbTemplate);


    attachResizable();

    //getInAppMessage();

    AJS.$('#tree-docker').on('click', function() {
        AJS.$('.tree-tcr').toggleClass('collapse');
        if(AJS.$('.tree-tcr').hasClass('collapse')) {
            // AJS.$(this).attr('data-title', 'Expand tree');
            AJS.$('#tree-tcr').find('.ui-resizable-e').addClass('hideResizer');
            AJS.$('#create-cycle-tree-trigger').hide();
            //AJS.$('#tree-docker').css("visibility", "visible");
            AJS.$('#tree-tcr').find('.module').css("visibility", "hidden");
        } else {
            // AJS.$(this).attr('data-title', 'Collapse tree');
            AJS.$('#tree-tcr').find('.ui-resizable-e').removeClass('hideResizer');
            AJS.$('#create-cycle-tree-trigger').show();
            //AJS.$('#tree-docker').css("visibility", "hidden");
            AJS.$('#tree-tcr').find('.module').css("visibility", "visible");
        }
    });

    AJS.$('#tree-docker').on('hover', function() {
        AJS.$('#cycle-details').css('border-left', '1px solid #4C9AFF');
    });

    AJS.$('#tree-docker').on('mouseleave', function() {
        AJS.$('#cycle-details').css('border-left', '1px solid #ecedf0');
    });

    AJS.$('#create-cycle-tree-trigger').on('click', function() {

        var projectId = AJS.$("#zprojectId")[0].value;
        var lastSelectedVersion = AJS.$("#select-version2 :selected").val();

        AJS.$('#create-cycle-tree-trigger').addClass('no-hover');
        var projectId = AJS.$("#zprojectId")[0].value;
        var lastSelectedVersion = AJS.$("#select-version2 :selected").val();
        if(selectedVersionFromTree) {
            lastSelectedVersion = selectedVersionFromTree;
        }
        ZEPHYR.Cycle.CycleFunction.createCycleDialog(projectId, ZEPHYR.Cycle.versionList, lastSelectedVersion.toString(), null, null, function(response) {
            var parentVersion;
            if(response.versionId) {
                parentVersion = 'version-' + response.versionId;
            }
            var parentNode = zephyrjQuery3('#js-tree').jstree().get_node(parentVersion);
            zephyrjQuery3("#js-tree").jstree(true).refresh_node(parentNode);
            showExecStatusSuccessMessage(htmlEncode(response.responseMessage));
        });
    });

    AJS.$('#zephyr-je-dlgclose').live('click', function() {
        AJS.$('#create-cycle-tree-trigger').removeClass('no-hover');
    });

    if(!AJS.$("#projId").val()) {
        return;
    }

    var xhrCustomField = {
        url: '/customfield/entity?entityType=EXECUTION&projectId=' + AJS.$("#projId").val(),
        method: 'GET'
    }
    xhrCall(xhrCustomField, function(response) {

        // var xhrCustomFieldByProject = {
        //     url: '/customfield/byEntityTypeAndProject?entityType=EXECUTION&projectId=' + AJS.$("#projId").val(),
        //     method: 'GET'
        // }
        // xhrCall(xhrCustomFieldByProject, function (responseByProject) {
            // for (var counterByProject = 0; counterByProject < responseByProject.length; counterByProject += 1) {
            //     response.push(responseByProject[counterByProject]);
            // }
            allCustomFieldsPC = response;
            setCustomFieldsObjectsPlanCycle();
            getExecutionCustomizationPlanCycle(function () {
                ZEPHYR.Cycle.planCycleCFOrder.map(function (customField) {
                    if (!ZEPHYR.Cycle.executionColumns[customField.customfieldId]) {
                        ZEPHYR.Cycle.executionColumns[customField.customfieldId] = { 'displayName': customField.customFieldName, 'isVisible': 'false' }
                    }
                })

                // treeViewInstance.render();
            });
        // });

    });

    var treeViewInstance = new ZEPHYR.Cycle.TreeView({
        el : '#js-tree'
    });

    treeViewInstance.render();
    var tabType = window.location.hash;
    if(tabType.indexOf('test-summary-tab') > -1 || tabType === "") {
        AJS.$("#selected-tab").text(AJS.I18n.getText('zephyr-je.pdb.test.summary.label'));
    }
    if(tabType.indexOf('traceability-tab') > -1) {
        AJS.$("#selected-tab").text(AJS.I18n.getText('zephyr-je.pdb.traceability.label'));
    }

    var PlanCycleRouter = Backbone.Router.extend({
        routes: {
            '*path&view=*query'   :       "updateView"
        },
        updateView: function(path, query) {
            var viewType = query.split('&')[0],
                executionId = query.split('&executionId=')[1];
                selectedExecutionId = executionId;
            //console.log('path and query', query, viewType, executionId);
            var grid;
            var intervalId = setInterval(function() {
                if(grid) {
                    clearInterval(intervalId);
                    if(viewType === 'list') {
                        //vanillaGrid.templates.selectRow( null, 'cycleSummaryGrid', executionId);
                        highlightRow();
                    } else {
                        detailView(selectedExecutionId);
                        ZEPHYR.Loading.hideLoadingIndicator();
                    }
                }
                grid = document.getElementById('cycleSummaryGrid');

            }, 100);
            if(viewType === 'detail') {
                if(!(AJS.$('.tree-tcr').hasClass('collapse'))) {
                    AJS.$('#tree-docker').trigger('click');
                }
                AJS.$('#cycle-executions-wrapper').addClass('hide');
                AJS.$('#cycle-info-wrapper').addClass('hide');
                AJS.$('.execution-details').removeClass('hide');
                AJS.$('#detailViewBtn').addClass('active-view');
                AJS.$('#listViewBtn').removeClass('active-view');
                ZEPHYR.Loading.showLoadingIndicator();
            }


        }
    });
    ZEPHYR.Cycle.router = new PlanCycleRouter();
    Backbone.history.start();
});


var createMoveExecutionGrid = function (executionRow, allColumns, customColumns) {
    var config = {
        "head": [
            {
                key: 'issueKey',
                displayName: AJS.I18n.getText('project.cycle.schedule.table.column.id'),
                isFreeze: false,
                editable: false,
                isInlineEdit: false,
                type: 'String',
                isSortable: false,
                sortOrder: '',
                isVisible: true,
            },
            {
                key: 'status',
                displayName: AJS.I18n.getText('enav.status.label'),
                isFreeze: false,
                editable: false,
                isInlineEdit: false,
                type: 'SELECT_STATUS',
                imgUrl: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg',
                isSortable: false,
                sortOrder: '',
                executionSummaries: statusMap,
                isVisible: true,
                isGrid: true
            },
            {
                key: 'summary',
                displayName: AJS.I18n.getText('project.cycle.schedule.table.column.summary'),
                isFreeze: false,
                editable: false,
                isInlineEdit: false,
                isSortable: false,
                type: 'String',
                isVisible: true,
            },
            {
                key: 'defects',
                displayName: AJS.I18n.getText('execute.test.defect.label'),
                isFreeze: false,
                editable: false,
                isInlineEdit: false,
                isSortable: false,
                type: 'String',
                isVisible: true,
            },
            {
                key: 'component',
                displayName: AJS.I18n.getText('project.cycle.schedule.table.column.component'),
                isFreeze: false,
                editable: false,
                isInlineEdit: false,
                isSortable: false,
                type: 'String',
                isVisible: true,
            },
            {
                key: 'label',
                displayName: AJS.I18n.getText('project.cycle.schedule.table.column.label'),
                isFreeze: false,
                editable: false,
                isInlineEdit: false,
                isSortable: false,
                type: 'String',
                isVisible: true,
            },
            {
                key: 'executedBy',
                displayName: AJS.I18n.getText('project.cycle.schedule.table.column.executedBy'),
                isFreeze: false,
                editable: false,
                isInlineEdit: false,
                isSortable: false,
                sortOrder: '',
                type: 'String',
                isVisible: true,
            },
            {
                key: 'executedOn',
                displayName: AJS.I18n.getText('project.cycle.schedule.table.column.executedOn'),
                isFreeze: false,
                editable: false,
                isInlineEdit: false,
                isSortable: false,
                sortOrder: '',
                type: 'String',
                isVisible: true,
            },
            {
                key: 'assignee',
                displayName: AJS.I18n.getText('execute.test.execution.assignee.label'),
                isFreeze: false,
                editable: false,
                isInlineEdit: false,
                isSortable: false,
                type: 'String',
                isVisible: true,
            }
        ],
        "row": [],
        "bulkActions": [{
            actionName: AJS.I18n.getText('zephyr.je.cycle.heading.selectAll'),
            customEvent: 'selectRows',
            disabled: ((executionRow.length === 0) ? true : false),
            imgSrcChecked: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/select-all_button.svg',
            imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/deselect_button.svg'
        },
        ],
        "actions": [
            {
                actionName: AJS.I18n.getText('execute.dialog.execute.planned.label'),
                customEvent: 'executeRow',
                imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/execute_icon.svg',
            },
        ],
        "maxFreezed": 0,
        "hasBulkActions": true,
        "rowSelection": true,
        "highlightSelectedRows": false,
        "freezeToggle": false,
        "draggableRows": false,
        "checkedRowId": moveExecutionSelectedId,
        "resetSelectAll": resetSelectAll,
        "gridComponentPage": 'moveExecutionGrid',
        "showLoader": true,
        "freezeImageUrl": contextPath + '/download/resources/com.thed.zephyr.je/images/icons/pin_button.svg',
        "columnChooserUrl": contextPath + '/download/resources/com.thed.zephyr.je/images/icons/column-chooser-white_button.svg',
        "initialCount": getGridInitialCount(initialCountCycleSummary),
        "columnChooserHeading": AJS.I18n.getText('cycle.ColumnChooser.label'),
        "selectAtleaseoneOption": AJS.I18n.getText('zephyr.gridData.selectAtleastOneOption'),
        "submit": AJS.I18n.getText('zephyr.je.submit.form.Submit'),
        "cancel": AJS.I18n.getText('zephyr.je.submit.form.cancel'),
        "noPermission": AJS.I18n.getText('zephyr.cycle.noPermissionOnTestAndIssue'),
        "placeholderText": AJS.I18n.getText('zephyr.customfield.textarea.placeholder'),
        "action": AJS.I18n.getText('cycle.actions.label'),
        "loading": AJS.I18n.getText('zephyr.gridData.loading'),
        "contextPath": contextPath,
        "dataset": [{
            name: 'executionid',
            key: 'id'
        }],
        "noPermissionTestIssue": AJS.I18n.getText('cycle.noPermissionTestIssue.label')
    }

    customColumns.map(function (columnCell) {
        var obj = {
            "key": columnCell.customfieldId,
            "displayName": columnCell.customFieldName,
            "isFreeze": false,
            "editable": false,
            "isInlineEdit": false,
            "type": (columnCell.customFieldType == 'TEXT' || columnCell.customFieldType == 'LARGE_TEXT') ? 'HTMLContent' : 'String',
            "isSortable": false,
            "isVisible": false,
        }
        config.head.push(obj);
    });

    config.head.map(function (column) {
        if (column.key != 'issueKey' && (allColumns[column.key] && allColumns[column.key].isVisible === 'true')) {
            column.isVisible = true;
        } else if (column.key === 'issueKey') {
            column.isVisible = true;
        } else {
            column.isVisible = false;
        }
    });

    executionRow.map(function (row, index) {
        var obj = row;
        if (index === 0 && !selectedExecutionId) {
            selectedExecutionId = obj.id;
        }
        var customFields = {};
        if (typeof obj.customFields === 'string')
            customFields = JSON.parse(obj.customFields);
        else
            customFields = obj.customFields;

        Object.keys(customFields).map(function (field) {
            if (customFields[field].customFieldType === 'TEXT' || customFields[field].customFieldType === 'LARGE_TEXT') {
              obj[customFields[field].customFieldId] = customFields[field].htmlValue;
            } else {
               obj[customFields[field].customFieldId] = customFields[field].value;
            }
        });

        if (obj.defects) {
            obj.defects.map(function (defect) {
                defect['color'] = statusMap[obj.executionStatus].color;
            })
        }

        if ((obj.id === stepLevelDefectsExecutionId)) {
            if (showDefectsPopup) {
                obj['stepDefect'] = stepLevelDefects;
                obj['showPopup'] = true;
            } else {
                obj['showPopup'] = false;
            }
        }
        var url = getExecutionUrl({ execution: row, index: currentMoveExecutionPageNumber, paginationWidth: allPagesPaginationPageWidth.moveExecution, url: contextPath});
            url +='&view=detail';
        obj.executionUrl = url;
        obj.assignee = row.assignedTo;
        config.row.push(obj);
    });
    resetSelectAll = false;
    vanillaGrid.init(document.getElementById('moveExecutionGrid'), config);
    //AJS.$('#moveExecutionGrid').attr('config', JSON.stringify(config));
    AJS.$(document).unbind('gridBulkActions');
    AJS.$(document).bind('gridBulkActions', moveExecutionSelectRow);
    AJS.$(document).unbind('gridActions');
    AJS.$(document).bind('gridActions', gridComponentActions);

}

var moveExecutionSelectRow = function(ev){
    ev.preventDefault();
    ev.stopPropagation();
    ev.stopImmediatePropagation();
    if (ev.originalEvent.detail.actionName === 'rowSelection' || ev.originalEvent.detail.actionName === 'selectRows') {
        ev.originalEvent.detail.rowData.map(function(row){
            if (row.selected && moveExecutionSelectedId.indexOf(row.rowId) === -1) {
                moveExecutionSelectedId.push(row.rowId);
            } else if (!row.selected && moveExecutionSelectedId.indexOf(row.rowId) !== -1){
                moveExecutionSelectedId = moveExecutionSelectedId.filter(function(id){
                    return id !== row.rowId
                });
            }
        });
        if (moveExecutionSelectedId.length) {
            checkMoveFormValidity(true);
        } else {
            checkMoveFormValidity(false);
        }
    }

};

AJS.$('#moveExecutionsTables .dropDown-Trigger').live('click', function (ev) {
    ev.stopPropagation();
    ev.preventDefault();
    ev.stopImmediatePropagation();
    if (AJS.$('#moveExecutionsTables .entries-per-page .dropDown-container').hasClass('active')) {
        AJS.$('#moveExecutionsTables .entries-per-page .dropDown-container').removeClass('active');
    } else {
        AJS.$('#moveExecutionsTables .entries-per-page .dropDown-container').addClass('active');
    }
});

AJS.$('#moveExecutionsTables .chooseEntries').live('click', function (ev) {
    ev.stopPropagation();
    ev.preventDefault();
    ev.stopImmediatePropagation();

    var sendingData = allPagesPaginationPageWidth;
    sendingData.moveExecution = ev.target.dataset.entries;
    var params = {
        url: '/preference/paginationWidthPreference',
        data: sendingData,
        method: 'PUT',
        noSuccessMsg: true,
    };
    xhrCall(params, function (response) {
        var numberOfEntries = ev.target.dataset.entries;
        AJS.$('#moveExecutionsTables .entries-per-page .dropDown-container').removeClass('active');
        moveExecutionNodeParam.pageWidth = parseInt(numberOfEntries);
        fetchGridToMove(moveExecutionNodeParam)
    })
});

AJS.$('#moveExecutionsTables #prev-page-execution').live('click', function (ev) {
    moveExecutionNodeParam.soffset = (parseInt(moveExecutionNodeParam.soffset) || 0)  - parseInt(moveExecutionNodeParam.pageWidth);
    resetSelectAll = true;
    fetchGridToMove(moveExecutionNodeParam)
});

AJS.$('#moveExecutionsTables #next-page-execution').live('click', function (ev) {
    moveExecutionNodeParam.soffset = (parseInt(moveExecutionNodeParam.soffset) || 0) + parseInt(moveExecutionNodeParam.pageWidth);
    resetSelectAll = true;
    fetchGridToMove(moveExecutionNodeParam)
});

AJS.$('#moveExecutionsTables .goToPage').live('click', function (ev) {
    moveExecutionNodeParam.soffset = (ev.target.dataset.offset === "0") ? 0 : (parseInt(ev.target.dataset.offset) - 1) * moveExecutionNodeParam.pageWidth;
    resetSelectAll = true;
    fetchGridToMove(moveExecutionNodeParam)
});

var dummyConfig = {};
var createCycleSummaryGrid = function(executionRows, allColumns, customColumns, refresh) {
    var config = {
        "head" :[
          {
            key: 'issueKey',
            displayName: AJS.I18n.getText('project.cycle.schedule.table.column.id'),
            isFreeze : false,
            editable : false,
            isInlineEdit : false,
            type: 'String',
            isSortable : true,
            sortOrder : '',
            isVisible : true,
          },
          {
            key: 'status',
            displayName: AJS.I18n.getText('enav.status.label'),
            isFreeze: false,
            editable: true,
            isInlineEdit: true,
            type: 'SELECT_STATUS',
            imgUrl : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg',
            isSortable: true,
            sortOrder : '',
            executionSummaries: statusMap,
            isVisible : true,
            isGrid : true
          }, {
            key: 'summary',
            displayName: AJS.I18n.getText('project.cycle.schedule.table.column.summary'),
            isFreeze: false,
            editable: false,
            isInlineEdit: false,
            isSortable: false,
            type: 'String',
            isVisible : true,
          }, {
            key: 'defects',
            displayName: AJS.I18n.getText('execute.test.defect.label'),
            isFreeze: false,
            editable: false,
            isInlineEdit: false,
            isSortable: false,
            type: 'String',
            isVisible : true,
          }, {
            key: 'component',
            displayName: AJS.I18n.getText('project.cycle.schedule.table.column.component'),
            isFreeze: false,
            editable: false,
            isInlineEdit: false,
            isSortable: false,
            type: 'String',
            isVisible : true,
          },
          {
            key: 'label',
            displayName: AJS.I18n.getText('project.cycle.schedule.table.column.label'),
            isFreeze: false,
            editable: false,
            isInlineEdit: false,
            isSortable: false,
            type: 'String',
            isVisible : true,
          },
          {
            key: 'executedBy',
            displayName: AJS.I18n.getText('project.cycle.schedule.table.column.executedBy'),
            isFreeze: false,
            editable: false,
            isInlineEdit: false,
            isSortable: true,
            sortOrder : '',
            type: 'String',
            isVisible : true,
          },
          {
            key: 'executedOn',
            displayName: AJS.I18n.getText('project.cycle.schedule.table.column.executedOn'),
            isFreeze: false,
            editable: false,
            isInlineEdit: false,
            isSortable: true,
            sortOrder : '',
            type: 'String',
            isVisible : true,
          },
          {
            key: 'assignee',
            displayName: AJS.I18n.getText('execute.test.execution.assignee.label'),
            isFreeze: false,
            editable: false,
            isInlineEdit: false,
            isSortable: false,
            type: 'String',
            isVisible : true,
          }
        ],
        "row" :[],
        "actions" : [
            {
                actionName: AJS.I18n.getText('execute.dialog.execute.planned.label'),
                customEvent : 'executeRow',
                imgSrc : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/execute_icon.svg',
            },
            {
                actionName: AJS.I18n.getText('enav.bulk.delete.schedule.label'),
                customEvent : 'deleteRow',
                imgSrc : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/delete_button.svg',
            }
        ],
        "maxFreezed" : 2,
        "bulkActions" : [{
                actionName: AJS.I18n.getText('zephyr.je.cycle.heading.selectAll'),
                customEvent : 'selectRows',
                disabled : ((executionRows.length === 0) ? true : false),
                imgSrcChecked : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/select-all_button.svg',
                imgSrc : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/deselect_button.svg'
            },
            {
                actionName: AJS.I18n.getText('enav.bulk.delete.schedule.label'),
                customEvent : 'bulkDelete',
                disabled: cycleSummaryGridSelectedExecutions.length ? false : true,
                imgSrc : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/delete_button.svg',
                imgSrcChecked : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/delete_button.svg',
            }
        ],
        "columnchooser": {
            isEnabled: true,
            actionName: AJS.I18n.getText('enav.executions.view.column.title'),
            columnChooserValues: [],
            customEvent: 'columnchooser',
            imgSrc:  contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg'
        },
        "addTests" : {
            isEnabled: true,
            actionName: AJS.I18n.getText('cycle.operation.add.tests.label'),
            customEvent : 'addTests',
            imgSrc : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/plus_button.svg',
        },
        "hasBulkActions" : true,
        "rowSelection" : true,
        "freezeToggle" : true,
        "highlightSelectedRows": true,
        "draggableRows" : false,
        "refreshGrid": refresh ? !refreshGrid : refreshGrid,
        "gridComponentPage": 'cycleSummaryGrid',
        "freezeImageUrl" : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/pin_button.svg',
        "unfreezedImageUrl" : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/pin_button.svg',
        "columnChooserUrl" : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/column-chooser-white_button.svg',
        "selectedRowId": ZEPHYR.Cycle.clickedExecutionId,
        "initialCount": getGridInitialCount(initialCountCycleSummary),
        "showLoader" : true,
        "freezeTooltip" : AJS.I18n.getText('zephyr.common.freezePin.tooltip.label'),
        "unfreezeTooltip" : AJS.I18n.getText('zephyr.common.unfreezePin.tooltip.label'),
        "checkedRowId": cycleSummaryGridSelectedExecutions,
        "columnChooserHeading": AJS.I18n.getText('cycle.ColumnChooser.label'),
        "selectAtleaseoneOption": AJS.I18n.getText('zephyr.gridData.selectAtleastOneOption'),
        "submit": AJS.I18n.getText('zephyr.je.submit.form.Submit'),
        "cancel": AJS.I18n.getText('zephyr.je.submit.form.cancel'),
        "noPermission": AJS.I18n.getText('zephyr.cycle.noPermissionOnTestAndIssue'),
        "placeholderText": AJS.I18n.getText('zephyr.customfield.textarea.placeholder'),
        "action": AJS.I18n.getText('cycle.actions.label'),
        "loading": AJS.I18n.getText('zephyr.gridData.loading'),
        "contextPath": contextPath,
        "dataset": [{
            name: 'executionid',
            key: 'id'
        }],
        "noPermissionTestIssue": AJS.I18n.getText('cycle.noPermissionTestIssue.label')
    }

    customColumns.map(function(columnCell){
        var obj = {
            "key": columnCell.customfieldId,
            "displayName" : columnCell.customFieldName,
            "isFreeze" : false,
            "editable" : false,
            "isInlineEdit" : false,
            "type": (columnCell.customFieldType == 'TEXT' || columnCell.customFieldType == 'LARGE_TEXT') ? 'HTMLContent' : 'String',
            "isSortable" : false,
            "isVisible" : false,
          }
        config.head.push(obj);
    })

    var freezeColumns = [];
    if (allPagesPaginationPageWidth.freezeColumns) {
        freezeColumns = allPagesPaginationPageWidth.freezeColumns.split(',');
    } else if (!allPagesPaginationPageWidth.hasOwnProperty('freezeColumns')) {
        var defaultFreezeColumn = "issueKey,summary";
        freezeColumns = defaultFreezeColumn.split(',');
    }

    config.head.map(function(column) {
        if (freezeColumns.includes(column.key.toString())) {
            column.isFreeze = true;
        }
        if(column.isSortable) {
            if(column.key === sortKey) {
                column.sortOrder = sortOrder;
            } else {
            column.sortOrder = '';
            }
        }
        if (column.key != 'issueKey' && (allColumns[column.key] && allColumns[column.key].isVisible === 'true')) {
            column.isVisible = true;
        } else if(column.key === 'issueKey') {
            column.isVisible = true;
        } else {
            column.isVisible = false;
        }
    });

    //console.log('allColumns', allColumns, config.head);
    var columnChooserValues = {}
    config.head.forEach(function(column){
        if (column.key !== "issueKey" && column.key !== "orderId") {
            columnChooserValues[column.key] = {
                displayName: column.displayName,
                isVisible: allColumns[column.key] && allColumns[column.key]['isVisible']
            };
        }
    });
    config.columnchooser.columnChooserValues = columnChooserValues;
    executionRows.map(function(row, index) {
        var obj = row;
        if (ZEPHYR.Cycle.clickedExecutionId) {
            selectedExecutionId = ZEPHYR.Cycle.clickedExecutionId;
        } else if(index === 0 && !selectedExecutionId) {
            selectedExecutionId = obj.id;
        }
        var customFields = {};
        if(typeof obj.customFields === 'string')
            customFields = JSON.parse(obj.customFields);
        else
            customFields = obj.customFields;

        var url = getExecutionUrl({ execution: row, index: currentPaginationExecutionTable, paginationWidth: paginationWidth, url: contextPath});
            url +='&view=detail';
        obj.executionUrl = url;
        obj.assignee = row.assignedTo;
        Object.keys(customFields).map(function(field) {
            var _customFieldValue = customFields[field].value;
            if ((customFields[field].customFieldType == 'DATE' || customFields[field].customFieldType == 'DATE_TIME') && _customFieldValue) {
                try {
                    _customFieldValue = convertDateExecution({value: _customFieldValue, isDateTime: customFields[field].customFieldType == 'DATE_TIME'});
                }
                catch(err) {
                    //
                }
            }
            if(customFields[field].customFieldType == 'NUMBER') {
                _customFieldValue = _customFieldValue;
            }

            if (customFields[field].customFieldType === 'TEXT' || customFields[field].customFieldType === 'LARGE_TEXT') {
              _customFieldValue = customFields[field].htmlValue;
            }
            obj[customFields[field].customFieldId] = _customFieldValue;
        });

        if(obj.defects) {
            obj.defects.map(function(defect){
                defect['color'] = statusMap[obj.executionStatus] && statusMap[obj.executionStatus].color;
            })
        }

        if((obj.id === stepLevelDefectsExecutionId)) {
            if(showDefectsPopup) {
                obj['stepDefect'] = stepLevelDefects;
                obj['showPopup'] = true;
                updatedGridDataCycleSummary.rowData = {
                    index: index,
                    showPopup: true,
                    stepDefect : stepLevelDefects
                }
            } else {
                obj['showPopup'] = false;
                updatedGridDataCycleSummary.rowData = {
                    index: index,
                    showPopup: false,
                }
                stepLevelDefectsExecutionId = '';
            }
        }
        obj['permission'] = !row.canViewIssue;
        config.row.push(obj);
    });

  if (dummyConfig.head) {
    var that = this;
    config.head.map(function(dummy){
        dummyConfig.head.map(function(config){
          if(dummy.key === config.key) {
            dummy.isFreeze = config.isFreeze;
          }
        });
      })
  }
    config.columnchooser.disabled = config.row.length === 0 ? true : false;
    AJS.$(document).unbind('gridScrollEventCapture');
    AJS.$(document).bind('gridScrollEventCapture', gridScrollEventCapture);
    if(onlyUpdateGridValueCycleSummary) {
        AJS.$('#cycleSummaryGrid').attr('updatedconfig', JSON.stringify(updatedGridDataCycleSummary));
        onlyUpdateGridValueCycleSummary = false;
    } else {
        refreshGrid = !refreshGrid;
        if (initialCountCycleSummary != 10) {
            initialCountCycleSummary = 10;
        }
        initialCountCycleSummary = getGridInitialCount(initialCountCycleSummary);
        vanillaGrid.init(document.getElementById('cycleSummaryGrid'), config);
        //AJS.$('#cycleSummaryGrid').attr('config', JSON.stringify(config));
    }

    AJS.$('#cycleSummaryGrid').unbind('cycleSummaryGridUpdate');
    AJS.$('#cycleSummaryGrid').bind('cycleSummaryGridUpdate', cycleSummaryGridUpdate);

    updatedGridDataCycleSummary = {};
    AJS.$('#cycleSummaryGrid').unbind('gridActions');
    AJS.$('#cycleSummaryGrid').bind('gridActions', gridComponentActions);
    AJS.$('#cycleSummaryGrid').unbind('gridValueUpdated');
    AJS.$('#cycleSummaryGrid').bind('gridValueUpdated', gridValueUpdated);
    AJS.$('#cycleSummaryGrid').unbind('gridRowSelected');
    AJS.$('#cycleSummaryGrid').bind('gridRowSelected', gridSelectedExecution);
    AJS.$('#cycleSummaryGrid').unbind('gridBulkActions');
    AJS.$('#cycleSummaryGrid').bind('gridBulkActions', gridBulkActions);
    AJS.$('#cycleSummaryGrid').unbind('addTests');
    AJS.$('#cycleSummaryGrid').bind('addTests', addTests);
    AJS.$('#cycleSummaryGrid').unbind('freezetoggle');
    AJS.$('#cycleSummaryGrid').bind('freezetoggle', freezeToggle);
    AJS.$('#cycleSummaryGrid').unbind('defecthover');
    AJS.$('#cycleSummaryGrid').bind('defecthover', defectsPopup);
    AJS.$('#cycleSummaryGrid').unbind('defecthover');
    AJS.$('#cycleSummaryGrid').bind('defecthover', defectsPopup);
    AJS.$('#cycleSummaryGrid').unbind('defecthoverOff');
    AJS.$('#cycleSummaryGrid').bind('defecthoverOff', defectsPopupOff);
    AJS.$('#cycleSummaryGrid').unbind('dialogueScroll');
    AJS.$('#cycleSummaryGrid').bind('dialogueScroll', dialogueScrollPlanCycle);
}

var freezeToggle = function (ev) {
    ev.preventDefault();
    ev.stopPropagation();

    var sendingData = allPagesPaginationPageWidth;
    var updatedFreezedColumns = '';
    for (var counter = 0; counter < ev.originalEvent.detail.testConfig.head.length; counter += 1) {
        if (ev.originalEvent.detail.testConfig.head[counter].isFreeze == true) {
            if (updatedFreezedColumns.length == 0) {
                updatedFreezedColumns = ev.originalEvent.detail.testConfig.head[counter].key.toString();
            } else {
                updatedFreezedColumns = updatedFreezedColumns + ',' + ev.originalEvent.detail.testConfig.head[counter].key.toString();
            }
        }
    }
    sendingData.freezeColumns = updatedFreezedColumns;

    var params = {
        url: '/preference/paginationWidthPreference',
        data: sendingData,
        method: 'PUT',
        noSuccessMsg: true,
    };
    xhrCall(params, function (response) {
        dummyConfig = ev.originalEvent.detail.testConfig;
        createCycleSummaryGrid(ZEPHYR.Cycle.executions, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder);
    })
}

var updateLocationHash = function(viewType, executionId, clear) {
    var query;
    if (window.location.hash.indexOf('view=') == -1) {
        query = window.location.hash + '&view=' + viewType + '&executionId=' + executionId;
    } else {
        query = window.location.hash.replace(/(view=).*?(&)/,'$1' + viewType + '$2');
    }
    if(executionId) {
        query = query.replace(/(executionId=).*?(&|$)/,'$1' + executionId + '$2');
    }
    if(clear) {
        query = query.split('&')[0];
    }
    return query;
}

var updateRouter = function(query) {
    ZEPHYR.Cycle.router.navigate(query, {trigger: false, replace: true});
}

var gridSelectedExecution = function(ev) {
  ev.preventDefault();
  ev.stopPropagation();
  var viewType = 'list';
  selectedExecutionId = ev.originalEvent.detail.rowId;
  ZEPHYR.Cycle.clickedExecutionId = selectedExecutionId;
  if(ev.originalEvent.detail.dblClick) {
      isDetailedViewClicked = true;
    if(!(AJS.$('.tree-tcr').hasClass('collapse'))) {
        AJS.$('#tree-docker').trigger('click');
    }
    detailView(selectedExecutionId);
  }

  //to account for dbl click
  setTimeout(function(){
    //console.log(ev, ev.originalEvent);
    viewType = !isDetailedViewClicked ? 'list' : 'detail';
    query = updateLocationHash(viewType, selectedExecutionId);
    updateRouter(query);
  }, 300);
}

var gridComponentActions = function(ev) {
  ev.preventDefault();
  ev.stopPropagation();
  var scrollPosLeft = document.getElementById('cycleSummaryGrid').querySelector('#unfreezedGrid').scrollLeft;
  var actionDetail = ev.originalEvent.detail;
    if (actionDetail.customEvent === 'deleteRow') {
      ZEPHYR.Cycle.executions.map(function (row, index) {
          if (row.id === actionDetail.rowDetail.id) {
              updatedGridDataCycleSummary.rowData = {
                  index: index,
                  deleteRow: true,
              }
          }
      });
    var url = contextPath + '/rest/zephyr/latest/execution/' + actionDetail.rowDetail.id;
    ZEPHYR.Schedule.addDeleteTrigger(function(){
    //   onlyUpdateGridValueCycleSummary = true;
    //   clearData = false;
        initialCountCycleSummary = ZEPHYR.Cycle.executions.length;
        triggerTreeRefresh();
    }, actionDetail.rowDetail.id, url, false, true);
    AJS.$('a.trigger-delete-dialog').trigger('click');
    } else if (actionDetail.customEvent === 'executeRow') {

        var sendingData = allPagesPaginationPageWidth;
        sendingData.fromPage = 'planCycleSummaryactions';
        var params = {
            url: '/preference/paginationWidthPreference',
            data: sendingData,
            method: 'PUT',
            noSuccessMsg: true,
        };
        xhrCall(params, function (response) {
            allPagesPaginationPageWidth = response;
            var rowDetails;
            ZEPHYR.Cycle.executions.map(function (row, index) {
                if (row.id === parseInt(actionDetail.rowDetail.id)) {
                    rowDetails = row;
                }
            });


            var url = getExecutionUrl({ execution: rowDetails, index: currentPaginationExecutionTable, paginationWidth: paginationWidth, url: contextPath});
            url +='&view=detail';
           window.location.assign(url);
        // window.open(url,'_blank');
        })
    } else if (actionDetail.customEvent ==='columnChooser') {

    var params = {
        url: '/preference/setcyclesummarycustomization',
        method: 'POST',
        data: {},
        noSuccessMsg: true
    }
    actionDetail.columnDetails.map(function(column) {
      if(column.key !== "issueKey") {
        if(column.isVisible) {
          ZEPHYR.Cycle.executionColumns[column.key].isVisible = "true";
        } else {
    	  ZEPHYR.Cycle.executionColumns[column.key].isVisible = "false";
        }
      }
    });

    params.data['preferences'] = ZEPHYR.Cycle.executionColumns;

    xhrCall(params, function(response){
      createCycleSummaryGrid(ZEPHYR.Cycle.executions, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder);
    })
    } else if (actionDetail.customEvent ==='sortGrid') {
    var sortQuery = actionDetail.sortQuery;
    var sorter = '';
    sortKey = sortQuery.sortkey;
    sortOrder = sortQuery.sortOrder;


    if(sortQuery.sortkey === 'issueKey') {
      sorter = 'ID:' + sortQuery.sortOrder;
    } else if(sortQuery.sortkey === 'status') {
      sorter = 'ExecutionStatus:' + sortQuery.sortOrder;
    } else if(sortQuery.sortkey === 'executedOn') {
      sorter = 'ExecutionDate:' + sortQuery.sortOrder;
    } else if(sortQuery.sortkey === 'executedBy') {
      sorter = 'ExecutedBy:' + sortQuery.sortOrder;
    }

    cycleSelected.sortQuery = sorter;
    queryParamsAllCycles.forEach(function(cycle, index) {
        if (cycle.cycleId ==cycleSelected.cycleId && cycle.projectId == cycleSelected.projectId && cycle.versionId == cycleSelected.versionId && cycle.folderId == cycleSelected.folderId ) {
            queryParamsAllCycles[index] = JSON.parse(JSON.stringify(cycleSelected));
        }
    });

    //   THE OFFSET IN THE BELOW API IS SET TO 0 CAUSE THE QA TEAM WANTED TO NAVIGATE TO THE FIRST PAGE OF THE PAGINATION AS SOON AS THE USER CLICKS ON ANY OF THE SORTING.
     jQuery.ajax({
        url: getRestURL() + "/execution?cycleId=" + ZEPHYR.Cycle.executions[0].cycleId + "&action=expand&projectId=" + ZEPHYR.Cycle.executions[0].projectId +  "&versionId="+
            ZEPHYR.Cycle.executions[0].versionId + (ZEPHYR.Cycle.executions[0].folderId ? '&folderId=' + ZEPHYR.Cycle.executions[0].folderId : '') +"&offset=0&sorter=" + sorter + "&limit=" + paginationWidth,
        type : "get",
        contentType :"application/json",
        dataType: "json",
        success : function(response) {
            ZEPHYR.Cycle.executions = response.executions;
            ZEPHYR.Cycle.clickedExecutionId = response.executions[0].id;
            selectedExecutionId = response.executions[0].id;
            executionsTableModelNew.set('currentOffset', 0);
            executionsTableModelNew.set('sortQuery', sorter);
            executionsTableModelNew.set('executions', response.executions);
            createCycleSummaryGrid(ZEPHYR.Cycle.executions, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder);
            var navigationHtml = ZEPHYR.Templates.PlanCycle.addPaginationNewUI({
              totalPages : Math.ceil((executionsTableModelNew.get('totalCount')) / paginationWidth),
              selectedPage : 1,
              entriesCount: paginationWidth,
              totalCount : executionsTableModelNew.get('totalCount')
            });
            AJS.$('.pagination-wrapper-newUI').replaceWith(navigationHtml);
            setTimeout(function(){
                document.getElementById('cycleSummaryGrid').querySelector('#unfreezedGrid').scrollLeft = scrollPosLeft;
            }, 0)
            // executionsTableModelNew.set('executions', response.executions);
            // executionsTableModelNew.set('emptyExecutions', !executionsTableModelNew.get('emptyExecutions'));
         }
     });
  }
}

var cycleSummaryGridUpdate = function(ev) {
    //console.log('12');
};

var gridValueUpdated = function(ev) {
  dummyConfig = ev.originalEvent.detail.testConfig || dummyConfig;
  var tempConfig = ev.originalEvent.detail.config;
  ev.preventDefault();
  ev.stopPropagation();
  var data = ev.originalEvent.detail.updatedValue;
    if (ev.originalEvent.detail.isObject) {
        Object.keys(data).map(function (key) {
            data[key] = data[key].value;
        });
    }
  data['changeAssignee'] = false;
  var executionId = ev.originalEvent.detail.executionId,
      executionStatus='',
      rowId = ev.originalEvent.detail.rowId;
  var xhr = {
      url: '/execution/'+  executionId + '/execute',
      method: 'PUT',
      data: data
  }
  var cellConfig = {};
  cellConfig.header = tempConfig.head.filter(function(header){
    return header.key === 'status' || header.key === 'executedOn' || header.key === 'executedBy'
  });
  xhrCall(xhr, function(response) {
    onlyUpdateGridValueCycleSummary = true;
    ZEPHYR.Cycle.executions.map(function(row, index){
        if (row.id === executionId) {
            executionStatus = response.executionStatus;
            updatedGridDataCycleSummary.rowData = {
                index: index,
                executionStatus: response.executionStatus,
                executedBy: response.executedBy,
                executedOn: response.executedOn,
                executedOn: response.executedOn,
            }
        }
    });
    clearData = false;
    triggerTreeRefresh();
    cellConfig.row = response;
    vanillaGrid.templates.partialRender('cycleSummaryGrid', rowId, cellConfig, tempConfig);
    //createCycleSummaryGrid([], ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder);

      //for analytics
      var zaObj = {
          'event': ZephyrEvents.EXECUTE,
          'eventType': 'Change',
          'executionId': executionId,
          'executionStatus': executionStatus
      };
      if (za != undefined){
          za.track(zaObj, function(){
              //res
          })
      }
  }, function(err) {
      if(err.status == 403) {
          createCycleSummaryGrid(ZEPHYR.Cycle.executions, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder, true);
          if (err && err.status == 403) {
              var _responseJSON = {};
              try {
                  _responseJSON = jQuery.parseJSON(err.responseText);
              } catch (e) {
                  //console.log(e);
              }
              if (_responseJSON.PERM_DENIED) {
                  showPermissionError(err);
                  return;
              }
          }
      }
  });
}

var detailView = function(executionId) {
  AJS.$('#tree-docker').hide();
  var self = AJS.$('#cycle-executions-wrapper');
  var targetElem = AJS.$('#cycle-executions-wrapper table tbody tr');
  executionStatusContainer = 1;
  // var executionId = targetElem.first().attr('data-exeuctionid');
  var executionId = executionId;
  var elementIndex = 0;
  var executionIds = [];
  ZEPHYR.Cycle.executions.forEach(function(execution) {
      executionIds.push(execution.id);
  });
  self.executionIdDetails = executionId;
  AJS.$('#cycle-executions-wrapper').addClass('hide');
  AJS.$('#cycle-info-wrapper').addClass('hide');
  AJS.$('.execution-details').removeClass('hide');
  // AJS.$('.executionSummaryBar').addClass('hide');
  AJS.$('#detailViewBtn').addClass('active-view');
  AJS.$('#listViewBtn').removeClass('active-view');
  AJS.$(document).trigger( "triggerExecutionDetails", [ executionId, false, executionsTableModelNew.attributes.allStatusList, {total:parseInt(ZEPHYR.Cycle.executions.length), dbIndex: parseInt(cycleSelected.soffset) + parseInt(elementIndex), localIndex: parseInt(elementIndex), executionIds:executionIds} ] );
  if(!executionStatusContainer && (!currentElement.closest('.zfj-editable-field').length) && !self.model.get('isCheckbox')) {
      self.model.set('selectedId' , executionId);
      self.model.set('emptyExecutions' , !self.model.get('emptyExecutions'));
  }
}

var gridBulkActions = function(ev) {
  ev.preventDefault();
  ev.stopPropagation();
    if (ev.originalEvent.detail.actionName === 'bulkDelete') {
        if (ev.originalEvent.detail.rowsSelection.length)
            bulkDeleteConfirmationDialog(ev.originalEvent.detail.rowsSelection);
    } else if (ev.originalEvent.detail.actionName === 'rowSelection' || ev.originalEvent.detail.actionName === 'selectRows') {
      ev.originalEvent.detail.rowData.map(function (row) {
          if (row.selected && cycleSummaryGridSelectedExecutions.indexOf(row.rowId) === -1) {
              cycleSummaryGridSelectedExecutions.push(row.rowId);
          } else if (!row.selected && cycleSummaryGridSelectedExecutions.indexOf(row.rowId) !== -1) {
              cycleSummaryGridSelectedExecutions = cycleSummaryGridSelectedExecutions.filter(function (id) {
                  return id !== row.rowId
              });
          }
      });
  }
}

var addTests =  function(ev) {
    ev.preventDefault();
    ev.stopPropagation();
    var node = zephyrjQuery3("#js-tree").jstree("get_selected", true)[0];
    addTestsToCycle(ev, node);
}

var returnDefectsPopoverPosition = function (target) {
    var triggerElement = target.getBoundingClientRect();
    var viewportHeight = window.innerHeight;
    var viewportWidth = window.innerWidth;
    //var height = that.shadowRoot.querySelector('.defects-inlineDialogWrapper').clientHeight;
    var height = '200px';
    var topHeight = triggerElement.top + triggerElement.height + 2;

    var leftOffset, rightOffset, topOffset, bottomOffset;

    //bottom condition
    if (viewportHeight > topHeight + 200 + 30) {
        topOffset = topHeight;
        bottomOffset = 'auto';

        //bottom - right coniditon
        if (viewportWidth > triggerElement.left + 450) {
            leftOffset = triggerElement.left;
            rightOffset = 'auto';
        } else { //bottom - left coniditon
            rightOffset = viewportWidth - (triggerElement.left + triggerElement.width);
            leftOffset = 'auto';
            if (rightOffset < 0) {
                rightOffset = 0;
            }
        }

    //top condition
    } else if (triggerElement.top > (200 + 30 )) {
        topOffset = 'auto';
        bottomOffset = viewportHeight - topHeight + triggerElement.height + 5;

        //top - right coniditon
        if (viewportWidth > triggerElement.left + 450) {
            leftOffset = triggerElement.left;
            rightOffset = 'auto';
        } else { //top - left coniditon
            rightOffset = viewportWidth - (triggerElement.left + triggerElement.width);
            leftOffset = 'auto';
            if (rightOffset < 0) {
                rightOffset = 0;
            }
        }

     //cell same height condition
    } else {
        topOffset = triggerElement.top;
        bottomOffset = 'auto';
        //  left coniditon
        if (viewportWidth > 450) {
            rightOffset = viewportWidth - triggerElement.left;
            leftOffset = 'auto';
        } else { // right coniditon
            leftOffset = triggerElement.left + triggerElement.width;
            rightOffset = 'auto';
            if (leftOffset < 0) {
                leftOffset = 0;
            }
        }
    }



    return {
        right: rightOffset,
        left: leftOffset,
        bottom: bottomOffset,
        top: topOffset
    }
}

var returnDefectListMarkup = function(defectList) {
    return defectList.map(function(defect){
        return '<div class="defectsList">'
                + '<div class="statusColor" style="background-color : '+ defect.color +'"></div>'
                + '<div class="defectKey">'
                    + '<a href="'+contextPath +'/browse/' + defect.key +'">'+ defect.key +'</a>'
                + '</div>'
                + '<div class="defectStatus">'+ defect.status +'</div>'
                + '<div class="defectSummary">'+ defect.summary +'</div>'
            + '</div>'
        }).join('');

}

var defectsPopupOff = function(ev) {
    if(AJS.$('#cycle-executions-wrapper').find('.defects-inlineDialogWrapper').length){
        AJS.$('#cycle-executions-wrapper').find('.defects-inlineDialogWrapper').remove();
      }
}

var defectsPopup = function(ev) {
  ev.preventDefault();
  ev.stopPropagation();
  showDefectsPopup = !showDefectsPopup;
  if(ev.originalEvent.detail.defectsPopup) {
    showDefectsPopup = true;
    stepLevelDefects = [];
    jQuery.ajax({
        url: getRestURL() + "/stepResult/stepDefects?executionId=" + ev.originalEvent.detail.id + "&expand=executionStatus&",
        type : "get",
        contentType :"application/json",
        dataType: "json",
        success : function(response) {
          Object.keys(response.stepDefects).map(function(index){
            var color;
            var executionStatus = response.stepDefects[index].currentStepExecutionStatus;
            color = response.executionStatus[executionStatus].color;
            response.stepDefects[index].stepDefects.map(function(defect) {
              defect['color'] = color;
              stepLevelDefects.push(defect);
            });
          });
          var executionList = executionsTableModelNew.get('executions');
          var currentExecution = executionList.filter(function(execution) {
            return execution.id === parseInt(ev.originalEvent.detail.id);
          })[0];

          var dimensions = returnDefectsPopoverPosition(ev.originalEvent.detail.targetEle);
          Object.keys(dimensions).forEach(function(key){
            dimensions[key] = dimensions[key] !== 'auto' ? dimensions[key] + 'px' : dimensions[key];
          });
          var defectsPopupOver = '<div id="defects-inlineDialogWrapper" class="defects-inlineDialogWrapper" style="right: '+ dimensions.right +'; left:' + dimensions.left +'; top: '+ dimensions.top +'; bottom: '+ dimensions.bottom +'">'
                                    + '<div class="defects-container">'
                                        + '<div class="executionLevelDefects">'
                                            + '<span>Defects Filed For </span>'
                                            + '<div class="defectsList-Container">'
                                                + returnDefectListMarkup(currentExecution.defects)
                                            + '</div>'
                                        + '</div>'
                                        + (stepLevelDefects.length ? '<div class="stepLevelDefects">'
                                            + '<span>Step Level Defects Filed</span>'
                                            + '<div class="defectsList-Container">'
                                                + returnDefectListMarkup(stepLevelDefects)
                                            + '</div>'
                                        + '</div>'
                                        : '')
                                    + '</div>'
                                + '</div>';


            AJS.$('#cycle-executions-wrapper').append(defectsPopupOver);
            AJS.$("#defects-inlineDialogWrapper").mouseenter(function() {
                this.dataset['isDefectPopoverHovered'] = 'true';
            });
            AJS.$("#defects-inlineDialogWrapper").mouseleave(function() {
                setTimeout(function() {
                    ZEPHYR.GRID.scrollableDialogue = ZEPHYR.GRID.scrollableDialogue.filter(function (dialogue) {
                        return dialogue.dialogueElement != AJS.$('#defects-inlineDialogWrapper')[0];
                    });
                    defectsPopupOff();
                }, 100);
            });
            ZEPHYR.GRID.scrollableDialogue.push({ isOpen: true, target: ev.originalEvent.detail.targetEle, dialogueElement: AJS.$('#defects-inlineDialogWrapper')[0] });
            stepLevelDefectsExecutionId = ev.originalEvent.detail.id;
            onlyUpdateGridValueCycleSummary = true;
            createCycleSummaryGrid(ZEPHYR.Cycle.executions, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder);
        }
    });
  } else {
    showDefectsPopup = false;
    onlyUpdateGridValueCycleSummary = true;
    stepLevelDefects = [];
    stepLevelDefectsExecutionId = ev.originalEvent.detail.id;
    createCycleSummaryGrid(ZEPHYR.Cycle.executions, ZEPHYR.Cycle.executionColumns, ZEPHYR.Cycle.planCycleCFOrder);
  }
}

function dialogueScrollPlanCycle(event) {
    if (event.originalEvent.detail.isOpen) {
        ZEPHYR.GRID.scrollableDialogue.push(event.originalEvent.detail);
    } else {
        ZEPHYR.GRID.scrollableDialogue = ZEPHYR.GRID.scrollableDialogue.filter(function (dialogue) {
            return dialogue.dialogueElement != event.originalEvent.detail.dialogueElement;
        });
    }
}