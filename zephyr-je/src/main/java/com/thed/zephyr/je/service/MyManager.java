package com.thed.zephyr.je.service;

import com.thed.zephyr.je.permissions.aop.ValidatePermissions;
import com.thed.zephyr.je.permissions.model.PermissionType;

/**
 * Created by mukul on 7/30/15.
 */

public interface MyManager {

    @ValidatePermissions(permissionType = {PermissionType.ZEPHYR_BROWSE_CYCLE})
    void foo();
}
