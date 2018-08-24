/* Performs AJAX operation to call backend. Deletes the row on success */
if(!ZEPHYR.Schedule)
	ZEPHYR.Schedule = {}
/**
* This code can be associated to any delete functionality of a row on AUI table.
*  - Uses soy js to create the confirmation dialog, this message is current specific to schedule but can be passed as init param to this method
*  - Performs delete using rest call and removes the row in the main table.
*/
ZEPHYR.Schedule.addDeleteTrigger = function(successCallback, id, url, isFolder, isGridComponent){
	new JIRA.FormDialog({
	    id: "zephyr-je-delete-execution-link",
	    trigger: (isGridComponent ? "a.trigger-delete-dialog":"a.issue-schedule-operations"),
	    handleRedirect:true,
	    content: function (callback, a, b) {
	    	try{
	    		/*publish deletion started event on top level div, there is a listener added in zephyr-project-dashboard.js */
    			AJS.$("#project-panel-cycle-list-summary").trigger("scheduleDeleteStarted");
						var innerHtmlStr;
						if(isGridComponent) {
							innerHtmlStr = ZEPHYR.Templates.Schedule.deleteScheduleConfirmationDialog({schedule:{id:id, url:url, isFolder: isFolder}});
						} else {
        			innerHtmlStr = ZEPHYR.Templates.Schedule.deleteScheduleConfirmationDialog({schedule:{id:this.$activeTrigger.attr("rel"), url:this.$activeTrigger.attr("href"), isFolder: this.$activeTrigger.attr("isFolder")}});
						}
						callback(innerHtmlStr);
	    	}catch(err){console.log(err)}
        },
        submitHandler: function (e, callback) {
        	performScheduleDelete(e.target.action, this, callback);
        	e.preventDefault();
        },
	    issueMsg : 'thanks_issue_worklogged',
	    onContentRefresh: function () {
	        AJS.$(".overflow-ellipsis").textOverflow();
	    }
	});

	var performScheduleDelete = function(deleteUrl, dialog, dialogCallback){
		AJS.$('#zephyr-je-delete-execution-link').addClass('zephyr-je-delete-execution-link')
		jQuery.ajax({
    		url: deleteUrl,	//Use it here to test error cases: "http://localhost:2990/jira/rest/zephyr/latest/execution/500000"
    		type : "delete",
    		contentType :"application/json",
    		dataType: "json",
    		success : function(response) {
                dialog.hide();
								if(isGridComponent) {
                                    if(showExecStatusSuccessMessage && typeof showExecStatusSuccessMessage === 'function') {
                                        showExecStatusSuccessMessage(response.success);
                                    }
								} else {
									var trigger = dialog.$activeTrigger
					    			var deletedScheduleRow = trigger.parent().parent();
					    			/*publish deletion event on top level div, there is a listener added in zephyr-project-dashboard.js <a>*/
					    			AJS.$("#project-panel-cycle-list-summary").trigger("scheduleDeleted");
					    			deletedScheduleRow.fadeOut(800, function(){
					    				deletedScheduleRow.remove();
									});
								}
                if(successCallback) {
                    successCallback();
                }
    		},
    		error : function(response) {
    			var cxt = AJS.$("#schedule-aui-message-bar"),
    				responseText = response.responseText;

    			var errorResponse = jQuery.parseJSON(response.responseText);
    			if (response.status == 403 && errorResponse.PERM_DENIED) {
    				responseText = errorResponse.PERM_DENIED;
    			} else if (response.status == 409) {
                    responseText = errorResponse.errorDescHtml;
                } else {
                    AJS.$(':submit', dialog.$form).removeAttr('disabled');	//enabling submit button on failure - to be resubmission
        			dialog.$form.removeClass("submitting");
                    responseText = AJS.I18n.getText("zephyr.je.submit.form.error.message", "schedule");
    			}
    			cxt.empty();
    			AJS.messages.error(cxt, {
    				title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
    			    body: responseText,
    			    closeable: false
    			});
                dialogCallback();	//implemented in FormDialog.js, Removes the throbber
    		}
    	});
	}
}
