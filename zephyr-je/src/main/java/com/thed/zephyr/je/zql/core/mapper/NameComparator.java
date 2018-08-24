package com.thed.zephyr.je.zql.core.mapper;

import java.util.Comparator;


public class NameComparator implements Comparator<String> {
    /**
     * Compares two Names 
     */
    public static final NameComparator INSTANCE = new NameComparator();

	@Override
	public int compare(String name1, String name2) {
        // check nulls
        if ((name1 == null) && (name2 == null))
        {
            return 0;
        }
        else if (name1 == null) // null is less than any value
        {
            return -1;
        }
        else if (name2 == null) // any value is greater than null
        {
            return 1;
        }
        return name1.compareToIgnoreCase(name2);
     }
}
