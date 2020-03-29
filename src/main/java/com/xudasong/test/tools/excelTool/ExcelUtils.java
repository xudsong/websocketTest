package com.xudasong.test.tools.excelTool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

@Slf4j
public class ExcelUtils {

    private static final String EXCEL_EXTENSION = ".xls";

    public static void exportExcelWithResponse(String title, TableHeader[] headers, Object[] data, HttpServletResponse response){
        Workbook wb = buildSingleExcel(title,headers,data);
        writeExcel2Response(title,wb,response);
    }

    public static void export(String title,TableHeader[] headers,Object[] data,HttpServletResponse response){
        long startTime = System.currentTimeMillis();
        exportExcelWithResponse(title,headers,data,response);
        long endTime = System.currentTimeMillis();
        log.info("finish to export,spend time: " + (endTime - startTime));
    }

    private static void writeExcel2Response(String title,Workbook wb,HttpServletResponse response){
        try (OutputStream outputStream = response.getOutputStream()){
            response.reset();
            String codedFieName = URLEncoder.encode(title,"UTF-8");
            response.setHeader("Content-Disposition","attachment: filename="+codedFieName+"_"+DateUtils.getCurrentDayStr()+EXCEL_EXTENSION);
            response.setContentType("application/vnd.ms-excel");
            wb.write(outputStream);
        }catch (IOException e){
            log.error("导出表格失败",e);
        }
    }

    private static Workbook buildSingleExcel(String title,TableHeader[] headers,Object[] data){
        Workbook wb = new HSSFWorkbook();
        Sheet sheet = createSheet(wb,title);
        int headerIndex = 0;
        createTableHeader(wb,sheet,headers,headerIndex);
        int contentStartIndex = headerIndex + 1;
        createTableContent(sheet,data,headers,contentStartIndex);
        return wb;
    }

    private static Sheet createSheet(Workbook wb,String title){
        Sheet sheet = wb.createSheet(title);
        sheet.setDefaultColumnWidth(25);
        sheet.setDefaultRowHeightInPoints(25);
        return sheet;
    }

    private static void createTableHeader(Workbook wb,Sheet sheet,TableHeader[] headers, int headerIndex){
        Row headerRow = sheet.createRow(headerIndex);
        for (int i=0;i<headers.length;i++){
            Cell cell = headerRow.createCell(i);
            cell.setCellStyle(createHeaderStyle(wb));
            cell.setCellValue(headers[i].getName());
        }
    }

    private static CellStyle createHeaderStyle(Workbook wb){
        CellStyle titleStyle = wb.createCellStyle();
        titleStyle.setWrapText(true);
        titleStyle.setAlignment(HorizontalAlignment.LEFT);
        Font font = wb.createFont();
        font.setBold(true);
        titleStyle.setFont(font);
        titleStyle.setLocked(true);
        return titleStyle;
    }

    private static void createTableContent(Sheet sheet,Object[] data,TableHeader[] headers, int contentStartIndex){
        if (data == null || data.length == 0 ){
            return;
        }
        for (int i=0;i<data.length;i++){
            Row newRow = sheet.createRow(i + contentStartIndex);
            JSONObject jsonObject = (JSONObject) JSON.toJSON(data[i]);
            createRowCell(headers,jsonObject,newRow);
        }
    }

    private static void createRowCell(TableHeader[] headers,JSONObject jsonObject,Row row){
        for (int i=0;i<headers.length;i++){
            String key = headers[i].getKey();
            Cell cell = row.createCell(i);
            if (headers[i].getHandler() != null){
                cell.setCellValue(headers[i].getHandler().handle(jsonObject.get(key),jsonObject));
            }else {
                cell.setCellValue(jsonObject.getString(key));
            }
        }
    }

}
