package com.thed.zephyr.je.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.plugin.webfragment.SimpleLinkFactory;
import com.atlassian.jira.plugin.webfragment.descriptors.SimpleLinkFactoryModuleDescriptor;
import com.atlassian.jira.plugin.webfragment.model.SimpleLink;
import com.atlassian.jira.user.ApplicationUser;

public class DummyLinkFactory implements SimpleLinkFactory {

	/**
	 * Example implementation: https://studio.plugins.atlassian.com/source/browse/JFAV/trunk/favourites-plugin/src/main/java/com/jtricks/web/links/FavouritesLinkFactory.java?r=168329
	 */
    DummyLinkFactory() {
    	
    }
    
	@Override
	public void init(SimpleLinkFactoryModuleDescriptor descriptor) {
	}

	@Override
	public List<SimpleLink> getLinks(ApplicationUser user, Map<String, Object> params) {
		return new ArrayList<SimpleLink>();
	}

}
