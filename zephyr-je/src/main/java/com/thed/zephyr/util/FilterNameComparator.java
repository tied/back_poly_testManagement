package com.thed.zephyr.util;

import java.io.Serializable;
import java.util.Comparator;

import com.thed.zephyr.je.zql.model.ZQLFilter;

public class FilterNameComparator implements Comparator<ZQLFilter>, Serializable
{
    public static final Comparator<ZQLFilter> COMPARATOR = new FilterNameComparator();

    public int compare(final ZQLFilter o1, final ZQLFilter o2)
    {
        // check nulls
        if ((o1 == null) && (o2 == null))
        {
            return 0;
        }
        else if (o1 == null) // null is less than any value
        {
            return -1;
        }
        else if (o2 == null) // any value is greater than null
        {
            return 1;
        }

        final String filterName1 = o1.getFilterName();
        final String filterName2 = o2.getFilterName();

        // check nulls
        if ((filterName1 == null) && (filterName2 == null))
        {
            return 0;
        }
        else if (filterName1 == null) // null is less than any value
        {
            return -1;
        }
        else if (filterName2 == null) // any value is greater than null
        {
            return 1;
        }
        return filterName1.compareToIgnoreCase(filterName2);
    }
}
