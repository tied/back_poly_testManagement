(function () {

    function createTestInlineAttach(context) {
    	context.find(".ignore-inline-attach").zInlineAttach();
    }

    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, context) {
    	createTestInlineAttach(context);
    });

})();
