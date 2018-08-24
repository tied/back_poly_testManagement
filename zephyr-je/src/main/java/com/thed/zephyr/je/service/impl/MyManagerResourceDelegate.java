package com.thed.zephyr.je.service.impl;

import com.thed.zephyr.je.permissions.aop.ValidatePermissions;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.service.MyManager;
import org.springframework.stereotype.Component;

/**
 * Created by mukul on 7/30/15.
 */

@Component
public class MyManagerResourceDelegate implements MyManager {

    @Override
    public void foo() {
        System.out.println("######## MyManagerImpl.foo() invoked ########");
    }
}
