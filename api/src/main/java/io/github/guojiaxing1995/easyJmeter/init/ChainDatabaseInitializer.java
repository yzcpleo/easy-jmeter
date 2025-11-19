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
 * 链路监控数据库表初始化器
 * 在应用启动时自动创建链路监控相关的数据库表
 */
@Component
@Order(1)
@Slf4j
public class ChainDatabaseInitializer implements ApplicationRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始初始化链路监控数据库表...");

        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();

            // 检查表是否已存在
            if (isTableExists(connection, "chain_trace_config")) {
                log.info("链路监控表已存在，跳过初始化");
                return;
            }

            // 创建链路追踪配置表
            String createChainTraceConfigTable =
                "CREATE TABLE chain_trace_config (" +
                "    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID'," +
                "    task_id BIGINT NOT NULL COMMENT '关联的测试任务ID'," +
                "    chain_name VARCHAR(100) NOT NULL COMMENT '链路名称'," +
                "    chain_description TEXT COMMENT '链路描述'," +
                "    version VARCHAR(20) DEFAULT '1.0' COMMENT '链路版本'," +
                "    created_by VARCHAR(50) COMMENT '创建人'," +
                "    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "    updated_by VARCHAR(50) COMMENT '更新人'," +
                "    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "    delete_time DATETIME COMMENT '删除时间'," +
                "    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链路追踪配置表'";

            // 创建链路节点配置表
            String createChainNodeConfigTable =
                "CREATE TABLE chain_node_config (" +
                "    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID'," +
                "    chain_id BIGINT NOT NULL COMMENT '所属链路ID'," +
                "    node_name VARCHAR(50) NOT NULL COMMENT '节点名称'," +
                "    node_type VARCHAR(20) DEFAULT 'APPLICATION' COMMENT '节点类型'," +
                "    node_description VARCHAR(200) COMMENT '节点描述'," +
                "    sequence_order INT DEFAULT 1 COMMENT '执行顺序'," +
                "    log_path TEXT COMMENT '日志文件路径'," +
                "    log_pattern TEXT COMMENT '日志匹配模式'," +
                "    timestamp_pattern TEXT COMMENT '时间戳提取模式'," +
                "    latency_pattern TEXT COMMENT '延时提取模式'," +
                "    request_id_pattern TEXT COMMENT '请求ID提取模式'," +
                "    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用'," +
                "    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "    delete_time DATETIME COMMENT '删除时间'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链路节点配置表'";

            // 创建链路延时数据表
            String createChainLatencyDataTable =
                "CREATE TABLE chain_latency_data (" +
                "    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID'," +
                "    task_id BIGINT NOT NULL COMMENT '测试任务ID'," +
                "    chain_id BIGINT NOT NULL COMMENT '链路ID'," +
                "    node_id BIGINT NOT NULL COMMENT '节点ID'," +
                "    request_id VARCHAR(100) NOT NULL COMMENT '请求ID'," +
                "    node_name VARCHAR(50) NOT NULL COMMENT '节点名称'," +
                "    request_start_time DATETIME NOT NULL COMMENT '请求开始时间戳'," +
                "    request_end_time DATETIME NOT NULL COMMENT '请求结束时间戳'," +
                "    latency BIGINT NOT NULL COMMENT '延时（毫秒）'," +
                "    success TINYINT DEFAULT 1 COMMENT '是否成功：1-成功，0-失败'," +
                "    error_message TEXT COMMENT '错误信息'," +
                "    collection_time DATETIME NOT NULL COMMENT '数据收集时间'," +
                "    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链路延时数据表'";

            // 执行建表语句
            log.info("创建 chain_trace_config 表...");
            statement.executeUpdate(createChainTraceConfigTable);

            log.info("创建 chain_node_config 表...");
            statement.executeUpdate(createChainNodeConfigTable);

            log.info("创建 chain_latency_data 表...");
            statement.executeUpdate(createChainLatencyDataTable);

            // 插入示例数据
            log.info("插入示例数据...");
            insertSampleData(statement);

            log.info("链路监控数据库表初始化完成！");

        } catch (SQLException e) {
            log.error("初始化链路监控数据库表失败", e);
            throw e;
        }
    }

    private boolean isTableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void insertSampleData(Statement statement) throws SQLException {
        // 插入示例链路配置
        String insertChainConfig =
            "INSERT IGNORE INTO chain_trace_config (task_id, chain_name, chain_description, version, created_by, status) VALUES " +
            "(1, 'user-service-chain', '用户服务调用链路', '1.0', 'admin', 1), " +
            "(2, 'order-service-chain', '订单服务调用链路', '1.0', 'admin', 1)";
        statement.executeUpdate(insertChainConfig);

        // 插入示例节点配置
        String insertNodeConfig =
            "INSERT IGNORE INTO chain_node_config (chain_id, node_name, node_type, node_description, sequence_order, log_path, status) VALUES " +
            "(1, 'gateway', 'APPLICATION', 'API网关', 1, '/var/log/gateway/app.log', 1), " +
            "(1, 'user-service', 'APPLICATION', '用户服务', 2, '/var/log/user-service/app.log', 1), " +
            "(1, 'mysql-db', 'DATABASE', '用户数据库', 3, '/var/log/mysql/slow.log', 1)";
        statement.executeUpdate(insertNodeConfig);
    }
}