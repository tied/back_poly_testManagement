package com.thed.zephyr.util;

import java.util.Comparator;

import com.thed.zephyr.je.vo.ColumnItemLayoutBean;

public 	class ColumnItemLayoutBeanComparator implements Comparator<ColumnItemLayoutBean> {
    /**
     * Compares two Custom Layout Item Beans 
     */
    public static final ColumnItemLayoutBeanComparator INSTANCE = new ColumnItemLayoutBeanComparator();
    
	@Override
	public int compare(ColumnItemLayoutBean columnItemLayoutBean1, 
			ColumnItemLayoutBean columnItemLayoutBean2) {
        if (columnItemLayoutBean1 == columnItemLayoutBean2)
            return 0;

        if (columnItemLayoutBean1 == null)
            return 1;

        if (columnItemLayoutBean2 == null)
            return -1;
        
        if(columnItemLayoutBean1.getOrderId() == null && columnItemLayoutBean2.getOrderId() == null) {
            return 0;
        }
        if(columnItemLayoutBean1.getOrderId() == null) {
        	return 1;
        }
        if(columnItemLayoutBean2.getOrderId() == null) {
        	return -1;
        }
        Long columnItemId1 = Long.valueOf(columnItemLayoutBean1.getOrderId());
        Long columnItemId2 = Long.valueOf(columnItemLayoutBean2.getOrderId());

        int columnItemComparison = columnItemId1.compareTo(columnItemId2);
        return columnItemComparison;
   }
}