package io.github.guojiaxing1995.easyJmeter.common.jmeter;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReportDataProcess 单元测试
 * 测试修复后的 overall 对象处理逻辑
 */
class ReportDataProcessTest {

    private ReportDataProcess reportDataProcess;

    @BeforeEach
    void setUp() {
        reportDataProcess = new ReportDataProcess();
    }

    @Test
    void testGetDashBoardData_WithOverallObject(@TempDir Path tempDir) throws IOException {
        // 创建测试用的 dashboard.js 文件
        File dashboardFile = tempDir.resolve("dashboard.js").toFile();
        
        // 模拟 JMeter 生成的 dashboard.js 内容
        String dashboardContent = "// Create statistics table\n" +
                "createTable($(\"#statisticsTable\"), {\n" +
                "    \"supportsControllersDiscrimination\": true,\n" +
                "    \"overall\": {\n" +
                "        \"data\": [\"Total\", 68, 0, 0.0, 506.88235294117635, 168, 2080, 249.0, 2040.8, 2063.6, 2080.0, 1.148939765143195, 0.392704021289178, 0.0],\n" +
                "        \"isController\": false\n" +
                "    },\n" +
                "    \"titles\": [\"Label\", \"#Samples\", \"FAIL\", \"Error %\", \"Average\", \"Min\", \"Max\", \"Median\", \"90th pct\", \"95th pct\", \"99th pct\", \"Transactions/s\", \"Received\", \"Sent\"],\n" +
                "    \"items\": [{\n" +
                "        \"data\": [\"gtp-nto_transfer\", 68, 0, 0.0, 506.88235294117635, 168, 2080, 249.0, 2040.8, 2063.6, 2080.0, 1.148939765143195, 0.392704021289178, 0.0],\n" +
                "        \"isController\": false\n" +
                "    }]\n" +
                "}, function(index, item){\n" +
                "    switch(index){\n" +
                "        case 3:\n" +
                "            item = item.toFixed(2) + '%';\n" +
                "            break;\n" +
                "        case 4:\n" +
                "        case 7:\n" +
                "        case 8:\n" +
                "        case 9:\n" +
                "        case 10:\n" +
                "        case 11:\n" +
                "        case 12:\n" +
                "        case 13:\n" +
                "            item = item.toFixed(2);\n" +
                "            break;\n" +
                "    }\n" +
                "    return item;\n" +
                "}, [[0, 0]], 0, summaryTableHeader);";

        try (FileWriter writer = new FileWriter(dashboardFile)) {
            writer.write(dashboardContent);
        }

        // 执行测试
        Map<String, List<JSONObject>> result = reportDataProcess.getDashBoardData(dashboardFile.getAbsolutePath());

        // 验证结果
        assertNotNull(result, "返回的 map 不应为 null");
        assertTrue(result.containsKey("statisticsTable"), "应包含 statisticsTable");
        
        List<JSONObject> statisticsTable = result.get("statisticsTable");
        assertNotNull(statisticsTable, "statisticsTable 不应为 null");
        assertEquals(2, statisticsTable.size(), "statisticsTable 应包含 2 条记录（Total + gtp-nto_transfer）");

        // 验证第一条记录是 Total
        JSONObject firstRecord = statisticsTable.get(0);
        assertNotNull(firstRecord, "第一条记录不应为 null");
        assertEquals("Total", firstRecord.getString("label"), "第一条记录的 label 应为 'Total'");
        assertEquals(68, firstRecord.getLongValue("samples"), "Total 记录的 samples 应为 68");
        assertEquals(0, firstRecord.getLongValue("fail"), "Total 记录的 fail 应为 0");
        assertEquals(0.0, firstRecord.getDoubleValue("error"), "Total 记录的 error 应为 0.0");

        // 验证第二条记录是 gtp-nto_transfer
        JSONObject secondRecord = statisticsTable.get(1);
        assertNotNull(secondRecord, "第二条记录不应为 null");
        assertEquals("gtp-nto_transfer", secondRecord.getString("label"), "第二条记录的 label 应为 'gtp-nto_transfer'");
        assertEquals(68, secondRecord.getLongValue("samples"), "gtp-nto_transfer 记录的 samples 应为 68");
    }

    @Test
    void testGetDashBoardData_WithHTTPRequest(@TempDir Path tempDir) throws IOException {
        // 创建测试用的 dashboard.js 文件（HTTP Request 场景）
        File dashboardFile = tempDir.resolve("dashboard.js").toFile();
        
        String dashboardContent = "createTable($(\"#statisticsTable\"), {\n" +
                "    \"supportsControllersDiscrimination\": true,\n" +
                "    \"overall\": {\n" +
                "        \"data\": [\"Total\", 92347, 0, 0.0, 6.372356438216705, 2, 80, 5.0, 10.0, 12.0, 21.0, 1541.0944044857567, 502.2277938980024, 239.29102569651886],\n" +
                "        \"isController\": false\n" +
                "    },\n" +
                "    \"titles\": [\"Label\", \"#Samples\", \"FAIL\", \"Error %\", \"Average\", \"Min\", \"Max\", \"Median\", \"90th pct\", \"95th pct\", \"99th pct\", \"Transactions/s\", \"Received\", \"Sent\"],\n" +
                "    \"items\": [{\n" +
                "        \"data\": [\"HTTP Request\", 92347, 0, 0.0, 6.372356438216705, 2, 80, 5.0, 10.0, 12.0, 21.0, 1541.0944044857567, 502.2277938980024, 239.29102569651886],\n" +
                "        \"isController\": false\n" +
                "    }]\n" +
                "}, function(index, item){ return item; });";

        try (FileWriter writer = new FileWriter(dashboardFile)) {
            writer.write(dashboardContent);
        }

        // 执行测试
        Map<String, List<JSONObject>> result = reportDataProcess.getDashBoardData(dashboardFile.getAbsolutePath());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("statisticsTable"));
        
        List<JSONObject> statisticsTable = result.get("statisticsTable");
        assertNotNull(statisticsTable);
        assertEquals(2, statisticsTable.size(), "应包含 2 条记录（Total + HTTP Request）");

        // 验证 Total 记录
        JSONObject totalRecord = statisticsTable.get(0);
        assertEquals("Total", totalRecord.getString("label"));
        assertEquals(92347, totalRecord.getLongValue("samples"));
        assertEquals(1541.0944044857567, totalRecord.getDoubleValue("transactions"), 0.0001);

        // 验证 HTTP Request 记录
        JSONObject httpRecord = statisticsTable.get(1);
        assertEquals("HTTP Request", httpRecord.getString("label"));
        assertEquals(92347, httpRecord.getLongValue("samples"));
    }

    @Test
    void testGetDashBoardData_WithoutOverall(@TempDir Path tempDir) throws IOException {
        // 测试没有 overall 对象的情况（向后兼容）
        File dashboardFile = tempDir.resolve("dashboard.js").toFile();
        
        String dashboardContent = "createTable($(\"#statisticsTable\"), {\n" +
                "    \"supportsControllersDiscrimination\": true,\n" +
                "    \"titles\": [\"Label\", \"#Samples\"],\n" +
                "    \"items\": [{\n" +
                "        \"data\": [\"Test Request\", 100, 0, 0.0],\n" +
                "        \"isController\": false\n" +
                "    }]\n" +
                "}, function(index, item){ return item; });";

        try (FileWriter writer = new FileWriter(dashboardFile)) {
            writer.write(dashboardContent);
        }

        // 执行测试
        Map<String, List<JSONObject>> result = reportDataProcess.getDashBoardData(dashboardFile.getAbsolutePath());

        // 验证结果（应该只有 items 中的数据）
        assertNotNull(result);
        if (result.containsKey("statisticsTable")) {
            List<JSONObject> statisticsTable = result.get("statisticsTable");
            assertNotNull(statisticsTable);
            assertEquals(1, statisticsTable.size(), "没有 overall 时，应只有 items 中的记录");
            assertEquals("Test Request", statisticsTable.get(0).getString("label"));
        }
    }

    @Test
    void testGetDashBoardData_WithMultipleItems(@TempDir Path tempDir) throws IOException {
        // 测试多个 items 的情况
        File dashboardFile = tempDir.resolve("dashboard.js").toFile();
        
        String dashboardContent = "createTable($(\"#statisticsTable\"), {\n" +
                "    \"overall\": {\n" +
                "        \"data\": [\"Total\", 200, 0, 0.0, 100.0, 50, 200, 100.0, 150.0, 180.0, 200.0, 10.0, 5.0, 2.0],\n" +
                "        \"isController\": false\n" +
                "    },\n" +
                "    \"titles\": [\"Label\", \"#Samples\", \"FAIL\", \"Error %\", \"Average\", \"Min\", \"Max\", \"Median\", \"90th pct\", \"95th pct\", \"99th pct\", \"Transactions/s\", \"Received\", \"Sent\"],\n" +
                "    \"items\": [\n" +
                "        {\"data\": [\"Request1\", 100, 0, 0.0, 80.0, 50, 150, 80.0, 120.0, 140.0, 150.0, 5.0, 2.5, 1.0], \"isController\": false},\n" +
                "        {\"data\": [\"Request2\", 100, 0, 0.0, 120.0, 60, 200, 120.0, 180.0, 200.0, 200.0, 5.0, 2.5, 1.0], \"isController\": false}\n" +
                "    ]\n" +
                "}, function(index, item){ return item; });";

        try (FileWriter writer = new FileWriter(dashboardFile)) {
            writer.write(dashboardContent);
        }

        // 执行测试
        Map<String, List<JSONObject>> result = reportDataProcess.getDashBoardData(dashboardFile.getAbsolutePath());

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("statisticsTable"));
        
        List<JSONObject> statisticsTable = result.get("statisticsTable");
        assertNotNull(statisticsTable);
        assertEquals(3, statisticsTable.size(), "应包含 3 条记录（Total + Request1 + Request2）");

        // 验证顺序：Total 应该在第一位
        assertEquals("Total", statisticsTable.get(0).getString("label"));
        assertEquals("Request1", statisticsTable.get(1).getString("label"));
        assertEquals("Request2", statisticsTable.get(2).getString("label"));
    }

    @Test
    void testGetDashBoardData_InvalidFile(@TempDir Path tempDir) {
        // 测试文件不存在的情况
        File nonExistentFile = tempDir.resolve("non_existent.js").toFile();
        
        // 执行测试（不应该抛出异常）
        Map<String, List<JSONObject>> result = reportDataProcess.getDashBoardData(nonExistentFile.getAbsolutePath());
        
        // 验证结果为空 map
        assertNotNull(result);
        assertTrue(result.isEmpty() || !result.containsKey("statisticsTable"));
    }
}

