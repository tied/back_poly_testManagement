/**
 * Assign executions to user UI
 */
if(typeof ZEPHYR == 'undefined') ZEPHYR = {};
if(typeof ZEPHYR.Execution == 'undefined') ZEPHYR.Execution = {};
if(typeof ZEPHYR.Execution.Assignee == 'undefined') ZEPHYR.Execution.Assignee = {};
ZEPHYR.Execution.Assignee.type = {};
ZEPHYR.Execution.Assignee.type.CURRENT_USER = 'currentUser';
ZEPHYR.Execution.Assignee.type.ASSIGNEE = 'assignee';


ZEPHYR.Execution.Assignee._getAssigneeOptions = function(assignee, $elID) {
	var restPath = "/rest/api/1.0/users/picker";
	AJS.$.ajax({
		 url: contextPath + restPath,
		 type : "GET",
		 data: {
			 query: assignee,
			 showAvatar: true
		 },
		 contentType :"application/json",
		 dataType: 'json',
		 success: function(response) {
			 _.each(response.users, function(user) {
				 if(user.name == assignee) {
					 /*$assigneeOption = '<option value="' + user.name +'" selected="selected" style="background-image: url(' + user.avatarUrl + ');">' + user.displayName + '</option>';
					 AJS.$('select' + $elID).append($assigneeOption);*/
					 AJS.$('input' + $elID + '-field').val(user.displayName);
					 AJS.$('div' + $elID + '-single-select').attr('data-query', user.displayName)
					 	.addClass('aui-ss-has-entity-icon')
					 	.append('<img class="aui-ss-entity-icon" alt src="' + user.avatarUrl + '" />');

				 }
			 });
		 }
	});
}
/**
 * Execution Assignee User picker
 */
ZEPHYR.Execution.Assignee.createSingleUserPickers = function($el, clearPrevSelection) {
    var restPath = "/rest/api/1.0/users/picker";

    AJS.$($el).each(function () {
        var $this = AJS.$(this);
        if ($this.data("aui-ss")) {
        	if(clearPrevSelection && this.assigneeSelect)
        		this.assigneeSelect.clear();
        	return;
        }

        var data = {showAvatar: true},
            inputText = $this.data('inputValue');

        this.assigneeSelect = new AJS.SingleSelect({
            element: $this,
            submitInputVal: true,
            showDropdownButton: !!$this.data('show-dropdown-button'),
            errorMessage: AJS.format("There is no user \'\'{0}\'\'.", "'{0}'"),
            ajaxOptions: {
                url: contextPath + restPath,
                query: true, // keep going back to the sever for each keystroke
                data: data,
                formatResponse: function(response) {
                	var ret = [];

                    AJS.$(response).each(function(i, suggestions) {
                        var groupDescriptor = new AJS.GroupDescriptor({
                            weight: i, // order or groups in suggestions dropdown
                            label: suggestions.footer
                        });

                        AJS.$(suggestions.users).each(function(){
                            groupDescriptor.addItem(new AJS.ItemDescriptor({
                                value: this.name, // value of item added to select
                                label: this.displayName, // title of lozenge
                                html: this.html,
                                icon: this.avatarUrl,
                                allowDuplicate: false,
                                highlighted: true
                            }));
                        });
                        ret.push(groupDescriptor);
                    });
                    return ret;
                }
            },
            inputText: inputText
        });
    });
}

ZEPHYR.Execution.Assignee.getSelectedParams = function(elID, unassignedCallback) {
	var _radioName = elID ? elID + '-type' :  'zephyr-je-execution-assignee-type';
		_assigneeType = AJS.$('input[name=' + _radioName + ']:checked').val(),
		//assigneeParams = {},
		_elID = '#' + elID || '#zephyr-je-execution-assignee';

	// if(_assigneeType == ZEPHYR.Execution.Assignee.type.CURRENT_USER) {
	// 	assigneeParams.assigneeType = ZEPHYR.Execution.Assignee.type.CURRENT_USER;
	// 	return assigneeParams;
	// } else if(_assigneeType == ZEPHYR.Execution.Assignee.type.ASSIGNEE) {
	// 	var _assignee = AJS.$(_elID).val();
	// 	if(_assignee && _assignee.length > 0) {
	// 		assigneeParams.assigneeType = ZEPHYR.Execution.Assignee.type.ASSIGNEE;
	// 		assigneeParams.assignee = _assignee[0];			// Ex: ['admin']
	// 		return assigneeParams;
	// 	} else {
	// 		AJS.$('.buttons').find('span.icon.throbber').remove('.loading');
	// 		AJS.$('#bulk-assign-user-form-submit').removeAttr('disabled');
 //      AJS.$('input[name=' + _radioName + ']').closest('ul').find('.assignee-error').html(AJS.I18n.getText('execute.test.execution.assignee.type.error'));

 //      setTimeout(function(){
 //        AJS.$('input[name=' + _radioName + ']').closest('ul').find('.assignee-error').html('');
 //      }, 4000);
	// 		//AJS.$('#assignee-error').html(AJS.I18n.getText('execute.test.execution.assignee.type.error'));
	// 		return false;
	// 	}
	// }
  var dataset = AJS.$(_elID).find('.readonly')[0].dataset;
  var assigneeParams = {
    assigneeType: dataset.assigneetype,
    assignee: dataset.assignee
  }
	if(unassignedCallback)
		unassignedCallback();
	else
		return assigneeParams;
}

ZEPHYR.Execution.Assignee.init = function(options) {
	options = options || {};
	var _elID = options.id ? '#' + options.id: '#zephyr-je-execution-assignee';
	var _radioName = options.id ? options.id + '-type' :  'zephyr-je-execution-assignee-type';
	var _assigneeUser = _elID + '-assignee';
	var clearPrevSelection = options.clear;
	var assignee = null;

	// Check if previous selections are present
	if(options.assigneeeJSON){
		clearPrevSelection = false;

		AJS.$('input[name=' + _radioName + '][value=' + options.assigneeeJSON.assigneeType + ']').attr('checked','checked');
		if(options.assigneeeJSON.assigneeType == ZEPHYR.Execution.Assignee.type.ASSIGNEE) {
			assignee = options.assigneeeJSON.assignee;
			AJS.$(_elID + '-field').find('option[value=' + options.assigneeeJSON.assignee + ']').attr('selected');
		}
	}
	try {
	// Attach single select
	ZEPHYR.Execution.Assignee.createSingleUserPickers(AJS.$(_elID), clearPrevSelection, assignee);
	} catch(e) {
		console.log(e);
	}
	if(options.assigneeeJSON && options.assigneeeJSON.assigneeType == ZEPHYR.Execution.Assignee.type.ASSIGNEE && assignee)
		ZEPHYR.Execution.Assignee._getAssigneeOptions(assignee, _elID);

	if(AJS.$('input' + _assigneeUser + ':checked').length == 0)
		AJS.$(_elID + '-field').attr('disabled', 'disabled');

	AJS.$('input[name=' + _radioName + ']').unbind('click');
	AJS.$('input[name=' + _radioName + ']').bind('click', function(ev) {
		ev.stopImmediatePropagation();
		var _assigneeType = AJS.$(this).val();

		if(ev.ctrlKey ||  ev.metaKey) { // Ctrl + click, then unselect the radio button
			AJS.$(this).attr('checked', false);
			AJS.$(_elID + '-field').attr('disabled', 'disabled');
		} else {
			if(_assigneeType == ZEPHYR.Execution.Assignee.type.CURRENT_USER) {
				/*AJS.$('#zephyr-je-execution-assignee-field').val('');
				AJS.$('#zephyr-je-execution-assignee-single-select').find('img.aui-ss-entity-icon').remove();*/
				AJS.$(_elID + '-field').attr('disabled', 'disabled');
			} else if(_assigneeType = ZEPHYR.Execution.Assignee.type.ASSIGNEE) {
				AJS.$(_elID + '-field').removeAttr('disabled');
			}

		}
	});
}
