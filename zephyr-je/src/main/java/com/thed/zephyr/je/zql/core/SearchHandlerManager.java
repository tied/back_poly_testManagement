package com.thed.zephyr.je.zql.core;

import java.util.Collection;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.NotNull;

/**
 * Manager that holds all references to search related information in ZFJ.
 *
 */
public interface SearchHandlerManager
{
 
    /**
     * Refreshes the {@link com.atlassian.jira.issue.search.managers.SearchHandlerManager}.
     */
    void refresh();

    /**
     * Gets the field ids that are associated with the provided zqlClauseName. The reason this returns a collection is
     * that custom fields can have the same JQL clause name and therefore resolve to multiple field ids. This will only
     * return the fields associated with clause handlers that the user has permission to see as specified by the {@link
     * com.atlassian.jira.jql.permission.ClausePermissionHandler#hasPermissionToUseClause(com.atlassian.crowd.embedded.api.User)}
     * method.
     *
     * @param searcher that will be used to perform a permission check.
     * @param zqlClauseName the clause name to find the field id for.
     *
     * @return the field ids that are associated with the provided zqlClauseName, empty collection if not found
     */
    @NotNull
    Collection<String> getFieldIds(final ApplicationUser searcher, String zqlClauseName);

    /**
     * Gets the field ids that are associated with the provided zqlClauseName. The reason this returns a collection is
     * that custom fields can have the same JQL clause name and therefore resolve to multiple field ids.
     *
     * @param zqlClauseName the clause name to find the field id for.
     * @return the field ids that are associated with the provided zqlClauseName, empty collection if not found
     */
    @NotNull
    Collection<String> getFieldIds(String zqlClauseName);
    
    /**
     * Return a collection of {@link com.atlassian.jira.jql.ClauseHandler}s registered against the passed JQL clause
     * name. This will only return the handlers that the user has permission to see as specified by the {@link
     * com.atlassian.jira.jql.permission.ClausePermissionHandler#hasPermissionToUseClause(User)}
     * method. The reason this is returning a collection is that custom fields can have the same JQL clause name and
     * therefore resolve to multiple clause handlers, this will never be the case for System fields, we don't allow it!
     *
     * @param user that will be used to perform a permission check.
     * @param zqlClauseName the clause name to search for.
     * @return A collection of ClauseHandler that are associated with the passed JQL clause name. An empty collection
     *         will be returned to indicate failure.
     */
    @NotNull
    Collection<ClauseHandler> getClauseHandler(final ApplicationUser user, final String zqlClauseName);

    /**
     * Return a collection of {@link com.atlassian.jira.jql.ClauseHandler}s registered against the passed JQL clause
     * name. This will return all available handlers, regardless of permissions. The reason this is returning a collection
     * is that custom fields can have the same JQL clause name and therefore resolve to multiple clause handlers, this
     * will never be the case for System fields, we don't allow it!
     *
     * @param zqlClauseName the clause name to search for.
     * @return A collection of ClauseHandler that are associated with the passed JQL clause name. An empty collection
     *         will be returned to indicate failure.
     */
    @NotNull
    Collection<ClauseHandler> getClauseHandler(final String zqlClauseName);

   
    /**
     * Get all the available clause handlers that the searcher can see.
     *
     * @param searcher that will be used to perform a permission check.
     * @return the {@link com.atlassian.jira.jql.ClauseHandler} visible to the user. Empty collection
     * is returned when the can see no clauses.
     */
    @NotNull
    Collection<ClauseHandler> getVisibleClauseHandlers(ApplicationUser searcher);

}
