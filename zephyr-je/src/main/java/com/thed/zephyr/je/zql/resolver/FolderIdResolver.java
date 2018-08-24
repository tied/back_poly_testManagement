package com.thed.zephyr.je.zql.resolver;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.jql.resolver.NameResolver;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.service.FolderManager;
import com.thed.zephyr.util.ApplicationConstants;

/**
 * Resolves folder ids from their names.
 * 
 * @author manjunath
 *
 */
public class FolderIdResolver implements NameResolver<Folder> {
	
	private final FolderManager folderManager;
	
    public FolderIdResolver(FolderManager folderManager) {
    	this.folderManager = folderManager;
    }

    public List<String> getIdsFromName(final String name) {
        notNull("name", name);
        List<String> values = new ArrayList<String>();

        List<String> names = new ArrayList<String>();
        names.add(name);
		List<Folder> allFolders = folderManager.getValuesByKey("ID", names);
		for(Folder folder : allFolders) {
			values.add(String.valueOf(folder.getID()));
		}
        return values;
    }

    public boolean nameExists(final String name) {
        notNull("name", name);
        List<String> names = new ArrayList<String>();
        names.add(name);
		List<Folder> allFolders = folderManager.getValuesByKey("ID", names);
		for(Folder folder : allFolders) {
			if(StringUtils.equalsIgnoreCase(name, folder.getName())) {
				return true;
			}
		}
		return false;
    }

    public boolean idExists(final Long id) {
        notNull("id", id);
        if (id.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID)) {
			return true;
		} else if(get(id) != null) {
			return true;
		}
        return false;
    }

    public Folder get(final Long id) {
		Folder folder = folderManager.getFolder(id);
		return folder;
    }

	@Override
	public Collection<Folder> getAll() {
        return new ArrayList<Folder>();
	}
}

