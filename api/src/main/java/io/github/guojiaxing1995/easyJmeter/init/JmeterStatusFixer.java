package io.github.guojiaxing1995.easyJmeter.init;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JMeterStatus 数据库修复器
 * 用于修复数据库中错误存储的 jmeter_status 值
 */
@Component
@Order(2) // 在 ChainDatabaseInitializer 之后执行
@Slf4j
public class JmeterStatusFixer implements ApplicationRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始检查和修复 jmeter_status 数据...");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // 1. 查看当前有问题的数据 (检查字符串类型的值)
            String checkQuery =
                "SELECT id, name, jmeter_status FROM machine " +
                "WHERE delete_time IS NULL AND (jmeter_status NOT IN ('0','1','2','3','4','5') OR jmeter_status IS NULL)";

            log.info("检查问题数据...");
            ResultSet resultSet = statement.executeQuery(checkQuery);

            int problemCount = 0;
            while (resultSet.next()) {
                problemCount++;
                log.warn("发现问题数据 - ID: {}, Name: {}, jmeter_status: '{}'",
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getString("jmeter_status"));
            }
            resultSet.close();

            if (problemCount > 0) {
                log.info("发现 {} 条问题数据，开始修复...", problemCount);

                // 2. 修复错误数据 - 将枚举名称转换为对应的整数值
                String fixQuery =
                    "UPDATE machine " +
                    "SET jmeter_status = CASE " +
                    "  WHEN jmeter_status = 'IDLE' THEN '0' " +
                    "  WHEN jmeter_status = 'CONFIGURE' THEN '1' " +
                    "  WHEN jmeter_status = 'RUN' THEN '2' " +
                    "  WHEN jmeter_status = 'COLLECT' THEN '3' " +
                    "  WHEN jmeter_status = 'CLEAN' THEN '4' " +
                    "  WHEN jmeter_status = 'INTERRUPT' THEN '5' " +
                    "  WHEN jmeter_status = 'IN_PROGRESS' THEN '2' " +
                    "  WHEN jmeter_status = 'RUNNING' THEN '2' " +
                    "  ELSE '0' " +
                    "END " +
                    "WHERE delete_time IS NULL AND (jmeter_status NOT IN ('0','1','2','3','4','5') OR jmeter_status IS NULL)";

                int updatedRows = statement.executeUpdate(fixQuery);
                log.info("修复完成，更新了 {} 行数据", updatedRows);

                // 3. 验证修复结果
                String verifyQuery =
                    "SELECT COUNT(*) as total FROM machine " +
                    "WHERE delete_time IS NULL AND (jmeter_status NOT IN ('0','1','2','3','4','5') OR jmeter_status IS NULL)";

                ResultSet verifyResult = statement.executeQuery(verifyQuery);
                if (verifyResult.next()) {
                    int remainingProblems = verifyResult.getInt("total");
                    if (remainingProblems == 0) {
                        log.info("验证通过：所有问题数据已修复");
                    } else {
                        log.error("验证失败：仍有 {} 条问题数据未修复", remainingProblems);
                    }
                }
                verifyResult.close();

            } else {
                log.info("未发现问题数据，jmeter_status 字段状态正常");
            }

            log.info("jmeter_status 数据检查和修复完成");

        } catch (SQLException e) {
            log.error("修复 jmeter_status 数据时发生错误", e);
            throw e;
        }
    }
}