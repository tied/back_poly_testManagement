AJS.$.namespace("ZEPHYR.ISSUE.Testcase")
if (typeof Zephyr == "undefined") { Zephyr = {};}

AJS.$("#zephyr-create-test").live("contextmenu", function (e) {
    e.preventDefault();
    return false;
});
AJS.$("#zephyr-create-test").live("click", function (e) {
    e.preventDefault();
    e.stopImmediatePropagation();
    jQuery.ajax({
        url: getRestURL() + "/util/zephyrTestIssueType",
        type : "get",
        contentType :"application/json",
        dataType: "json",
        success : function(response) {
            var params = parseUri(AJS.$("#zephyr-create-test").attr('href')).queryKey;
            var parentIssueKey = params['parentIssueId'];

            if(parentIssueKey === '${issue.id}') {
                //finding the element & getting the parent issue id. In case of issue detail view.
                params = parseUri(AJS.$('#opsbar-operations_more_drop').find('#zephyr-create-test').attr('href')).queryKey;
            }
            var zephyrTestcaseId = response.testcaseIssueTypeId;
            var options = {
                parentIssueId: params['parentIssueId'],
                issueType: response.testcaseIssueTypeId
            };
            // If data attributes exist, we will prefer those over the params.
            options = jQuery.extend(options, {
                parentIssueId: params['parentIssueId'],
                issueType: response.testcaseIssueTypeId
            });

            JIRA.Forms.createCreateIssueForm(options).asDialog({
                id: "create-issue-dialog",
                windowTitle: AJS.I18n.getText('admin.issue.operations.create'),
                parentIssueId:params['parentIssueId']
            }).show();

            AJS.$('#create-issue-dialog').on('remove', function () {
                jQuery(AJS).unbind("QuickCreateIssue.issueCreated");
                if (AJS.$('#linkingmodule').length > 0) {
                    AJS.$('#linkingmodule').load(' #linkingmodule');
                } else {
                    location.reload(); // reloading the page for first time linking of test with non-test issues.
                }
            });

            jQuery(AJS).unbind("QuickCreateIssue.issueCreated");
            jQuery(AJS).bind("QuickCreateIssue.issueCreated", function (event, issueData) {
                if(issueData.createdIssueDetails && issueData.createdIssueDetails.fields.issuetype && issueData.createdIssueDetails.fields.issuetype.id == zephyrTestcaseId) {
                    var parentIssueId = params['parentIssueId'];
                    jQuery.ajax({
                        url: getRestURL() + "/test/addIssueLink?parentIssueId="+parentIssueId+"&testcaseId="+issueData.createdIssueDetails.id,
                        type: "POST",
                        contentType: "application/json",
                        dataType: "json",
                        success: function (response) {
                        }
                    });
                }
            });
        }
    });
});
