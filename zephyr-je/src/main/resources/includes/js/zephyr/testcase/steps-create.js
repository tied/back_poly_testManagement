/**
 * Zephyr Test Step Create
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.ISSUE == 'undefined') { ZEPHYR.ISSUE = {}; }
if (typeof ZEPHYR.ISSUE.Create == 'undefined') { ZEPHYR.ISSUE.Create = {}; }

var createStepPreference;
var createStepsColumnCustomization;
var createStepInlineDialog;
var testStepAttachmentVisible = true;

ZEPHYR.ISSUE.Create.TESTSTEP_TYPE_REQUIRED = 'req';
ZEPHYR.ISSUE.Create.TESTSTEP_TYPE_OPTIONAL = 'opt';
ZEPHYR.ISSUE.Create.TESTSTEP_ISSUE_ACTION_NEW = 'new';
ZEPHYR.ISSUE.Create.TESTSTEP_ISSUE_ACTION_EDIT = 'edit';
ZEPHYR.ISSUE.Create.TESTSTEP_DATA_NO_CHANGE = 'noChange';
ZEPHYR.ISSUE.Create.TESTSTEP_DATA_DELETE_ALL = 'deleteAll';
ZEPHYR.ISSUE.Create.TESTSTEP_DATA_ADD_UPDATE = 'addUpdate';

ZEPHYR.ISSUE.Create.Teststep = {
    init: function ($context) {
    	var isEditDialog = this.isEditDialog();
        this.setTeststepType();
        this.setIssueAction(isEditDialog);
        this.setCustomFieldId();
    	ZEPHYR.ISSUE.Create.Teststep.hasEditStepsLoaded = false;
        this.attachNoChangeUI();
    	if(isEditDialog) {
    		AJS.$('#zephyr-je-action-name').closest('.field-group')
    			.prepend('<span class="zfj-module-toggle-wrapper aui-icon aui-icon-small aui-iconfont-collapsed collapse" />');
    		AJS.$('.zfj-module-toggle-wrapper').unbind('click')
    			.bind('click', function(ev) {
    				ev.preventDefault();
    				AJS.$('.zfj-module-toggle-wrapper').toggleClass('aui-iconfont-expanded aui-iconfont-collaped expand collapse');
    				if(ZEPHYR.ISSUE.Create.Teststep.isFirst) {
    					ZEPHYR.ISSUE.Create.Teststep.hasEditStepsLoaded = true;
    					ZEPHYR.ISSUE.Create.Teststep.initRestfultable(isEditDialog, $context, function(){
                setTimeout( function(){
                  getCreateStepColumn();
                },0)
              });
    				}
    			});
    	} else {
    		this.initRestfultable(isEditDialog, $context,function() {
            getCreateStepColumn();
        });
    	}

        return true;
    },
    attachNoChangeUI: function() {
        var $inputCustomField = AJS.$('<input type="hidden" name="' + this.getCustomFieldId() + '" id="' + this.getCustomFieldId() + '-no-change" />');
        var _parsedData = ZEPHYR.ISSUE.Create.Teststep.setStepJSON(ZEPHYR.ISSUE.Create.TESTSTEP_DATA_NO_CHANGE, {});
        $inputCustomField.val(JSON.stringify(_parsedData));
        AJS.$("#project-config-panel-versions-teststep").append($inputCustomField);
        ZEPHYR.ISSUE.Create.Teststep.noChange = true;
    },
    removeNoChangeElement: function() {
        AJS.$('#' + this.getCustomFieldId() + '-no-change').remove();
    },
    setCustomFieldId: function() {
        this.customFieldId = AJS.$('#zephyr-je-customfield-id').val();
    },
    getCustomFieldId: function() {
        return this.customFieldId;
    },
    initRestfultable: function(isEditDialog, $context,complete) {
    	var issueId = null,
			projectId  = null,
			dialogSteps = new ZEPHYR.RestfulTable({
            autoFocus: true,
            allowReorder: true,
            allowDelete: true,
            allowEdit: true,
            createPosition: "bottom",
            noEntriesMsg: AJS.I18n.getText('view.issue.steps.none'),
            loadingMsg: AJS.I18n.getText('view.issue.steps.loading'),
            isEditDialog: isEditDialog,
            el: $context.find("#teststep-table"),
            resources: {
                all:  getRestURL() + "/teststep/"+ JIRA.Issue.getIssueId(),
                self: "/",
            },
            model: this.TestStepModel(projectId, issueId),
            columns: [
                {
                    id: "orderId",
                    htmlId: 'orderId',
                    header: "",
                    allowEdit: false
                },
                {
                    id: "step",
                    htmlId: 'htmlStep',
                    header: AJS.I18n.getText('view.issue.steps.table.column.step'),
                    emptyText: AJS.I18n.getText('view.issue.steps.table.column.add.step')
                },
                {
                    id: "data",
                    htmlId: 'htmlData',
                    header: AJS.I18n.getText('view.issue.steps.table.column.data'),
                    emptyText: AJS.I18n.getText('view.issue.steps.table.column.add.data')
                },
                {
                    id: "result",
                    htmlId: 'htmlResult',
                    header: AJS.I18n.getText('view.issue.steps.table.column.result'),
                    emptyText: AJS.I18n.getText('view.issue.steps.table.column.add.result')
                }
            ]
        });
        ZEPHYR.ISSUE.Create.Teststep.isFirst = false;
        if(complete) {
          complete.call();
        }
    },
    // TestStep model inheriting from EntryModel
    TestStepModel: function(projectId, issueId) {
    	return ZEPHYR.RestfulTable.EntryModel.extend({
	    	url: function(move) {
	    		if(move) {
	    			return "/";
	    		} else
	    			return "/";
	    	}
	    })
    },
    isEditDialog: function() {
    	ZEPHYR.ISSUE.Create.Teststep.isFirst = true;
    	var _action = AJS.$('#zephyr-je-action-name').val(),
            _actionLCString = (_action) ? _action.toString().toLowerCase(): '';

    	if(_actionLCString.indexOf('quickeditissue') > -1 || _actionLCString.indexOf('commentassignissue') > -1) // Fix for ZFJ-2127
    		return true;
    	else
    		return false;
    },
    setTeststepType: function() {
        var _isRequired = AJS.$('#zephyr-je-customfield-isrequired').val();
        this.teststepType = (_isRequired && _isRequired !== 'false') ? ZEPHYR.ISSUE.Create.TESTSTEP_TYPE_REQUIRED: ZEPHYR.ISSUE.Create.TESTSTEP_TYPE_OPTIONAL;
    },
    setIssueAction: function(isEditDialog) {
        this.issueaction = (isEditDialog) ? ZEPHYR.ISSUE.Create.TESTSTEP_ISSUE_ACTION_EDIT: ZEPHYR.ISSUE.Create.TESTSTEP_ISSUE_ACTION_NEW;
    },
    setStepJSON: function(stepaction, data) {
        return {
            'type': this.teststepType,
            'issueaction': this.issueaction,
            'stepaction': stepaction,
            'data': data
        };
    }
}

ZEPHYR.ISSUE.Create.Teststep.showError = function(e){
	var zError = AJS.$('input#zerrors').val();
	if(zError && zError.length > 0 ){
		var error = "<span class='aui-icon icon-error'></span>";
		error += zError;
		AJS.$("div.zephyr-je-ex-error").html(decHTMLifEnc(error));
	}
}


function isLoadedInIframe() {
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
	var showCreateSteps = function($context) {
        var _action = AJS.$('#zephyr-je-action-name').val(),
            _actionLCString = (_action) ? _action.toString().toLowerCase(): '';
        /** Get a better way to remove bulk label elements (Fix for ZFJ-2128) */
		if(_actionLCString.indexOf('bulkworkflowtransition') > -1) {
            AJS.$('#zephyr-je-action-name').closest('tr').remove()
        } else if(_actionLCString.indexOf('bulkedit') > -1) {
			AJS.$('#zephyr-je-action-name').closest('tr.availableActionRow').remove();
		} else {
			ZEPHYR.ISSUE.Create.Teststep.init($context);
		}
	}
	JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, $context) {
		if($context.find("#teststep-table").length && !$context.find("#teststep-table thead").length) {
			showCreateSteps($context);
		}
    });
	if(AJS.$("#teststep-table").length) {
		showCreateSteps(AJS.$("#project-config-panel-versions-teststep"));
	}
});

function getCreateStepColumn() {
  jQuery.ajax({
    url: getRestURL() + "/preference/getteststepcustomization",
    type : "get",
    contentType :"application/json",
    dataType: "json",
    success : function(response) {
        createStepPreference = response.preferences;
       initializeCreateStepInlineDialog();
       customizeColumnCreateSteps();
    }
  });
}

function initializeCreateStepInlineDialog() {
  var editButton = '<div class="step-column-picker-trigger-container"><a class="aui-button aui-button-subtle step-column-picker-trigger" href="#" id="inlineDialogCreateSteps">Columns</a></div>';
  createStepsColumnCustomization = '<div id="inline-dialog-createStep-column-picker"><div class="step-column-picker-dialog-wrapper"> <h3>' + AJS.I18n.getText('cycle.ChooseColumnAttribute.label') + '</h3> <div class="aui-list-wrapper aui-list"> <div class= "aui-list-scroll"> <ul class="aui-list"><li class="check-list-item"> <label class="item-label"><input type="checkbox" value="teststep" class="step-column-input TestStep"><span class="" original-title="">' + AJS.I18n.getText('view.issue.steps.table.column.step') + '</span></label></li> <li class="check-list-item"> <label class="item-label"><input class="TestStepData step-column-input" type="checkbox" value="testdata"><span class="" original-title="">'+ AJS.I18n.getText('view.issue.steps.table.column.data') + '</span></label></li> <li class="check-list-item"> <label class="item-label"><input class="TestStepExpectedResult step-column-input" type="checkbox" value="testresult"><span class="" original-title="">' + AJS.I18n.getText('view.issue.steps.table.column.result') + '</span></label></li> </ul></div></div> <div id="errorColumnSelector">Select atleast one option</div> <div class="button-panel"><input class="aui-button" type="button" value="' + AJS.I18n.getText('zephyr.je.done.button.label') + '" id="submitCreateStepColumn" href="javascript:void(0)" > <a href="javascript:void(0)" class="aui-button aui-button-link close-dialog" id="closeCreateStepInlineDialog">' + AJS.I18n.getText('zephyr-je.close.link.title') + '</a></div></div></div>';

  var lastColumn = AJS.$("#teststep-table thead tr th:last-child");
  lastColumn.each(function(){
    if(!AJS.$(this).find(".step-column-picker-trigger-container").length) {
      AJS.$(this).append(editButton);
      AJS.$(this).append(createStepsColumnCustomization);
      setInlineDialogCreateStepContent();
    }
  });
}

function setInlineDialogCreateStepContent() {
  AJS.$(".step-column-input").prop('checked', false);
  if(createStepPreference && createStepPreference.teststep.isVisible === "true")
  {
    AJS.$(".TestStep").prop('checked', true);
  }
  if(createStepPreference.testdata.isVisible === "true")
  {
    AJS.$(".TestStepData").prop('checked', true);
  }
  if(createStepPreference.testresult.isVisible === "true")
  {
    AJS.$(".TestStepExpectedResult").prop('checked', true);
  }
}

function customizeColumnCreateSteps() {
  AJS.$(".teststepTable").each(function(){
    var table = AJS.$(this);
    var noOfColumnsVisible = 0;
    if(createStepPreference && !(createStepPreference.teststep.isVisible  === "true"))
    {
      if(!table.hasClass("removeTestStep"))
        table.addClass("removeTestStep");
    }
    else {
      noOfColumnsVisible++;
      if(table.hasClass("removeTestStep"))
        table.removeClass("removeTestStep");
    }
    if(!(createStepPreference.testdata.isVisible === "true"))
    {
      if(!table.hasClass("removeTestStepData"))
        table.addClass("removeTestStepData");
    }
    else {
      noOfColumnsVisible++;
      if(table.hasClass("removeTestStepData"))
        table.removeClass("removeTestStepData");
    }
    if(!(createStepPreference.testresult.isVisible === "true"))
    {
      if(!table.hasClass("removeTestStepExpectedResult"))
        table.addClass("removeTestStepExpectedResult");
    }
    else {
      noOfColumnsVisible++;
      if(table.hasClass("removeTestStepExpectedResult"))
        table.removeClass("removeTestStepExpectedResult");
    }

    if(noOfColumnsVisible > 0) {
      var widthPercentage = 100/noOfColumnsVisible;
      table.find(".zephyr_create_test_step_th").width(widthPercentage+"%");
      table.find(".zephyr_create_test_result_th").width(widthPercentage+"%");
      table.find(".zephyr_create_test_data_th").width(widthPercentage+"%");
    }

  });
}

AJS.$(".step-column-picker-trigger-container").live("click", function(e) {
  e.preventDefault();
  var parentColumn = AJS.$(this).closest("th");
  setInlineDialogCreateStepContent();
  parentColumn.find("#inline-dialog-createStep-column-picker").show();
  parentColumn.find(".step-column-picker-trigger-container").addClass("active");
});

AJS.$("#closeCreateStepInlineDialog").live("click", function(e) {
  var parentColumn = AJS.$(this).closest("th");
  parentColumn.find("#errorColumnSelector").hide();
  parentColumn.find("#inline-dialog-createStep-column-picker").hide();
  parentColumn.find(".step-column-picker-trigger-container").removeClass("active");
});

AJS.$("#submitCreateStepColumn").live("click", function(e) {
  var parentColumn = AJS.$(this).closest("th");
  var data = {};
  var columnInput = parentColumn.find("#inline-dialog-createStep-column-picker .step-column-input");
  var count = 0;
  columnInput.each(function() {
    var thisInput = AJS.$(this);
    var value = thisInput.context.value;
    if(thisInput.prop('checked')) {
      count++;
      createStepPreference[value]['isVisible'] = 'true';
    } else {
      createStepPreference[value]['isVisible'] = 'false'
    }
  });
  if(count === 0)
  {
    parentColumn.find("#errorColumnSelector").show();
  } else {
    parentColumn.find("#errorColumnSelector").hide();
    data = { 'preferences' : createStepPreference }
    data = JSON.stringify(data);
    jQuery.ajax({
      url: getRestURL() + "/preference/setteststepcustomization",
      type : "post",
      contentType :"application/json",
      data : data,
      dataType: "json",
      success : function(response) {
          createStepPreference = response.preferences;
         customizeColumnCreateSteps();
         parentColumn.find("#inline-dialog-createStep-column-picker").hide();
         parentColumn.find(".step-column-picker-trigger-container").removeClass("active");
         setInlineDialogCreateStepContent();
      }
    });
  }
});
