package com.thed.zephyr.je.zql.core.mapper;

import java.util.Comparator;


public class ListValueComparator implements Comparator<String> {
    /**
     * Compares two Ids
     */
    public static final ListValueComparator INSTANCE = new ListValueComparator();

    public int compare(String s1, String s2) {
        if(s1 == "-1" || s2 == "-1") {
            return 0;
        }
        return s1 == null && s2 == null ? 0 : (s1 == null ? 1 : (s2 == null ? -1 : Long.valueOf(s1).compareTo(Long.valueOf(s2))));
    }
}
