package com.thed.zephyr.je.zql.permission.impl;

import com.thed.zephyr.util.JiraUtil;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.issue.search.parameters.lucene.PermissionsFilterCache;
import com.atlassian.jira.security.JiraAuthenticationContextImpl;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.permission.ZephyrPermissionQueryFactory;
import com.thed.zephyr.je.zql.permission.ZephyrPermissionsFilterGenerator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

public class ZephyrPermissionsFilterGeneratorImpl implements
		ZephyrPermissionsFilterGenerator {
    private static final Logger log = LoggerFactory.getLogger(ZephyrPermissionsFilterGeneratorImpl.class);
    private final ZephyrPermissionQueryFactory zephyrPermissionQueryFactory;

    public ZephyrPermissionsFilterGeneratorImpl(ZephyrPermissionQueryFactory zephyrPermissionQueryFactory)
    {
        this.zephyrPermissionQueryFactory =  zephyrPermissionQueryFactory;
    }

    public Query getQuery(final ApplicationUser searcher)
    {
        // if we have a cached query, just return that
        Query query = null;
        if (JiraUtil.isJIRAGreaterThan710()) {

            try {
                Method getQueryMethod = getCache().getClass().getMethod("getQuery", ApplicationUser.class, Collection.class);

                try {
                    Object[] args = {searcher, null};
                    query = (Query) getQueryMethod.invoke(getCache(),args);

                    if (query == null)
                    {
                        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
                        query = zephyrPermissionQueryFactory.getQuery(searcher, projectPermissionKey);

                        Method storeQueryMethod = getCache().getClass().getMethod("storeQuery",Query.class, ApplicationUser.class, Collection.class);

                        Object[] storeQueryArgs = {query, searcher, null};
                        storeQueryMethod.invoke(getCache(),storeQueryArgs);
                    }
                } catch (IllegalAccessException iae) {
                    log.error("IllegalAccessException while calling getQuery/storeQuery in ZephyrPermissionsFilterGeneratorImpl.",iae);
                } catch (InvocationTargetException ite) {
                    log.error("InvocationTargetException while calling getQuery/storeQuery in ZephyrPermissionsFilterGeneratorImpl.",ite);
                }

            } catch (NoSuchMethodException ex) {
                log.error("no such method found.",ex);
            }
        }else {
            query = getCache().getQuery(searcher);
            if (query == null)
            {
                ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
                query = zephyrPermissionQueryFactory.getQuery(searcher, projectPermissionKey);
                getCache().storeQuery(query, searcher);
            }
        }
        return query;
    }

    private PermissionsFilterCache getCache()
    {
        PermissionsFilterCache cache = (PermissionsFilterCache) JiraAuthenticationContextImpl.getRequestCache().get(
            SystemSearchConstant.PERMISSIONS_FILTER_CACHE);

        if (cache == null)
        {
            log.debug("Creating new PermissionsFilterCache");
            cache = new PermissionsFilterCache();
            JiraAuthenticationContextImpl.getRequestCache().put(SystemSearchConstant.PERMISSIONS_FILTER_CACHE, cache);
        }

        return cache;
    }

}
