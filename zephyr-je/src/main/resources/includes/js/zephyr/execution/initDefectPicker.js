//(function() {
//    function createPicker($selectField) {
//        new AJS.MultiSelect({
//           element: $selectField,
//           itemAttrDisplayed: "label",
//           errorMessage: AJS.I18n.getText("zephyr.je.autocomplete.defects.error"),
//           maxInlineResultsDisplayed: 15
//        });
//    }
//
//    function locateSelect(parent) {
//        var $parent = AJS.$(parent),
//            $selectField;
//
//        if ($parent.is("select")) {
//            $selectField = $parent;
//        } else {
//            $selectField = $parent.find("select");
//        }
//
//        return $selectField;
//    }
//
//    var DEFAULT_SELECTORS = [
//        "div.aui-field-defectspicker.frother-control-renderer", // aui forms
//        "td.aui-field-defectspicker.frother-control-renderer", // convert to subtask and move
//        "tr.aui-field-defectspicker.frother-control-renderer" // bulk edit
//    ];
//
//    function findDefectSelectAndConvertToPicker(context, selector) {
//        selector = selector || DEFAULT_SELECTORS.join(", ");
//
//        AJS.$(selector, context).each(function () {
//
//            var $selectField = locateSelect(this);
//
//            if ($selectField.length) {
//                createPicker($selectField);
//            }
//
//        });
//    }
//
//    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, context) {
//        findDefectSelectAndConvertToPicker(context);
//    });
//
//})();

(function () {
//    function initDefectPicker(el) {
//    	AJS.$(document.body).find('.aui-field-tescasepickers').each(function () {
//        	new ZEPHYR.IssuePicker({
//	                element: AJS.$(this),
//	                userEnteredOptionsMsg: AJS.I18n.getText('linkissue.enter.issue.key'),
//	                uppercaseUserEnteredOnSelect: true
//	        });
//        });
//    }
//
//    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, context) {
//        initDefectPicker(context);
//    });
})();

