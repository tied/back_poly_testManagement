if(typeof ZEPHYR == 'undefined') ZEPHYR = {};
if(typeof ZEPHYR.Execution == 'undefined') ZEPHYR.Execution = {};
if(typeof ZEPHYR.Execution.CustomField == 'undefined') ZEPHYR.Execution.CustomField = {};

ZEPHYR.Execution.CustomField.ColumnChooser = {};

var totalCustomFields = [];
var currentIssueId;
var apiID;
var customFields = [];
var customizeCustomFieldsHtml;
var customFieldsSubmitButtonId = 'updateCustomizeFieldsFilter';
var customFieldsCloseButtonId = 'cancelCustomizeFieldsFilter';
var executionCustomColumnInlineDialog;

ZEPHYR.Execution.CustomField.init = function(execution) {
  ZEPHYR.Execution.CustomField.execution = execution;
  if(typeof ZEPHYR.Execution.CustomField.execution.customFields === 'string')
    ZEPHYR.Execution.CustomField.execution.customFields = JSON.parse(ZEPHYR.Execution.CustomField.execution.customFields);
  getCustomField();
}

function getCustomField(){
  var xhr = {
		url: '/customfield/entity?entityType=EXECUTION&projectId=' + ZEPHYR.Execution.CustomField.execution.projectId,
		method: 'GET',
	};
	xhrCallCustom(xhr, function(response) {
    getCustomFieldByProject(response);
  });
}

function getCustomFieldByProject(commonCustomFields){
  var xhr = {
		url: '/customfield/byEntityTypeAndProject?entityType=EXECUTION' + '&projectId=' + ZEPHYR.Execution.CustomField.execution.projectId,
		method: 'GET',
	};
	xhrCallCustom(xhr, function(response) {
    customFields = commonCustomFields.concat(response);
    getCustomFieldPrefernces();
  });
}

function getCustomFieldPrefernces() {
  var xhr = {
    url: '/preference/getExecutionCustomFieldCustomization?projectId=' + ZEPHYR.Execution.CustomField.execution.projectId,
    method: 'GET',
  };
  xhrCallCustom(xhr, function(response) {
    Object.keys(response.preferences).forEach(function(key) {
        var obj = {
            "displayName": response.preferences[key].displayName,
            "isVisible": response.preferences[key].isVisible
        }
        ZEPHYR.Execution.CustomField.ColumnChooser[key] = obj;
    });
    customFields.forEach(function(field) {
      var visible = (Object.keys(response.preferences).length ? 'false' : 'true')
      if(!ZEPHYR.Execution.CustomField.ColumnChooser[field.id]) {
        var obj = {
          "displayName": field.name,
          "isVisible": visible
        }
        ZEPHYR.Execution.CustomField.ColumnChooser[field.id] = obj;
      }
    });

    var htmlCustomFields = ZEPHYR.Execution.Templates.ExecutionCustomField.customFieldsDisplayTemplate();
    AJS.$("#execution-customField-container").empty();
    AJS.$("#execution-customField-container").append(htmlCustomFields);
    customFieldCollapsingTemplate = ZEPHYR.Execution.Templates.ExecutionCustomField.executionDetailsCustomFieldsCollapsingTemplate();

    AJS.$("#custom-fidld-display-area").empty();
    AJS.$("#custom-fidld-display-area").append(customFieldCollapsingTemplate);
    loadCustomFieldData();
  })
}

function setCustomFieldPrefernces() {
  var data = {
    'preferences' : ZEPHYR.Execution.CustomField.ColumnChooser
  };
  var xhr = {
    url: '/preference/setExecutionCustomFieldCustomization?projectId=' + ZEPHYR.Execution.CustomField.execution.projectId,
    method: 'POST',
    data: data
  };
  xhrCallCustom(xhr, function(response) {
    Object.keys(response.preferences).forEach(function(key) {
        ZEPHYR.Execution.CustomField.ColumnChooser[key].isVisible = response.preferences[key].isVisible;
    });
  })
}

function loadCustomFieldData() {

		apiID = ZEPHYR.Execution.CustomField.execution.id;
		var selectedFieldsData = ZEPHYR.Execution.CustomField.execution.customFields;

		var newResponse = [];
		for (var counter = 0; counter < customFields.length; counter += 1) {
			var temp = JSON.parse(JSON.stringify(customFields[counter]));
			newResponse.push(temp);
		}

		var jsonData = {};
		for (var counter = 0; counter < newResponse.length; counter += 1) {
			jsonData[newResponse[counter].id] = {
				"customFieldValueId": null,
				"customFieldId": newResponse[counter].id,
				"customFieldName": newResponse[counter].name,
				"entityId": ZEPHYR.Execution.CustomField.execution.id,
				"customFieldType": newResponse[counter].fieldType,
				"value": "",
				"options": newResponse[counter].customFieldOptionValues,
			};

			if(selectedFieldsData[newResponse[counter].id]) {
        jsonData[newResponse[counter].id].selectedOptions = selectedFieldsData[newResponse[counter].id].selectedOptions;

				if(selectedFieldsData[newResponse[counter].id].value == ""){
					jsonData[newResponse[counter].id].value = selectedFieldsData[newResponse[counter].id].value;
					if (selectedFieldsData[newResponse[counter].id].customFieldType === "LARGE_TEXT" || selectedFieldsData[newResponse[counter].id].customFieldType === "TEXT" ) {
						jsonData[newResponse[counter].id].htmlValue = selectedFieldsData[newResponse[counter].id].htmlValue;
					}
				} else{
					jsonData[newResponse[counter].id].value = (selectedFieldsData[newResponse[counter].id].customFieldType === "NUMBER" && selectedFieldsData[newResponse[counter].id].value != '')? selectedFieldsData[newResponse[counter].id].value:selectedFieldsData[newResponse[counter].id].value;
					if (selectedFieldsData[newResponse[counter].id].customFieldType === "LARGE_TEXT" || selectedFieldsData[newResponse[counter].id].customFieldType === "TEXT") {
						jsonData[newResponse[counter].id].htmlValue = selectedFieldsData[newResponse[counter].id].htmlValue;
					}
				}
				jsonData[newResponse[counter].id].customFieldValueId = selectedFieldsData[newResponse[counter].id].customFieldValueId;
			}

		}

		appendCustomFieldTemplateToContainer(jsonData);
}
//  Function to add trigger images
function configureTriggerOptions(type) {
  var imgUrl;
  switch(type) {
    case 'DATE' :
      imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/date-picker_icon.svg'
      break;
    case 'DATE_TIME' :
      imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/date-and-time_icon.svg'
      break;
    case 'SINGLE_SELECT' :
      imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg'
      break;
    case 'MULTI_SELECT' :
      imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/multi-select_icon.svg'
      break;
    case 'RADIO_BUTTON' :
      imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/radio-button_icon.svg'
      break;
    case 'CHECKBOX' :
      imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/check-box_icon.svg'
      break;
  }
  return imgUrl;
}
//	FUNDTION TO APPEND THE CUSTOM FIELD TO THE ID DIV
function appendCustomFieldTemplateToContainer(jsonData) {
  console.log('jsonData', jsonData);
	var executionCustomFieldsData = [];
	Object.keys(jsonData).forEach(function(key) {
		if ((jsonData[key].customFieldType == 'DATE' || jsonData[key].customFieldType == 'DATE_TIME') && jsonData[key].value) {
			jsonData[key].displayDate = convertDateExecution({value: jsonData[key].value, isDateTime: jsonData[key].customFieldType == 'DATE_TIME'});
      jsonData[key].options = [{'value': jsonData[key].displayDate }];
		} else if (jsonData[key].customFieldType == 'MULTI_SELECT' || jsonData[key].customFieldType == 'CHECKBOX'
          || jsonData[key].customFieldType == 'RADIO_BUTTON' || jsonData[key].customFieldType == 'SINGLE_SELECT') {
			var newOptions = [];
			var selectedArray = jsonData[key].selectedOptions && jsonData[key].selectedOptions.split(',');

      Object.keys(jsonData[key].options).forEach(function(id) {
        var tempOption = {};
        tempOption.value = id;
        tempOption.content = jsonData[key].options[id];
        tempOption.selected = (selectedArray && selectedArray.length) ? selectedArray.indexOf(id) >= 0 : false;
        newOptions.push(tempOption);
      })

			jsonData[key].options = newOptions;
		} else if (jsonData[key].customFieldType == 'TEXT' || jsonData[key].customFieldType == 'LARGE_TEXT') {
			jsonData[key].options = [{
				value: jsonData[key].htmlValue,
				readValue: jsonData[key].value,
				type: jsonData[key].customFieldType,
				isHTMLKey: true,
			}];
		} else {
      jsonData[key].options = [{
        value: jsonData[key].value,
        content: jsonData[key].value,
        type: jsonData[key].customFieldType
      }];
    }
    jsonData[key].options = JSON.stringify(jsonData[key].options);
    jsonData[key].imgUrl = contextPath + configureTriggerOptions(jsonData[key].customFieldType);
		executionCustomFieldsData.push(jsonData[key]);
	});
	totalCustomFields = executionCustomFieldsData;
	var showMoreStatusValue = false;
	var miniCustomField;



	miniCustomField = ZEPHYR.Execution.Templates.ExecutionCustomField.executionDetailsCustomFieldsReadModeNew({customFields: executionCustomFieldsData, mode: 'read', isGrid: false, executionId: ZEPHYR.Execution.CustomField.execution.id});

	AJS.$("#custom-field-collapsing-container").empty();
	AJS.$("#custom-field-collapsing-container").append(miniCustomField);

	AJS.$('.textarea-custom-field').on('click', function(ev) {
		var target = ev.target;
		var container = target.closest('.cell-wrapper');
    	var height = container.clientHeight;
		AJS.$(target).closest('.cell-readMode').removeClass('readMode').addClass('editMode');
		AJS.$(container).find('.cell-editMode').removeClass('hide');
    	AJS.$(container).find('.textarea-custom-field-element').css({'height': height + 'px'});
    	setTimeout(function() {
    		var textareaElem = AJS.$(container).find('.textarea-custom-field-element')[0];
			textareaElem.focus();
			var val = textareaElem.value;
			textareaElem.value = '';
			textareaElem.value = val;
		}, 100);
	});

	AJS.$('.textarea-custom-field-element').on('focusout', function(ev) {
		var target = ev.target;
		var container = target.closest('.cell-wrapper');
		var oldValue = AJS.$(target).data('oldvalue');
		var value = AJS.$(target).val();
		var type = AJS.$(target).data('type');
		if(oldValue == value) {
			AJS.$(container).find('.cell-readMode').removeClass('editMode').addClass('readMode');
			AJS.$(container).find('.cell-editMode').addClass('hide');
			return;
		}
		target.closest('.textarea-custom-field-wrapper').dispatchEvent(new CustomEvent('submitvalue', { detail: { oldValue: oldValue, type: type, value: value }, bubbles: true, composed: true }));
	});

	AJS.$('.textarea-custom-field-wrapper').unbind('submitvalue');
	AJS.$('.textarea-custom-field-wrapper').bind('submitvalue', _submit);

  AJS.$('drop-downcheckbox').unbind('submitvalue');
  AJS.$('drop-downcheckbox').bind('submitvalue', _submit);
  AJS.$('drop-downradio').unbind('submitvalue');
  AJS.$('drop-downradio').bind('submitvalue', _submit);
  AJS.$('drop-down').unbind('submitvalue');
  AJS.$('drop-down').bind('submitvalue', _submit);
  AJS.$('drop-downmultiselect').unbind('submitvalue');
  AJS.$('drop-downmultiselect').bind('submitvalue', _submit);
  AJS.$('custom-textarea').unbind('submitvalue');
  AJS.$('custom-textarea').bind('submitvalue', _submit);
  AJS.$('custom-text').unbind('submitvalue');
  AJS.$('custom-text').bind('submitvalue', _submit);
  AJS.$('drop-downdate').unbind('triggerdatechooser');
  AJS.$('drop-downdate').bind('triggerdatechooser', _triggerdatechooser);
}
//  TOGGLE CUSTOM FIELDS
AJS.$( '#show-content' ).live('click',function(event){
    event.preventDefault();
    event.stopPropagation();
    AJS.$( '#show-content' ).parents('.toggle-wrap').find('#custom-field-collapsing-container').toggleClass('more');
});

AJS.$('#inline-dialog-custom-field-configeration-options-inner-container').live("click", function(e) {
	e.stopPropagation();
 });

function customizeCustomFields() {
	if(AJS.$("#inline-dialog-custom-field-configeration-options-inner-container").length > 0) {
		AJS.$("#inline-dialog-custom-field-configeration-options-inner-container").remove();
	}

	customizeCustomFieldsHtml = ZEPHYR.Execution.Templates.ExecutionCustomField.customFieldsCustomizer({columns: ZEPHYR.Execution.CustomField.ColumnChooser, submitButtonId: customFieldsSubmitButtonId, closeButtonId: customFieldsCloseButtonId, selectAll: false });
	executionCustomColumnInlineDialog = AJS.InlineDialog(AJS.$("#custom-field-configeration-options"), "custom-field-configeration-options-inner-container",
	function(content, trigger, showPopup) {
		content.css({"padding":"10px 0 0", "max-height":"none"}).html(customizeCustomFieldsHtml);
		showPopup();
		return false;
	},
	{
	  width: 250,
	  closeOnTriggerClick: true,
	  persistent: true,
	}
  );
}

function addCustomFieldValues() {
	var customFieldValues = {};
	var values = {};
	AJS.$(".execution-details-customFields").each(function() {
		var obj = {};
		var that = AJS.$(this);
		var customFieldId = that[0].dataset.customfieldid;
		obj.customFieldId = customFieldId;
		obj.customFieldType = that[0].dataset.customfieldtype;
		if (that[0].dataset && that[0].dataset.customfieldvalueid) {
			obj.customFieldValueId = that[0].dataset.customfieldvalueid;
		} else {
			obj.customFieldValueId = '';
		}
		if (that[0].dataset && that[0].dataset.entityid) {
			obj.entityId = that[0].dataset.entityid;
		} else {
			obj.entityId = '';
		}

		var type = AJS.$(this)[0].dataset.customfieldtype;
		var value;

		if (type === 'MULTI_SELECT') {
			that.find('option:selected').each(function(){
				if(value) {
					value = value + ',' + AJS.$(this).val();
				}
				else {
					value = AJS.$(this).val();
				}
			  }
			)
		} else if (type === 'CHECKBOX') {
			that.find('input[type=checkbox]:checked').each(function(){
				if(value) {
					value = value + ',' + AJS.$(this)[0].dataset.value;
				}
				else {
					value = AJS.$(this)[0].dataset.value;
				}
			  }
			)
		} else if (type === 'NUMBER') {
			value = parseFloat(that.find('input').val());
		} else if (type === 'RADIO_BUTTON') {
			var selectedButton = that.find('input[type=radio]:checked');

			if(selectedButton.length) {
				value = selectedButton[0].dataset.value;
			}
		} else if (type === 'SINGLE_SELECT') {
			var selectedField = that.find('option:selected');

			if(selectedField.length) {
				value = that.find('option:selected').val();
			}
		} else if (type === 'LARGE_TEXT') {
			value = that.find('input, textarea').val();
		} else if (type === 'TEXT') {
			value = that.find('input').val();
		} else if (type === 'DATE' || type === 'DATE_TIME') {
			value = that.find('input').val();
			if(value.length) {
				var date = new Date(value);
				value = date.getTime();
				value = Math.round(value/1000);
			}
		}
		obj.value = value;
		values[customFieldId] = obj;
	});
	return values;
  }

//CONVERT DATE FUNCTION
function convertDateExecution(data) {
	if(data.value) {
		date = new Date(Number(data.value));
		h =  date.getHours(), m = date.getMinutes();
		if (m < 10) {
			m = "0" + m;
		}
		if (h < 10) {
			h = "0" + h;
		}
		time = h + ':' + m;
		if(data.isDateTime) {
			return dateStr = (date.getMonth()+1) + '/' + date.getDate() + '/' + date.getFullYear() + ' ' + time;
		} else {
			return dateStr = (date.getMonth()+1) + '/' + date.getDate() + '/' + date.getFullYear();
		}
	} else {
		return '';
	}
}

//DATE PICKER FUNCTION
function datePicker(rowId, customField) {
	var date = null;
	var inputField;
	if(rowId) {
	  inputField = 'date-' + customField.customFieldId;
	  date = convertDateExecution({'value': customField.value, 'isDateTime' : false});
	}
   Calendar.setup({
		firstDay : 0,
		inputField : inputField,
		align : 'Br',
		singleClick : true,
		showsTime : false,
		useISO8601WeekNumbers : false,
		timeFormat: '12',
		ifFormat : '%m/%d/%Y',
		date: date,
	});
}

//DATE TIME PICKER FUNCTION
function dateTimePicker(rowId, customField) {
	var date = null;
	var inputField;
	if(rowId) {
		inputField = 'dateTime-' + customField.customFieldId;
		date = convertDateExecution({'value': customField.value, 'isDateTime' : true});
	}
	Calendar.setup({
		firstDay : 0,
		inputField : inputField,
		align : 'Br',
		singleClick : true,
		showsTime : true,
		timeFormat: '12',
		useISO8601WeekNumbers : false,
		ifFormat : '%m/%d/%Y %I:%M %p',
		date: date,
	});
}

// API HITTING FUNCTION
// THE COMPLETE URL IS WHEN YOU WANT TO HIT A COMPLETE DIFFERENT API URL WITHOUT ANY getRestURL to it
function xhrCallCustom(xhr, successCallback, errorCallback) {
    var method = '';
    if (!!xhr.data) {
        method = 'POST';
    } else {
        method = 'GET';
    }

    if (!!xhr.method) {
        method = xhr.method;
	}

	var ApiUrl = getRestURL() + xhr.url;

    AJS.$.ajax({
        url: ApiUrl,
        type : method,
        contentType :"application/json",
        data: JSON.stringify(xhr.data),
        Accept : "application/json",
        success : function(response) {
            successCallback(response);
            console.log('saved successfully');
        },
        error : function(xhr, status, error) {
            if(xhr.status === 404) {
            	if(successCallback && typeof successCallback === 'function') {
                	successCallback({preferences:{}});
                }
            }
            if(xhr.status !== 403) {
                console.log('status code : ', xhr.status)
                if(errorCallback && typeof errorCallback === 'function') {
                	errorCallback(xhr);
                }
            }
            console.log('error', xhr, error);
        },
        statusCode: {
            403: function(xhr, status, error) {
                console.log('status code : 403')
                errorCallback(xhr);
            }
        }
    });
}

var _triggerdatechooser = function(ev) {
	if (ev.originalEvent.detail.onlyUpdateValue) {
			ev.originalEvent.detail.value = '';
			ev.originalEvent.detail.type = ev.target.dataset.type;
			_submit(ev);
	} else {
		var inputDateField = AJS.$('#date-pickerCustomField');
		var positionInputFiled = inputDateField[0].getBoundingClientRect();
		var targetPosition = ev.target.getBoundingClientRect();
		var dateValue = '';
		inputDateField.val('');
		inputDateField.css({ "top": 0, "left": 0 })
		inputDateField.css({ "top": (targetPosition.top + targetPosition.height) - positionInputFiled.top, "left": targetPosition.left - positionInputFiled.left });
		inputDateField.datetimepicker({
			value: ev.originalEvent.detail.value,
			step: 30,
			timepicker: (ev.target.dataset.type === 'DATE') ? false : true,
			format: (ev.target.dataset.type === 'DATE') ? 'm/d/Y' : 'n/j/Y H:i',
			onChangeDateTime: function (dp, $input) {
				var date = dp;
				if(date.getMinutes() != 30) {
					date.setMinutes(00);
				}
				var value = date.getTime();
				value = Math.round(value / 1000);
				dateValue = value;
			},
			onClose: function() {
				if(dateValue) {
					ev.originalEvent.detail.value = dateValue.toString();
					ev.originalEvent.detail.type = ev.target.dataset.type;
					_submit(ev);
				}
				inputDateField.datetimepicker('destroy');
				inputDateField.css({"top":0, "left" : 0});
			}
		});
		inputDateField.datetimepicker('show');
	}
}

var _submit = function(ev) {
  console.log(ev, ev.target.dataset.executionid, ev.originalEvent.detail);
  var executionid = ev.target.dataset.executionid,
      customFieldId = ev.target.dataset.customfieldid,
      customFieldType = ev.originalEvent.detail.type,
      entityId = ev.target.dataset.entityid,
      customFieldValueId = ev.target.dataset.customfieldvalueid,
      value, selectedOptions;

  if(customFieldType === 'SINGLE_SELECT' || customFieldType === 'MULTI_SELECT'
      || customFieldType === 'RADIO_BUTTON' || customFieldType === 'CHECKBOX') {
      value = Array.isArray(ev.originalEvent.detail.contentValue) ? ev.originalEvent.detail.contentValue.join(',') : ev.originalEvent.detail.contentValue,
      selectedOptions = Array.isArray(ev.originalEvent.detail.value) ? ev.originalEvent.detail.value.join(',') : ev.originalEvent.detail.value;

  } else {
      value = Array.isArray(ev.originalEvent.detail.value) ? ev.originalEvent.detail.value.join(',') : ev.originalEvent.detail.value;
      selectedOptions = '';
  }
  var values = {
    customFieldId: customFieldId,
    customFieldValueId : (customFieldValueId !== 'null') ? customFieldValueId : '',
    customFieldType: ev.originalEvent.detail.type,
    value: value || '',
    entityId: entityId,
    entityType : "EXECUTION",
    selectedOptions: selectedOptions

  }
  var xhr = {
    url: '/customfieldvalue',
    method: 'PUT',
    data: values
  };
  xhrCallCustom(xhr, function(response) {
    //console.log(showExecStatusSuccessMessage());
    showExecStatusSuccessMessage(AJS.I18n.getText('cycle.customField.update'));
    ZEPHYR.Schedule.Execute.updateExecutionHistory();
    ZEPHYR.Execution.CustomField.execution.customFields = response;
		loadCustomFieldData();
    console.log(response);
  }, function(err) {
		if(err.status == 403) {
			loadCustomFieldData();
		}
    showErrorMessage((JSON.parse(err.responseText)).error);
	});
	
	// if (!window.standalone && AJS.$('#zqltext')[0] && AJS.$('#zqltext')[0].value.length != 0) {
	// 	setTimeout(function () {
	// 		ZEPHYR.ZQL.maintain = true;
	// 		if (AJS.$('#zephyr-transform-all') && AJS.$('#zephyr-transform-all')[0]) {
	// 			AJS.$('#zephyr-transform-all')[0].click();
	// 		}
	// 	}, 1000)
	// }
}
