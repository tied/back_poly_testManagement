package com.thed.zephyr.util;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.thed.zephyr.je.rest.exception.ImporterException;
import com.thed.zephyr.je.vo.ImportJob;
import com.thed.zephyr.je.vo.ImportJobHistory;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class ExcelFileUtil {

	private static final Logger log = LoggerFactory.getLogger(ExcelFileUtil.class);

	public ExcelFileUtil() {
	}

	public static Map<String, String> getColumnHeaderMap(Sheet sheet, int rowNum){
		Map<String, String> columnHeaderMap = new LinkedHashMap<>();
//		int fRowNum = sheet.getFirstRowNum();
//		int lRowNum = sheet.getLastRowNum();
//		for(int i=fRowNum; i<lRowNum+1; i++)
//		{
//
//			if(i == rowNum)
//			{
//				Row row = sheet.getRow(i);
//				if(row != null){
//					int fCellNum = row.getFirstCellNum();
//					int lCellNum = row.getLastCellNum();
//
//					/* Loop in cells, add each cell value to the list.*/
//
//					for(int j = fCellNum; j <= lCellNum; j++)
//					{
//						if(row.getCell(j) == null){ break;}
//						String cValue = row.getCell(j).getRichStringCellValue().getString();
//						//columnHeaderMap.put(CellReference.convertNumToColString(j), cValue);
//						columnHeaderMap.put(cValue, CellReference.convertNumToColString(j));
//					}
//				}
//			}
//		}
		final Integer[] columnNo = {0};
		Row row = sheet.getRow(rowNum);
		if(row == null) return null;

		row.forEach(cell -> {
			String fieldName = cell.getRichStringCellValue().toString();
			String fieldColumn = CellReference.convertNumToColString(columnNo[0]);
			if(StringUtils.isNotEmpty(fieldName)) {
				columnHeaderMap.put(fieldName, fieldColumn);
			}
			columnNo[0]++;
		});

		return columnHeaderMap;
	}



	public static void validateImportFile(final JiraAuthenticationContext authContext, List<File> filesList) throws ImporterException {
		File file = filesList.get(0);//only work with 1st file
		if(file == null || file.length() == 0){
			throw new ImporterException(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.file.empty.error"));
		}
		else if(file.length() > ApplicationConstants.IMPORTER_MAX_UPLOAD_FILE_SIZE*1024*1024){//check if file size is less than 5mb
			throw new ImporterException(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.file.size.error"));
		}
	}


	/**
	 * Get list of sheet from excel file.
	 * @param file
	 * @return List of Excel Sheets
	 */
	public List<Sheet> getAllSheetsByFile(ImportJob importJob, File file) {
		List<Sheet> sheets = new ArrayList<>();
//		InputStream fis = null;
		Workbook wb = null;
		try {
//			fis = FileUtils.openInputStream(file);
//			wb 	= WorkbookFactory.create(fis);
			wb 	= WorkbookFactory.create(file);
			Iterator<Sheet> sheetIterator = wb.sheetIterator();
			String sheetFilter = importJob.getImportDetails().getSheetFilter();
			Boolean filter = StringUtils.isNotEmpty(sheetFilter);

			while(sheetIterator.hasNext()){
				Sheet sheet = sheetIterator.next();
				boolean match = false;
				if(filter) match = Pattern.matches(sheetFilter, sheet.getSheetName());

				if(!filter) {
					sheets.add(sheet);
				}else if(match){
					sheets.add(sheet);
				}
			}

			if (StringUtils.isEmpty(importJob.getImportDetails().getSheetFilter())
					&& sheets != null && sheets.size() > 0){ //non selected filter
				return new ArrayList<>(Arrays.asList(sheets.get(0)));
			}
		} catch (Exception ex) {
			log.error("Error during convert file >> input stream >> sheet " + ex.getMessage());
		} finally {
			//close workbook
			try {
				if (wb != null) {
					wb.close();
				}
			} catch (IOException e) {
				log.warn("Exception in closing file work book process", e);
				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, ApplicationConstants.IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
			}
			//close input stream
//			try {
//				if (null != fis) {
//					fis.close();
//				}
//			} catch (Exception e) {
//				log.warn("Exception in closing file input stream process", e);
//				ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, ApplicationConstants.IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
//			}
		}
		return sheets;
	}

		public static List<String> getRowData(Sheet sheet, int rowNum) {
		List<String> rowData = new ArrayList<String>();
		int fRowNum = sheet.getFirstRowNum();
		int lRowNum = sheet.getLastRowNum();
		for(int i=fRowNum; i<lRowNum+1; i++)
		{
			/* Only get desired row data. */
			if(i == rowNum)
			{
				Row row = sheet.getRow(i);

				int fCellNum = row.getFirstCellNum();
				int lCellNum = row.getLastCellNum();

				/* Loop in cells, add each cell value to the list.*/
				
				for(int j = fCellNum; j < lCellNum; j++)
				{
					String cValue = "";
					if(row.getCell(j) != null){
						cValue = row.getCell(j).getStringCellValue();
					}
					rowData.add(cValue);
				}

			}
		}

		return rowData;
	}

	public static List<List<String>> getExcelData(Sheet sheet, int startRow, int endRow)
	{
		List<List<String>> ret = new ArrayList<List<String>>();
		int fRowNum = sheet.getFirstRowNum();
		int lRowNum = sheet.getLastRowNum();

		/*  First row is excel file header, so read data from row next to it. */
		for(int i=fRowNum+1; i<lRowNum+1; i++)
		{
			/* Only get desired row data. */
			if(i>=startRow && i<=endRow)
			{
				Row row = sheet.getRow(i);

				int fCellNum = row.getFirstCellNum();
				int lCellNum = row.getLastCellNum();

				/* Loop in cells, add each cell value to the list.*/
				List<String> rowDataList = new ArrayList<String>();
				for(int j = fCellNum; j < lCellNum; j++)
				{
					String cValue = getCellValue(row.getCell(j));
					rowDataList.add(cValue);
				}

				ret.add(rowDataList);
			}
		}

		return ret;
	}

	/* Create a new excel sheet with data. 
	 * excelFilePath :  The exist excel file need to create new sheet.
	 * dataList : Contains all the data that need save to the new sheet.
	 * */
	public static void populateExcelSheetWithData(Sheet sheet, List<String> headerdata,  List<List<String>> dataList)
	{

		/* Create header row. */
		Row headerRow = sheet.createRow(0);
		int index =0;
		for(String cellValue : headerdata) {
			headerRow.createCell(index).setCellValue(cellValue);
			index++;
		}
		/* Loop in the row data list, add each row data into the new sheet. */
		if(dataList!=null)
		{
			int size = dataList.size();
			for(int i=0;i<size;i++)
			{
				List<String> cellDataList = dataList.get(i);

				/* Create row to save the copied data. */
				Row row = sheet.createRow(i+1);

				int columnNumber = cellDataList.size();

				for(int j=0;j<columnNumber;j++)
				{
					String cellValue = cellDataList.get(j);
					row.createCell(j).setCellValue(cellValue);
				}
			}
		}


	}

	public static Cell getCell(int[] cellRef, Sheet sheet, Row currentRow) {
		if (cellRef == null) {
			return null;
		}
		if (cellRef[1] == -1) {
			return currentRow.getCell(cellRef[0]);
		} else {
			return sheet.getRow(cellRef[1]).getCell(cellRef[0]);
		}
	}

	public static String getCellValue(Cell originalCell) {
		if(originalCell == null) return null;

		if (originalCell != null && originalCell.getCellType() != Cell.CELL_TYPE_BLANK) {

			DataFormatter formatter = new DataFormatter();
			return formatter.formatCellValue(originalCell);
//		if (originalCell != null && evaluator != null
//				&& originalCell.getCellType() != Cell.CELL_TYPE_BLANK) {
//			CellValue cell = evaluator.evaluate(originalCell);
//			switch (originalCell.getCellType()) {
//			case Cell.CELL_TYPE_NUMERIC:
//				Double val = Double.valueOf(cell.getNumberValue());
//				if ((val - val.longValue()) == 0) {
//					return String.valueOf(val.longValue());
//				} else {
//					return String.valueOf(val);
//				}
//			case Cell.CELL_TYPE_STRING:
//				return cell.getStringValue();
//			case Cell.CELL_TYPE_BOOLEAN:
//				return String.valueOf(cell.getBooleanValue());
//			case Cell.CELL_TYPE_ERROR:
//				return String.valueOf(cell.getErrorValue());
//			case Cell.CELL_TYPE_FORMULA: // This should never be called
//				// return cell.getCellFormula();
//			default:
//				return null;
//			}
		}
		return null;
	}

	public static String getCellValue(String cellMapping, Sheet sheet, Row currentRow) {

		String staticText = convertToStatic(cellMapping);
		if (staticText != null) {
			return staticText;// allow the cell mapping to contain a static value between inverted commas
		}
		int[] cellRef = convertField(cellMapping);

		if (cellRef != null ) {
			Cell originalCell = getCell(cellRef, sheet, currentRow);
			return getCellValue(originalCell);

		} else {
			return null; 
		}
	}
	public static boolean isRowNull(Row row) {
		if (row == null) {
			return true;
		}
		if (row.getLastCellNum() <= 0) {
			return true;
		}
		for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
			Cell cell = row.getCell(cellNum);
			if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK && StringUtils.isNotBlank(cell.toString())) {
				return false;
			}
		}
		return true;
	}

	public static Workbook createWorkBook(Workbook wb) {
		if (wb instanceof HSSFWorkbook) {
			return new HSSFWorkbook();
		} else if (wb instanceof XSSFWorkbook) {
			return new XSSFWorkbook();
		}
		throw new IllegalArgumentException("Unknown workbook type");
	}

	public static File saveWorkbook(Sheet sheet, File file, Workbook newWb) throws IOException {
		//String name = file.getName();

		File tempFile = new File(file.getParentFile().getPath(), sheet.getSheetName() + ".xls");
		//File tempFile = new File("test.xls");
		FileOutputStream os = new FileOutputStream(tempFile);
		try {
			newWb.write(os);
		} finally {
			os.close();
		}
		return tempFile;
	}
	public static int[] convertField(String fieldRef) {

		try {
			if (StringUtils.isEmpty(fieldRef)) {
				return null;
			}
			// Handle reference to cell by returning [colIndex, rowIndex]
			CellReference cellReference = new CellReference(fieldRef);
			return new int[] { cellReference.getCol(), cellReference.getRow() };
		} catch (Exception iae) {
            log.warn("Exception during excel field value conversion" + iae);
			// Handle reference to column only by returning [colIndex, -1]
			int colIndex = CellReference.convertColStringToIndex(fieldRef);
			if (colIndex < 0) {
				return null; // return null so validation works
			}
			return new int[] { colIndex, -1 };
		}

	}

	public static String convertToStatic(String cellMapping) {
		if (isStaticMapping(cellMapping)) {
			return StringUtils.substringBetween(cellMapping, "\"");
		}
		return null;
	}

	public static boolean isStaticMapping(String cellMapping) {
		return StringUtils.startsWith(cellMapping, "\"") 
				&& StringUtils.endsWith(cellMapping, "\"");
	}

	/**
	 * It check if there is LOV in the picklist of custom field , it there is it put them in hasp map 
	 * if no value is found it return 
	 * @param prefKey
	 * @param map
	 */
	public static void populateHashMapFromPreference(String prefKey, Map<String, String> map){
		String[] nameValues = prefKey.split(";");
		synchronized (map) {
			for(String entry : nameValues){
				String []nameValue = entry.split("=");
				if(nameValue.length == 2)
					map.put(nameValue[0], nameValue[1]);
			}
		}
	}

	public HSSFWorkbook mergeExcelFiles(HSSFWorkbook book,
			ArrayList<FileInputStream> inList) throws IOException {

		for (FileInputStream fin : inList) {
			HSSFWorkbook b = new HSSFWorkbook(fin);
			for (int i = 0; i < b.getNumberOfSheets(); i++) {
				// not entering sheet name, because of duplicated names
				copySheets(book.createSheet(), b.getSheetAt(i));
			}
		}
		return book;
	}

	/**
	 * @param newSheet
	 *          the sheet to create from the copy.
	 * @param sheet
	 *          the sheet to copy.
	 */
	public static void copySheets(Sheet newSheet, Sheet sheet) {
		copySheets(newSheet, sheet, true);
	}

	/**
	 * @param newSheet
	 *          the sheet to create from the copy.
	 * @param sheet
	 *          the sheet to copy.
	 * @param copyStyle
	 *          true copy the style.
	 */
	public static void copySheets(Sheet newSheet, Sheet sheet, boolean copyStyle) {
		int maxColumnNum = 0;
		Map<Integer, CellStyle> styleMap = (copyStyle) ? new HashMap<Integer, CellStyle>()
				: null;
		for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
			Row srcRow = sheet.getRow(i);
			Row destRow = newSheet.createRow(i);
			if (srcRow != null) {
				copyRow(sheet, newSheet, srcRow, destRow, styleMap);
				if (srcRow.getLastCellNum() > maxColumnNum) {
					maxColumnNum = srcRow.getLastCellNum();
				}
			}
		}
		for (int i = 0; i <= maxColumnNum; i++) {
			newSheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i), 256*255));
		}
	}

	/**
	 * @param srcSheet
	 *          the sheet to copy.
	 * @param destSheet
	 *          the sheet to create.
	 * @param srcRow
	 *          the row to copy.
	 * @param destRow
	 *          the row to create.
	 * @param styleMap
	 *          -
	 */
	public static void copyRow(Sheet srcSheet, Sheet destSheet, Row srcRow,
			Row destRow, Map<Integer, CellStyle> styleMap) {
		// manage a list of merged zone in order to not insert two times a merged
		// zone
		Set<CellRangeAddressWrapper> mergedRegions = new TreeSet<CellRangeAddressWrapper>();
		destRow.setHeight(srcRow.getHeight());
		// reckoning delta rows
		int deltaRows = destRow.getRowNum() - srcRow.getRowNum();
		// pour chaque row
		if (srcRow.getFirstCellNum() >= 0) {
			for (int j = srcRow.getFirstCellNum(); j <= srcRow.getLastCellNum(); j++) {
				Cell oldCell = srcRow.getCell(j); // ancienne cell
				Cell newCell = destRow.getCell(j); // new cell
				if (oldCell != null) {
					if (newCell == null) {
						newCell = destRow.createCell(j);
					}
					// copy chaque cell
					copyCell(oldCell, newCell, styleMap);
					// copy les informations de fusion entre les cellules
					// System.out.println("row num: " + srcRow.getRowNum() + " , col: " +
					// (short)oldCell.getColumnIndex());
					CellRangeAddress mergedRegion = getMergedRegion(srcSheet,
							srcRow.getRowNum(), (short) oldCell.getColumnIndex());

					if (mergedRegion != null) {
						// System.out.println("Selected merged region: " +
						// mergedRegion.toString());
						CellRangeAddress newMergedRegion = new CellRangeAddress(
								mergedRegion.getFirstRow() + deltaRows, mergedRegion.getLastRow()
								+ deltaRows, mergedRegion.getFirstColumn(),
								mergedRegion.getLastColumn());
						// System.out.println("New merged region: " +
						// newMergedRegion.toString());
						CellRangeAddressWrapper wrapper = new CellRangeAddressWrapper(
								newMergedRegion);
						if (isNewMergedRegion(wrapper, mergedRegions)) {
							mergedRegions.add(wrapper);
							destSheet.addMergedRegion(wrapper.range);
						}
					}
				}
			}
		}
	}

	/**
	 * @param oldCell
	 * @param newCell
	 * @param styleMap
	 */
	public static void copyCell(Cell oldCell, Cell newCell,
			Map<Integer, CellStyle> styleMap) {
		if (styleMap != null) {
			if (oldCell.getSheet().getWorkbook() == newCell.getSheet().getWorkbook()) {
				newCell.setCellStyle(oldCell.getCellStyle());
			} else {
				int stHashCode = oldCell.getCellStyle().hashCode();
				CellStyle newCellStyle = styleMap.get(stHashCode);
				if (newCellStyle == null) {
					newCellStyle = newCell.getSheet().getWorkbook().createCellStyle();
					newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
					styleMap.put(stHashCode, newCellStyle);
				}
				newCell.setCellStyle(newCellStyle);
			}
		}
		switch (oldCell.getCellType()) {
		case HSSFCell.CELL_TYPE_STRING:
			newCell.setCellValue(oldCell.getStringCellValue());
			break;
		case HSSFCell.CELL_TYPE_NUMERIC:
			newCell.setCellValue(oldCell.getNumericCellValue());
			break;
		case HSSFCell.CELL_TYPE_BLANK:
			newCell.setCellType(HSSFCell.CELL_TYPE_BLANK);
			break;
		case HSSFCell.CELL_TYPE_BOOLEAN:
			newCell.setCellValue(oldCell.getBooleanCellValue());
			break;
		case HSSFCell.CELL_TYPE_ERROR:
			newCell.setCellErrorValue(oldCell.getErrorCellValue());
			break;
		case HSSFCell.CELL_TYPE_FORMULA:
			newCell.setCellFormula(oldCell.getCellFormula());
			break;
		default:
			break;
		}

	}

	/**
	 * Récupère les informations de fusion des cellules dans la sheet source pour
	 * les appliquer à la sheet destination... Récupère toutes les zones merged
	 * dans la sheet source et regarde pour chacune d'elle si elle se trouve dans
	 * la current row que nous traitons. Si oui, retourne l'objet
	 * CellRangeAddress.
	 * 
	 * @param sheet
	 *          the sheet containing the data.
	 * @param rowNum
	 *          the num of the row to copy.
	 * @param cellNum
	 *          the num of the cell to copy.
	 * @return the CellRangeAddress created.
	 */
	public static CellRangeAddress getMergedRegion(Sheet sheet, int rowNum,
			short cellNum) {
		for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
			CellRangeAddress merged = sheet.getMergedRegion(i);
			if (merged.isInRange(rowNum, cellNum)) {
				return merged;
			}
		}
		return null;
	}

	/**
	 * Check that the merged region has been created in the destination sheet.
	 * 
	 * @param newMergedRegion
	 *          the merged region to copy or not in the destination sheet.
	 * @param mergedRegions
	 *          the list containing all the merged region.
	 * @return true if the merged region is already in the list or not.
	 */
	private static boolean isNewMergedRegion(
			CellRangeAddressWrapper newMergedRegion,
			Set<CellRangeAddressWrapper> mergedRegions) {
		return !mergedRegions.contains(newMergedRegion);
	}

	public static void addJobHistory(ImportJob importJob, String comment) {
		if (importJob.getHistory() == null) {
			//throw new IllegalStateException("Histories must not be null");
			importJob.setHistory(new LinkedHashSet<>());
		}
		ImportJobHistory jobHistory;
		jobHistory = new ImportJobHistory();
		jobHistory.setActionDate(new Date());
		jobHistory.setComments(comment);
		importJob.getHistory().add(jobHistory);
	}


	public static void addJobHistoryAndUpdateStatus(ImportJob importJob, String status, String msg) {
		addJobHistory(importJob, msg);
	}

}

class CellRangeAddressWrapper implements Comparable<CellRangeAddressWrapper> {

	public CellRangeAddress range;

	/**
	 * @param theRange
	 *          the CellRangeAddress object to wrap.
	 */
	public CellRangeAddressWrapper(CellRangeAddress theRange) {
		this.range = theRange;
	}

	/**
	 * @param o
	 *          the object to compare.
	 * @return -1 the current instance is prior to the object in parameter, 0:
	 *         equal, 1: after...
	 */
	public int compareTo(CellRangeAddressWrapper o) {

		if (range.getFirstColumn() < o.range.getFirstColumn()
				&& range.getFirstRow() < o.range.getFirstRow()) {
			return -1;
		} else if (range.getFirstColumn() == o.range.getFirstColumn()
				&& range.getFirstRow() == o.range.getFirstRow()) {
			return 0;
		} else {
			return 1;
		}

	}

}
