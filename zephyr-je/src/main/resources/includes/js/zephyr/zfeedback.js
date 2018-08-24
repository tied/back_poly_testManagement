var feedbackDialog;
var name = '';
var email = '';

var isLoadedInIframe = function() {
	try {
		return (window !== window.parent);
	} catch(e) {
		return false;
	}
};

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
};

InitPageContent(function(){
	var showFeedbackDialog = function () {
		name = '';
		email = '';
	    var instance = this;
	    feedbackDialog = new JIRA.FormDialog({
			'id': 'show-feedback-dialog',
			'class': 'aui-layer aui-dialog2 aui-dialog2-large',
	        content: function (callback) {
	        	/*Short cut of creating view, move it to Backbone View and do it in render() */
				var title = AJS.I18n.getText('com.thed.zephyr.je.topnav.tests.feedback.label');
				var zfjTitle = AJS.I18n.getText('zephyr.je.admin.plugin.test.section.zephyr.name');

				var innerHtmlStr = ZEPHYR.Templates.Feedback.testing();
                callback(innerHtmlStr);
	        },
	        submitHandler: function (e) {
	            e.preventDefault();
	        }
	    });
		feedbackDialog.show();
	};
	/*JIRA 6.x changed the generated link ID (removed lnk), so we are looking for either id*/
	AJS.$('a[id^="add-feedback"]').live('click', function(e) {
		window.isFeedbackZephyrClicked = true;
		e.preventDefault();
		showFeedbackDialog();
	});

	//TO TOGGLE THE ANONYMOUS BUTTON
	AJS.$('#send-aconymouslt-container').live('click', function(event) {
		if (event.target.checked) {
			AJS.$("#name-input").attr('disabled', "true");
			AJS.$("#email-input").attr('disabled', "true");

			AJS.$("#name-required-icon").addClass('hide');
			AJS.$("#email-required-icon").addClass('hide');

			name = AJS.$("#name-input")[0].value;
			email = AJS.$("#email-input")[0].value;

			AJS.$("#name-input")[0].value = '-';
			AJS.$("#email-input")[0].value = '-';
		} else {
			AJS.$("#name-input").removeAttr('disabled');
			AJS.$("#email-input").removeAttr('disabled');

			AJS.$("#name-required-icon").removeClass('hide');
			AJS.$("#email-required-icon").removeClass('hide');
			
			AJS.$("#name-input")[0].value = name;
			AJS.$("#email-input")[0].value = email;
		}
	});

	//ON CLICKING CANCEL BUTTON
	AJS.$('#cancel-feedback').live('click', function(event) {
		feedbackDialog && feedbackDialog.hide();
	});

	//ON CLICKING SUBMIT
	AJS.$('#submit-feedback').live('click', function(event) {
		if (AJS.$('#summary-select')[0].value != '' && AJS.$("#description-text-area")[0].value != ''
		    && AJS.$("#summary-input")[0].value != '') {

			var emailRegex = /^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;
			var sendingData = {
				component: AJS.$('#summary-select')[0].value,
                summary: AJS.$('#summary-input')[0].value,
                description: AJS.$("#description-text-area")[0].value,
                sendAnonymous: AJS.$("#send-anonymously")[0].checked,
                userName : AJS.$("#name-input")[0].value
		    };
			if (AJS.$("#send-anonymously")[0].checked) {
				submitData(sendingData);
			} else if (AJS.$("#name-input")[0].value != '' && AJS.$("#email-input")[0].value != '' &&
				emailRegex.test(AJS.$("#email-input")[0].value.toLowerCase())) {
				sendingData.email = AJS.$("#email-input")[0].value;
				submitData(sendingData);
			} else if(!AJS.$("#name-input")[0].value || !AJS.$("#email-input")[0].value) {
                displayErrorMessage("<b>Missing Required Parameters</b>");
			}

        } else {
        	displayErrorMessage("<b>Missing Required Parameters</b>");
		}
	});

	function displayErrorMessage(msg) {
        var cxt = AJS.$("#zephyr-feedback-aui-message-bar");
        cxt.empty();

        AJS.messages.error(cxt, {
            title: '',
            body: msg,
            closeable: true
        });
        setTimeout(function() {
            AJS.$("#submit-feedback").removeAttr('disabled');
            AJS.$("#cancel-feedback").removeAttr('disabled');
            AJS.$('#feedback-buttons-container').find('.icon.throbber').empty();
        }, 1000);
        setTimeout(function() {
            cxt.empty();
        }, 4000);
	}

	function submitData(sendingData) {
        AJS.$.ajax({
            url: getRestURL() + "/feedback",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify(sendingData),
            success: function (response) {
                AJS.$("#submit-feedback").removeAttr('disabled');
            	AJS.$("#cancel-feedback").removeAttr('disabled');
            	AJS.$('#feedback-buttons-container').find('.icon.throbber').empty();
                AJS.messages.success("#zephyr-feedback-aui-message-bar", {
                	body: AJS.I18n.getText('com.thed.zephyr.je.topnav.tests.feedback.success.label'),
                	closeable: true
                });
                setTimeout(function() {
                	feedbackDialog && feedbackDialog.hide();
                }, 2000);
            },
            error: function (response) {
            	displayErrorMessage(AJS.I18n.getText('com.thed.zephyr.je.topnav.tests.feedback.error.label'));
            },
            statusCode: {
                403: function (xhr, status, error) {
                    if (completed)
                        completed.call(null, xhr);
                }
            }
        });
    }
});
