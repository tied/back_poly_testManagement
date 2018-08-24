package com.thed.zephyr.util;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.thed.zephyr.je.vo.ImportJob;

public class SheetIterator implements Iterator<Sheet> {

	public static Iterable<Sheet> create(final Workbook wb, final ImportJob importJob) {
		return new Iterable<Sheet>() {
			
			@Override
			public Iterator<Sheet> iterator() {
				return new SheetIterator(wb, importJob);
			}
		};
	}
	
	private Workbook wb;
	private ImportJob importJob;
	private int sheetNo = -1;
	private Sheet next;
	
	public SheetIterator(Workbook wb, ImportJob importJob) {
		this.wb = wb;
		this.importJob = importJob;
		next = getNext();
		
	}

	@Override
	public boolean hasNext() {
		
		return next != null;
	}
	
	@Override
	public Sheet next() {
		Sheet next = this.next;
		this.next = getNext();
		return next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
		
	}
	
	private Sheet getNext() {
		while(sheetNo + 1 < wb.getNumberOfSheets()) {
			sheetNo++;
			Sheet next = wb.getSheetAt(sheetNo);
			
			if (importJob != null && StringUtils.isNotEmpty(importJob.getImportDetails().getSheetFilter())) {
				if (skipSheet(next, importJob)) {
					continue;
				}
				
			} else {
				if ((sheetNo > 0 ) && (importJob == null
						|| (importJob != null && importJob.getImportDetails() != null && !importJob.getImportDetails().isImportAllSheetsFlag()))
						){
					break; // only process first sheet if sheet filter is not defined
				}
			}
			return next;
		} 
		return null;
	}
	
	private boolean skipSheet(Sheet sheet, ImportJob importJob) {
		if (importJob != null && StringUtils.isNotEmpty(importJob.getImportDetails().getSheetFilter())) {
			boolean match = Pattern.matches(importJob.getImportDetails().getSheetFilter(), sheet.getSheetName());
			return !match;
		}
		return false;
	}

}
