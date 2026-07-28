package com.bizzan.bitrade;

import com.bizzan.bitrade.domain.TestExportVO;
import com.bizzan.bitrade.util.FileUtil;
import com.bizzan.bitrade.util.MessageResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootTest
public class FileUtilTest {

    @Test
    public void testExportExcel() {
        // 1. 模拟请求/响应对象（Spring Mock工具）
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 2. 构造测试数据
        List<TestExportVO> exportList = new ArrayList<>();
        TestExportVO vo = new TestExportVO();
        vo.setOrderNo("TEST_001");
        vo.setAmount(new BigDecimal("999.99"));
        vo.setCreateTime(new Date());
        vo.setUserName("测试用户");
        exportList.add(vo);

        // 3. 调用导出方法
        FileUtil<TestExportVO> fileUtil = new FileUtil<>();
        MessageResult result = fileUtil.exportExcel(request, response, exportList, "单元测试导出");

        // 4. 验证结果
        assert result.getCode() == 0; // 导出成功
        assert response.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assert response.getContentLength() > 0; // 响应流非空
        System.out.println("单元测试导出成功，响应长度：" + response.getContentLength());
    }

}
