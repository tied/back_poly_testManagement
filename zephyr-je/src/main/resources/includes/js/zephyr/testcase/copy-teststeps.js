/**
 * Zephyr Test Steps copy
 */
if(typeof ZEPHYR == 'undefined') ZEPHYR = {};
if(typeof ZEPHYR.TESTSTEPS == 'undefined') ZEPHYR.TESTSTEPS = {};
if(typeof ZEPHYR.TESTSTEPS.COPY == 'undefined') ZEPHYR.TESTSTEPS.COPY = {};

ZEPHYR.TESTSTEPS.COPY.Steps = {
    defineSource: "DEFINE_SOURCE",
    defineDestination: "DEFINE_DESTINATION",
    processing: "PROCESSING"
};
ZEPHYR.TESTSTEPS.COPY.DestSource = {
    issuePicker: "ISSUE_PICKER",
    filterPicker: "FILTER_PICKER"
};

ZEPHYR.TESTSTEPS.COPY.SourceModel = Backbone.Model.extend({
    defaults : {
        issueKey: null,
        progressTicket: null,
        destSource: ZEPHYR.TESTSTEPS.COPY.DestSource.issuePicker
    }
});

AJS.$(document).ready(function (event) {
	if (AJS.$('#jiraVersionGreaterThan7-10')[0].dataset.versionstatus == "true") {
		AJS.$('body').addClass('jira-greater-7-10')
	}
});

function savedSearchChanged() {
    var value = (AJS.$("#addTestsSavedSearch").val() || []).join(',');
	if(!value || value.length == 0){
		return;
	}
	AJS.$('.error').css('display', 'none');
	AJS.$('#filter-details').css('display', 'block').find('pre').text(value);
    AJS.$('#filter-test-found .button-spinner').spin();
    //Fetching the selected filter query and count.
	fetchDataFrom("/rest/zephyr/latest/test/mySearches/"+value).done(function(fetchedData){
		AJS.$('#filter-test-found .button-spinner').spinStop();
        AJS.$('#found-tests').text(fetchedData.count);
        AJS.$('#filter-details').css('display', 'block').find('pre').text(fetchedData.query);
	})
}

function fetchDataFrom(url) {
    var deferred = jQuery.Deferred();
    JIRA.SmartAjax.makeRequest({
        url: contextPath + url,
        complete: function (xhr, textStatus, smartAjaxResult) {
            if (smartAjaxResult.successful) {
                deferred.resolve(smartAjaxResult.data);
            } else {
                deferred.reject(JIRA.SmartAjax.buildDialogErrorContent(smartAjaxResult));
            }
        }
    });
    return deferred.promise();
}

ZEPHYR.TESTSTEPS.COPY.CopyStepsView = Backbone.View.extend({
	initialize: function(options) {
        _.bindAll(this, 'render', 'sourceNextBtnClick', 'initIssuePicker', 'initFilterPicker', 'destBackBtnClick', 'issuePickerBtnClick', 'filterPickerBtnClick', 'submit',
        		'submitSuccess', 'submitError');
        this.step = options.step;
        this.baseUrl = options.baseUrl;
        this.model.set('issueKey',options.model.get('issueKey'));
        this.$navList = AJS.$('.steps');
        AJS.$('#source-next-btn').bind('click', this.sourceNextBtnClick);
        AJS.$('#dest-back-btn').bind('click', this.destBackBtnClick);
        AJS.$('#dest-submit-btn').bind('click', this.submit);
        AJS.$('#' + ZEPHYR.TESTSTEPS.COPY.DestSource.issuePicker + '-btn').bind('click', this.issuePickerBtnClick);
        AJS.$('#' + ZEPHYR.TESTSTEPS.COPY.DestSource.filterPicker + '-btn').bind('click', this.filterPickerBtnClick);
        this.initIssuePicker();
        this.initFilterPicker();
    },
    render: function () {
        AJS.$('.step-content').css('display', 'none');
        AJS.$("#" + this.step).css('display', 'block');
        AJS.$('#step-' + this.step).removeClass().addClass('current').prevAll().removeClass().addClass('done');
        AJS.$('#step-' + this.step).nextAll().removeClass().addClass('todo');
        if (this.step == ZEPHYR.TESTSTEPS.COPY.Steps.defineDestination){
            AJS.$('.dest-source-btn').removeClass('button-selected');
            AJS.$('#' + this.model.get('destSource') + '-btn').addClass('button-selected');
            AJS.$('.picker-container').css('display', 'none');
            AJS.$("#" + this.model.get('destSource') + '-container').css('display', 'block');
        }
    },
    sourceNextBtnClick: function () {
        this.step = ZEPHYR.TESTSTEPS.COPY.Steps.defineDestination;
        this.render();
    },
    destBackBtnClick: function () {
        this.step = ZEPHYR.TESTSTEPS.COPY.Steps.defineSource;
        this.render();
    },
    initIssuePicker: function () {
    	AJS.$(document.body).find('.aui-field-tescasepickers').each(function () {
	    	new JIRA.IssuePicker({
	                element: AJS.$(this),
	                userEnteredOptionsMsg: AJS.I18n.getText('linkissue.enter.issue.key'),
	                uppercaseUserEnteredOnSelect: true
	        });
	    });
    },
    initFilterPicker: function () {
    	AJS.$(document.body).find('.aui-field-filterpicker').each(function () {
	        new AJS.SingleSelect({
 	           element: AJS.$("#addTestsSavedSearch"),
	           itemAttrDisplayed: "label",
	           maxInlineResultsDisplayed: 15,
	           maxWidth:350,
	           ajaxOptions: {
	        	   url:contextPath + "/rest/zephyr/latest/picker/filters",
	        	   query:"true",
	        	   formatResponse: function (response) {
	        		   var ret = [];
	        		   AJS.$(response).each(function(i, category) {
        	                var groupDescriptor = new AJS.GroupDescriptor({
        	                    weight: i, // order or groups in
        	                    label: category.label
        	                });

        	                AJS.$(category.options).each(function(){
        	                    groupDescriptor.addItem(new AJS.ItemDescriptor({
        	                        value: this.id, // value of
        	                        label: this.label, // title
        	                        html: this.html,
        	                        highlighted: true
        	                    }));
        	                });
        	                ret.push(groupDescriptor);
        	            });
        	            return ret;
	        	   }
	           }
	        });
	        AJS.$('#addTestsSavedSearch-multi-select').css('width', '350px')
	        var filterSelect = AJS.$('#addTestsSavedSearch')
	        filterSelect.change('selected', function() {
	        	savedSearchChanged();
	        });
	        filterSelect.bind('unselect', function() {
	        	savedSearchChanged();
	        });
    	});
    },
    issuePickerBtnClick: function () {
        this.model.set('destSource', ZEPHYR.TESTSTEPS.COPY.DestSource.issuePicker);
        this.render();
    },
    filterPickerBtnClick: function () {
        this.model.set('destSource', ZEPHYR.TESTSTEPS.COPY.DestSource.filterPicker);
        this.render();
    },
    submit: function () {
    	AJS.$('.error').css('display', 'none');
    	var sourceIssue = this.model.get('issueKey');
    	var value="", isJql="";
    	if(this.model.get('destSource') == ZEPHYR.TESTSTEPS.COPY.DestSource.issuePicker){
    		value = (AJS.$("#zephyr-je-testkey").val() || []).join(',');
    		isJql = "false";
    	} else {
    		value = (AJS.$("#addTestsSavedSearch").val() || []).join(',');
    		isJql = "true";
    	}
        var copyCustomField = AJS.$('#copyCustomFieldId').is(":checked");
    	if(!value || value.length == 0) {
    		AJS.$('.error').css('display', 'block');
    		return;
    	}
    	this.step = ZEPHYR.TESTSTEPS.COPY.Steps.processing;
    	var data = '{"destinationIssues" : "' + value + '", "isJql" : "' + isJql + '", "copyCustomField" : "' + copyCustomField + '"}';
        this.render();
        AJS.$.ajax({
        	url: AJS.contextPath() + "/rest/zephyr/latest/teststep/" + sourceIssue + "/copyteststeps" ,
            type : "POST",
            contentType :"application/json",
            data: data,
            success : this.submitSuccess,
            error: this.submitError
        });
    },
    submitSuccess: function (response) {
        this.model.set('progressTicket', response.jobProgressToken);
        var token = response.jobProgressToken;
		var intervalId = setInterval(function () {
			jQuery.ajax({
				url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + token,
				data: {'type': "copy_teststeps_from_source_to_destination"},
				complete: function (jqXHR, textStatus) {
					var data = jQuery.parseJSON(jqXHR.responseText);
					if (data != undefined) {
						AJS.$("#copy-teststeps-progress .aui-progress-indicator").show().attr("data-value", data.progress);
						AJS.$("#copy-teststeps-progress .aui-progress-indicator-value").css("width", data.progress * 100 + "%");
						AJS.$("#copy-teststeps-progress .timeTaken").html(AJS.I18n.getText('zephyr.je.cycle.timeTaken') + ": " + data.timeTaken);
						if (data.progress == 1) {
							var summaryMessage = JSON.parse(data.summaryMessage);
							var message1 = "steps", message2="issues";
							if(parseInt(summaryMessage.teststeps) <= 1) {
								if(parseInt(summaryMessage.copiedIssues) <= 1) {
									message2 = "issue";
								}
								message1 = "step"
							} else {
								if(parseInt(summaryMessage.copiedIssues) <= 1) {
									message2 = "issue";
								}
							}
							AJS.$("#copy-teststeps-progress .timeTaken").append(" <br /><strong style='color:green'>" + AJS.I18n.getText('zephyr.je.copyteststeps.success', summaryMessage.teststeps, message1, summaryMessage.copiedIssues, message2) + "</strong>");
                            if(summaryMessage.notfoundissues) {
                                AJS.$("#copy-teststeps-progress .timeTaken").append(" <br /><strong style='color:red'>" + AJS.I18n.getText('zephyr.je.copyteststeps.failed', summaryMessage.notfoundissues.trim()) + "</strong>");
                            }
                            if(summaryMessage.noIssuePermission) {
                                AJS.$("#copy-teststeps-progress .timeTaken").append(" <br /><strong style='color:red'>" + AJS.I18n.getText('zephyr.je.copyteststeps.result.noJIRAPermission', summaryMessage.noIssuePermission) + "</strong>");
                            }
							clearInterval(intervalId);
							AJS.$('#done-btn').removeClass('disabled-link').removeAttr('disabled').removeAttr('aria-disabled');
						} else if(data.progress == 2) {
							AJS.$("#copy-teststeps-progress .timeTaken").append(" <br /><strong style='color:red'>Copy failed.</strong>");
							clearInterval(intervalId);
							AJS.$('#done-btn').removeClass('disabled-link').removeAttr('disabled').removeAttr('aria-disabled');
						}
					}
				}
			})
		}, 1000);
    },
    submitError: function (xhr, status) {
    	this.step = ZEPHYR.TESTSTEPS.COPY.Steps.defineDestination;
        this.render();
    }
});

ZEPHYR.TESTSTEPS.COPY.init = function (issueKey, baseUrl) {
    var sourceModel = new ZEPHYR.TESTSTEPS.COPY.SourceModel({issueKey: issueKey});
    var copyStepsView = new ZEPHYR.TESTSTEPS.COPY.CopyStepsView({model:sourceModel, step: ZEPHYR.TESTSTEPS.COPY.Steps.defineSource, baseUrl: baseUrl});
    copyStepsView.render();
};
