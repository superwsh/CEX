package com.bizzan.bitrade.util;

import com.bizzan.bitrade.annotation.Excel;
import com.bizzan.bitrade.annotation.ExcelSheet;
import com.bizzan.bitrade.vo.OtcOrderVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Spring Boot 3.x适配的Excel工具类（基于Apache POI重构）
 * 替换原jxl实现，支持.xlsx（无行数限制）/.xls格式，保留原核心逻辑
 */
@Slf4j
public class ExcelUtil {
    // Excel 2007+ (.xlsx) 单sheet最大行数（POI无硬限制，此处保留原逻辑）
    private static final int SHEET_SIZE = 1048575;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出Excel（注解驱动，支持分sheet）
     * 原listToExcel方法重构，替换jxl为POI
     */
    public static <T> void listToExcel(List<T> list, Field[] fields, OutputStream out) throws RuntimeException {
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("数据源中没有任何数据");
        }

        // 创建XSSFWorkbook（.xlsx格式），兼容Spring Boot3.x
        Workbook workbook = new XSSFWorkbook();
        try {
            // 计算需要的sheet数量
            double sheetNum = Math.ceil(list.size() / (double) SHEET_SIZE);
            String sheetBaseName = OtcOrderVO.class.getAnnotation(ExcelSheet.class).name();

            // 分sheet填充数据
            for (int i = 0; i < sheetNum; i++) {
                Sheet sheet = workbook.createSheet(sheetBaseName + (sheetNum > 1 ? (i + 1) : ""));
                int firstIndex = i * SHEET_SIZE;
                int lastIndex = (i + 1) * SHEET_SIZE - 1 > list.size() - 1 ? list.size() - 1 : (i + 1) * SHEET_SIZE - 1;
                fillSheet(sheet, list, fields, firstIndex, lastIndex);
            }

            // 写入输出流
            workbook.write(out);
        } catch (Exception e) {
            log.error("Excel导出失败", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException("导出Excel失败：" + e.getMessage());
            }
        } finally {
            try {
                workbook.close();
                out.close();
            } catch (Exception e) {
                log.error("关闭流失败", e);
            }
        }
    }

    /**
     * Excel导入为List（保留原重复行校验、反射赋值逻辑）
     */
    public static <T> List<T> excelToList(
            InputStream in,
            String sheetName,
            Class<T> entityClass,
            LinkedHashMap<String, String> fieldMap,
            String[] uniqueFields
    ) throws RuntimeException {
        List<T> resultList = new ArrayList<>();
        Workbook workbook = null;

        try {
            // 自动识别.xls/.xlsx格式
            workbook = WorkbookFactory.create(in);
            Sheet sheet = sheetName == null ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new RuntimeException("工作表[" + sheetName + "]不存在");
            }

            // 获取有效行数（过滤空行）
            int realRows = getRealRowCount(sheet);
            if (realRows <= 1) {
                throw new RuntimeException("Excel文件中没有任何数据");
            }

            // 获取表头行
            Row headerRow = sheet.getRow(0);
            String[] excelFieldNames = getExcelFieldNames(headerRow);

            // 校验必要字段是否存在
            validateExcelFields(excelFieldNames, fieldMap.keySet());

            // 列名->列号映射
            LinkedHashMap<String, Integer> colMap = buildColumnMap(headerRow, excelFieldNames);

            // 校验重复行（复合主键）
            validateUniqueRows(sheet, realRows, colMap, uniqueFields);

            // 反射填充数据
            resultList = fillDataFromSheet(sheet, realRows, colMap, fieldMap, entityClass);

        } catch (Exception e) {
            log.error("Excel导入失败", e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException("导入Excel失败：" + e.getMessage());
            }
        } finally {
            try {
                if (workbook != null) workbook.close();
                in.close();
            } catch (Exception e) {
                log.error("关闭流失败", e);
            }
        }
        return resultList;
    }

    // ===================== 私有辅助方法（重构核心） =====================
    /**
     * 获取有效行数（过滤全空行）
     */
    private static int getRealRowCount(Sheet sheet) {
        int realRows = 0;
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            int nullCols = 0;
            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                if (cell == null || getCellValue(cell).trim().isEmpty()) {
                    nullCols++;
                }
            }
            if (nullCols != row.getLastCellNum()) {
                realRows++;
            } else {
                break; // 连续空行，终止计数
            }
        }
        return realRows;
    }

    /**
     * 获取Excel表头字段名
     */
    private static String[] getExcelFieldNames(Row headerRow) {
        List<String> fieldNames = new ArrayList<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            fieldNames.add(cell == null ? "" : getCellValue(cell).trim());
        }
        return fieldNames.toArray(new String[0]);
    }

    /**
     * 校验Excel是否包含必要字段
     */
    private static void validateExcelFields(String[] excelFieldNames, Set<String> requiredFields) throws RuntimeException {
        List<String> excelFieldList = Arrays.asList(excelFieldNames);
        for (String cnName : requiredFields) {
            if (!excelFieldList.contains(cnName)) {
                throw new RuntimeException("Excel中缺少必要字段：" + cnName);
            }
        }
    }

    /**
     * 构建列名->列号映射
     */
    private static LinkedHashMap<String, Integer> buildColumnMap(Row headerRow, String[] excelFieldNames) {
        LinkedHashMap<String, Integer> colMap = new LinkedHashMap<>();
        for (int i = 0; i < excelFieldNames.length; i++) {
            colMap.put(excelFieldNames[i], headerRow.getCell(i).getColumnIndex());
        }
        return colMap;
    }

    /**
     * 校验复合主键重复行
     */
    private static void validateUniqueRows(Sheet sheet, int realRows, LinkedHashMap<String, Integer> colMap, String[] uniqueFields) throws RuntimeException {
        if (uniqueFields == null || uniqueFields.length == 0) return;

        Set<String> uniqueKeySet = new HashSet<>();
        for (int i = 1; i < realRows; i++) {
            Row row = sheet.getRow(i);
            StringBuilder key = new StringBuilder();
            for (String field : uniqueFields) {
                int col = colMap.get(field);
                Cell cell = row.getCell(col);
                key.append(getCellValue(cell)).append("_");
            }
            String uniqueKey = key.toString();
            if (uniqueKeySet.contains(uniqueKey)) {
                throw new RuntimeException("Excel第" + (i + 1) + "行与已有行重复（复合主键冲突）");
            }
            uniqueKeySet.add(uniqueKey);
        }
    }

    /**
     * 从Sheet反射填充数据到List
     */
    private static <T> List<T> fillDataFromSheet(Sheet sheet, int realRows, LinkedHashMap<String, Integer> colMap,
                                                 LinkedHashMap<String, String> fieldMap, Class<T> entityClass) throws Exception {
        List<T> resultList = new ArrayList<>();
        for (int i = 1; i < realRows; i++) {
            Row row = sheet.getRow(i);
            T entity = entityClass.getDeclaredConstructor().newInstance(); // JDK17+兼容的实例化方式

            for (Map.Entry<String, String> entry : fieldMap.entrySet()) {
                String cnName = entry.getKey();
                String enName = entry.getValue();
                int col = colMap.get(cnName);
                Cell cell = row.getCell(col);
                String content = cell == null ? "" : getCellValue(cell).trim();

                setFieldValueByName(enName, content, entity);
            }
            resultList.add(entity);
        }
        return resultList;
    }

    /**
     * 填充Sheet数据（注解驱动）
     */
    private static <T> void fillSheet(Sheet sheet, List<T> list, Field[] fields, int firstIndex, int lastIndex) throws Exception {
        // 填充表头
        int colIndex = 0;
        for (Field field : fields) {
            Excel excelAnno = field.getAnnotation(Excel.class);
            if (excelAnno == null) continue;

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) headerRow = sheet.createRow(0);
            Cell cell = headerRow.createCell(colIndex);
            cell.setCellValue(excelAnno.name());
            colIndex++;
        }

        // 填充数据行
        int rowNo = 1;
        for (int index = firstIndex; index <= lastIndex; index++) {
            T item = list.get(index);
            Row dataRow = sheet.createRow(rowNo);
            colIndex = 0;

            for (Field field : fields) {
                Excel excelAnno = field.getAnnotation(Excel.class);
                if (excelAnno == null) continue;

                Object objValue = getFieldValueByNameSequence(field.getName(), item);
                String fieldValue = objValue == null ? "" : objValue.toString();
                Cell cell = dataRow.createCell(colIndex);
                cell.setCellValue(fieldValue);
                colIndex++;
            }
            rowNo++;
        }

        // 自动调整列宽
        autoSizeColumns(sheet, 5);
    }

    /**
     * 自动调整列宽
     */
    private static void autoSizeColumns(Sheet sheet, int extraWidth) {
        for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + extraWidth * 256); // POI列宽单位是1/256个字符
        }
    }

    /**
     * 获取单元格值（统一处理不同类型单元格）
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DATE_FORMAT.format(cell.getDateCellValue());
                } else {
                    // 处理数字型单元格（避免科学计数法）
                    return String.valueOf(cell.getNumericCellValue()).replaceAll("\\.0$", "");
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    // ===================== 原反射方法（兼容JDK17+） =====================
    private static Object getFieldValueByName(String fieldName, Object o) throws Exception {
        Field field = getFieldByName(fieldName, o.getClass());
        if (field == null) {
            throw new RuntimeException(o.getClass().getSimpleName() + "类不存在字段：" + fieldName);
        }
        field.setAccessible(true);
        return field.get(o);
    }

    private static Field getFieldByName(String fieldName, Class<?> clazz) {
        // 先查当前类
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        // 递归查父类
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null && superClazz != Object.class) {
            return getFieldByName(fieldName, superClazz);
        }
        return null;
    }

    private static Object getFieldValueByNameSequence(String fieldNameSequence, Object o) throws Exception {
        String[] attributes = fieldNameSequence.split("\\.");
        if (attributes.length == 1) {
            return getFieldValueByName(fieldNameSequence, o);
        } else {
            Object fieldObj = getFieldValueByName(attributes[0], o);
            String subField = fieldNameSequence.substring(fieldNameSequence.indexOf(".") + 1);
            return getFieldValueByNameSequence(subField, fieldObj);
        }
    }

    private static void setFieldValueByName(String fieldName, Object fieldValue, Object o) throws Exception {
        Field field = getFieldByName(fieldName, o.getClass());
        if (field == null) {
            throw new RuntimeException(o.getClass().getSimpleName() + "类不存在字段：" + fieldName);
        }
        field.setAccessible(true);
        Class<?> fieldType = field.getType();

        // JDK17+兼容的类型转换
        if (String.class == fieldType) {
            field.set(o, String.valueOf(fieldValue));
        } else if (Integer.TYPE == fieldType || Integer.class == fieldType) {
            field.set(o, fieldValue.toString().isEmpty() ? 0 : Integer.parseInt(fieldValue.toString()));
        } else if (Long.TYPE == fieldType || Long.class == fieldType) {
            field.set(o, fieldValue.toString().isEmpty() ? 0L : Long.parseLong(fieldValue.toString()));
        } else if (Double.TYPE == fieldType || Double.class == fieldType) {
            field.set(o, fieldValue.toString().isEmpty() ? 0.0 : Double.parseDouble(fieldValue.toString()));
        } else if (Date.class == fieldType) {
            field.set(o, fieldValue.toString().isEmpty() ? null : DATE_FORMAT.parse(fieldValue.toString()));
        } else {
            field.set(o, fieldValue);
        }
    }

    public static void main(String[] args) throws IOException {
        // 1. 构造测试数据
        List<TestOrderVO> dataList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            TestOrderVO order = new TestOrderVO();
            order.setOrderSn("ORDER_" + i);
            order.setMoney(new BigDecimal("100.00").add(new BigDecimal(i)));
            order.setCreateTime(new Date());
            order.setMemberName("测试用户" + i);
            order.setTempField("非导出字段" + i); // 验证注解过滤
            dataList.add(order);
        }

        // 2. 获取实体类字段（注解驱动）
        Field[] fields = TestOrderVO.class.getDeclaredFields();

        // 3. 导出到本地文件
        File exportFile = new File("test_export.xlsx");
        try (FileOutputStream out = new FileOutputStream(exportFile)) {
            ExcelUtil.listToExcel(dataList, fields, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 验证：文件是否生成且非空
        assert exportFile.exists() && exportFile.length() > 0;
        System.out.println("导出成功，文件路径：" + exportFile.getAbsolutePath());


        // 1. 准备导入文件（使用上面导出的test_export.xlsx）
//        File importFile = new File("test_export.xlsx");
//        // 2. 构建字段映射（中文列名->英文属性名）
//        LinkedHashMap<String, String> fieldMap = new LinkedHashMap<>();
//        fieldMap.put("订单编号", "orderSn");
//        fieldMap.put("交易金额", "money");
//        fieldMap.put("交易时间", "createTime");
//        fieldMap.put("用户名称", "memberName");
//
//        // 3. 复合主键（订单编号唯一）
//        String[] uniqueFields = {"订单编号"};
//
//        // 4. 执行导入
//        try (FileInputStream in = new FileInputStream(importFile)) {
//            List<TestOrderVO> resultList = ExcelUtil.excelToList(
//                    in,
//                    TestOrderVO.class.getAnnotation(ExcelSheet.class).name(),
//                    TestOrderVO.class,
//                    fieldMap,
//                    uniqueFields
//            );
//
//            // 验证：导入数据量是否匹配
//            assert resultList.size() == 100;
//            // 验证：第一条数据是否正确
//            TestOrderVO firstOrder = resultList.get(0);
//            assert firstOrder.getOrderSn().equals("ORDER_1");
//            assert firstOrder.getMoney().equals(new BigDecimal("101.00"));
//            assert firstOrder.getTempField() == null; // 非导入字段为空
//
//            System.out.println("导入成功，共导入" + resultList.size() + "条数据");
//            System.out.println("第一条数据：" + firstOrder);
//        }
    }
}

@Data
@ExcelSheet(name = "测试订单表")
class TestOrderVO {
    @Excel(name = "订单编号")
    private String orderSn;

    @Excel(name = "交易金额")
    private BigDecimal money;

    @Excel(name = "交易时间")
    private Date createTime;

    @Excel(name = "用户名称")
    private String memberName;

    // 非Excel字段（验证注解过滤）
    private String tempField;
}
