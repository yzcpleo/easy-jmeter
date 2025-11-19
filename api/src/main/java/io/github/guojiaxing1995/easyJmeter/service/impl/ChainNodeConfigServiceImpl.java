package io.github.guojiaxing1995.easyJmeter.service.impl;

import io.github.guojiaxing1995.easyJmeter.dto.chain.CreateOrUpdateChainNodeDTO;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainNodeConfigMapper;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainTraceConfigMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import io.github.guojiaxing1995.easyJmeter.model.ChainTraceConfigDO;
import io.github.guojiaxing1995.easyJmeter.service.ChainNodeConfigService;
import io.github.talelin.autoconfigure.exception.NotFoundException;
import io.github.talelin.autoconfigure.exception.ParameterException;
import io.github.talelin.core.token.DoubleJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 链路节点配置服务实现
 *
 * @author Assistant
 * @version 1.0.0
 */
@Slf4j
@Service
public class ChainNodeConfigServiceImpl implements ChainNodeConfigService {

    @Autowired
    private ChainNodeConfigMapper chainNodeConfigMapper;

    @Autowired
    private ChainTraceConfigMapper chainTraceConfigMapper;

    @Autowired
    private DoubleJWT jwt;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChainNodeConfigDO createNode(CreateOrUpdateChainNodeDTO dto) {
        // 1. 验证链路是否存在
        ChainTraceConfigDO chain = chainTraceConfigMapper.selectById(dto.getChainId());
        if (chain == null || chain.getDeleteTime() != null) {
            throw new NotFoundException("链路配置不存在，链路ID: " + dto.getChainId(), 20001);
        }

        // 2. 验证节点名称是否重复
        ChainNodeConfigDO existingNode = chainNodeConfigMapper.selectByChainIdAndNodeName(
                dto.getChainId(), dto.getNodeName());
        if (existingNode != null) {
            throw new ParameterException("节点名称已存在: " + dto.getNodeName(), 20002);
        }

        // 3. 创建节点
        ChainNodeConfigDO node = new ChainNodeConfigDO();
        BeanUtils.copyProperties(dto, node);
        node.setCreatedTime(LocalDateTime.now());
        node.setUpdatedTime(LocalDateTime.now());
        node.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        chainNodeConfigMapper.insert(node);
        log.info("创建链路节点成功，节点ID: {}, 节点名称: {}", node.getId(), node.getNodeName());

        return node;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChainNodeConfigDO updateNode(Long id, CreateOrUpdateChainNodeDTO dto) {
        // 1. 验证节点是否存在
        ChainNodeConfigDO existingNode = chainNodeConfigMapper.selectById(id);
        if (existingNode == null || existingNode.getDeleteTime() != null) {
            throw new NotFoundException("节点配置不存在，节点ID: " + id, 20003);
        }

        // 2. 验证链路是否存在
        ChainTraceConfigDO chain = chainTraceConfigMapper.selectById(dto.getChainId());
        if (chain == null || chain.getDeleteTime() != null) {
            throw new NotFoundException("链路配置不存在，链路ID: " + dto.getChainId(), 20001);
        }

        // 3. 如果修改了节点名称，验证是否重复
        if (!existingNode.getNodeName().equals(dto.getNodeName())) {
            ChainNodeConfigDO duplicateNode = chainNodeConfigMapper.selectByChainIdAndNodeName(
                    dto.getChainId(), dto.getNodeName());
            if (duplicateNode != null) {
                throw new ParameterException("节点名称已存在: " + dto.getNodeName(), 20002);
            }
        }

        // 4. 更新节点
        BeanUtils.copyProperties(dto, existingNode, "id", "createdTime");
        existingNode.setUpdatedTime(LocalDateTime.now());

        chainNodeConfigMapper.updateById(existingNode);
        log.info("更新链路节点成功，节点ID: {}, 节点名称: {}", existingNode.getId(), existingNode.getNodeName());

        return existingNode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long id) {
        // 1. 验证节点是否存在（使用 selectById 会自动过滤已删除的记录）
        ChainNodeConfigDO node = chainNodeConfigMapper.selectById(id);
        if (node == null) {
            throw new NotFoundException("节点配置不存在，节点ID: " + id, 20003);
        }

        // 2. 软删除节点
        // 注意：由于 deleteTime 字段带有 @TableLogic 注解，MyBatis-Plus 的 updateById 方法
        // 不会更新逻辑删除字段，需要使用自定义的 SQL 方法来更新 delete_time
        LocalDateTime deleteTime = LocalDateTime.now();
        int result = chainNodeConfigMapper.updateDeleteTime(id, deleteTime);
        if (result <= 0) {
            log.warn("删除节点失败，可能节点不存在或已被删除，节点ID: {}", id);
            throw new NotFoundException("节点配置不存在或删除失败，节点ID: " + id, 20003);
        }
        log.info("删除链路节点成功，节点ID: {}, 节点名称: {}", node.getId(), node.getNodeName());
    }

    @Override
    public ChainNodeConfigDO getNodeById(Long id) {
        try {
            ChainNodeConfigDO node = chainNodeConfigMapper.selectById(id);
            if (node == null || node.getDeleteTime() != null) {
                throw new NotFoundException("节点配置不存在，节点ID: " + id, 20003);
            }
            
            // 确保新字段有默认值（兼容旧数据）
            if (node.getOsType() == null || node.getOsType().trim().isEmpty()) {
                node.setOsType("LINUX");
            }
            if (node.getConnectionType() == null || node.getConnectionType().trim().isEmpty()) {
                node.setConnectionType("LOCAL");
            }
            if (node.getParseMethod() == null || node.getParseMethod().trim().isEmpty()) {
                node.setParseMethod("JAVA_REGEX");
            }
            if (node.getNodePort() == null && node.getNodeHost() != null && !node.getNodeHost().trim().isEmpty()) {
                node.setNodePort(22);
            }
            if (node.getConnectionTimeout() == null) {
                node.setConnectionTimeout(30);
            }
            if (node.getReadTimeout() == null) {
                node.setReadTimeout(60);
            }
            if (node.getUseCustomScript() == null) {
                node.setUseCustomScript(0);
            }
            
            return node;
        } catch (Exception e) {
            log.error("获取节点配置失败，节点ID: {}", id, e);
            throw new NotFoundException("节点配置不存在或查询失败，节点ID: " + id, 20003);
        }
    }

    @Override
    public List<ChainNodeConfigDO> getNodesByChainId(Long chainId) {
        List<ChainNodeConfigDO> nodes = chainNodeConfigMapper.selectByChainIdOrderBySequence(chainId);
        
        // 确保新字段有默认值（兼容旧数据）
        for (ChainNodeConfigDO node : nodes) {
            if (node.getOsType() == null || node.getOsType().trim().isEmpty()) {
                node.setOsType("LINUX");
            }
            if (node.getConnectionType() == null || node.getConnectionType().trim().isEmpty()) {
                node.setConnectionType("LOCAL");
            }
            if (node.getParseMethod() == null || node.getParseMethod().trim().isEmpty()) {
                node.setParseMethod("JAVA_REGEX");
            }
            if (node.getNodePort() == null && node.getNodeHost() != null && !node.getNodeHost().trim().isEmpty()) {
                node.setNodePort(22);
            }
            if (node.getConnectionTimeout() == null) {
                node.setConnectionTimeout(30);
            }
            if (node.getReadTimeout() == null) {
                node.setReadTimeout(60);
            }
            if (node.getUseCustomScript() == null) {
                node.setUseCustomScript(0);
            }
        }
        
        return nodes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ChainNodeConfigDO> batchCreateNodes(Long chainId, List<CreateOrUpdateChainNodeDTO> dtos) {
        // 1. 验证链路是否存在
        ChainTraceConfigDO chain = chainTraceConfigMapper.selectById(chainId);
        if (chain == null || chain.getDeleteTime() != null) {
            throw new NotFoundException("链路配置不存在，链路ID: " + chainId, 20001);
        }

        // 2. 批量创建节点
        List<ChainNodeConfigDO> nodes = new ArrayList<>();
        for (CreateOrUpdateChainNodeDTO dto : dtos) {
            dto.setChainId(chainId);
            ChainNodeConfigDO node = createNode(dto);
            nodes.add(node);
        }

        log.info("批量创建链路节点成功，链路ID: {}, 节点数量: {}", chainId, nodes.size());
        return nodes;
    }
}
