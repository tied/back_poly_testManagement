package com.thed.zephyr.je.permissions.model;

/**
 * Zephyr Custom Permission Types
 */
public enum PermissionType {
    ZEPHYR_BROWSE_CYCLE("Browse Cycle"), 
	ZEPHYR_CREATE_CYCLE("Create Cycle"), 
    ZEPHYR_EDIT_CYCLE("Edit Cycle"), 
    ZEPHYR_DELETE_CYCLE("Delete Cycle"), 
    ZEPHYR_CREATE_EXECUTION("Create Execution"), 
    ZEPHYR_EDIT_EXECUTION("Edit Execution"), 
    ZEPHYR_DELETE_EXECUTION("Delete Execution"),
    ZEPHYR_TEST_MANAGEMENT_PERMISSION("View Test Management");
    
    private String PermissionType = null;

    PermissionType(String permissionType) {
    	this.PermissionType = permissionType;
    }
    
	public String getPermissionType() {
		return PermissionType;
	}

	public void setPermissionType(String permissionType) {
		PermissionType = permissionType;
	}
}
