if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
AJS.$.namespace("ZEPHYR.Issue")
if (typeof ZEPHYR.Schedule == 'undefined') { ZEPHYR.Schedule = {}; }
AJS.$.namespace("ZEPHYR.Schedule.Execute")
AJS.$.namespace("ZEPHYR.Schedule.Execution")
AJS.$.namespace("ZEPHYR.ZQL.ZQLSearch")
AJS.$.namespace("ZEPHYR.ZQL.Project")
AJS.$.namespace("ZEPHYR.Dialogs");
if (typeof ZEPHYR.globalVars == 'undefined') { ZEPHYR.globalVars = {unloaded:false}; }

/**
 * Fix for ZFJ-1966
 * Keeping the previously loaded serializeObject method in global variable
 */
ZEPHYR.serializeObject = jQuery.fn.serializeObject;

/*Capture is page is unloaded. Can be used to stop error handling in AJAX as all outstanding AJAX requests will be aborted*/
window.addEventListener("beforeunload", function() {
	ZEPHYR.globalVars.unloaded = true;
}, false);

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

var setTextOverflowEllipses = function() {
	if(jQuery(".overflow-ellipsis").textOverflow) {
		jQuery(".overflow-ellipsis").textOverflow();
	}
};

InitPageContent(function() {
	//JIRA.Dialogs.addCycleDialog =
	var triggerElement;
	var intervalId = setInterval(function() {
    if(triggerElement) {
      clearInterval(intervalId);
      var triggerSelector = (triggerElement && triggerElement.tagName === 'AUI-ITEM-LINK') ? 'aui-item-link.add-cycle-dialog' : 'a.add-cycle-dialog';
			new JIRA.FormDialog({
   	    id: "viewissue-add-cycle",
   	    trigger: triggerSelector,
   	    handleRedirect:true,
   	    issueMsg : 'thanks_issue_worklogged',
   	    onContentRefresh: function () {
   	        setTextOverflowEllipses();
   	    }
   		});
    }
		triggerElement = document.getElementById('viewissue-add-cycle');
	}, 1000);
});


InitPageContent(function() {
	new JIRA.FormDialog({
	    id: "zephyr-je-assign-schedule",
	    trigger: "a.enav-assign-user-schedule",
	    handleRedirect:true,
	    issueMsg : 'thanks_issue_worklogged',
	    onContentRefresh: function () {
	        setTextOverflowEllipses();
	    }
	});
});


InitPageContent(function() {
	new JIRA.FormDialog({
	    id: "zephyr-je-add-execute",
	    trigger: "a.viewissue-add-execute",
	    handleRedirect:true,
	    issueMsg : 'thanks_issue_worklogged',
	    onContentRefresh: function () {
	        setTextOverflowEllipses();
	    }
	});
});

InitPageContent(function() {
	new JIRA.FormDialog({
	    id: "zephyr-attach-file-dialog",
	    trigger: "a.zephyr-file-dialog",
	    handleRedirect:true,
	    issueMsg : 'thanks_issue_worklogged',
	    onContentRefresh: function () {
	        setTextOverflowEllipses();
	    }
	});
});

InitPageContent(function() {
	new JIRA.FormDialog({
	    id: "delete-attachment-dialog",
	    trigger: "a.delete-execute-attachment",
	    handleRedirect:true,
	    issueMsg : 'thanks_issue_worklogged',
	    onContentRefresh: function () {
	        setTextOverflowEllipses();
	    }
	});
});

InitPageContent(function() {
	var timeInterval = 0;
		removeRapidView = function() {
		if(GH && GH.DetailsView && GH.DetailsView.rapidViewConfig && !GH.DetailsView.rapidViewConfig.sprintSupportEnabled) {
			if(AJS.$('[id^="zephyr-je.test.board.tool.section-button"]').length) {
				clearInterval(timeInterval);
				AJS.$('[id^="zephyr-je.test.board.tool.section-button"]').remove();
			}
		} else {
			clearInterval(timeInterval);
		}
	}
	/**
	 * Fix for ZFJ-1689
	 * Remove the TestView toolbar in Kanban board
	 * Adding this condition here because, in latest JIRA (7.2) they are loading the
	 * testboard.vm after an event is triggered on the TestView button.
	 * Remove the TestView from DOM, on an interval of 1 sec for the DOM to load
	 */
	timeInterval = setInterval(function() {
		removeRapidView();
	}, 1000);
});

//
ZEPHYR.Loading = {
	showLoadingIndicator: function() {
		if(this.get$loadingIndicator().css('display') == 'block') return;
		var heightOfSprite = 440, currentOffsetOfSprite = 0, instance = this;
		clearInterval(this.loadingTimer);
		this.get$loadingIndicator().show();
		this.get$zfjAuiBlanket().show();
		this.loadingTimer = window.setInterval(function () {
			if (currentOffsetOfSprite === heightOfSprite) {
				currentOffsetOfSprite = 0;
			}
			currentOffsetOfSprite = currentOffsetOfSprite + 40;
			instance.get$loadingIndicator().css("backgroundPosition", "0 -" + currentOffsetOfSprite + "px");
		}, 50);
	},

	hideLoadingIndicator: function() {
		clearInterval(this.loadingTimer);
		this.get$loadingIndicator().hide();
		this.get$zfjAuiBlanket().hide();
		// this.get$zfjLoadingMessage('').hide();
	},

	showLoadingMessage: function(message) {
		this.get$zfjLoadingMessage(message).show();
	},

	get$loadingIndicator: function() {
		if (!ZEPHYR.Loading.$loadingIndicator) {
			ZEPHYR.Loading.$loadingIndicator = AJS.$("<div />").addClass("jira-page-loading-indicator").css("zIndex", 9999).appendTo("body");
		}
		return ZEPHYR.Loading.$loadingIndicator;
	},

	get$zfjAuiBlanket: function() {
		if (!ZEPHYR.Loading.$zfjAuiBlanket) {
			ZEPHYR.Loading.$zfjAuiBlanket = AJS.$('<div class="aui-blanket" id="zfj-aui-blanket"></div>').appendTo('body');
		}
		return ZEPHYR.Loading.$zfjAuiBlanket;

	},

	get$zfjLoadingMessage: function(message) {
		if (!ZEPHYR.Loading.$zfjLoadingMessage) {
			ZEPHYR.Loading.$zfjLoadingMessage = AJS.$('<div class="zfj-loading-message" id="zfj-loading-message">' + message + '</div>').appendTo('body');
			AJS.$('#zfj-loading-message').css({
				'margin-left': -(AJS.$('#zfj-loading-message').width()/2)
			});
		}
		return ZEPHYR.Loading.$zfjLoadingMessage;

	}
};

function htmlEncode(value){
	if(!value) {
		return;
	}
	return AJS.$('<div/>').text(value).html();
}
function htmlDecode(value){
	if(!value) {
		return;
	}
	return AJS.$('<div/>').html(value).text();
}

ZEPHYR.isIE = function(userAgent) {
	userAgent = userAgent || navigator.userAgent;
  	return userAgent.indexOf("MSIE ") > -1 || userAgent.indexOf("Trident/") > -1;
}

ZEPHYR.scrollLock = function(selector, scrollInterval) {
	var DEFAULT_SCROLL_INTERVAL = 30;
	if (typeof selector !== 'string') {
		scrollInterval = selector;
		selector = null;
	}
	if (!scrollInterval) {
		scrollInterval = DEFAULT_SCROLL_INTERVAL;
	}
	AJS.$(selector).on('DOMMouseScroll mousewheel', function(ev) {
		ev.preventDefault();
		var d;
		if (ev.originalEvent.wheelDelta) d = ev.originalEvent.wheelDelta / 120;
		if (ev.originalEvent.detail) d = -ev.originalEvent.detail / 3;
		this.scrollTop -= d * scrollInterval;
	});
};

//Generating page numbers for pagination
ZEPHYR.generatePageNumbers = function(totalCount, currentIndex, maxResultHit) {
	var pageList = [];
	var pageNumber = 1;
	var endIndex = Math.ceil(totalCount/maxResultHit);
	var iterateIndex=0;
	var ALLOWED_MAX_PAGE_NUMBERS = 9;

	//If currentIndex >= 9 and less than endIndex + 4, than we increment by 4 . At all point, we keep the pages total displayed as 9
	if(currentIndex >= ALLOWED_MAX_PAGE_NUMBERS) { //1
	 	if(endIndex >= (currentIndex+4)) {
	     	pageNumber = currentIndex - 4;
	 	} else {
	         if(totalCount % maxResultHit != 0) {
	        		pageNumber = (endIndex +1) - ALLOWED_MAX_PAGE_NUMBERS + 1;
	         } else {
	         	pageNumber = (endIndex+1) - ALLOWED_MAX_PAGE_NUMBERS;
	         }
	 	}
	}
	//If the Total/Max hit is not equal to 0, we will just add one to it as Round will round it off to previous value
	if(totalCount % maxResultHit != 0) {
	 	//If End Index >= 9, than we need to only show 9 links
	     if(endIndex >= 9) {
	     	endIndex = 8;
	     }
	 	iterateIndex = endIndex;
	} else {
	     //If End Index >= 9, than we need to only show 9 links
	     if(endIndex >= 9) {
	     	endIndex = 9;
	     }
	 	iterateIndex = endIndex;
	}

	for (var index = 0; index < iterateIndex; index++) {
 		pageList.push(pageNumber++);
 	}
 	return pageList;
};

function getErrorMessageFromResponse(response) {
	var consolidatedErrorMessage = "";
	try	{
		var errorResponse = response.responseText;
		if(response.responseText && typeof response.responseText != "object") {
			errorResponse = jQuery.parseJSON(response.responseText);
		}
		if(errorResponse && Object.keys(errorResponse).length) {
			jQuery.each(errorResponse, function(key, value){
				consolidatedErrorMessage += value + "<br/>";
			});
		}

	}catch(err){
		console.log(response.responseText);
		//console.log(err);
		consolidatedErrorMessage = response.responseText;
	}

	return consolidatedErrorMessage;
}

function showExecStatusInfoMessage(title, message) {
	var _message = message;
	AJS.messages.info(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
	    title: title,
	    body: _message,
	    shadowed: true
	});
	setTimeout(function(){
		AJS.$(".aui-message").fadeOut(2000, function(){
			AJS.$(".aui-message").remove();
		});
	}, 1000);
}

function showExecStatusSuccessMessage(message) {
	var _message = message || AJS.I18n.getText("execute.test.success.info");
	AJS.messages.success(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
	    title: AJS.I18n.getText("execute.test.success.header"),
	    body: _message,
	    shadowed: true
	});
	setTimeout(function(){
		AJS.$(".aui-message").not(".zfjEvalLic").fadeOut(2000, function(){
			AJS.$(".aui-message").not(".zfjEvalLic").remove();
		});
	}, 1000);
}

function showErrorMessage(message, timeout, notFadeMessage) {
	var _timeout = timeout || 2000;
	AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
	    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
	    body: message,
	    shadowed: true
	});
	if(notFadeMessage)
		return;
	setTimeout(function(){
		AJS.$(".aui-message.closeable").fadeOut(2000, function(){
			AJS.$(".aui-message.closeable").remove();
		});
	}, _timeout);
}


/**
 * response: xhr object
 * Context: message div selector - defaults to element with id #zephyr-aui-message-bar
 */
function buildExecutionError(response, context){
	var contextSelector = context || ".zephyr-aui-message-bar";
	AJS.$(contextSelector).empty()
	/*Incase response is not JSON based*/
	try{
		var errorResponse = jQuery.parseJSON(response.responseText);
		var consolidatedErrorMessage = "";
		jQuery.each(errorResponse, function(key, value){
			consolidatedErrorMessage += value + "<br/>";
		});
	}catch(err){
		console.log(response.responseText);
		console.log(err);
	}
	consolidatedErrorMessage = consolidatedErrorMessage || (response.status + " - " + response.statusText)
	AJS.messages.error(contextSelector, {
	    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
	    body: consolidatedErrorMessage
	});

	if(typeof(dialog) != "undefined")
		AJS.$(':submit', dialog.$form).removeAttr('disabled');
}

function appendSoyOutputOnCall(str, output) {
	if(output && output.append) {
		return output.append(str);
	} else
		return str;
}

function getRestURL() {
	var baseURL = contextPath + "/rest/zephyr/latest";
	return baseURL;
}

function getZAPIRestURL() {
	var baseURL = contextPath + "/rest/zfjapi/latest";
	return baseURL;
}

function getJiraRestURL() {
	var baseURL = contextPath + "/rest/api/latest";
	return baseURL;
}

//checking if the request is coming from Blueprint gadget
function getRestURLForBlueprints() {
    var pathParam = "zephyr";
    try {
    	if(window && window.top && window.top.Confluence)
    		pathParam = "zapi";
	} catch(e) {
		console.log(e)
	}
    //var baseURL = contextPath + "/rest/"+pathParam+"/latest";
    var baseURL;
    if(pathParam == 'zapi' || !JIRA.Version.isGreaterThanOrEqualTo("7.2")) {
        baseURL = contextPath + "/rest/" + pathParam + "/latest";
    } else {
        baseURL = "/rest/" + pathParam + "/latest";
    }
    return baseURL;
}

function addSlashes(str) {
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

/**
 * @Input key, value
 * @Output encoded and escaped 'key=value'
 */
ZEPHYR.createQueryString = function(param, output) {
	var key = param[0],
		str = param[1],
		query = '';
	if(key && str) {
		str = addSlashes(str);
		query = key + '="' + str + '"';
	}
	return appendSoyOutputOnCall(encodeURIComponent(query), output);
}
//Since AJS.escapeHtml() is not working as expected.
var escapeHtml = function(str) {
    var entityMap = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
        '/': '&#x2F;',
        '`': '&#x60;',
        '=': '&#x3D;'
    };
    return String(str).replace(/[&<>"'`=\/]/g, function (s) {
        return entityMap[s];
    });
}
var showZQLError = function(jqXHR){
	if(jqXHR.responseText){
		var cxt = AJS.$("#zql-message-bar");
		cxt.empty();
		var msg = jQuery.parseJSON(jqXHR.responseText);
		var message;
		var title = "Error";
		if(msg.warning) {
			message = msg.warning;
			title="Warning:";
		} else if(msg.error) {
			message = msg.error;
		} else{
			message = jqXHR.responseText;
		}
		AJS.messages.error(cxt, {
			title: title,
		    body: escapeHtml(message),
		    closeable: true
		});
	}
}

var showAUIError = function(jqXHR, cxtElement){
	if(jqXHR.responseText){
		cxtElement.empty();
		var msg = jQuery.parseJSON(jqXHR.responseText);
		var message = msg.PERM_DENIED;
		AJS.messages.error(cxtElement, {
			body: message,
		    closeable: true
		});
	}
}

var showPermissionError = function(jqXHR) {
	if(jqXHR.responseText) {
		var cxt = AJS.$("[id^=zfj-permission].active");
		if(cxt.length > 0) {
			cxt.empty();
			var msg = jQuery.parseJSON(jqXHR.responseText);
			var message = msg.PERM_DENIED;
			var title = "Permission Denied";
			AJS.messages.error(cxt, {
				title: title,
			    body: message,
			    closeable: true
			});
		}
	}
}

var doPerformCallbackErrorHandling = function() {
	var isCallbackErrorHandling = false;

	if(ZEPHYR && ZEPHYR.page == 'testview-issues') {
		isCallbackErrorHandling = true;
	}
	return isCallbackErrorHandling;
}

var showErrorMessageInDialog = function() {
	/**
	 * Checking by element and not just by JIRA dialog class, because if JIRA changes their class name then these conditions will fail
	 * Execute
	 * Add To test cycle
	 * Reorder Execution
	 * Sprint dialogs
	 */
	return (AJS.$('#zephyr-tb-link-to-test-cycles').length) || (AJS.$('#zephyr-tb-remove-test-cycle').length) ||
	(AJS.$('#reorder-executions-dialog').length) || (AJS.$('#zephyr-tb-link-to-test-cycles').length) ||
	(AJS.$('#viewissue-add-cycle').length && AJS.$('#viewissue-add-cycle').hasClass('zephyr-je-add-execution')) ||
	(AJS.$('#zephyr-je-add-execute').length && AJS.$('#zephyr-je-add-execute').hasClass('zephyr-je-add-execution')) ||
	(AJS.$("#zephyr-je-delete-execution-link").length && AJS.$("#zephyr-je-delete-execution-link").hasClass('zephyr-je-delete-execution-link'));
}

var downloadImage = function(ev){
	var href = ev.dataset.href;
	var win = window.open(href, '_blank');
    win.focus();
};

var ajaxPrefilterFn = function(options, originalOptions, jqXHR) {
	if (/\/zephyr/.test(options.url)) {
		   if(window.zEncKeyFld) {
			   jqXHR.setRequestHeader(window.zEncKeyFld, window.zEncKeyVal);
		   } else {
   	        jqXHR.setRequestHeader(parent.zEncKeyFld, parent.zEncKeyVal);
	       }

		   var statusCode = jqXHR.statusCode
		   if(!statusCode)
			   jqXHR.statusCode = statusCode = {}
		   statusCode[403] = function(){
			   var dialog = new AJS.Dialog({width:200, height:200, id:"",
				    closeOnOutsideClick: true
				});
			   dialog.addPanel("", (AJS.I18n.getText("common.forms.ajax.unauthorised.alert")));
			   dialog.show();
			   dialog.addButton(AJS.I18n.getText("admin.common.words.ok"), function (dialog) {
				    dialog.hide();
				});
		   }
		   var error = options.error;
		   options.error = function (jqXHR, textStatus, errorThrown) {
		   		var errorResponse = {};
		   		try {
					errorResponse = jQuery.parseJSON(jqXHR.responseText);
		   		} catch(e) {
		   			console.log(e);
		   		}
				if (jqXHR.status == 403 && errorResponse.PERM_DENIED) {
					// For delete link, link to test cycle, reorder execution, execute and add test to cycle, display 403 in same dialog
					if(showErrorMessageInDialog()) {
						jqXHR.retainDialog = true;
						return error(jqXHR, textStatus, errorThrown);
					}
					if(AJS.$("#view_issue_execution_section").length) {
						var issueEl = AJS.$("#zfj-permission-message-bar-issue-executions-detail");
						// Remove the table headers if no permission to view executions
						if(AJS.$('#ztestSchedulesTable #zephyr-je-execution-body tr').length == 0) {
							AJS.$('#ztestSchedulesTable').empty();
						}
						AJS.$(".jira-dialog").hide();
						showAUIError(jqXHR,issueEl);
					} else if(doPerformCallbackErrorHandling()) {
						return error(jqXHR, textStatus, errorThrown);
					} else {
						showPermissionError(jqXHR);
						AJS.$(".jira-dialog").hide();
					}
					AJS.$('.loading').removeClass('loading'); // Remove the loading indicator
				} else {
					if(error) {
						return error(jqXHR, textStatus, errorThrown);
					} else {
						return options.error;
					}
				}
		   };
	   }
}

AJS.$.ajaxPrefilter(function( options, originalOptions, jqXHR ) {
	   ajaxPrefilterFn(options, originalOptions, jqXHR);
});

if($) {
	$.ajaxPrefilter(function( options, originalOptions, jqXHR ) {
		   ajaxPrefilterFn(options, originalOptions, jqXHR);
	});
	jQuery.ajaxPrefilter(function( options, originalOptions, jqXHR ) {
		   ajaxPrefilterFn(options, originalOptions, jqXHR);
	});
}

function getGridInitialCount(initialCount) {
	var userAgent = navigator.userAgent && navigator.userAgent.toLowerCase();
	if((userAgent.indexOf("msie") > -1) || (userAgent.indexOf("trident") > -1) || (userAgent.indexOf("edge") > -1)) {
		return 1;
	}
	return initialCount;
}

function convertToLowerCase(str, output) {
	var _str = str[0] || '';
	return appendSoyOutputOnCall(_str.toLowerCase(), output);
}

function displayUnAuthErrorDialog(statusText) {
	var dialog = new AJS.Dialog({
 		    width:800,
 		    height:230,
 		    id:	"dialog-error"
 		});
 	dialog.addHeader(statusText);

 	dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
 	AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
		title: AJS.I18n.getText('zephyr.je.submit.form.error.title'),
	    body: AJS.I18n.getText('zephyr.common.login.error'),
	    closeable: false
	});

 	dialog.addLink("Close", function (dialog) {
 	    dialog.hide();
 	}, "#");
		dialog.show();
}

/*
 * Listen to AJAX error
 */
AJS.$(document).ajaxError(function (ev, request, settings) {
	if (settings.url.indexOf('/zephyr') > -1 && (request.status == 403) && (request.responseText && (JSON.parse(request.responseText).errorId ? (JSON.parse(request.responseText).errorId.toLowerCase() != 'insufficient issue permissions' && JSON.parse(request.responseText).errorId.toLowerCase() != 'insufficient project permissions') : true))) {
 		/*
 		 * ZFJ-1208: Adding a check if there is a about dialog already present do nothing.
 		 */
 		if(request.responseText) {
			var msg = jQuery.parseJSON(request.responseText);
	 		if(settings.url.indexOf('/zephyr/latest/license')) {

	 			/*
		 		 * ZFJ-1287: Should not show the license expiration message when click on "About Zephyr" with expired license.
		 		 */
	 			if(window.isAboutZephyrClicked){
	 				window.isAboutZephyrClicked = false;
	 				return;
	 			}

	 			if(AJS.$('#show-about-dialog').length > 0) {
	 				AJS.$('#show-about-dialog').find('.aui-dialog-content').html('');
	 				AJS.messages.error(AJS.$('#show-about-dialog').find('.aui-dialog-content'), {
	 				    body: msg.errorDescHtml,
	 				    closeable: false
	 				});
	 				return;
	 			}
	 		}
	 		/* ZFJ-1211, ZFJ-1274 */
            /* Standard Errors are handled in corresponding UI. This dialog is only meant for expired tokens.
                Consult ApplicationConstants.java for more details on errorCodes.
            */
			if(msg.errorId && (msg.errorId >= 10 && msg.errorId <= 21)) {
                AJS.log("Zephyr License Error " + msg.errorId);
	 			return;
	 		}
 		}

	 	// Remove loading indicator and dialogs.
 		ZEPHYR.Loading.hideLoadingIndicator();
 		if(!request.retainDialog) {
		 	AJS.$('.aui-dialog').remove();
		 	AJS.$('.jira-dialog').remove();
		 	AJS.$('#zql-message-bar').html('');
		 	AJS.$(AJS.$('.zephyr-aui-message-bar')[0]).html('');
		 	// Hide JIRA Loading
	 		setTimeout(function() {
	 	 		AJS.$('.jira-page-loading-indicator').hide();
	 	 		AJS.$('.aui-blanket').hide();
	 	 		AJS.$('body').css('overflow', '');
	 		}, 300);
 		}
		/**
		 * ZFJ-2307
		 */
		if ((settings.url.indexOf('/latest/teststep') > -1 )){
		 return;
		}
		if ((settings.url.indexOf('/latest/attachment/attachmentsByEntity') > -1 )){
			return;
		}
		var errorResponse = jQuery.parseJSON(request.responseText);
		if (!errorResponse.PERM_DENIED) {
 			displayUnAuthErrorDialog(request.statusText);
		 }
 	}
});

window.baseContextPath = contextPath;
