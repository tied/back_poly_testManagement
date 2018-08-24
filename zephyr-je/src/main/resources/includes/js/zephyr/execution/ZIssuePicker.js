/**
 * A multiselect list for querying and selecting issues. Issues can also be selected via a popup. 
 *
 * @constructor ZEPHYR.IssuePicker
 * @extends AJS.MultiSelect
 */
ZEPHYR.IssuePicker = JIRA.IssuePicker.extend({


});


/** Preserve legacy namespace
    @deprecated jira.issuepicker */
AJS.namespace("zephyr.issuepicker", null, ZEPHYR.IssuePicker);

/** Preserve legacy namespace
    @deprecated AJS.IssuePicker */
AJS.namespace("AJS.IssuePicker", null, ZEPHYR.IssuePicker);
