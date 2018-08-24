AJS.$.namespace("ZEPHYR.Admin.Config")
AJS.$.namespace("ZEPHYR.Templates.ZephyrProjects")
var allProjects = [];
var selectedProjectIds = [];
var disabledProjectList = [];
var paginatedProjectList = [];
var reindexProjects = [];
var reindexProjectList = [];
var selectedReindexProjectList = [];
var currentPage = 1;
var recordsPerPage;
var selectedReindexProjects = {};

//For Execution Workflow disable
var selectedEWProjectIds = [];
var disabledEWProjectList = [];
var paginatedEWProjectList = [];
var currentEWPage = 1;
var recordsPerPageEW;
var reindexEWProjects = [];
var reindexEWProjectList = [];
var selectedEWReindexProjectList = [];

if (typeof Zephyr == "undefined") { Zephyr = {};}
Zephyr.Admin = (function (){
	var configClass = new function(){
		this.updateWorkflowSettings=function(){
			var value = AJS.$("#zephyr-show-workflow").is(":checked");

			jQuery.ajax({
				url: contextPath + "/secure/ZSetWorkflowSettings.jspa?decorator=none&showWorkflow="+value,
				type : "post",
				complete : showError
			});
		};

		/*To store User preference in DB about Version Ping*/
		this.updateVersionSettings=function(){
			var value = AJS.$("#zephyr-version-check").is(":checked");

			jQuery.ajax({
				url: contextPath + "/secure/ZSetVersionSettings.jspa?decorator=none&versionCheck="+value,
				type : "post",
				complete : showError
			});
		};

		this.bindIssueTypeTestOnProjectCreate=function(){
			var value = AJS.$("#zephyr-bind-issuetype-test-project").is(":checked");

			jQuery.ajax({
				url: contextPath + "/secure/ZSetIssueTypeTestProjectCreateSettings.jspa?decorator=none&associateIssueTypeTestOnCreate="+value,
				type : "post",
				complete : showError
			});
		};

		this.issueLinkToggle=function(){
			var value = AJS.$("#zephyr-issuelink-check").is(":checked");

			jQuery.ajax({
				url: contextPath + "/secure/ZLinkIssueToTest.jspa?decorator=none&issueLink="+value,
				type : "post",
				complete : showError
			});
			Zephyr.Admin.Config.initIssueLinkType();
		};

		this.issueLinkStepToggle=function(){
			var value = AJS.$("#zephyr-issuelinkstep-check").is(":checked");

			jQuery.ajax({
				url: contextPath + "/secure/ZLinkIssueToTestStep.jspa?decorator=none&issueLinkStep="+value,
				type : "post",
				complete : showError
			});
			Zephyr.Admin.Config.initIssueLinkType();

		};

		this.initIssueLinkType=function(){
			if(AJS.$("#zephyr-issuelink-check").is(":checked")){
				this.propIssueLinkType(window.globalIssueLinkTypes,window.issueLinkTypeId);
			}else if(AJS.$("#zephyr-issuelinkstep-check").is(":checked")){
				this.propIssueLinkType(window.globalIssueLinkTypes,window.issueLinkTypeId);
			}else if(!AJS.$("#zephyr-issuelinkstep-check").is(":checked")){
				AJS.$("#issue-link-relation-select").html(" ");
				AJS.$("#zephyr-ril-refresh").prop('disabled', true);
			}
		},
		this.propIssueLinkType=function(issueLinkTypes,issueLinkTypeId){
			var opHtml = "";
			for(var i = 0; i < issueLinkTypes.length; i++) {
				if(issueLinkTypeId==issueLinkTypes[i][1]) {
					opHtml += "<option value=" + issueLinkTypes[i][1] + " selected>" + issueLinkTypes[i][0] + "</option>";
				}else{
					opHtml += "<option value=" + issueLinkTypes[i][1] + ">" + issueLinkTypes[i][0] + "</option>";
				}
			}
			AJS.$("#issue-link-relation-select").html(opHtml);
			AJS.$("#zephyr-ril-refresh").prop('disabled', false);
		},
		this.updatePermissionCheck=function(){
			var value = AJS.$("#zephyr-bind-custom-permission").is(":checked");

			jQuery.ajax({
				url: contextPath + "/secure/ZConfigurePermissionSetting.jspa?decorator=none&permissionCheck="+value,
				type : "post",
				complete : showError
			});
		};

        this.updateExecutionExecutedonFlag=function(){
            var value = AJS.$("#zephyr-update-execution-executedon").is(":checked");

            jQuery.ajax({
                url: contextPath + "/secure/ZEnableZephyrUpdateExecutionExecutedon.jspa?decorator=none&updateExecutionExecutedonFlag="+value,
                type : "post",
                complete : showError
            });
        };

        this.updateLogLevel=function(){
            var logLevelType = AJS.$("#zephyr-log-level-select option:selected").val();
            jQuery.ajax({
                url: contextPath + "/secure/ZConfigureLogLevel.jspa?decorator=none&zephyrLogLevel="+logLevelType,
                type : "post",
                complete : showError
            });
        };

		this.updateLogMaxSize=function(){
 			var logMaxSize = AJS.$("#zephyr-log-max-size").val().trim();
            if (isNaN(logMaxSize) || logMaxSize<1 || logMaxSize > 1024){
                var cxt = AJS.$("#general-config-aui-message-bar");
                cxt.empty();
                AJS.messages.error(cxt, {
                    title: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.settings.error"),
                    body: AJS.I18n.getText("zephyr.je.admin.log.max.length","Size",1,1024),
                    closeable: true
                });
                return;
            }
			jQuery.ajax({
				url: contextPath + "/secure/ZConfigureLogMaxSize.jspa?decorator=none&zephyrLogMaxSize="+logMaxSize,
				type : "post",
				complete : showError
			});
		};

        this.updateLogMaxBackup=function(){
            var logMaxBackup = AJS.$("#zephyr-log-max-backup").val().trim();
			if (logMaxBackup % 1 != 0){
				var cxt = AJS.$("#general-config-aui-message-bar");
				cxt.empty();
				AJS.messages.error(cxt, {
					title: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.settings.error"),
					body: AJS.I18n.getText("zephyr.je.admin.log.max.backup.error"),
					closeable: true
				});
				return;
			}
            if (isNaN(logMaxBackup) || logMaxBackup == "" || logMaxBackup < 0 || logMaxBackup > 30000 || logMaxBackup.indexOf(' ') > 0 ){
                var cxt = AJS.$("#general-config-aui-message-bar");
                cxt.empty();
                AJS.messages.error(cxt, {
                    title: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.settings.error"),
                    body: AJS.I18n.getText("zephyr.je.admin.log.max.length","Backup",0,30000),
                    closeable: true
                });
                return;
            }
            jQuery.ajax({
                url: contextPath + "/secure/ZConfigureLogMaxBackup.jspa?decorator=none&zephyrLogMaxBackup="+logMaxBackup,
                type : "post",
                complete : showError
            });
        };

        this.enableIssueSecurity=function(){
            var issueSecurityFlag = AJS.$("#zephyr-bind-issue-security").is(":checked");
            jQuery.ajax({
                url: contextPath + "/secure/ZEnableIssueSecurity.jspa?decorator=none&enableIssueSecurity="+issueSecurityFlag,
                type : "post",
                complete : showError
            });
        };

        this.disableTestSummaryLabelsFilter=function(){
        	var disableTestSummaryLabels = AJS.$("#zephyr-bind-labels-filter").is(":checked");
            jQuery.ajax({
                url: contextPath + "/secure/ZDisableTestSummaryLabelsFilter.jspa?decorator=none&disableTestSummaryLabels="+disableTestSummaryLabels,
                type : "post",
                complete : showError
            });
        };

        this.disableTestSummaryAllFilters=function(){
            var disableTestSummaryAllFilters = AJS.$("#zephyr-bind-all-filters-testsummary").is(":checked");
            jQuery.ajax({
                url: contextPath + "/secure/ZDisableTestSummaryAllFilters.jspa?decorator=none&disableTestSummaryAllFilters="+disableTestSummaryAllFilters,
                type : "post",
                complete : showError
            });
        };

        this.enableDisableZephyrAnalytics=function () {
            var value = AJS.$("#zephyrAnalyticCheck").is(":checked");
            jQuery.ajax({
                url:  contextPath +"/secure/ZDisableZephyrAnalytics.jspa?decorator=none&zephyrAnalytics="+value,
                type : "post",
                complete : showError
            });
        };

		this.remoteIssueLinkToggle=function(){
			var value = AJS.$("#zephyr-ril-check").is(":checked");

			jQuery.ajax({
				url: contextPath + "/secure/ZRemoteLinkIssueToTestExecution.jspa?decorator=none&remoteIssueLink="+value,
				type : "post",
				complete : showError
			});
		};

		this.remoteIssueLinkStepToggle=function(){
			var value = AJS.$("#zephyr-rilstep-check").is(":checked");

			jQuery.ajax({
				url: contextPath + "/secure/ZRemoteLinkIssueToTestStepExecution.jspa?decorator=none&remoteIssueLinkStep="+value,
				type : "post",
				complete : showError
			});
		};

		this.inverseIssueLinkToggle = function () {
			var value = AJS.$("#zephyr-ril-inverse").is(":checked");
			jQuery.ajax({
				url: contextPath + "/secure/ZLinkTestToIssue.jspa?decorator=none&issueLink=" + value,
				type: "post",
				complete: function (jqXHR, textStatus) {
					var dialog = new AJS.Dialog({
						width: 600,
						height: 250,
						id: "reset-link-dialog",
						closeOnOutsideClick: false
					});

					dialog.addHeader(AJS.I18n.getText("zephyr.je.admin.issue.relinking.reset"));
					dialog.addPanel("Panel", "<div class='aui-message warning'><p class='title'><span class='aui-icon icon-warning'></span></p><p><strong>"
						+ AJS.I18n.getText("zephyr.je.admin.issue.relinking.reset.desc") + "</strong></p></div>", "panel-body");
					dialog.addButton("Reset", function (dialog) {
						dialog.hide();
						Zephyr.Admin.Config.refreshRemoteIssueLinks();
					});
					dialog.gotoPage(0);
					dialog.gotoPanel(0);
					dialog.show();
				}
			});
		};

		this.indexAll=function() {
            AJS.$("#reindexJobProgress").hide();
            if(AJS.$('input[name="allProjects"]:checked').length > 0) {

                AJS.$("#zephyr-index-all").attr("disabled", "disabled");
                AJS.$("#zephyr-sync-all").attr("disabled", "disabled");
                var isHardIndex = AJS.$("#hardIndexId").is(":checked");
                var progressBarContainer = AJS.$("#reIndexProgressBar");
                AJS.$("#reindexJobProgress .aui-progress-indicator").attr("data-value", 0);
                AJS.$("#reindexJobProgress .aui-progress-indicator-value").css("width", "0%");
                jQuery.ajax({
                    url: contextPath + "/rest/zephyr/latest/execution/indexAll?isHardIndex=" + isHardIndex,
                    type : "post",
                    complete : function(jqXHR, textStatus){
                        if(jqXHR.status == 200){
                            var token = jQuery.parseJSON(jqXHR.responseText).jobProgressToken;
                            var intervalId = setInterval(function () {
                                AJS.$("#reindexJobProgress").show();
                                jQuery.ajax({
                                    url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + token,
                                    data: {'type': "reindex_job_progress"},
                                    complete: function (jqXHR, textStatus) {
                                        var data = jQuery.parseJSON(jqXHR.responseText);
                                        if (data != undefined) {
                                            if (data.message == AJS.I18n.getText('zephyr.je.admin.reIndex.already.in.progress')) {
                                                AJS.$("#reindexJobProgress .aui-progress-indicator").hide();
                                                AJS.$("#reindexJobProgress .timeTaken").html(data.message);
                                                clearInterval(intervalId);
                                                AJS.$("#zephyr-index-all").removeAttr("disabled");
                                                AJS.$("#zephyr-sync-all").removeAttr("disabled");
                                            } else {
                                                AJS.$("#reindexJobProgress .aui-progress-indicator").show().attr("data-value", data.progress);
                                                AJS.$("#reindexJobProgress .aui-progress-indicator-value").css("width", data.progress * 100 + "%");
                                                AJS.$("#reindexJobProgress .timeTaken").html(AJS.I18n.getText('zephyr.je.cycle.timeTaken') + ": " + data.timeTaken);
                                                if(data.completedSteps >= data.totalSteps) {
                                                    AJS.$("#reindexJobProgress .indexingSteps").html("Indexed " + data.completedSteps + " Of " + data.totalSteps);
												} else {
                                                    AJS.$("#reindexJobProgress .indexingSteps").html("Indexing " + data.completedSteps + " Of " + data.totalSteps);
                                                }
                                                AJS.$("#reindexJobProgress .timeTaken #messageId").remove();
												if (data.progress == 1) {
                                                    AJS.$("#reindexJobProgress .timeTaken").append(" <br /><strong id='messageId'>" + data.message + "</strong>");
                                                    clearInterval(intervalId);
                                                    AJS.$("#zephyr-index-all").removeAttr("disabled");
                                                    Zephyr.Admin.Config.disableSyncButton();
                                                } else {
                                                	if(data.stepMessage){
                                                		AJS.$("#reindexJobProgress .timeTaken").append(" <br /><strong id='messageId'>" + data.stepMessage + "</strong>");
                                                	}
                                                }
                                            }
                                        }
                                    }
                                })
                            }, 1000);
                        }
                        if(jqXHR.status == 401){
                            window.location=contextPath + "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
                        }
                    }
                });
                var concludeIndexOperation = function(jqXHR, textStatus){
                    AJS.$("#zephyr-index-all").removeAttr("disabled");
                    progressBarContainer.prev('span').addClass("hidden");
                    if(jqXHR.status == 200)
                        progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('admin.indexing.reindexing.was.successful', '<strong>', '</strong>', '<strong>'+ jQuery.parseJSON(jqXHR.responseText).took + '</strong>') + '</label>'));
                    else{
                        showError(jqXHR, textStatus)
                    }
                }
                progressBarContainer.empty();
                progressBarContainer.prev('span').removeClass("hidden");

            } else {
                Zephyr.Admin.Config.reindexByProjectIds();
            }

		};

        this.syncIndex=function() {
            AJS.$("#reindexJobProgress").hide();
            if(AJS.$('input[name="allProjects"]:checked').length > 0) {
                AJS.$("#zephyr-reindex-all").attr("disabled", "disabled");
                AJS.$("#zephyr-sync-all").attr("disabled", "disabled");
                var progressBarContainer = AJS.$("#reIndexProgressBar");
                AJS.$("#reindexJobProgress .aui-progress-indicator").attr("data-value", 0);
                AJS.$("#reindexJobProgress .aui-progress-indicator-value").css("width", "0%");
                jQuery.ajax({
                    url: contextPath + "/rest/zephyr/latest/execution/syncIndex",
                    type : "post",
                    complete : function(jqXHR, textStatus){
                        if(jqXHR.status == 200){
                            var token = jQuery.parseJSON(jqXHR.responseText).jobProgressToken;
                            var intervalId = setInterval(function () {
                                AJS.$("#reindexJobProgress").show();
                                jQuery.ajax({
                                    url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + token,
                                    data: {'type': "reindex_job_progress"},
                                    complete: function (jqXHR, textStatus) {
                                        var data = jQuery.parseJSON(jqXHR.responseText);
                                        if (data != undefined) {
                                            if (data.message == AJS.I18n.getText('zephyr.je.admin.reIndex.already.in.progress')) {
                                                AJS.$("#reindexJobProgress .aui-progress-indicator").hide();
                                                AJS.$("#reindexJobProgress .timeTaken").html(data.message);
                                                clearInterval(intervalId);
                                                AJS.$("#zephyr-index-all").removeAttr("disabled");
                                            } else {
                                                AJS.$("#reindexJobProgress .aui-progress-indicator").show().attr("data-value", data.progress);
                                                AJS.$("#reindexJobProgress .aui-progress-indicator-value").css("width", data.progress * 100 + "%");
                                                AJS.$("#reindexJobProgress .timeTaken").html("Time Taken: " + data.timeTaken);
                                                if(data.completedSteps >= data.totalSteps) {
                                                    AJS.$("#reindexJobProgress .indexingSteps").html("Indexed " + data.completedSteps + " Of " + data.totalSteps);
                                                } else {
                                                    AJS.$("#reindexJobProgress .indexingSteps").html("Indexing " + data.completedSteps + " Of " + data.totalSteps);
                                                }
                                                if (data.progress == 1) {
                                                    AJS.$("#reindexJobProgress .timeTaken").append(" <br /><strong>" + AJS.I18n.getText('zephyr.je.reindexed.success') + "</strong>");
                                                    clearInterval(intervalId);
                                                    AJS.$("#zephyr-index-all").removeAttr("disabled");
                                                    Zephyr.Admin.Config.disableSyncButton();
                                                } else {
                                                	if(data.stepMessage){
                                                		AJS.$("#reindexJobProgress .timeTaken").append(" <br /><strong id='messageId'>" + data.stepMessage + "</strong>");
                                                	}
                                                }
                                            }
                                        }
                                    }
                                })
                            }, 1000);
                        }
                        if(jqXHR.status == 401){
                            window.location=contextPath + "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
                        }
                    }
                });
                var concludeIndexOperation = function(jqXHR, textStatus){
                    AJS.$("#zephyr-index-all").removeAttr("disabled");
                    progressBarContainer.prev('span').addClass("hidden");
                    if(jqXHR.status == 200)
                        progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('admin.indexing.reindexing.was.successful', '<strong>', '</strong>', '<strong>'+ jQuery.parseJSON(jqXHR.responseText).took + '</strong>') + '</label>'));
                    else{
                        showError(jqXHR, textStatus)
                    }
                }
                progressBarContainer.empty();
                progressBarContainer.prev('span').removeClass("hidden");

            } else {
                Zephyr.Admin.Config.syncIndexByProjectIds();
            }

        };

        /**
         * Added for reindex by project ids.
         */

        this.reindexByProjectIds = function(){

            AJS.$("#zephyr-index-all").attr("disabled", "disabled");
            AJS.$("#zephyr-sync-all").attr("disabled", "disabled");
            var progressBarContainer = AJS.$("#reIndexProgressBar");
            AJS.$("#reindexJobProgress .aui-progress-indicator").attr("data-value", 0);
            AJS.$("#reindexJobProgress .aui-progress-indicator-value").css("width", "0%");
            var projectIds = new Array();
            AJS.$('#project-view-reindex-detail input[type="checkbox"]:checked').each(function() {
                projectIds.push(AJS.$(this).attr('id'));
            });
            var isHardIndex = AJS.$("#hardIndexId").is(":checked");
            jQuery.ajax({
                url: contextPath + "/rest/zephyr/latest/execution/reindex/byProject?projectIds="+projectIds + "&isHardIndex=" + isHardIndex,
                type : "post",
                complete : function(jqXHR, textStatus){
                    if(jqXHR.status == 200){
                        var token = jQuery.parseJSON(jqXHR.responseText).jobProgressToken;
                        var intervalId = setInterval(function () {
                            AJS.$("#reindexJobProgress").show();
                            jQuery.ajax({
                                url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + token,
                                data: {'type': "reindex_job_progress"},
                                complete: function (jqXHR, textStatus) {
                                    var data = jQuery.parseJSON(jqXHR.responseText);
                                    if (data != undefined) {
                                        if (data.message == AJS.I18n.getText('zephyr.je.admin.reIndex.already.in.progress')) {
                                            AJS.$("#reindexJobProgress .aui-progress-indicator").hide();
                                            AJS.$("#reindexJobProgress .timeTaken").html(data.message);
                                            clearInterval(intervalId);
                                            AJS.$("#zephyr-index-all").removeAttr("disabled");
                                            AJS.$("#zephyr-sync-all").removeAttr("disabled");
                                        } else {
                                            AJS.$("#reindexJobProgress .aui-progress-indicator").show().attr("data-value", data.progress);
                                            AJS.$("#reindexJobProgress .aui-progress-indicator-value").css("width", data.progress * 100 + "%");
                                            AJS.$("#reindexJobProgress .timeTaken").html("Time Taken Now: " + data.timeTaken);
                                            if(data.completedSteps >= data.totalSteps) {
                                                AJS.$("#reindexJobProgress .indexingSteps").html("Indexed " + data.completedSteps + " Of " + data.totalSteps);
                                            } else {
                                                AJS.$("#reindexJobProgress .indexingSteps").html("Indexing " + data.completedSteps + " Of " + data.totalSteps);
                                            }
                                            AJS.$("#reindexJobProgress .timeTaken #messageId").remove();
                                            if (data.progress == 1) {
                                                AJS.$("#reindexJobProgress .timeTaken").append(" <br /><strong id='messageId'>" + data.message + "</strong>");
                                                clearInterval(intervalId);
                                                AJS.$("#zephyr-index-all").removeAttr("disabled");
                                                Zephyr.Admin.Config.disableSyncButton();
                                            } else {
                                            	if(data.stepMessage || data.stepMessage  != ""){
                                            		AJS.$("#reindexJobProgress .timeTaken").append(" <br /><strong id='messageId'>" + data.stepMessage + "</strong>");
                                            	}                                                	
                                            }
                                        }
                                    }
                                }
                            })
                        }, 1000);
                    }
                    if(jqXHR.status == 401){
                        window.location=contextPath + "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
                    }
                }
            });
            var concludeIndexOperation = function(jqXHR, textStatus){
                AJS.$("#zephyr-index-all").removeAttr("disabled");
                AJS.$("#zephyr-sync-all").removeAttr("disabled");
                progressBarContainer.prev('span').addClass("hidden");
                if(jqXHR.status == 200)
                    progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('admin.indexing.reindexing.was.successful', '<strong>', '</strong>', '<strong>'+ jQuery.parseJSON(jqXHR.responseText).took + '</strong>') + '</label>'));
                else{
                    showError(jqXHR, textStatus)
                }
            }
            progressBarContainer.empty();
            progressBarContainer.prev('span').removeClass("hidden");
            selectedReindexProjectList = [];
            reindexProjectList = [];
            AJS.$("#zephyr-reindex-projects-view").delay(15000).fadeIn(1000);
        };

        this.syncIndexByProjectIds = function() {

            AJS.$("#zephyr-index-all").attr("disabled", "disabled");
            AJS.$("#zephyr-sync-all").attr("disabled", "disabled");
            var progressBarContainer = AJS.$("#reIndexProgressBar");
            AJS.$("#reindexJobProgress .aui-progress-indicator").attr("data-value", 0);
            AJS.$("#reindexJobProgress .aui-progress-indicator-value").css("width", "0%");
            var projectIds = new Array();
            AJS.$('#project-view-reindex-detail input[type="checkbox"]:checked').each(function() {
                projectIds.push(AJS.$(this).attr('id'));
            });

            jQuery.ajax({
                url: contextPath + "/rest/zephyr/latest/execution/syncIndex/byProject?projectIds="+projectIds,
                type : "post",
                complete : function(jqXHR, textStatus){
                    if(jqXHR.status == 200){
                        var token = jQuery.parseJSON(jqXHR.responseText).jobProgressToken;
                        var intervalId = setInterval(function () {
                            AJS.$("#reindexJobProgress").show();
                            jQuery.ajax({
                                url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + token,
                                data: {'type': "reindex_job_progress"},
                                complete: function (jqXHR, textStatus) {
                                    var data = jQuery.parseJSON(jqXHR.responseText);
                                    if (data != undefined) {
                                        if (data.message == AJS.I18n.getText('zephyr.je.admin.reIndex.already.in.progress')) {
                                            AJS.$("#reindexJobProgress .aui-progress-indicator").hide();
                                            AJS.$("#reindexJobProgress .timeTaken").html(data.message);
                                            clearInterval(intervalId);
                                            AJS.$("#zephyr-index-all").removeAttr("disabled");
                                        } else {
                                            AJS.$("#reindexJobProgress .aui-progress-indicator").show().attr("data-value", data.progress);
                                            AJS.$("#reindexJobProgress .aui-progress-indicator-value").css("width", data.progress * 100 + "%");
                                            AJS.$("#reindexJobProgress .timeTaken").html("Time Taken Now: " + data.timeTaken);
                                            if(data.completedSteps >= data.totalSteps) {
                                                AJS.$("#reindexJobProgress .indexingSteps").html("Indexed " + data.completedSteps + " Of " + data.totalSteps);
                                            } else {
                                                AJS.$("#reindexJobProgress .indexingSteps").html("Indexing " + data.completedSteps + " Of " + data.totalSteps);
                                            }
                                            if (data.progress == 1) {
                                                AJS.$("#reindexJobProgress .timeTaken").append(" <br /><strong>" + AJS.I18n.getText('zephyr.je.reindexed.success') + "</strong>");
                                                clearInterval(intervalId);
                                                AJS.$("#zephyr-index-all").removeAttr("disabled");
                                                Zephyr.Admin.Config.disableSyncButton();
                                            } else {
                                            	if(data.stepMessage || data.stepMessage  != ""){
                                            		AJS.$("#reindexJobProgress .timeTaken").append(" <br /><strong id='messageId'>" + data.stepMessage + "</strong>");
                                            	}
                                            }
                                        }
                                    }
                                }
                            })
                        }, 1000);
                    }
                    if(jqXHR.status == 401){
                        window.location=contextPath + "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
                    }
                }
            });
            var concludeIndexOperation = function(jqXHR, textStatus){
                AJS.$("#zephyr-index-all").removeAttr("disabled");
                progressBarContainer.prev('span').addClass("hidden");
                if(jqXHR.status == 200)
                    progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('admin.indexing.reindexing.was.successful', '<strong>', '</strong>', '<strong>'+ jQuery.parseJSON(jqXHR.responseText).took + '</strong>') + '</label>'));
                else{
                    showError(jqXHR, textStatus)
                }
            }
            progressBarContainer.empty();
            progressBarContainer.prev('span').removeClass("hidden");
            selectedReindexProjectList = [];
            reindexProjectList = [];
            AJS.$("#zephyr-reindex-projects-view").delay(15000).fadeIn(1000);
        };

		this.cleanupSprints=function(){
			AJS.$("#zephyr-cleanupSprints").attr("disabled", "disabled");
			var progressBarContainer = AJS.$("#cleanupSprintsProgressBar")
			jQuery.ajax({
				url: contextPath + "/rest/zephyr/latest/cycle/cleanupSprints",
				type : "post",
				complete : function(jqXHR, textStatus){
					AJS.$("#zephyr-cleanupSprints").next('span').addClass("hidden");
					if(jqXHR.status == 200){
						progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.configuration.sprintCleanup.was.successful', '<strong>', '</strong>', '<strong>'+ jQuery.parseJSON(jqXHR.responseText).took + '</strong>') + '</label>'));
					}
					if(textStatus === 'timeout'){
						progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.configuration.sprintCleanup.inProgress') + '</label>'));
					}
					if(jqXHR.status == 401){
						window.location=contextPath + "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
					}else if(jqXHR.status > 400){
						showError(jqXHR, textStatus)
					}
					AJS.$("#zephyr-cleanupSprints").removeAttr("disabled");
				}
			});
			progressBarContainer.empty();
			progressBarContainer.prev('span').removeClass("hidden");
		};


        this.cleanupCycleCache=function(){
            AJS.$("#zephyr-cleanupCycleCache").attr("disabled", "disabled");
            var progressBarContainer = AJS.$("#cleanupCycleCacheProgressBar")
            jQuery.ajax({
                url: contextPath + "/rest/zephyr/latest/cycle/cleanupCycleCache",
                type : "post",
                complete : function(jqXHR, textStatus){
                    AJS.$("#zephyr-cleanupSprints").next('span').addClass("hidden");
                    if(jqXHR.status == 200){
                        progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.configuration.sprintCleanup.was.successful', '<strong>', '</strong>', '<strong>'+ jQuery.parseJSON(jqXHR.responseText).took + '</strong>') + '</label>'));
                    }
                    if(textStatus === 'timeout'){
                        progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.configuration.sprintCleanup.inProgress') + '</label>'));
                    }
                    if(jqXHR.status == 401){
                        window.location=contextPath + "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
                    }else if(jqXHR.status > 400){
                        showError(jqXHR, textStatus)
                    }
                    AJS.$("#zephyr-cleanupCycleCache").removeAttr("disabled");
                }
            });
            progressBarContainer.empty();
            progressBarContainer.prev('span').removeClass("hidden");
        };


		this.updateWorkflowSettings=function(){
			var value = AJS.$("#zephyr-show-workflow").is(":checked");

			jQuery.ajax({
				url: contextPath + "/secure/ZSetWorkflowSettings.jspa?decorator=none&showWorkflow="+value,
				type : "post",
				complete : showError
			});
		};

		this.refreshRemoteIssueLinks = function () {
			AJS.$("#zephyr-ril-refresh").attr("disabled", "disabled");
			var issueLinkTypeId = AJS.$("#issue-link-relation-select option:selected").val();
			window.issueLinkTypeId=issueLinkTypeId;
			if(!issueLinkTypeId)
				issueLinkTypeId = -1;
			var progressBarContainer = AJS.$("#relinkProgressBar");
			progressBarContainer.find('label').remove();
			jQuery.ajax({
				url: contextPath + "/rest/zephyr/latest/execution/refreshRemoteLinks?issueLinkTypeId=" + issueLinkTypeId,
				type: "post",
				complete: function (jqXHR, textStatus) {
					if (jqXHR.status == 401) {
						window.location = contextPath + "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
					}
					if (jqXHR.status == 200) {
						var token = jQuery.parseJSON(jqXHR.responseText).token;
						var intervalId = setInterval(function () {
							jQuery.ajax({
								url: contextPath + "/rest/zephyr/latest/execution/refreshLinksStatus/" + token,
								complete: function (jqXHR, textStatus) {
									AJS.log("Querying...")
									var status = jQuery.parseJSON(jqXHR.responseText).status;
									if (status == 'completed') {
										clearInterval(intervalId);
										concludeRefreshLinksOperation(jqXHR, textStatus);
									}
								}
							})
						}, 2000)
                    } else if (jqXHR.status == 204) {
						progressBarContainer.find('span').addClass("hidden");
						progressBarContainer.append(AJS.$('<label>' + AJS.I18n.getText('zephyr.je.admin.issue.relinking.was.successful', '<strong>', '</strong>', '<strong>' + 0 + ' seconds</strong>') + '</label>'));
						AJS.$("#zephyr-ril-refresh").removeAttr("disabled");
					} else if (jqXHR.status == 403) {
						progressBarContainer.find('span').addClass("hidden");
						showError(jqXHR, textStatus)
					} else {
                        progressBarContainer.find('span').addClass("hidden");
                        showError(jqXHR, textStatus)
                    }
				}
			});

			var concludeRefreshLinksOperation = function (jqXHR, textStatus) {
				AJS.$("#zephyr-ril-refresh").removeAttr("disabled");
				progressBarContainer.find('span').addClass("hidden");
				if (jqXHR.status == 200) {
					progressBarContainer.append(AJS.$('<label>' + AJS.I18n.getText('zephyr.je.admin.issue.relinking.was.successful', '<strong>', '</strong>', '<strong>' + jQuery.parseJSON(jqXHR.responseText).took + '</strong>') + '</label>'));
				}
				else {
					showError(jqXHR, textStatus)
				}
			}
			progressBarContainer.find('span').removeClass("hidden");
		};

        /*
        * commented as part of ZFJ-2445
        this.resetZephyrTestIssueLinks = function () {
            AJS.$("#zephyr-test-reset").attr("disabled", "disabled");

            var issueTypeMap = {};
            AJS.$.each(AJS.$('#issue-type-select option:selected'), function() {
                var key = ''+AJS.$(this).attr('id');
                var value = AJS.$(this).attr('value');
                issueTypeMap[key] = value;
            });

            if(AJS.$.isEmptyObject(issueTypeMap)) {
                alert("Please select atleast one value for the non issue type test.");
                AJS.$("#zephyr-test-reset").removeAttr("disabled");
                return;
            }

            var issueLinkTypeId = AJS.$("#test-link-req-relation-select option:selected").val();
            window.issueLinkTypeId=issueLinkTypeId;
            if(!issueLinkTypeId)
                issueLinkTypeId = -1;
            var progressBarContainer = AJS.$("#linkReqProgressBar");
            progressBarContainer.find('label').remove();
            jQuery.ajax({
                url: contextPath + "/rest/zephyr/latest/test/resetIssueLink?issueLinkTypeId=" + issueLinkTypeId,
                type: "post",
                contentType :"application/json",
                data: JSON.stringify(issueTypeMap),
                complete: function (jqXHR, textStatus) {
                    if (jqXHR.status == 401) {
                        window.location = contextPath + "secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination=secure/admin/ZephyrGeneralConfiguration!default.jspa"
                    }
                    if (jqXHR.status == 200) {
                        var token = jQuery.parseJSON(jqXHR.responseText).token;
                        var intervalId = setInterval(function () {
                            jQuery.ajax({
                                url: contextPath + "/rest/zephyr/latest/test/resetIssueLinkStatus/" + token,
                                complete: function (jqXHR, textStatus) {
                                    var status = jQuery.parseJSON(jqXHR.responseText).status;
                                    if (status == 'completed') {
                                        clearInterval(intervalId);
                                        concludeRefreshLinksOperation(jqXHR, textStatus);
                                    }
                                }
                            })
                        }, 2000)
                    } else if (jqXHR.status == 204) {
                        progressBarContainer.find('span').addClass("hidden");
                        progressBarContainer.append(AJS.$('<div style="float: left"><label>' + AJS.I18n.getText('zephyr.je.admin.zephyr.test.reset.was.successful', '<strong>', '</strong>', '<strong>' + 0 + ' seconds</strong>') + '</label></div>'));
                        AJS.$("#zephyr-test-reset").removeAttr("disabled");
                    } else if (jqXHR.status == 403) {
                        progressBarContainer.find('span').addClass("hidden");
                        showError(jqXHR, textStatus)
                    } else {
                        progressBarContainer.find('span').addClass("hidden");
                        showError(jqXHR, textStatus)
                    }
                }
            });

            var concludeRefreshLinksOperation = function (jqXHR, textStatus) {
                AJS.$("#zephyr-test-reset").removeAttr("disabled");
                progressBarContainer.find('span').addClass("hidden");
                if (jqXHR.status == 200) {
                    var status = jQuery.parseJSON(jqXHR.responseText).status;
                    if (status == 'completed') {
                        progressBarContainer.append(AJS.$('<div style="float: left;"><label>' + AJS.I18n.getText('zephyr.je.admin.zephyr.test.reset.was.successful', '<strong>', '</strong>', '<strong>' + jQuery.parseJSON(jqXHR.responseText).took  + '</strong>') + '</label></div>'));
                    }

                }
                else {
                    showError(jqXHR, textStatus)
                }
            }
            progressBarContainer.find('span').removeClass("hidden");
        };*/

        this.refreshTestRequirementIssueLinks = function () {

            var testReqLinkType = AJS.$("#test-link-req-relation-select option:selected").val();
            var oldTestReqLinkType = AJS.$('#oldTestReqLinkType').val().split('/')[0];
            if(!testReqLinkType)
                testReqLinkType = -1;
            jQuery.ajax({
                url: contextPath + "/secure/ZSetTestRequirementLinkType.jspa?decorator=none&testReqLinkType="+testReqLinkType+"&oldTestReqLinkType="+oldTestReqLinkType,
                type : "post",
                success : function() {
                    AJS.$('#oldTestReqLinkType').val(testReqLinkType);
                },
                complete : showError
            });
        };

		var showError = function(jqXHR, textStatus){
			if(jqXHR.responseText != ""){
				var cxt = AJS.$("#general-config-aui-message-bar");
				cxt.empty();

				var errorDesc = jqXHR.responseText;
				var responseJson = jQuery.parseJSON(jqXHR.responseText);
				if(responseJson && responseJson.errorDesc)
					errorDesc = responseJson.errorDesc;

				AJS.messages.error(cxt, {
					title: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.settings.error"),
				    body: errorDesc,
				    closeable: true
				});
			}
		};

        var showAnalyticsError = function(jqXHR, textStatus, value){
            if(jqXHR.responseText != ""){
                var cxt = AJS.$("#zephyr-aui-aui-message-bar"),msg=cxt.attr('gen-err');
                cxt.empty();
                try {
                    if (jqXHR.status==200 && jqXHR.statusText == 'OK' && value == 'true') {
                        msg = cxt.attr('enabled-msg');
                    }else{
                        msg = cxt.attr('disabled-msg');
                    }
                    AJS.messages.success(cxt, {
                        title: cxt.attr('succ-title').capitalize(),
                        body: msg,
                        closeable: true
                    });
                }catch (error){
                    //error
                    AJS.messages.error(cxt, {
                        title: cxt.attr('err-title'),
                        body: msg,
                        closeable: true
                    });
                }
            }
        };


        this.saveNonIssueTypeTestPreference = function () {
            AJS.$("#non-issue-type-test-save").attr("disabled", "disabled");

            var nonIssueTypeTestList = '';
            AJS.$.each(AJS.$('#non-issue-type-test-select option:selected'), function() {
                nonIssueTypeTestList += ''+AJS.$(this).attr('id')+',';
            });
            nonIssueTypeTestList = nonIssueTypeTestList.replace(/,\s*$/, "");
            var progressBarContainer = AJS.$("#nonIssueTypeTestProgress");
            progressBarContainer.find('label').remove();

            jQuery.ajax({
                url: contextPath + "/secure/ZSaveNonIssueTypeTestList.jspa?decorator=none&nonIssueTypeTestList="+nonIssueTypeTestList,
                type : "post",
                success : function() {
                    AJS.$("#non-issue-type-test-save").removeAttr("disabled");
                    progressBarContainer.find('span').addClass("hidden");
                    AJS.$("#zPreferenceSaveMessageId").text(AJS.$('<strong>' + AJS.I18n.getText('zephyr.je.admin.plugin.show.zephyr.test.issue.type.save.successful') + '</strong>'));
                    AJS.$("#zPreferenceSaveMessageId").html(AJS.$('<span><strong>' + AJS.I18n.getText('zephyr.je.admin.plugin.show.zephyr.test.issue.type.save.successful') + '</strong></span>'));
                    AJS.$("#zPreferenceSaveMessageId").removeClass();
                },
                complete : showError
            });
            progressBarContainer.find('span').removeClass("hidden");
        };

        this.disableSyncButton = function() {
            if(AJS.$("#hardIndexId")[0].checked
            		&& (AJS.$('input[name="allProjects"]:checked').length > 0 || AJS.$('#project-view-reindex-detail input[type="checkbox"]:checked').length > 0)) {
            	if(!AJS.$("#zephyr-sync-all").is(":disabled")) {            		
            		AJS.$("#zephyr-sync-all").attr("disabled", "disabled");
            	}                
            } else if(!AJS.$("#hardIndexId")[0].checked
            		&& (AJS.$('input[name="allProjects"]:checked').length > 0 || AJS.$('#project-view-reindex-detail input[type="checkbox"]:checked').length > 0)) {
                AJS.$("#zephyr-sync-all").removeAttr("disabled");
            }
        }

		this.toggleLicense = function(){
			AJS.$('#zLic').toggleClass("hidden")
		};

        this.toggleReindexProjectList = function(){
        	if(AJS.$('input[name="allProjects"]:checked').length > 0) {
                AJS.$("#zephyr-reindex-project-field").attr("disabled", true);
                AJS.$("#zephyr-reindex-projects-view").empty();
                selectedReindexProjectList = [];
                reindexProjectList = [];
                AJS.$("#zephyr-index-all").removeAttr("disabled");
                Zephyr.Admin.Config.disableSyncButton();
            }else {
                AJS.$("#zephyr-reindex-project-field").attr("disabled", false);
                AJS.$("#zephyr-index-all").attr("disabled", true);
                AJS.$("#zephyr-sync-all").attr("disabled", true);
            }
        };

        this.enableDisableReindexButton = function(){
            if(AJS.$('#project-view-reindex-detail input[type="checkbox"]:checked').length > 0 || AJS.$('input[name="allProjects"]:checked').length > 0) {
                AJS.$("#zephyr-index-all").removeAttr("disabled");
                Zephyr.Admin.Config.disableSyncButton();
            }else {
                AJS.$("#zephyr-index-all").attr("disabled", "disabled");
                AJS.$("#zephyr-sync-all").attr("disabled", "disabled");
            }
        };

		var offset = 0;
		var limit = 20;
		var lastResultSize = 0;
		this.navigateChangeHistory = function(e){
			if(e.currentTarget.name == "prev"){
				if(offset > limit)
					offset -= limit;
				else
					offset = 0;
			}else if(e.currentTarget.name == "next"){
				if(lastResultSize < limit)
					return;
				offset += lastResultSize;
			} else if(e.currentTarget.name == "refresh"){
				offset = 0;
			}
			e.preventDefault();
			Zephyr.Admin.Config.fetchChangeHistory();
		}
        this.htmlDecode = function(value) {
            return AJS.$('<div/>').html(value).text();
        }

		this.fetchChangeHistory = function(){
			AJS.log("Fetching History")
			AJS.$(".overlay-icon").removeClass('hidden');

			var data = {maxRecords:limit, offset:offset}, eventType = AJS.$("#eventType").val(), entityType = AJS.$("#entityType").val()
			if(entityType) data.entityType = entityType;
			if(eventType) data.event = eventType;
			jQuery.ajax({
				url: contextPath + "/rest/zephyr/latest/audit",
				data:data,
				success:function(data, textStatus, jqXHR) {
					var items = data.auditLogs;
					var tbody ="";
					// Since IE browser does not support const keyword changed the keyword from const to var
					var NONE = "N/A";
					for(var i=0; i< items.length; i++){
						var issueTxt = NONE;
                        var authorText = NONE;
						if(items[i].issueKey){
							var maskedKey = items[i].issueKey.substr(items[i].issueKey.length - 5);
							if (maskedKey !=undefined && maskedKey == 'XXXXX'){
								issueTxt = "<span>" + items[i].issueKey + "</span>";
							}else {
								issueTxt = "<a href='" + contextPath + "/browse/" + items[i].issueKey + "'>" + items[i].issueKey + "</a>";
							}
						}
                        if (items[i].creatorExists) {
                            authorText = "<a href='"+ contextPath + "/secure/ViewProfile.jspa?name=" + items[i].creatorKey + "' id='audithistoryauthor' rel='" + items[i].creatorKey + "' class='user-hover'>" ;
                            if(items[i].avatarUrl)
                                authorText += '<span class="aui-avatar aui-avatar-xsmall"> <span class="aui-avatar-inner"> <img src="'+items[i].avatarUrl+'"></span> </span>&nbsp;';

                            authorText += items[i].creator;
                            if (!items[i].creatorActive)
                                authorText = authorText + '&nbsp;(Inactive)';

                            authorText = authorText + '</a>';
                        } else {
                            authorText = '<span class="user-hover user-avatar" rel="' + items[i].creatorKey + '">';
                            if(items[i].avatarUrl)
                                authorText += '<span class="aui-avatar aui-avatar-xsmall"> <span class="aui-avatar-inner"> <img src="'+items[i].avatarUrl+'"></span> </span>&nbsp;';
                            authorText += items[i].creator + '</span>';
                        }

						var issueWithAnchorOld = "";
						var issueWithAnchorNew = "";
						if(items[i].auditItems.field=='execution_defect'
							|| items[i].auditItems.field=='step_defect') {
							var arr1 = items[i].auditItems.oldValue.split(",");
							arr1.forEach(function (el, idx) {
								if (el=='XXXXX'){
									issueWithAnchorOld += "<span>" + el + "</span> ";
								}else {
									issueWithAnchorOld += "<a href='" + contextPath + "/browse/" + el + "' >" + el + "</a> ";
								}
								if (arr1.length > 1 && idx < (arr1.length-1)){
									issueWithAnchorOld += ", ";
								}
							})
							var arr2 = items[i].auditItems.newValue.split(",");
							arr2.forEach(function (el, idx) {
								if (el=='XXXXX'){
									issueWithAnchorNew += "<span>" + el + "</span> ";
								}else {
									issueWithAnchorNew += "<a href='" + contextPath + "/browse/" + el + "' >" + el + "</a> ";
								}
								if (arr2.length > 1 && idx < (arr2.length-1)){
									issueWithAnchorNew += ", ";
								}
							})
						} else if(items[i].auditItems.field == 'executed_on') {
							var date, h, m, time, dateStr;
							if(items[i].auditItems.oldValue) {
								date = new Date(Number(items[i].auditItems.oldValue));
								h =  date.getHours(), m = date.getMinutes();
								if(h === 0 ) {
									time = '12' + ':' + m +' AM';
								} else {
									time = (h > 12) ? (h-12 + ':' + m +' PM') : (h + ':' + m +' AM');
								}
								dateStr = date.getDate() + '/' + (date.getMonth()+1) + '/' + date.getFullYear() + ' ' + time;
								issueWithAnchorOld = (dateStr || NONE);
							} else {
								issueWithAnchorOld = NONE;
							}
							if(items[i].auditItems.newValue) {
								date = new Date(Number(items[i].auditItems.newValue));
								h =  date.getHours(), m = date.getMinutes();
								if(h === 0 ) {
									time = '12' + ':' + m +' AM';
								} else {
									time = (h > 12) ? (h-12 + ':' + m +' PM') : (h + ':' + m +' AM');
								}
								dateStr = date.getDate() + '/' + (date.getMonth()+1) + '/' + date.getFullYear() + ' ' + time;
								issueWithAnchorNew = (dateStr || NONE);
							} else {
								issueWithAnchorNew = NONE;
							}

						} else{
							issueWithAnchorOld = (items[i].auditItems.oldValue || NONE);
							issueWithAnchorNew = (items[i].auditItems.newValue || NONE);
						}
						tbody += 	"<tr><td>" + (offset + i+1) +
						 			"</td><td>" + issueTxt +
									"</td><td>"+ (items[i].entityType || NONE) +
									"</td><td>"+ (items[i].entityEvent || NONE) +
									"</td><td>"+ (items[i].auditItems.field || NONE) +
									"</td><td style=\"word-wrap:break-word\">"+ Zephyr.Admin.Config.htmlDecode(issueWithAnchorOld || NONE) +
									"</td><td style=\"word-wrap:break-word\">"+ Zephyr.Admin.Config.htmlDecode(issueWithAnchorNew || NONE) +
									"</td><td>"+ (items[i].creationDate || NONE) +
									"</td><td>"+ authorText  + "</td></tr>"
					}
					lastResultSize = items.length;
					AJS.$("#changeHistoryTable tbody").empty();
					AJS.$("#changeHistoryTable tbody").append(tbody);
					AJS.$("#changeHistoryDetails").removeClass('hidden')
					AJS.$(".overlay-icon").addClass('hidden');
				},
				error : function(jqXHR, textStatus){
					if(jqXHR.status == 401) {
						var dialog = new AJS.Dialog({
							width:800,
							height:270,
							id:	"dialog-error"
						});
						dialog.addHeader(AJS.I18n.getText('zephyr.common.forbidden.error.label'));

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
					AJS.$("#changeHistoryDetails").removeClass('hidden');
					AJS.$(".overlay-icon").addClass('hidden');
				}
			});
		}

		this.initDashboard = function(){
			var dashboardDD = new AJS.SingleSelect({
				element: AJS.$("#zephyr-metrics-dashboard"),
				itemAttrDisplayed: "label",
				maxInlineResultsDisplayed: 15,
				maxWidth:200,
				showDropdownButton: true,
				submitInputVal: true,
				overlabel: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.testmetricsmenu.label"),
				errorMessage: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.testmetricsmenu.no.matching.dashboard"),
				ajaxOptions: {
	        	   url:contextPath + "/rest/zephyr/latest/util/dashboard",
	        	   query: true,
	        	   minQueryLength: 2,
	        	   formatResponse: function (response) {
	        		   var ret = [];
	        		   AJS.$(response).each(function() {
	    	                var groupDescriptor = new AJS.GroupDescriptor();
	    	                groupDescriptor.addItem(new AJS.ItemDescriptor({
								value: this.id.toString(), // value of
								label: this.name, // title
								title:this.name,
								html: this.name,
								highlighted: true
	    	                }));
	    	                ret.push(groupDescriptor);
	    	            });
	    	            return ret;
	        	   }
	           }
	        });
			//AJS.$('#zephyr-metrics-dashboard').bind('change', function(){
			AJS.$(dashboardDD.model.$element).on('selected', function(event, descriptor){
				jQuery.ajax({
					url: contextPath + "/secure/ZUpdateTestMetricsMenu.jspa?decorator=none&dashboardId="+descriptor.properties.value,
					type : "post",
					success: function(){
						AJS.$("#selectedDashboard").text(descriptor.properties.title);
						dashboardDD.clear();
					},
					error : showError
				});
			});
		}

		//Function to get all the disabled projectIds on load.
		this.getDisabledProjectIds = function(){
			jQuery.ajax({
				url: contextPath + "/secure/ZUpdateDisableTestMenu.jspa",
				type : "post",
				async: false,
				success: function(response, status, jqXHR){
					selectedProjectIds = response;
				},
				error : showError
			});
		}
		//Function to get all the details of the projects which are disabled test menu.
		this.populateDisabledProjects = function(){
			jQuery.ajax({
				url: contextPath + "/rest/api/2/project/",
				type : "get",
				async : false,
				success: function(response, status, jqXHR){
					var ret = [];
					var allValues = [];
					AJS.$(response).each(
							function() {
	     				    	var itemDescriptor= new AJS.ItemDescriptor({
	     				    		value: this.id.toString(), // value of
									label: this.name, // title
									title: this.projectTypeKey,
									key: this.key,
									highlighted: true
 		    	                 });
		     				    if(_.contains(selectedProjectIds, this.id.toString())){
	 		    	                 ret.push(itemDescriptor);
		     				    }
	 		    	            allValues.push(itemDescriptor);
							}
						);
					allProjects = allValues;
					if(ret && ret.length > 0){
						disabledProjectList = ret;
					}
					return;
				},
			    error : showError
			});
		}
		this.displayProjectsOnScreen = function(){
			Zephyr.Admin.Config.displayProjectsCreation(currentPage);
			var html;
			if(paginatedProjectList.length < 1){
				html = AJS.I18n.getText("zephyr.admin.menu.globalsettings.config.projects.delete.none");
				document.getElementById("totalDPPages").innerHTML = "";
				var projectFilter = AJS.$("#test-disabled-project-filter").val().trim().toLowerCase();
				if(projectFilter == ''){
					AJS.$('#removeSearchInput').removeClass('aui-iconfont-remove').addClass('aui-iconfont-search-small')
				} else {
					AJS.$('#removeSearchInput').removeClass('aui-iconfont-search-small').addClass('aui-iconfont-remove');
				}
			} else {
				var newProjectList = [];
				var projectFilter = AJS.$("#test-disabled-project-filter").val().trim().toLowerCase();
				if(projectFilter == ''){
					newProjectList = paginatedProjectList;
					Zephyr.Admin.Config.displayProjectsCreation(currentPage);
					AJS.$('#removeSearchInput').removeClass('aui-iconfont-remove').addClass('aui-iconfont-search-small')
				} else {
					AJS.$('#removeSearchInput').removeClass('aui-iconfont-search-small').addClass('aui-iconfont-remove');
					document.getElementById("buttonDPPrev").style.visibility = 'hidden';
		    		document.getElementById("buttonDPNext").style.visibility = 'hidden';
				    document.getElementById("totalDPPages").style.visibility = 'hidden';
					for (var j = 0; j < disabledProjectList.length; j++){
						if(disabledProjectList[j].properties.label.toLowerCase().indexOf(projectFilter)>-1){
							newProjectList.push(disabledProjectList[j]);
						}
					}

				}
				html = ZEPHYR.Templates.ZephyrProjects.listSelectedProjects({disabledProjectList:newProjectList});
			}
			AJS.$("#test-disabled-projects-view").empty();
			AJS.$("#test-disabled-projects-view").append(html);
			Zephyr.Admin.Config.attachDeleteEvent();
		}
		this.prevDPPage = function(){
		    if (currentPage > 1) {
		        currentPage--;
		        Zephyr.Admin.Config.displayProjectsOnScreen();
		    }
		}
		this.nextDPPage = function(){
		    if (currentPage < Zephyr.Admin.Config.numOfPages()) {
		        currentPage++;
		        Zephyr.Admin.Config.displayProjectsOnScreen();
		    }
		}
		this.numOfPages = function(){
			var pages = Math.ceil(disabledProjectList.length / recordsPerPage);
			if(pages == 0 ) pages = 1;
		    return pages;
		}
		this.displayProjectsCreation = function(pageNo){
		    var buttonPrev = document.getElementById("buttonDPPrev");
		    var buttonNext = document.getElementById("buttonDPNext");
		    var pageSpan = document.getElementById("totalDPPages");

		    // Validate page
		    if (pageNo <= 1) pageNo = 1;
		    else if (pageNo > Zephyr.Admin.Config.numOfPages()) pageNo = Zephyr.Admin.Config.numOfPages();
		    paginatedProjectList = [];

		    for (var i = (pageNo-1) * recordsPerPage; i < (pageNo * recordsPerPage) && i < disabledProjectList.length; i++) {
		        paginatedProjectList.push(disabledProjectList[i]);
		    }
		    if(paginatedProjectList.length > 0){
		    	pageSpan.innerHTML = "Page: " + pageNo + "/" + Zephyr.Admin.Config.numOfPages();
		    	pageSpan.style.visibility = "visible";
		    }
		    if (pageNo == 1) {
		        buttonPrev.style.visibility = "hidden";
		    } else {
		    	buttonPrev.style.visibility = "visible";
		    }

		    if (pageNo == Zephyr.Admin.Config.numOfPages()) {
		    	buttonNext.style.visibility = "hidden";
		    } else {
		    	buttonNext.style.visibility = "visible";
		    }

		}
		this.attachDeleteEvent = function() {
			//event delegation jquery
			AJS.$('#project-view-test-disable-detail').on('click', '.delete-project', function(event){
				var clickedButton = event.target;
				var projectId = clickedButton.parentElement.parentElement.dataset.projectid;
				var projectName;
				for (var j = 0; j < disabledProjectList.length; j++){
					if(disabledProjectList[j].properties.value.indexOf(projectId) > -1){
						projectName = disabledProjectList[j].properties.label;
						break;
					}
				}
				var instance = this,
			        dialog = new JIRA.FormDialog({
			            //id: "cycle-" + cycleId + "-delete-dialog",
			            content: function (callback) {
			            	/*Short cut of creating view, move it to Backbone View and do it in render() */
			            	var innerHtmlStr = ZEPHYR.Templates.ZephyrProjects.deleteTestDisableProjectConfirmationDialog();
			                callback(innerHtmlStr);
			            },

			            submitHandler: function (e) {
			            	jQuery.ajax({
								url: contextPath + "/secure/ZRemoveDisableTestMenu.jspa?projectId="+projectId,
								type : "post",
								async : false,
								success: function(response){
									AJS.$("#deletedProjectSpan").text(AJS.I18n.getText("zephyr.admin.menu.globalsettings.config.projects.delete.confirm") + projectName).css({'color': '#008000'}).show().delay(5000).hide(1000);
									selectedProjectIds = response;
									dialog.hide();
									Zephyr.Admin.Config.deleteProjectFromDisplayList(projectId);
									Zephyr.Admin.Config.displayProjectsOnScreen();
								},
								error : showError
							});
			            	e.preventDefault();
			            }
			        });
			        dialog.show();
			});
		}
		this.deleteProjectFromDisplayList = function(projectId){
			var tempList = disabledProjectList.filter(function(obj){
				return obj.properties.value != projectId;
			});
			disabledProjectList = tempList;
		}
		this.initProjectConfig = function(){
			Zephyr.Admin.Config.getDisabledProjectIds();
			Zephyr.Admin.Config.populateDisabledProjects();
			recordsPerPage = AJS.I18n.getText("zephyr.admin.menu.globalsettings.config.projects.delete.pageSize");
		    AJS.$('#buttonDPPrev').on('click', function() {
		    	Zephyr.Admin.Config.prevDPPage();
		    });
		    AJS.$('#buttonDPNext').on('click', function() {
		    	Zephyr.Admin.Config.nextDPPage();
		    });
			var projectDD = new AJS.SingleSelect({
				element: AJS.$("#zephyr-metrics-project"),
				itemAttrDisplayed: "label",
				maxInlineResultsDisplayed: 15,
				maxWidth:200,
				showDropdownButton: true,
				submitInputVal: true,
				overlabel: AJS.I18n.getText("zephyr-je.testboard.select.project.label"),
				errorMessage: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.disable.testmenu.project.nomatching"),
				ajaxOptions: {
	        	   url: contextPath + "/rest/api/2/project/" ,
	        	   query: true,
	        	   minQueryLength: 2,
	        	   formatResponse: function (response) {
	        		   var ret = [];
	        		   var allValues = [];
	        		   var query = AJS.$("#zephyr-metrics-project").val();
	        		   AJS.$(response).each(function() {
	        			   var itemDescriptor = new AJS.ItemDescriptor({
										 value: this.id.toString(), // value of
										 label: this.name, // title
										 title: this.projectTypeKey,
										 key: this.key,
										 highlighted: true
									 });

        				   if(!(_.contains(selectedProjectIds, this.id.toString()))){
        					   if((query && this.name.toLowerCase().indexOf(query.toString().toLowerCase()) > -1)
        							   || (!query)){
 		    	                 ret.push(itemDescriptor);
        					   }
        				   }
	        			   allValues.push(itemDescriptor);
	    	            });
	        		   allProjects = allValues;
	    	           return ret;
	        	   }
	           }
	        });
			// On select of a project, add the projectId to the configuration.
			AJS.$(projectDD.model.$element).on('selected', function(event, descriptor){
				jQuery.ajax({
					url: contextPath + "/secure/ZUpdateDisableTestMenu.jspa?projectId="+descriptor.properties.value,
					type : "post",
					dataType: "json",
					aysnc : false,
					success: function(response){
						AJS.$("#selectedProjectSpan").text(AJS.I18n.getText("zephyr.admin.menu.globalsettings.config.projects.addition.confirm")+ descriptor.properties.label).css({'color': '#008000'}).show().delay(5000).hide(1000);;
						projectDD.clear();
						selectedProjectIds = response;
						Zephyr.Admin.Config.addProjectToDisplayList(descriptor.properties.value);
						Zephyr.Admin.Config.displayProjectsOnScreen();
					},
					error : showError
				});
			});
			Zephyr.Admin.Config.displayProjectsOnScreen();
		}
		this.addProjectToDisplayList = function(projectId){
			var result = allProjects.filter(function(obj){
				return obj.properties.value == projectId;
			});
			disabledProjectList.push(result[0]);
		}

		// Project list UI for reindex
		this.initProjectConfigForReindex = function () {
            var projectList = new AJS.SingleSelect({
                element: AJS.$("#zephyr-reindex-project"),
                itemAttrDisplayed: "label",
                maxInlineResultsDisplayed: 15,
                maxWidth:200,
                showDropdownButton: true,
                submitInputVal: true,
                overlabel: AJS.I18n.getText("zephyr-je.testboard.select.project.label"),
                errorMessage: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.disable.testmenu.project.nomatching"),
                ajaxOptions: {
                    url: contextPath + "/rest/api/2/project/" ,
                    query: true,
                    minQueryLength: 2,
                    formatResponse: function (response) {
                        var ret = [];
                        var projects = [];
                        var query = AJS.$("#zephyr-reindex-project").val();
                        AJS.$(response).each(function() {
                            var itemDescriptor = new AJS.ItemDescriptor({
                                value: this.id.toString(),
                                key: this.key,
                                label: this.name,
                                title: this.projectTypeKey,
                                highlighted: true
                            });

                            //add logic to handle already added project
                            if(!(_.contains(selectedReindexProjectList, this.id.toString()))){
                                if((query && this.name.toLowerCase().indexOf(query.toString().toLowerCase()) > -1)
                                    || (!query)){
                                    ret.push(itemDescriptor);
                                }
                            }
                            projects.push(itemDescriptor);
                        });
                        reindexProjects = projects;
                        return ret;
                    }
                }
            });
            // On select of a project, add the projectId to the configuration.
            AJS.$(projectList.model.$element).on('selected', function(event, item){
                var projectId = item.properties.value;
                var result = reindexProjects.filter(function(obj){
                    return obj.properties.value == projectId;
                });
                selectedReindexProjectList.push(result[0].properties.value);
                reindexProjectList.push(result[0]);
                selectedReindexProjects[projectId] = 'true';
                Zephyr.Admin.Config.displaySelectedReindexProjects();
                projectList.clear();
                if(Object.keys(selectedReindexProjects).length > 0 ) {
                    AJS.$("#zephyr-index-all").removeAttr("disabled");
                    Zephyr.Admin.Config.disableSyncButton();
                }
            });
            Zephyr.Admin.Config.displaySelectedReindexProjects();
        }

        this.displaySelectedReindexProjects = function(){
            var html = ZEPHYR.Templates.ZephyrProjects.listReindexSelectedProjects({reindexProjectList:reindexProjectList});

            AJS.$("#zephyr-reindex-projects-view").empty();
            AJS.$("#zephyr-reindex-projects-view").append(html);
        }

        //Start: Disabling Execution Workflow for projects
		//Function to get all the execution workflow disabled projectIds on load.
		this.getDisabledEWProjectIds = function(){
			jQuery.ajax({
				url: contextPath + "/secure/ZUpdateDisableExecWorkflow.jspa",
				type : "post",
				async: false,
				success: function(response, status, jqXHR){
					selectedEWProjectIds = response;
				},
				error : showError
			});
		}
		//Function to get all the details of the projects which are disabled execution workflow
		this.populateDisabledEWProjects = function(){
			jQuery.ajax({
				url: contextPath + "/rest/api/2/project/",
				type : "get",
				async : false,
				success: function(response, status, jqXHR){
					var ret = [];
					var allValues = [];
					AJS.$(response).each(
							function() {
	     				    	var itemDescriptor= new AJS.ItemDescriptor({
	     				    		value: this.id.toString(), // value of
									label: this.name, // title
									title: this.projectTypeKey,
									key: this.key,
									highlighted: true
 		    	                 });
		     				    if(_.contains(selectedEWProjectIds, this.id.toString())){
	 		    	                 ret.push(itemDescriptor);
		     				    }
	 		    	            allValues.push(itemDescriptor);
							}
						);
					allProjects = allValues;
					if(ret && ret.length > 0){
						disabledEWProjectList = ret;
					}
					return;
				},
			    error : showError
			});
		}
		this.displayEWProjectsOnScreen = function(){
			Zephyr.Admin.Config.displayEWProjectsCreation(currentPage);
			var html;
			if(paginatedEWProjectList.length < 1){
				html = AJS.I18n.getText("zephyr.admin.menu.globalsettings.config.projects.delete.none");
				document.getElementById("totalEWDPPages").innerHTML = "";
				var projectFilter = AJS.$("#ew-disabled-project-filter").val().trim().toLowerCase();
				if(projectFilter == ''){
					AJS.$('#removeEWSearchInput').removeClass('aui-iconfont-remove').addClass('aui-iconfont-search-small')
				} else {
					AJS.$('#removeEWSearchInput').removeClass('aui-iconfont-search-small').addClass('aui-iconfont-remove');
				}
			} else {
				var newProjectList = [];
				var projectFilter = AJS.$("#ew-disabled-project-filter").val().trim().toLowerCase();
				if(projectFilter == ''){
					newProjectList = paginatedEWProjectList;
					Zephyr.Admin.Config.displayEWProjectsCreation(currentPage);
					AJS.$('#removeEWSearchInput').removeClass('aui-iconfont-remove').addClass('aui-iconfont-search-small')
				} else {
					AJS.$('#removeEWSearchInput').removeClass('aui-iconfont-search-small').addClass('aui-iconfont-remove');
					document.getElementById("buttonEWDPPrev").style.visibility = 'hidden';
		    		document.getElementById("buttonEWDPNext").style.visibility = 'hidden';
				    document.getElementById("totalEWDPPages").style.visibility = 'hidden';
					for (var j = 0; j < disabledEWProjectList.length; j++){
						if(disabledEWProjectList[j].properties.label.toLowerCase().indexOf(projectFilter)>-1){
							newProjectList.push(disabledEWProjectList[j]);
						}
					}

				}
				html = ZEPHYR.Templates.ZephyrProjects.listSelectedEWProjects({disabledEWProjectList:newProjectList});
			}
			AJS.$("#ew-disabled-projects-view").empty();
			AJS.$("#ew-disabled-projects-view").append(html);
			Zephyr.Admin.Config.attachEWDeleteEvent();
		}
		this.prevEWDPPage = function(){
		    if (currentPage > 1) {
		        currentPage--;
		        Zephyr.Admin.Config.displayEWProjectsOnScreen();
		    }
		}
		this.nextEWDPPage = function(){
		    if (currentPage < Zephyr.Admin.Config.numOfEWPages()) {
		        currentPage++;
		        Zephyr.Admin.Config.displayEWProjectsOnScreen();
		    }
		}
		this.numOfEWPages = function(){
			var pages = Math.ceil(disabledEWProjectList.length / recordsPerPageEW);
			if(pages == 0 ) pages = 1;
		    return pages;
		}
		this.displayEWProjectsCreation = function(pageNo){
		    var buttonPrev = document.getElementById("buttonEWDPPrev");
		    var buttonNext = document.getElementById("buttonEWDPNext");
		    var pageSpan = document.getElementById("totalEWDPPages");

		    // Validate page
		    if (pageNo <= 1) pageNo = 1;
		    else if (pageNo > Zephyr.Admin.Config.numOfEWPages()) pageNo = Zephyr.Admin.Config.numOfEWPages();
		    paginatedEWProjectList = [];

		    for (var i = (pageNo-1) * recordsPerPageEW; i < (pageNo * recordsPerPageEW) && i < disabledEWProjectList.length; i++) {
		        paginatedEWProjectList.push(disabledEWProjectList[i]);
		    }
		    if(paginatedEWProjectList.length > 0){
		    	pageSpan.innerHTML = "Page: " + pageNo + "/" + Zephyr.Admin.Config.numOfEWPages();
		    	pageSpan.style.visibility = "visible";
		    }
		    if (pageNo == 1) {
		        buttonPrev.style.visibility = "hidden";
		    } else {
		    	buttonPrev.style.visibility = "visible";
		    }

		    if (pageNo == Zephyr.Admin.Config.numOfEWPages()) {
		    	buttonNext.style.visibility = "hidden";
		    } else {
		    	buttonNext.style.visibility = "visible";
		    }

		}
		this.attachEWDeleteEvent = function() {
			//event delegation jquery
			AJS.$('#project-view-ew-disable-detail').on('click', '.delete-project', function(event){
				var clickedButton = event.target;
				var projectId = clickedButton.parentElement.parentElement.dataset.projectid;
				var projectName = clickedButton.parentElement.parentElement.dataset.projectname;
				for (var j = 0; j < disabledProjectList.length; j++){
					if(disabledEWProjectList[j].properties.value.indexOf(projectId) > -1){
						projectName = disabledEWProjectList[j].properties.label;
						break;
					}
				}
				var instance = this,
			        dialog = new JIRA.FormDialog({
			            content: function (callback) {
			            	/*Short cut of creating view, move it to Backbone View and do it in render() */
			            	var innerHtmlStr = ZEPHYR.Templates.ZephyrProjects.deleteExecWorkflowProjectConfirmationDialog();
			                callback(innerHtmlStr);
			            },
			            submitHandler: function (e) {
			            	jQuery.ajax({
								url: contextPath + "/secure/ZRemoveDisableExecWorkflow.jspa?projectId="+projectId,
								type : "post",
								async : false,
								success: function(response){
									AJS.$("#deletedEWProjectSpan").text(AJS.I18n.getText("zephyr.admin.menu.globalsettings.config.projects.disable.execWorkflow.delete.confirm") + projectName).css({'color': '#008000'}).show().delay(5000).hide(1000);
									selectedEWProjectIds = response;
									dialog.hide();
									Zephyr.Admin.Config.deleteEWProjectFromDisplayList(projectId);
									Zephyr.Admin.Config.displayEWProjectsOnScreen();
								},
								error : showError
							});
			            	e.preventDefault();
			            }
			        });
			        dialog.show();
			});
		}
		this.deleteEWProjectFromDisplayList = function(projectId){
			var tempList = disabledEWProjectList.filter(function(obj){
				return obj.properties.value != projectId;
			});
			disabledEWProjectList = tempList;
		}
		this.addEWProjectToDisplayList = function(projectId){
			var result = allProjects.filter(function(obj){
				return obj.properties.value == projectId;
			});
			disabledEWProjectList.push(result[0]);
		}
		this.initEWProjectConfig = function(){
			Zephyr.Admin.Config.getDisabledEWProjectIds();
			Zephyr.Admin.Config.populateDisabledEWProjects();
			recordsPerPageEW = AJS.I18n.getText("zephyr.admin.menu.globalsettings.config.projects.delete.pageSize");
		    AJS.$('#buttonEWDPPrev').on('click', function() {
		    	Zephyr.Admin.Config.prevEWDPPage();
		    });
		    AJS.$('#buttonEWDPNext').on('click', function() {
		    	Zephyr.Admin.Config.nextEWDPPage();
		    });
			var projectEWDD = new AJS.SingleSelect({
				element: AJS.$("#zephyr-metrics-project-ew"),
				itemAttrDisplayed: "label",
				maxInlineResultsDisplayed: 15,
				maxWidth:200,
				showDropdownButton: true,
				submitInputVal: true,
				overlabel: AJS.I18n.getText("zephyr-je.testboard.select.project.label"),
				errorMessage: AJS.I18n.getText("zephyr.je.admin.plugin.test.section.item.zephyr.configuration.enable.execution.workflow.nomatching"),
				ajaxOptions: {
	        	   url: contextPath + "/rest/api/2/project/" ,
	        	   query: true,
	        	   minQueryLength: 2,
	        	   formatResponse: function (response) {
	        		   var ret = [];
	        		   var allValues = [];
	        		   var query = AJS.$("#zephyr-metrics-project-ew").val();
	        		   AJS.$(response).each(function() {
	        			   var itemDescriptor = new AJS.ItemDescriptor({
										 value: this.id.toString(), // value of
										 label: this.name, // title
										 title: this.projectTypeKey,
										 key: this.key,
										 highlighted: true
									 });

        				   if(!(_.contains(selectedEWProjectIds, this.id.toString()))){
        					   if((query && this.name.toLowerCase().indexOf(query.toString().toLowerCase()) > -1)
        							   || (!query)){
 		    	                 ret.push(itemDescriptor);
        					   }
        				   }
	        			   allValues.push(itemDescriptor);
	    	            });
	        		   allProjects = allValues;
	    	           return ret;
	        	   }
	           }
	        });
			// On select of a project, add the projectId to the configuration.
			AJS.$(projectEWDD.model.$element).on('selected', function(event, descriptor){
				jQuery.ajax({
					url: contextPath + "/secure/ZUpdateDisableExecWorkflow.jspa?projectId="+descriptor.properties.value,
					type : "post",
					dataType: "json",
					aysnc : false,
					success: function(response){
						AJS.$("#selectedEWProjectSpan").text(AJS.I18n.getText("zephyr.admin.menu.globalsettings.config.projects.disable.execWorkflow.addition.confirm")+ descriptor.properties.label).css({'color': '#008000'}).show().delay(5000).hide(1000);;
						projectEWDD.clear();
						selectedEWProjectIds = response;
						Zephyr.Admin.Config.addEWProjectToDisplayList(descriptor.properties.value);
						Zephyr.Admin.Config.displayEWProjectsOnScreen();
					},
					error : showError
				});
			});
			Zephyr.Admin.Config.displayEWProjectsOnScreen();
		}
        //End: Disabling Execution Workflow for projects
		/*this.stopJobProgress=function(){
            var jobProgressKey = AJS.$("#jobprogress-input").val();
            if(jobProgressKey == null || jobProgressKey == ''){
            	AJS.$("#jobprogressStatus").text("Job Progress Key is Mandatory").css({'color': '#ff0000'}).show().delay(5000).hide(1000);
            }else{
            	jQuery.ajax({
					url: contextPath + "/rest/zephyr/latest/jobProgress/" + jobProgressKey + "?status=STOP",
					type : "put",
					complete : function(jqXHR, textStatus){
						var errorDesc;
						var jobStoppedBy;
						if (jqXHR.responseText != "") {
							errorDesc = jqXHR.responseText;
							try{
								var responseJson = jQuery.parseJSON(jqXHR.responseText);
								if (responseJson && responseJson.errorDesc)
									errorDesc = responseJson.errorDesc;
								if (responseJson && responseJson.jobStoppedBy){
									jobStoppedBy=responseJson.jobStoppedBy;
								}
							}catch(e){}
						}
						if(jqXHR.status == 200){
							//progressBarContainer.append(AJS.$('<label>' +AJS.I18n.getText('zephyr.je.admin.plugin.test.section.item.zephyr.configuration.sprintCleanup.was.successful', '<strong>', '</strong>', '<strong>'+ jQuery.parseJSON(jqXHR.responseText).took + '</strong>') + '</label>'));
							AJS.$("#jobprogressStatus").text("Job Stopped successfully by:" + jobStoppedBy).css({'color': '#008000'}).show().delay(5000).hide(1000);
						}else if(jqXHR.status == 404 || jqXHR.status == 400){
							AJS.$("#jobprogressStatus").text(errorDesc).css({'color': '#ff0000'}).show().delay(5000).hide(1000);
						}
					}
            	});
            }
		}*/

		AJS.$.ajaxPrefilter(function( options, originalOptions, jqXHR ) {
			   if (/\/zephyr/.test(options.url)) {
				  jqXHR.setRequestHeader(zEncKeyFld, zEncKeyVal);
			   }
		});
	}
	return {Config:configClass}
})()

AJS.$(document).ready(function() {
	AJS.$('#zephyr-show-workflow').bind('change', Zephyr.Admin.Config.updateWorkflowSettings);
	AJS.$('#zephyr-version-check').bind('change', Zephyr.Admin.Config.updateVersionSettings);
	AJS.$('#zephyr-issuelink-check').bind('change', Zephyr.Admin.Config.issueLinkToggle);
	AJS.$('#zephyr-issuelinkstep-check').bind('change', Zephyr.Admin.Config.issueLinkStepToggle);
    AJS.$('#zephyr-log-level-select').bind('change', Zephyr.Admin.Config.updateLogLevel);
    AJS.$('#zephyr-log-max-size').bind('blur', Zephyr.Admin.Config.updateLogMaxSize);
    AJS.$('#zephyr-log-max-backup').bind('blur', Zephyr.Admin.Config.updateLogMaxBackup);
    AJS.$('#zephyr-bind-issue-security').bind('change', Zephyr.Admin.Config.enableIssueSecurity);
    AJS.$('#zephyr-bind-labels-filter').bind('change', Zephyr.Admin.Config.disableTestSummaryLabelsFilter);
    AJS.$('#zephyr-bind-all-filters-testsummary').bind('change', Zephyr.Admin.Config.disableTestSummaryAllFilters);
    AJS.$("#zephyrAnalyticCheck").bind('change', Zephyr.Admin.Config.enableDisableZephyrAnalytics);

    AJS.$('#zephyr-ril-check').bind('change', Zephyr.Admin.Config.remoteIssueLinkToggle);
	AJS.$('#zephyr-rilstep-check').bind('change', Zephyr.Admin.Config.remoteIssueLinkStepToggle);
	AJS.$('#zephyr-ril-inverse').bind('change', Zephyr.Admin.Config.inverseIssueLinkToggle);
	AJS.$('#zephyr-ril-refresh').bind('click', Zephyr.Admin.Config.refreshRemoteIssueLinks);
	AJS.$('#zephyr-bind-issuetype-test-project').bind('change', Zephyr.Admin.Config.bindIssueTypeTestOnProjectCreate);
	AJS.$("#issue-link-relation-select").bind('change', Zephyr.Admin.Config.refreshRemoteIssueLinks);
    AJS.$("#test-link-req-relation-select").bind('change', Zephyr.Admin.Config.refreshTestRequirementIssueLinks);
    /*AJS.$('#zephyr-test-reset').bind('click', Zephyr.Admin.Config.resetZephyrTestIssueLinks);*/
    AJS.$('#non-issue-type-test-save').bind('click', Zephyr.Admin.Config.saveNonIssueTypeTestPreference);
	AJS.$('#zephyr-index-all').click(Zephyr.Admin.Config.indexAll);
    AJS.$("#hardIndexId").bind('change', Zephyr.Admin.Config.disableSyncButton);
    AJS.$('#zephyr-sync-all').click(Zephyr.Admin.Config.syncIndex);
    AJS.$('#zephyr-cleanupSprints').click(Zephyr.Admin.Config.cleanupSprints);
    AJS.$('#zephyr-cleanupCycleCache').click(Zephyr.Admin.Config.cleanupCycleCache);
	AJS.$('#zephyr-bind-custom-permission').bind('change', Zephyr.Admin.Config.updatePermissionCheck);
	AJS.$('#zLic').bind('click', Zephyr.Admin.Config.toggleLicense);
    AJS.$('#allProjects').bind('click', Zephyr.Admin.Config.toggleReindexProjectList);
    AJS.$('#zephyr-reindex-projects-view').bind('change', Zephyr.Admin.Config.enableDisableReindexButton);
    AJS.$('#zephyr-reindex-project').bind('change', Zephyr.Admin.Config.enableDisableReindexButton);
    AJS.$('#zephyr-update-execution-executedon').bind('change', Zephyr.Admin.Config.updateExecutionExecutedonFlag);
	AJS.$(document).on('click', ".zchangeHistory", Zephyr.Admin.Config.fetchChangeHistory);
	AJS.$(document).on('click', ".znav", Zephyr.Admin.Config.navigateChangeHistory);
	if(AJS.$("#zephyr-metrics-dashboard").length > 0)
		Zephyr.Admin.Config.initDashboard();
    Zephyr.Admin.Config.initIssueLinkType();
    Zephyr.Admin.Config.initProjectConfig();
    Zephyr.Admin.Config.initProjectConfigForReindex();
    AJS.$('#test-disabled-project-filter').keyup(Zephyr.Admin.Config.displayProjectsOnScreen);
		AJS.$('#test-disabled-project-filter').live('click', function(event){
		    var searchInput = AJS.$(event.target);
		    searchInput.addClass('active');
		});
		AJS.$('#removeSearchInput').live('click', function(event){
		  var parent = AJS.$(event.target).parent();
		  var searchInput = parent.find('.searchInput');
		  searchInput.val('');
		  if(searchInput.hasClass('active')) {
		    searchInput.trigger('keyup');
		    searchInput.removeClass('active');
		  } else {
		    searchInput.addClass('active');
		  }
		});
    Zephyr.Admin.Config.attachDeleteEvent();
    //Start: For disabling Execution Workflow for projects
    Zephyr.Admin.Config.initEWProjectConfig();
    AJS.$('#ew-disabled-project-filter').keyup(Zephyr.Admin.Config.displayEWProjectsOnScreen);
		AJS.$('#ew-disabled-project-filter').live('click', function(event){
		    var searchInput = AJS.$(event.target);
		    searchInput.addClass('active');
		});
		AJS.$('#removeEWSearchInput').live('click', function(event){
		  var parent = AJS.$(event.target).parent();
		  var searchInput = parent.find('.searchInput');
		  searchInput.val('');
		  if(searchInput.hasClass('active')) {
		    searchInput.trigger('keyup');
		    searchInput.removeClass('active');
		  } else {
		    searchInput.addClass('active');
		  }
		});
    Zephyr.Admin.Config.attachEWDeleteEvent();
    //AJS.$('#jobprogres-stop-button').bind('click', Zephyr.Admin.Config.stopJobProgress);
  //End: For disabling Execution Workflow for projects



});
