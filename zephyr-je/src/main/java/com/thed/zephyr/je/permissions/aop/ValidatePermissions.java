package com.thed.zephyr.je.permissions.aop;

import com.thed.zephyr.je.permissions.model.PermissionType;

import java.lang.annotation.*;

/**
 * To validate zephyr custom permissions
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface ValidatePermissions {

    PermissionType[] permissionType() default {};
}
