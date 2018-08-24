AJS.$.namespace("ZEPHYR.Templates.ZephyrCustomField")

if (typeof Zephyr == "undefined") { Zephyr = {}; }

var customFieldsTypes = [];
var selectedField = '';
var selectedDisplayField = '';
var selectedCustomFieldDetails = {};
var selectedFieldDetails = { name: '', description: '', options: [] };
var attribute = 'EXECUTION';
var customFieldSearchValue = '';
var totalCustomFields = [];
var updatingField = {};
var projectList = [];
var isProjectPage = false;
var projectId;
var customFieldTourXpathData = [];
var customFieldPagesUrlDetails = [];
var editedFields = {
    name: false,
    description: false,
};

AJS.$(function () {
    if (AJS.$('#customFieldProjectPage').length)
        isProjectPage = true;
    if (AJS.$('#zprojectId').length)
        projectId = AJS.$('#zprojectId').val();
    loadExecutionDashboard();
    loadTestSteps();
    getallProjects();
})

AJS.$(document).ready(function (event) {
    // customFieldTourXpathData = [
    //     {
    //         path: '//*[@id="name-sub-key"]',
    //         description: 'Freeze the columns',
    //         direction: 'down-right',
    //     }
    // ];
    // customFieldPagesUrlDetails = [
    //     {
    //         url: contextPath + '/projects/' + window.location.pathname.split("/")[window.location.pathname.split("/").length - 1].split('-')[0] + '?selectedItem=com.thed.zephyr.je%3Azephyr-tests-page#test-cycles-tab',
    //         title: 'Test Cycle Summary',
    //     }, {
    //         url: contextPath + '/projects/' + window.location.pathname.split("/")[window.location.pathname.split("/").length - 1].split('-')[0] + '?selectedItem=com.thed.zephyr.je:zephyr-custom-field-project-sidebar',
    //         title: 'Custom Fields',
    //     }
    // ];
    // setTimeout(function () {
    //     customFieldsCallWalkThrough('walkThrough', false);
    // }, 1000);
});

// function customFieldsCallWalkThrough(knowledgeTour, isNotFirstTime) {
//     if (knowledgeTour == 'walkThrough') {
//         walkThroughCall(customFieldTourXpathData, '#sidebar-page-container', 'customField', customFieldPagesUrlDetails, isNotFirstTime);
//     } else if (knowledgeTour == 'newPageLayout') {
//         newPageLayout(customFieldTourXpathData, '#sidebar-page-container', 'customField', event);
//     }
// }

function getallProjects() {
    AJS.$.ajax({
        url: contextPath + "/rest/api/2/project/",
        type: 'GET',
        contentType: "application/json",
        Accept: "application/json",
        success: function (response) {
            response.forEach(function (index) {
                var obj = {
                    id: index.id,
                    key: index.key,
                    name: index.name
                }
                projectList.push(obj);
            })
        }
    });
}

function loadExecutionDashboard() {
    var xhr = {
        url: projectId ? ('/customfield/byEntityTypeAndProject?entityType=EXECUTION' + '&projectId=' + projectId) : ('/customfield/entity?entityType=EXECUTION&isGlobal=true'),
        method: 'GET',
    };
    customXhrCall(xhr, function (response) {

        for (var counter = 0; counter < response.length; counter += 1) {
            totalCustomFields.push(response[counter]);
        }
        var templateConfig = {
            customFields: response
        }
        if(projectId) {
            templateConfig['projectId'] = true;
        }
        var displayCustomFields = ZEPHYR.Templates.ZephyrCustomField.listCustomFields(templateConfig);
        AJS.$("#existingCustomFields").empty();
        AJS.$("#existingCustomFields").append(displayCustomFields);

        if(projectId) {
            var xhrGlobal = {
                url: '/customfield/globalCustomFieldsByEntityTypeAndProject?entityType=EXECUTION&projectId=' + projectId,
                method: 'GET'
            }
            customXhrCall(xhrGlobal, function (global_response) {
                AJS.$("#globalexistingCustomFields").empty();
                AJS.$("#globalexistingCustomFields").append(ZEPHYR.Templates.ZephyrCustomField.listCustomFields({ customFields: global_response, configureEnableAction: true }));
            }, function (err) {

            })
        }

    }, function (response) {
        console.log('fail response : ', response);
    });


}

function loadTestSteps() {
    var xhr = {
        url: projectId ? ('/customfield/byEntityTypeAndProject?entityType=TESTSTEP&projectId=' + projectId) : ('/customfield/entity?entityType=TESTSTEP&isGlobal=true'),
        method: 'GET',
    };
    customXhrCall(xhr, function (response) {
        for (var counter = 0; counter < response.length; counter += 1) {
            totalCustomFields.push(response[counter]);
        }
        var templateConfig = {
            customFields: response
        }
        if(projectId) {
            templateConfig['projectId'] = true;
        }
        var displayCustomFields = ZEPHYR.Templates.ZephyrCustomField.listCustomFields(templateConfig);
        AJS.$("#existingCustomFieldsTestSteps").empty();
        AJS.$("#existingCustomFieldsTestSteps").append(displayCustomFields);

        if(projectId) {
            var xhrGlobal = {
                url: '/customfield/globalCustomFieldsByEntityTypeAndProject?entityType=TESTSTEP&projectId=' + projectId,
                method: 'GET'
            }
            customXhrCall(xhrGlobal, function (global_response) {
                AJS.$("#globalExistingCustomFieldsTestSteps").empty();
                AJS.$("#globalExistingCustomFieldsTestSteps").append(ZEPHYR.Templates.ZephyrCustomField.listCustomFields({ customFields: global_response, configureEnableAction: true }));
            }, function (err) {

            })
        }

    }, function (response) {
        console.log('fail response : ', response);
    });
}

function setAttrbute(event) {
    attribute = event.name;
}
var triggerApiCall = true
AJS.$('.actionEnableContainer label').live('change', function(e) {
    if(!triggerApiCall) {
        triggerApiCall = true;
        return;
    }
    //console.log(e, e.currentTarget.dataset.customfieldid, AJS.$(this).find('input').prop('checked'));
    var cfId = parseInt(e.currentTarget.dataset.customfieldid),
        enable = AJS.$(this).find('input').prop('checked'),
        that = this;

    var xhr = {
        method: 'PUT',
        url: '/customfield/' + cfId + '/' + projectId + '?enable=' + enable
    };
    customXhrCall(xhr, function (data) {
        showToastrMsg(data.message, 'success');
    }, function(err) {
        console.log(err);
        triggerApiCall = false;
        AJS.$(that).find('input').trigger('click');
        if(err && err.status == 403)
            return;
        showToastrMsg(err, 'error');
    })
});

function dataCardCustomFieldDetails() {
    if (AJS.$('#customFieldModel').is(':visible')) {
        return;
    }
    var xhr = {
        method: 'GET',
        url: '/customfield/metadata',
    };
    customXhrCall(xhr, function (data) {
        selectedField = '';
        selectedDisplayField = '';
        var customFieldsTypeModel = ZEPHYR.Templates.ZephyrCustomField.customFieldTypeModel();
        AJS.$("#creatingNewCustomFieldModelBox").empty();
        AJS.$("#creatingNewCustomFieldModelBox").append(customFieldsTypeModel);
        AJS.dialog2("#customFieldModel").show();
        AJS.$("#customFieldModel").focus();
        toggleCustomFieldSearch('add');
        if (data.customFields) {
            var metadata = jQuery.parseJSON(data.customFields);
            var customFields = [];
            metadata.forEach(function (customField) {
                customFields.push(JSON.parse(customField));
            });
            customFieldsTypes = customFields;
        } else {
            var response = {
                "data": {
                    "customFields": [
                        {
                            "imageClass": "checkbox",
                            "label": "Checkboxes",
                            "description": "Choose multiple values using checkboxes.",
                            "options": true,
                            "type": "CHECKBOX"
                        },
                        {
                            "imageClass": "datePicker",
                            "label": "Date Picker",
                            "description": "A custom field that stores dates and uses a date picker to view them.",
                            "options": false,
                            "type": "DATE"
                        },
                        {
                            "imageClass": "dateTimePicker",
                            "label": "Date Time Picker",
                            "description": "A custom field that stores dates with a time component.",
                            "options": false,
                            "type": "DATE_TIME"
                        },
                        {
                            "imageClass": "numberField",
                            "label": "Number Field",
                            "description": "A custom field that stores and validates numeric (floating point) input.",
                            "options": false,
                            "type": "NUMBER"
                        },
                        {
                            "imageClass": "radioButtons",
                            "label": "Radio Buttons",
                            "description": "A list of radio buttons.",
                            "options": true,
                            "type": "RADIO_BUTTON"
                        },
                        {
                            "imageClass": "selectListMultipleChoices",
                            "label": "Select List (multiple choices)",
                            "description": "Choose multiple values in a select list.",
                            "options": true,
                            "type": "MULTI_SELECT"
                        },
                        {
                            "imageClass": "selectListSingleChoice",
                            "label": "Select List (single choices)",
                            "description": "A single select list with a configurable list of options.",
                            "options": true,
                            "type": "SINGLE_SELECT"
                        },
                        {
                            "imageClass": "textFieldMultiLine",
                            "label": "Text Field (multi-line)",
                            "description": "A multiline text area custom field to allow input of longer text strings.",
                            "options": false,
                            "type": "LARGE_TEXT"
                        },
                        {
                            "imageClass": "textFieldSingleLine",
                            "label": "Text Field (single line)",
                            "description": "A basic single line text box custom field to allow simple text input.",
                            "options": false,
                            "type": "TEXT"
                        }
                    ]
                }
            };
            customFieldsTypes = response.data.customFields;
        }
        dataCardCustomFieldInnerDetails(customFieldsTypes);
    }, function (response) {
        console.log('fail response : ', response);
    });
}

function dataCardCustomFieldInnerDetails(fieldTypes) {
    selectedFieldDetails.options = [];

    var customFieldSelectionFooter = ZEPHYR.Templates.ZephyrCustomField.customFieldSelectionFooter();
    AJS.$("#custom-field-data-model-footer").empty();
    AJS.$("#custom-field-data-model-footer").append(customFieldSelectionFooter);

    var customFieldsTypeModelInnerData = ZEPHYR.Templates.ZephyrCustomField.fieldTypes({ customFieldTypes: fieldTypes, selectedField: selectedField });
    AJS.$("#customFieldModelCardData").empty();
    AJS.$("#customFieldModelCardData").append(customFieldsTypeModelInnerData);
}

function searchCustomFieldType(event) {
    customFieldSearchValue = event.target.value.toLowerCase();
    var tempCustomFieldsType = [];
    for (var counter = 0; counter < customFieldsTypes.length; counter += 1) {
        if (customFieldsTypes[counter].label.toLowerCase().indexOf(event.target.value.toLowerCase()) >= 0) {
            tempCustomFieldsType.push(customFieldsTypes[counter]);
        }
    }
    dataCardCustomFieldInnerDetails(tempCustomFieldsType);
}

function selectedType(e) {
    selectedField = e.dataset.selectedfield;
    selectedDisplayField = e.dataset.displayselectedfield;
    AJS.$("#customFieldTypesList>div").removeClass('selectedType');
    AJS.$(e).addClass('selectedType');
    AJS.$("#custom-field-dialog-next-button").removeClass("aui-button-secondary").addClass("aui-button-primary");
}

function customFieldDialogNextButton() {
    if (selectedField != '') {
        toggleCustomFieldSearch('remove');
        fieldDetailsModelPage();
    }
}

function fieldDetailsModelPage() {
    var customFieldDetailsFooter = ZEPHYR.Templates.ZephyrCustomField.customFieldDetailsFooter();
    var headerText = selectedField.toLowerCase().split("_").join(" ");
    //headerText = headerText[0].toUpperCase() + headerText.slice(1);
    headerText = "Add '" + selectedDisplayField + "' Field";
    AJS.$(".aui-dialog2-header .aui-dialog2-header-main").text(headerText);
    AJS.$("#custom-field-data-model-footer").empty();
    AJS.$("#custom-field-data-model-footer").append(customFieldDetailsFooter);

    for (var counter = 0; counter < customFieldsTypes.length; counter += 1) {
        if (customFieldsTypes[counter].type == selectedField) {
            AJS.$("#customFieldModelCardData").empty();
            selectedCustomFieldDetails = customFieldsTypes[counter];
            break;
        }
    }
    var selectedCustomFieldDetailsHtml = ZEPHYR.Templates.ZephyrCustomField.selectedCustomFieldDetails({ projectList: projectList, isProjectPage: isProjectPage });
    AJS.$("#customFieldModelCardData").append('<p class="simpleclass"></p>');
    AJS.$("#customFieldModelCardData").append(selectedCustomFieldDetailsHtml);
    AJS.$("#selectedFieldProject").auiSelect2();
    if (isProjectPage == true) {
        var currentProjectKey = window.location.pathname.split('/')[window.location.pathname.split('/').length - 1]
        for (var currentProjectCounter = 0; currentProjectCounter < projectList.length; currentProjectCounter += 1) {
            if (projectList[currentProjectCounter].key == currentProjectKey) {
                AJS.$("#selectedFieldProject").select2("val", [projectList[currentProjectCounter].id]);
                break;
            }
        }
    }
    if (customFieldsTypes[counter].options == true) {
        selectedFieldOptions();
    }
}

function doubleClickCustomFieldTypeSelection(e) {
    var temp = {
        dataset: {
            selectedfield: e.dataset.selectedfield,
            displayselectedfield: e.dataset.displayselectedfield
        },
    };
    selectedType(temp);
    customFieldDialogNextButton();
}

// DELETE BUTTON THE CUSTOM FIELD FUNCTION
function customFieldDialogDeleteButton(event) {
    var xhr = {
        url: '/customfield/' + event.dataset.deletingcustomfieldid,
        method: 'Delete',
    };
    customXhrCall(xhr, function (response) {
        showToastrMsg('Custom field deleted successfully');
        loadExecutionDashboard();
        loadTestSteps();
    }, function (response) {
        console.log('delete fail : ', response);
    });
    customFieldModifyClose();
}

// UPDATE BUTTON FOR CUSTOM FIELD
function customFieldDialogUpdateButton() {
    console.log('update the custom field api here : ', updatingField);
    // var sendingData = {
    //     name: updatingField.name,
    //     description: updatingField.description,
    // };
    var sendingData = {
        name: updatingField.name,
        description: updatingField.description,
    };
    editedFields.description = false;
    editedFields.name = false;
    // if (updatingField.description) {
    //     sendingData.description = updatingField.description;
    //     editedFields.description = false;
    // }
    // if (editedFields.name) {
    //     sendingData.name = updatingField.name;
    //     editedFields.name = false;
    // }
    var xhr = {
        url: '/customfield/' + updatingField.id,
        method: 'PUT',
        data: sendingData,
    };

    customXhrCall(xhr, function (response) {
        showToastrMsg('Custom field updated successfully');
        for (var counter = 0; counter < totalCustomFields.length; counter += 1) {
            if (totalCustomFields[counter].id == updatingField.id) {
                totalCustomFields[counter].description = updatingField.description;
                totalCustomFields[counter].name = updatingField.name; 1
            }
        }
        loadExecutionDashboard();
        loadTestSteps();
    }, function (response) {
        console.log('update fail : ', response);
    });

    customFieldModifyClose();
}

function customFieldModifyClose() {
    AJS.dialog2("#customFieldModel").hide();
}

function selectedFieldOptions() {
    var optionsHtml = ZEPHYR.Templates.ZephyrCustomField.optionsTemplate({ optionsEntered: selectedFieldDetails.options });
    AJS.$("#optionsOuterContainer").empty();
    AJS.$("#optionsOuterContainer").append(optionsHtml);
}

function updateOptionValues(event) {
    selectedFieldDetails.options.splice(AJS.$(event.parentElement).index(), 1);
    selectedFieldOptions();
}

function addNewOption() {
    var value = AJS.$("#addingNewOptions").val().trim();
    if (selectedFieldDetails.options.indexOf(value) >= 0) {
        AJS.$("#errorOptionExist").show();
        AJS.$("#errorEmptyOption").hide();
        AJS.$('#errorOptionLengthExceeding').hide();
    } else if (value.trim() == '') {
        AJS.$("#errorEmptyOption").show();
        AJS.$("#errorOptionExist").hide();
        AJS.$('#errorOptionLengthExceeding').hide();
    } else if (value.length > 255) {
        AJS.$("#errorEmptyOption").hide();
        AJS.$("#errorOptionExist").hide();
        AJS.$('#errorOptionLengthExceeding').show();
    } else {
        selectedFieldDetails.options.push(value);
        AJS.$("#errorEmptyOption").hide();
        AJS.$("#errorOptionExist").hide();
        AJS.$('#errorOptionLengthExceeding').hide();
        selectedFieldOptions();
    }
    AJS.$("#addingNewOptions").focus();

    // AJS.$(".optionsAdded").draggable({
    //     scroll: true,
    //     revert: "invalid",
    // });


    // AJS.$("#options-container div").droppable({
    //     accept: "div.optionsAdded",
    //     greedy: false,
    //     tolerance: "pointer",
    //     drop: function (e, ui) {
    //         // var idd = ui.draggable.attr("id");

    //         // var draggedElement = document.getElementById(idd);
    //         // var dropElement = document.getElementById(e.target.id);
    //         // var targettxt = AJS.$(e.target);
    //         // console.log(dropElement + " " + draggedElement);
    //         // AJS.$(this).html(ui.draggable.html());
    //         // document.getElementById(idd).innerHTML = targettxt;
    //         // document.getElementById(idd).style = "position: relative; left: 0px; top: 0px;";
    //     }
    // });

}

// function dragStart(event) {
//     // var data = event.dataTransfer.getData("Node");
//     event.target.appendChild(document.getElementById(event.currentTarget.id));
//     event.preventDefault();
// }

// function dragDrop(event) {
//     event.preventDefault();
// }

// function drop(event) {
//     event.dataTransfer.setData("Node", event.target.id);
// }

// AJS.$(function () {
// AJS.$("#sortable").sortable();
// AJS.$("#sortable").disableSelection();
// });

AJS.$(".help-item").live('click', function(){
    var htmlContent = AJS.$(this).next('#more-details-help').html();
    var descriptionInlineDialog = AJS.InlineDialog(AJS.$(".help-item"), "description",
        function(content, trigger, showPopup) {
            content.css({"width":"380px","padding":"5px"}).html(htmlContent);
            showPopup();
            return false;
        }
    );
    descriptionInlineDialog.show(event);
});

function customFieldDialogPreviousButton() {
    selectedFieldDetails.name = '';
    selectedFieldDetails.description = '';
    selectedFieldDetails.options = [];

    toggleCustomFieldSearch('add');
    AJS.$(".aui-dialog2-header .aui-dialog2-header-main").text(AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.custom.selectAFieldType'));
    AJS.$("#custom-field-dialog-next-button").removeClass("aui-button-secondary").addClass("aui-button-primary");
}

function toggleCustomFieldSearch(searchStatus) {
    if (searchStatus == 'add') {
        var customFieldSearch = ZEPHYR.Templates.ZephyrCustomField.customFieldSearchTemplate({ searchValue: '' });
        var searchData = {
            target: {
                value: customFieldSearchValue,
            },
        };
        searchCustomFieldType(searchData);
        AJS.$("#customFieldSearchInput").empty();
        AJS.$("#customFieldSearchInput").append(customFieldSearch);
    } else if (searchStatus == 'remove') {
        AJS.$("#customFieldSearchInput").empty();
    }
}

function displayFieldType(type) {
    var displayName;
    switch(type) {
        case 'RADIO_BUTTON' :
            displayName = 'Radio Buttons';
            break;

        case 'CHECKBOX' :
            displayName = 'Checkboxes';
            break;

        case 'MULTI_SELECT' :
            displayName = 'Select List (multiple choices)';
            break;

        case 'SINGLE_SELECT' :
            displayName = 'Select List (single choices)';
            break;

        case 'TEXT' :
            displayName = 'Text Field (single line)';
            break;

        case 'LARGE_TEXT' :
            displayName = 'Text Field (multi line)'
            break;

        case 'NUMBER' :
            displayName = 'Number Field'
            break;

        case 'DATE' :
            displayName = 'Date Picker';
            break;

        case 'DATE_TIME' :
            displayName = 'Date Time Picker'
            break;

    }

    return displayName;
}

function createCustomField() {
    if (AJS.$("#customFieldModelCardData")) {
        AJS.$("#customFieldModelCardData").scrollTop(0)
    }
    var projectArray = AJS.$('#selectedFieldProject').val();
    var project;
    if (projectArray && projectArray.length) {
        project = projectArray.toString();
    }
    var sendingData = {
        name: selectedFieldDetails.name,
        description: selectedFieldDetails.description,
        defaultValue: '',
        isActive: true,
        fieldType: selectedCustomFieldDetails.type,
        aliasName: '',
        projectId: project || '',
        displayName: '',
        displayFieldType: displayFieldType(selectedCustomFieldDetails.type),
        entityType: attribute,
        fieldOptions: selectedFieldDetails.options,
    };

    var xhr = {
        url: projectId ? ("/customfield/create?projectId=" + projectId) : "/customfield/create",
        method: "POST",
        data: sendingData
    };

    if ((isProjectPage ? sendingData.projectId.length > 0 : true) && (selectedFieldDetails.name.trim().length > 0) && (selectedCustomFieldDetails.options ? selectedFieldDetails.options.length > 0 : true)) {
        customXhrCall(xhr, function (response) {
            var toastrMsg = projectId ? ZEPHYR.Templates.ZephyrCustomField.createCustomFieldMessage({ responseMessage: JSON.parse(response.responseMessage) }) : 'Custom field created successfully';
            projectId ? showToastrMsg(toastrMsg, 'info') : showToastrMsg(toastrMsg, 'success');
            AJS.$("#custom-field-dialog-create-button1").prop('disabled', true);
            AJS.$('#customFieldDetailsOuterContainer').fadeTo('slow',.6);
            AJS.$('#customFieldDetailsOuterContainer').append('<div style="position: absolute;top:0;left:0;width: 100%;height:100%;z-index:2;opacity:0.4;filter: alpha(opacity = 50)"></div>');
            loadExecutionDashboard();
            loadTestSteps();
            selectedFieldDetails.description = '';
        }, function (response, jqXHR) {
            console.log('fail response : ', response);
            var responseText = jQuery.parseJSON(response.responseText);
            var responseMsg = jQuery.parseJSON(responseText.responseMessage);
            var messages = [];
            if(responseMsg) {
                Object.keys(responseMsg).forEach(function (key) {
                    messages.push(key);
                })
            } else if(responseText.error) {
                responseMsg = responseText.error;
                messages.push(responseMsg);
            }
            var outputMsg = '';
            messages.forEach(function (key) {
              if(responseMsg[key]) {
            	   outputMsg += key + ":" + responseMsg[key] + "<br/>";
               }
            });
            if(outputMsg.length != 0) {
              showToastrMsg(outputMsg, 'error');
            }
        });
    } else {
        showToastrMsg('Please fill all mandatory fields', 'error');
    }
}

function showToastrMsg(msg, type, cb) {

    if(!AJS.$('.simpleclass').length) {
        require(['aui/flag'], function(flag) {
            var customFieldFlag = flag({
                type: type,
                title: '',
                body: msg
            });
            setTimeout(function() {
                customFieldFlag.close();
            }, 4000);
        });
    }

    AJS.$('.simpleclass').empty();
    if (type == "error") {

        AJS.messages.error(AJS.$('.simpleclass'), {
            title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
            body: msg,
            closeable: true
        });
    }
    else if (type == "info") {

        AJS.messages.info(AJS.$('.simpleclass'), {
            title: '',
            body: msg,
            closeable: true
        });
    }
    else {
        AJS.messages.success(AJS.$('.simpleclass'), {
            title: AJS.I18n.getText("execute.test.success.header"),
            body: msg,
            closeable: true
        });
    }
}

function customFieldDialogCloseButton() {
    AJS.dialog2("#customFieldModel").hide();
}

function selectedFieldDetailsFilling(event) {
    if (event.target.name == 'selectedFieldName') {
        var regex = /[^,/"\:\\]*$/;;
        if (event.target.value.trim().match(regex) != event.target.value.trim()) {
            if (event.target.value.trim() != '') {
                //  FAIL CASE
                AJS.$('#selectedFieldName')[0].value = selectedFieldDetails.name;
            } else {
                //  SUCCESS CASE
                selectedFieldDetails.name = event.target.value.trim();
            }
        } else {
            //  SUCCESS CASE
            selectedFieldDetails.name = event.target.value.trim();
        }
    } else if (event.target.name == 'selectedFieldDescription') {
        selectedFieldDetails.description = event.target.value.trim();
    } else if (event.target.name == 'customFieldOptions') {
        if (event.key == 'Enter') {
            addNewOption();
        }
    }
}

// THIS FUNCTION SHOULD BE CALLED ON CLICK OF THE DELETE BUTTON
function deleteCustomField(event) {
    for (var counter = 0; counter < totalCustomFields.length; counter += 1) {
        if (event.dataset.customfieldid == totalCustomFields[counter].id) {
            var currentFieldType = totalCustomFields[counter].fieldType;

            var heading = AJS.I18n.getText('enav.bulk.delete.schedule.label') + " " + currentFieldType.toLowerCase().split("_").join(" ");
            var customFieldsTypeModel = ZEPHYR.Templates.ZephyrCustomField.modifyCustomFieldTypeModel({ customFieldModifyHeading: heading, mode: 'delete', customField: totalCustomFields[counter] });
            AJS.$("#creatingNewCustomFieldModelBox").empty();
            AJS.$("#creatingNewCustomFieldModelBox").append(customFieldsTypeModel);
            AJS.dialog2("#customFieldModel").show();

            break;
        }
    }
}

// THIS FUNCTION SHOULD BE CALLED ON CLICK OF THE EDIT BUTTON
function editCustomField(event) {
    for (var counter = 0; counter < totalCustomFields.length; counter += 1) {
        if (event.dataset.customfieldid == totalCustomFields[counter].id) {
            updatingField = JSON.parse(JSON.stringify(totalCustomFields[counter]));
            var heading = AJS.I18n.getText('zephyr.je.edit.button.title') + " " + (updatingField.displayFieldType ||  updatingField.fieldType.toLowerCase().split("_").join(" "));
            var customFieldsTypeModel = ZEPHYR.Templates.ZephyrCustomField.modifyCustomFieldTypeModel({ customFieldModifyHeading: heading, mode: 'edit', customField: totalCustomFields[counter] });
            AJS.$("#creatingNewCustomFieldModelBox").empty();
            AJS.$("#creatingNewCustomFieldModelBox").append(customFieldsTypeModel);
            AJS.dialog2("#customFieldModel").show();

            break;
        }
    }
}

function configureCustomField(event) {
    for (var counter = 0; counter < totalCustomFields.length; counter += 1) {
        if (event.dataset.customfieldid == totalCustomFields[counter].id) {
            updatingField = JSON.parse(JSON.stringify(totalCustomFields[counter]));
            var heading = AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.custom.configureCustomField') + " " + updatingField.fieldType.toLowerCase().split("_").join(" ");
            var customFieldsTypeModel = ZEPHYR.Templates.ZephyrCustomField.modifyCustomFieldTypeModel({ customFieldModifyHeading: heading, mode: 'configure', customField: totalCustomFields[counter] });
            AJS.$("#creatingNewCustomFieldModelBox").empty();
            AJS.$("#creatingNewCustomFieldModelBox").append(customFieldsTypeModel);
            AJS.dialog2("#customFieldModel").show();

            // ******************** NEED TO CHECK IF THIS CAN BE REMOVED OR NOT ********************
            if (totalCustomFields[counter].customFieldOptionValues && totalCustomFields[counter].customFieldOptionValues.length != 0) {
                insertOptions(totalCustomFields[counter].customFieldOptionValues);
            }

            break;
        }
    }
}

function updatingCustomField(event) {
    if (event.target.name == 'updatingCustomFieldDescription') {
        editedFields.description = true;
        updatingField.description = event.target.value;
        AJS.$('#edit-custom-field').removeClass('disabled');
    } else if (event.target.name == 'updatingCustomFieldName' && event.target.value != '') {
        // var regex = /[^,\\]+/;
        // var regex = /^[\sA-Za-z0-9!@#$%^&*()_+-={}[]|\:;"'<>.?\/]*$/i;
        // var regex = /^[[A-Za-z0-9!$%^*_@./#&+-]+[\sA-Za-z0-9!$%^*_@./#&+-]*]*$/i;
        var regex = /[^/,"\:\\]*$/;;
        if (event.target.value.trim().match(regex) != event.target.value.trim()) {
            if (event.target.value.trim() != '') {
                //  FAIL CASE
                AJS.$('#updatingCustomFieldName')[0].value = updatingField.name;
            } else {
                //  SUCCESS CASE
                editedFields.name = true;
                updatingField.name = event.target.value;
                AJS.$('#edit-custom-field').removeClass('disabled');
            }
        } else {
            //  SUCCESS CASE
            editedFields.name = true;
            updatingField.name = event.target.value;
            AJS.$('#edit-custom-field').removeClass('disabled');
        }
    } else if (event.target.name == 'updatingCustomFieldName' && event.target.value == '') {
        if (!AJS.$('#edit-custom-field').hasClass('disabled')) {
            AJS.$('#edit-custom-field').addClass('disabled');
        }
    }
}

function addNewOptionInEditFlow(event) {
    var value = AJS.$("#addingNewOptionsEditMode").val();
    var optionExist = false;
    Object.keys(updatingField.customFieldOptionValues).forEach(function (key) {
        if (updatingField.customFieldOptionValues[key] === value.trim()) {
            AJS.$('#errorOptionExist').show();
            AJS.$("#errorEmptyOption").hide();
            AJS.$("#errorOptionLengthExceeding").hide();
            optionExist = true;
        }
    })

    if (!value || value.trim() == '') {
        AJS.$("#errorEmptyOption").show();
        AJS.$('#errorOptionExist').hide();
        AJS.$("#errorOptionLengthExceeding").hide();
        return;
    }
    if (!optionExist && AJS.$("#addingNewOptionsEditMode").val() !== '' && AJS.$("#addingNewOptionsEditMode").val() !== undefined) {
        var xhr = {
            url: '/customfield/' + event.dataset.customfieldid + '/customfieldOption',
            data: {
                isDisabled: "false",
                optionValue: AJS.$("#addingNewOptionsEditMode").val(),
                sequence: "",
            },
            method: 'POST',
        };

        customXhrCall(xhr, function (response) {
            AJS.$('#errorOptionExist').hide();
            AJS.$("#errorEmptyOption").hide();
            AJS.$("#errorOptionLengthExceeding").hide();
            for (var counter = 0; counter < totalCustomFields.length; counter += 1) {
                if (totalCustomFields[counter].id == updatingField.id) {
                    totalCustomFields[counter].customFieldOptionValues[response.optionId] = response.optionValue;
                }
            }
            updatingField.customFieldOptionValues[response.optionId] = response.optionValue;
            AJS.$("#addingNewOptionsEditMode").val('');
            insertOptions(updatingField.customFieldOptionValues);
        }, function (response) {
            console.log('fail', response);
        });

    }
    AJS.$("#addingNewOptionsEditMode").focus();
    console.log('sending data : ', xhr);
    console.log('updatingField.fieldOptions : ', updatingField.customFieldOptionValues);
}

function deleteOptionInEditMode(event) {
    if (Object.keys(updatingField.customFieldOptionValues).length !== 1) {
        var xhr = {
            url: '/customfield/customfieldOption/' + event.dataset.id,
            method: 'DELETE',
        };
        customXhrCall(xhr, function (response) {
            for (var counter = 0; counter < totalCustomFields.length; counter += 1) {
                if (totalCustomFields[counter].id == updatingField.id) {
                    delete totalCustomFields[counter].customFieldOptionValues[event.dataset.id]
                }
            }
            delete updatingField.customFieldOptionValues[event.dataset.id];
            insertOptions(updatingField.customFieldOptionValues);
        }, function (response) {
            console.log('delete fail');
        });
    } else {
        AJS.$('#errorCustomField').show();
        setTimeout(function () { AJS.$('#errorCustomField').hide(); }, 2000);
    }
}

AJS.$('.edit-option-inner-container').live('click', function (event) {
    event.preventDefault();
    event.stopPropagation();
    console.log('inside the function : ', event.currentTarget.dataset.optionid);

    if (AJS.$('.optionsAdded.editable').length == 1) {
        AJS.$('.optionsAdded.editable').removeClass('editable');
        AJS.$('.errorOptionName').hide();
        AJS.$('#errorOptionExist').hide();
        AJS.$("#errorOptionLengthExceeding").hide();
    }
    var value = AJS.$('#custom-field-readmode-' + event.currentTarget.dataset.optionid)[0].innerText;
    AJS.$('#custom-field-outer-id-' + event.currentTarget.dataset.optionid).addClass('editable');
    AJS.$('#custom-field-option-update-' + event.currentTarget.dataset.optionid).val(value);
});

AJS.$('#cancel-custom-field-edit').live('click', function () {
    if (AJS.$('.optionsAdded.editable').length) {
        AJS.$('.optionsAdded.editable').removeClass('editable');
        AJS.$('.errorOptionName').hide();
        AJS.$('#errorOptionExist').hide();
        AJS.$('#errorOptionLengthExceeding').hide();
    }
});

AJS.$('#save-custom-field-option').live('click', function (event) {
    event.preventDefault();
    event.stopPropagation();
    var optionExist = false
    var value = AJS.$('#custom-field-option-update-' + event.currentTarget.dataset.optionid)[0].value;
    Object.keys(updatingField.customFieldOptionValues).forEach(function (key) {
        if (key != event.currentTarget.dataset.optionid && updatingField.customFieldOptionValues[parseInt(key, 10)] === value) {
            AJS.$('#errorOptionExist').show();
            AJS.$('#errorOptionLengthExceeding').hide();
            AJS.$('#custom-field-outer-id-' + event.currentTarget.dataset.optionid + ' .errorOptionName').hide();
            optionExist = true;
        }
    })

    if (value === '') {
        AJS.$('#custom-field-outer-id-' + event.currentTarget.dataset.optionid + ' .errorOptionName').show();
        AJS.$('#errorOptionExist').hide();
        AJS.$('#errorOptionLengthExceeding').hide();
    } else if (value.length > 255) {
        AJS.$('#errorOptionLengthExceeding').show();
        AJS.$('#modifyCustomFieldModelCardData').scrollTop(AJS.$('#modifyCustomFieldModelCardData').height())
        AJS.$('#errorOptionExist').hide();
        AJS.$('#custom-field-outer-id-' + event.currentTarget.dataset.optionid + ' .errorOptionName').hide();
        optionExist = true;
    } else if (!optionExist) {
        AJS.$('.errorOptionName').hide();
        var xhr = {
            url: '/customfield/customfieldOption/' + event.currentTarget.dataset.optionid,
            method: 'PUT',
            data: {
                optionValue: value,
            },
        };

        customXhrCall(xhr, function (response) {
            for (var counter = 0; counter < totalCustomFields.length; counter += 1) {
                if (totalCustomFields[counter].id == updatingField.id) {
                    totalCustomFields[counter].customFieldOptionValues[event.currentTarget.dataset.optionid] = AJS.$('#custom-field-option-update-' + event.currentTarget.dataset.optionid)[0].value;
                }
            }
            updatingField.customFieldOptionValues[event.currentTarget.dataset.optionid] = AJS.$('#custom-field-option-update-' + event.currentTarget.dataset.optionid)[0].value;
            insertOptions(updatingField.customFieldOptionValues);
            AJS.$('#errorOptionExist').hide();
            AJS.$('#errorOptionLengthExceeding').hide();
            console.log('updatingField : ', updatingField);
        }, function (responpse) {
            console.log('update fail : ', response);
        });
    } else {
        AJS.$('.errorOptionName').hide();
    }
});

AJS.$(document).on('click', '.custom-field-editmode-option-value', function (event) {
    event.stopImmediatePropagation();
    console.log('stopting body event propogatino');
});

AJS.$(document).on('click', function () {
    if (AJS.$('.optionsAdded.editable').length) {
        AJS.$('.optionsAdded.editable').removeClass('editable');
        AJS.$('.errorOptionName').hide();
        AJS.$('#errorOptionExist').hide();
        AJS.$('#errorOptionLengthExceeding').hide();
    }
});

function insertOptions(options) {
    var newOptions = [];
    for (var key in options) {
        if (options.hasOwnProperty(key)) {
            newOptions.push({
                id: key,
                value: options[key],
            });
        }
    }
    var customFieldDeletingContent = ZEPHYR.Templates.ZephyrCustomField.editCustomFieldOptions({ customFieldEditingOptions: newOptions });
    AJS.$("#optionsOuterContainer").empty();
    AJS.$("#optionsOuterContainer").append(customFieldDeletingContent);
}

// API HITTING FUNCTION
function customXhrCall(xhr, successCallback, errorCallback) {
    var method = '';
    if (!!xhr.data) {
        method = 'POST';
    } else {
        method = 'GET';
    }

    if (!!xhr.method) {
        method = xhr.method;
    }
    AJS.$.ajax({
        url: getRestURL() + xhr.url,
        type: method,
        contentType: "application/json",
        data: JSON.stringify(xhr.data),
        Accept: "application/json",
        success: function (response) {
            setTimeout(function() {
                successCallback(response);
            }, 500);
        },
        error: function (xhr, status, error) {
            if (xhr.status !== 403) {
                console.log('status code : ', xhr.status);
                var errorMsg = xhr.responseText && JSON.parse(xhr.responseText) && JSON.parse(xhr.responseText).error;
                if (errorMsg) {
                    showToastrMsg(errorMsg, 'error');
                }
                errorCallback(xhr);
            }
            console.log('error', xhr, error);
        },
        statusCode: {
            403: function (xhr, status, error) {
                console.log('status code : 403')
                errorCallback(xhr);
            }

        }
    });
}
