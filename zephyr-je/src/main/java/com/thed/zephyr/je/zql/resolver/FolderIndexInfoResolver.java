package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Index resolver that can find the index values for folder.
 * 
 * @author manjunath
 *
 */
public class FolderIndexInfoResolver implements IndexInfoResolver<Folder> {
	
    private final NameResolver<Folder> indexInfoResolver;

    public FolderIndexInfoResolver(NameResolver<Folder> indexInfoResolver) {
        this.indexInfoResolver = indexInfoResolver;
    }

    public List<String> getIndexedValues(final String rawValue) {
        notNull("rawValue", rawValue);
        if (indexInfoResolver.nameExists(rawValue)) {
            return Collections.singletonList(rawValue);
        }
        return null;
    }

    public List<String> getIndexedValues(final Long rawValue) {
        notNull("rawValue", rawValue);
        if (indexInfoResolver.idExists(rawValue)) {
            return CollectionBuilder.newBuilder(rawValue.toString()).asList();
        }
        else {
            return getIndexedValues(String.valueOf(rawValue));
        }
    }

    public String getIndexedValue(final Folder folder) {
        notNull("folder", folder);
        return getIdAsString(folder);
    }

    private String getIdAsString(final Folder folder) {
        return String.valueOf(folder.getID());
    }
}