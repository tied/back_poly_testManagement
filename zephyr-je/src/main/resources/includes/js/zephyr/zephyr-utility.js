/**
 * Zephyr Util resources
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Utility == 'undefined') { ZEPHYR.Utility = {}; }
/**
 * Allow only numbers
 */
AJS.$('input.zephyr_allow_only_number').live('keyup', function (ev) {
	if(AJS.$(ev.target).hasClass('zephyr_numbers_no_decimals')) {
		this.value = this.value.replace(/[^0-9]/g,'');
	} else {
		this.value = this.value.replace(/[^0-9\.]/g,'');
	}	
});

/**
 * Get sprint details from id
 */
ZEPHYR.Utility.getSprintDetailsFromId = function(sprintId, successCallback, errorCallback) {
	AJS.$.ajax({
	    url: "/rest/agile/1.0/sprint/" + sprintId,
	    contentType: "application/json",
	    success: function (sprint) {
	    	successCallback(sprint);
	    },
		error: function() {
			errorCallback();
		}
	});
}

ZEPHYR.Utility.displayConfirmationOnTimeout = function(xhrJSON) {
	setTimeout(function() {
		if(!xhrJSON.readyState || xhrJSON.readyState == 4) // Request is not initialized or complete
			return;
		var label = AJS.I18n.getText('zephyr-je.common.ajax.timeout.confirm.title'),
			dialogButtons = [
				{
					id : 'zephyr-je-dialog-yes',
				    name : AJS.I18n.getText('zephyr.je.yes.button.title')
				},
		        {
		        	id : 'zephyr-je-dialog-no',
		            name : AJS.I18n.getText('zephyr.je.no.button.title'),
		            type : 'link'
		        }
		     ],
		     dialogContentHTML = AJS.I18n.getText('zephyr-je.common.ajax.timeout.confirm.message'),
		     dialogId = "zephyr-tb-ajax-timeout-confirm",
		     dialogRenderHTML = ZEPHYR.Templates.Dialog.renderDialog({
				"dialogId" : dialogId, 
				"dialogSize" : "aui-dialog2-medium", 
				"dialogHeader" : label, 
				"dialogContent" : dialogContentHTML, 
				"dialogButtons" : dialogButtons,
				isModal: true
		     });
		     		
		AJS.$('body').append(dialogRenderHTML);
		var ajaxTimeoutConfirmDialog = AJS.dialog2('#' + dialogId); // Create Dialog2 component
		ajaxTimeoutConfirmDialog.show();
		ZEPHYR.Loading.hideLoadingIndicator();
		// Yes button
		ajaxTimeoutConfirmDialog.$el.find('#zephyr-je-dialog-yes').click(function(ev) {
		    ev.preventDefault();
		    if(xhrJSON.status != 200 || xhrJSON.readyState != 4)
		    	ZEPHYR.Loading.showLoadingIndicator();
		    ajaxTimeoutConfirmDialog.hide();
		});
		// No link
		ajaxTimeoutConfirmDialog.$el.find('#zephyr-je-dialog-no').click(function(ev) {
		    ev.preventDefault();
		    xhrJSON.abort();
		    ajaxTimeoutConfirmDialog.hide();
		});	
	}, 59000);
}