package com.thed.zephyr.je.zql.permission;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.lucene.search.Query;

public interface ZephyrPermissionsFilterGenerator {
    /**
     * Generates a lucene {@link Query} that is the canonical set of permissions for viewable issues for the given user.
     * This query can then be used to filter out impermissible documents from a lucene search.
     *
     * @param searcher the user performing the search
     * @return the query; could be null if an error occurred.
     */
    Query getQuery(ApplicationUser searcher);
}
