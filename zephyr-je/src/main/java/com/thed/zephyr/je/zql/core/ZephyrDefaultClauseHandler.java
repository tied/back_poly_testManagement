package com.thed.zephyr.je.zql.core;

/**
 * Created by smangal on 11/02/16.
 */
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.ClauseInformation;
import com.atlassian.jira.jql.context.ClauseContextFactory;
import com.atlassian.jira.jql.permission.ClausePermissionHandler;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.util.dbc.Assertions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public final class ZephyrDefaultClauseHandler implements ClauseHandler {
    private final ClauseQueryFactory factory;
    private final ClauseValidator validator;
    private final ClausePermissionHandler permissionHandler;
    private final ClauseContextFactory contextFactory;
    private final ClauseInformation clauseInformation;

    public ZephyrDefaultClauseHandler(ClauseInformation information, ClauseQueryFactory factory, ClauseValidator validator, ClausePermissionHandler permissionHandler, ClauseContextFactory contextFactory) {
        this.permissionHandler = (ClausePermissionHandler)Assertions.notNull("permissionHandler", permissionHandler);
        this.factory = (ClauseQueryFactory)Assertions.notNull("factory", factory);
        this.validator = (ClauseValidator)Assertions.notNull("validator", validator);
        this.contextFactory = (ClauseContextFactory)Assertions.notNull("contextFactory", contextFactory);
        this.clauseInformation = (ClauseInformation)Assertions.notNull("information", information);
    }

    public ClauseInformation getInformation() {
        return this.clauseInformation;
    }

    public ClauseQueryFactory getFactory() {
        return this.factory;
    }

    public ClauseValidator getValidator() {
        return this.validator;
    }

    public ClausePermissionHandler getPermissionHandler() {
        return this.permissionHandler;
    }

    public ClauseContextFactory getClauseContextFactory() {
        return this.contextFactory;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(o != null && this.getClass() == o.getClass()) {
            ZephyrDefaultClauseHandler that = (ZephyrDefaultClauseHandler)o;
            return !this.clauseInformation.equals(that.clauseInformation)?false:(!this.contextFactory.equals(that.contextFactory)?false:(!this.factory.equals(that.factory)?false:(!this.permissionHandler.equals(that.permissionHandler)?false:this.validator.equals(that.validator))));
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = this.factory.hashCode();
        result = 31 * result + this.validator.hashCode();
        result = 31 * result + this.permissionHandler.hashCode();
        result = 31 * result + this.contextFactory.hashCode();
        result = 31 * result + this.clauseInformation.hashCode();
        return result;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
