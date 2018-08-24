package com.thed.zephyr.util;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.thed.zephyr.je.event.ZQLFilterShareType;
import com.thed.zephyr.je.zql.model.ZQLFilter;
import com.thed.zephyr.je.zql.model.ZQLSharePermissions;

/**
 * Created by smangal on 11/23/14.
 */
public class ZQLFilterUtils {

    public static Boolean isFilterAccessibleByUser(ZQLFilter zqlFilter, ApplicationUser user){
        if (zqlFilter.getCreatedBy().equalsIgnoreCase(UserCompatibilityHelper.getKeyForUser(user.getDirectoryUser())))
            return true;
        else{
            ZQLSharePermissions[] zqlSharePermissions = zqlFilter.getZQLFilterSharePermissions();
            if(null == zqlSharePermissions || zqlSharePermissions.length <= 0 || ZQLFilterShareType.PRIVATE.getShareType().equals(zqlSharePermissions[0].getShareType())){
                return false;
            }else{
                return true;
            }
        }
    }
}
