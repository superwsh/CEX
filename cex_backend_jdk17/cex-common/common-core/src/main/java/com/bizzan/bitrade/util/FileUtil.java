package com.bizzan.bitrade.util;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FileUtil<E> {

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    /**
     * Excel导出（EasyPoi实现，Spring Boot3.x适配）
     * @param request  请求对象（用于处理浏览器兼容）
     * @param response 响应对象（输出Excel流）
     * @param list     导出数据列表
     * @param name     导出文件名（不含后缀）
     * @return 导出结果
     */
    public MessageResult exportExcel(HttpServletRequest request, HttpServletResponse response, List<E> list, String name) {
        // 1. 校验数据源
        if (list == null || list.isEmpty()) {
            log.warn("Excel导出失败：数据源为空");
            return MessageResult.error(-1, "没有数据");
        }

        // 2. 构建导出参数（EasyPoi核心）
        ExportParams exportParams = new ExportParams();
        // 可选：自定义Sheet名称（默认取实体类@ExcelSheet注解）
        exportParams.setSheetName("导出数据");

        Workbook workbook = null;
        OutputStream out = null;
        try {
            // 3. EasyPoi生成Workbook（核心API，无需修改）
            Class<?> clazz = list.get(0).getClass();
            workbook = ExcelExportUtil.exportExcel(exportParams, clazz, list);

            // 4. 设置响应头（解决文件名乱码+规范Content-Type）
            setExcelResponseHeader(request, response, name);

            // 5. 直接写入响应流（移除本地临时文件，提升性能）
            out = response.getOutputStream();
            workbook.write(out);
            out.flush();

            log.info("Excel导出成功，文件名：{}，数据量：{}", name, list.size());
            return MessageResult.success();

        } catch (Exception e) {
            log.error("Excel导出失败", e);
            return MessageResult.error(-2, "导出失败：" + e.getMessage());
        } finally {
            // 6. 强制释放资源（避免内存泄漏）
            closeResource(out, workbook);
        }

    }

    /**
     * 设置Excel响应头（解决不同浏览器文件名乱码）
     */
    private void setExcelResponseHeader(HttpServletRequest request, HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        // 处理文件名乱码（兼容IE/Chrome/Firefox）
        String userAgent = request.getHeader("User-Agent");
        String encodedFileName;
        if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
            // IE浏览器
            encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        } else {
            // 非IE浏览器
            encodedFileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        }

        // 统一设置响应头
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); // .xlsx格式
        response.setHeader("Content-Disposition", "attachment;filename=\"" + encodedFileName + ".xlsx\"");
        // 禁用缓存
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setDateHeader("Expires", 0);
    }

    /**
     * 关闭流和Workbook资源
     */
    private void closeResource(OutputStream out, Workbook workbook) {
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            log.error("关闭输出流失败", e);
        }
        try {
            if (workbook != null) {
                workbook.close();
            }
        } catch (IOException e) {
            log.error("关闭Workbook失败", e);
        }
    }


//    public  MessageResult exportExcel(HttpServletRequest request, HttpServletResponse response, List<E> list, String name) throws Exception{
//        if(list.isEmpty()){
//            return  MessageResult.error(-1,"没有数据");
//        }
//        String physicalPath = request.getSession().getServletContext().getRealPath("/")+"excel/";
//        String fileName = physicalPath+name+".xlsx";
//        File savefile = new File(physicalPath);
//        if (!savefile.exists()) {
//            savefile.mkdirs();
//        }
//        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), list.get(0).getClass(), list);
//        FileOutputStream fos = new FileOutputStream(fileName);
//        workbook.write(fos);
//        fos.close();
//        response.setContentType("multipart/form-data");
//        response.setHeader("Content-Disposition", "attachment;filename="+name+".xlsx");
//        response.setContentType("application/vnd.ms-excel;charset=utf-8");
//        OutputStream out = response.getOutputStream();
//        File file = new File(fileName);
//        InputStream in = new FileInputStream(file);
//        int data=in.read();
//        while(data!=-1){
//            out.write(data);
//            data=in.read();
//        }
//        in.close();
//        out.close();
//        file.delete();
//        return  MessageResult.success();
//    }
}
