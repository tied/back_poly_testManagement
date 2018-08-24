package com.thed.zephyr.je.index;

import java.util.Comparator;
import java.util.Map;

@SuppressWarnings("rawtypes")
class ValueComparator implements Comparator {

	Map<Integer, Integer> defectMap;
	public ValueComparator(Map<Integer, Integer> defectMap) {
		this.defectMap = defectMap;
	}

	public int compare(Object a, Object b) {
		if((Integer)defectMap.get(a).intValue() < (Integer)defectMap.get(b).intValue()) {
			return 1;
		} else if((Integer)defectMap.get(a).intValue() == (Integer)defectMap.get(b).intValue()) {
			return 0;
		} else {
			return -1;
		}
	}
}
