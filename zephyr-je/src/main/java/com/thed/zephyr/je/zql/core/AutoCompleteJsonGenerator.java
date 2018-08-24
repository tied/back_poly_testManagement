package com.thed.zephyr.je.zql.core;

import java.util.Locale;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;

public interface AutoCompleteJsonGenerator {
    /**
     * Will return an array of JSON objects containing the field names that the user can see, listed in alphabetical order.
     *
     * The JSON object will contain:
     * value: value that will be autocompleted
     * displayName: the html escaped display name for the value
     * auto (optional) : if present indicates that the field can have values autocompleted
     * orderable (optional) : if present indicates that the field can participate in the order by clause
     *
     * @param user that the page is being rendered for.
     * @param locale the locale of the user.
     *
     * @return JSON string as described above.
     *
     * @throws JSONException if there is a problem generating the JSON object
     */
    String getVisibleFieldNamesJson(final ApplicationUser user, final Locale locale) throws JSONException;
    
    /**
     * @return a JSON array that contains strings that are the JQL reserved words.
     *
     * @throws JSONException if there is a problem generating the JSON object
     */
    String getJqlReservedWordsJson() throws JSONException;
    
    String getVisibleFunctionNamesJson(final ApplicationUser user, final Locale locale) throws JSONException;
}
