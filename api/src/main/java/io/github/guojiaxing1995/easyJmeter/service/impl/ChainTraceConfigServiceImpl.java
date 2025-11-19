package io.github.guojiaxing1995.easyJmeter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.talelin.autoconfigure.exception.NotFoundException;
import io.github.talelin.autoconfigure.exception.ParameterException;
import io.github.guojiaxing1995.easyJmeter.common.mybatis.Page;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainTraceConfigMapper;
import io.github.guojiaxing1995.easyJmeter.mapper.ChainNodeConfigMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainTraceConfigDO;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import io.github.guojiaxing1995.easyJmeter.dto.chain.CreateOrUpdateChainTraceConfigDTO;
import io.github.guojiaxing1995.easyJmeter.dto.chain.QueryChainTraceConfigDTO;
import io.github.guojiaxing1995.easyJmeter.service.ChainTraceConfigService;
import io.github.guojiaxing1995.easyJmeter.common.LocalUser;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 链路追踪配置服务实现类
 *
 * @author Assistant
 * @version 1.0.0
 */
@Slf4j
@Service
public class ChainTraceConfigServiceImpl extends ServiceImpl<ChainTraceConfigMapper, ChainTraceConfigDO>
        implements ChainTraceConfigService {

    @Autowired
    private ChainTraceConfigMapper chainTraceConfigMapper;

    @Autowired
    private ChainNodeConfigMapper chainNodeConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChainTraceConfigDO createChainTraceConfig(CreateOrUpdateChainTraceConfigDTO dto) {
        // 检查链路名称是否已存在
        if (!validateChainName(dto.getChainName(), null)) {
            throw new ParameterException(20001, "链路名称已存在");
        }

        // 创建链路配置
        ChainTraceConfigDO chainConfig = ChainTraceConfigDO.builder()
                .taskId(dto.getTaskId())
                .chainName(dto.getChainName())
                .chainDescription(dto.getChainDescription())
                .version(dto.getVersion())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .createdBy(LocalUser.getLocalUser() != null ? LocalUser.getLocalUser().getUsername() : "system")
                .build();

        this.save(chainConfig);

        // 创建节点配置
        if (dto.getNodeConfigs() != null && !dto.getNodeConfigs().isEmpty()) {
            createNodeConfigs(chainConfig.getId(), dto.getNodeConfigs());
        }

        return chainConfig;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChainTraceConfigDO updateChainTraceConfig(CreateOrUpdateChainTraceConfigDTO dto) {
        if (dto.getId() == null) {
            throw new ParameterException(20002, "配置ID不能为空");
        }

        ChainTraceConfigDO existingConfig = this.getById(dto.getId());
        if (existingConfig == null) {
            throw new NotFoundException(10004, "链路配置不存在");
        }

        // 检查链路名称是否已存在（排除当前配置）
        if (!validateChainName(dto.getChainName(), dto.getId())) {
            throw new ParameterException(20001, "链路名称已存在");
        }

        // 更新链路配置
        ChainTraceConfigDO updateConfig = ChainTraceConfigDO.builder()
                .id(dto.getId())
                .taskId(dto.getTaskId())
                .chainName(dto.getChainName())
                .chainDescription(dto.getChainDescription())
                .version(dto.getVersion())
                .status(dto.getStatus())
                .updatedBy(LocalUser.getLocalUser() != null ? LocalUser.getLocalUser().getUsername() : "system")
                .build();

        this.updateById(updateConfig);

        // 更新节点配置
        if (dto.getNodeConfigs() != null) {
            // 先删除原有节点配置
            chainNodeConfigMapper.deleteByChainId(dto.getId());
            // 创建新的节点配置
            createNodeConfigs(dto.getId(), dto.getNodeConfigs());
        }

        return this.getById(dto.getId());
    }

    @Override
    public ChainTraceConfigDO getChainTraceConfigDetail(Long id) {
        ChainTraceConfigDO config = this.getById(id);
        if (config == null) {
            throw new NotFoundException(10004, "链路配置不存在");
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteChainTraceConfig(Long id) {
        ChainTraceConfigDO config = this.getById(id);
        if (config == null) {
            throw new NotFoundException(10004, "链路配置不存在");
        }

        // 删除关联的节点配置
        chainNodeConfigMapper.deleteByChainId(id);

        // 删除链路配置
        return this.removeById(id);
    }

    @Override
    public IPage<ChainTraceConfigDO> getChainTraceConfigPage(QueryChainTraceConfigDTO dto) {
        Page<ChainTraceConfigDO> pager = new Page<>(dto.getPage(), dto.getCount());
        QueryWrapper<ChainTraceConfigDO> wrapper = new QueryWrapper<>();

        if (dto.getTaskId() != null) {
            wrapper.eq("task_id", dto.getTaskId());
        }
        if (StringUtils.isNotBlank(dto.getChainName())) {
            wrapper.like("chain_name", dto.getChainName());
        }
        if (StringUtils.isNotBlank(dto.getVersion())) {
            wrapper.eq("version", dto.getVersion());
        }
        if (StringUtils.isNotBlank(dto.getCreatedBy())) {
            wrapper.eq("created_by", dto.getCreatedBy());
        }
        if (dto.getStatus() != null) {
            wrapper.eq("status", dto.getStatus());
        }
        if (StringUtils.isNotBlank(dto.getStartCreatedTime())) {
            wrapper.ge("created_time", dto.getStartCreatedTime());
        }
        if (StringUtils.isNotBlank(dto.getEndCreatedTime())) {
            wrapper.le("created_time", dto.getEndCreatedTime());
        }

        wrapper.orderByDesc("created_time");
        return this.page(pager, wrapper);
    }

    @Override
    public List<ChainTraceConfigDO> getChainTraceConfigsByTaskId(Long taskId) {
        return chainTraceConfigMapper.selectByTaskId(taskId);
    }

    @Override
    public boolean updateChainTraceConfigStatus(Long id, Integer status) {
        ChainTraceConfigDO config = this.getById(id);
        if (config == null) {
            throw new NotFoundException(10004, "链路配置不存在");
        }

        ChainTraceConfigDO updateConfig = ChainTraceConfigDO.builder()
                .id(id)
                .status(status)
                .updatedBy(LocalUser.getLocalUser() != null ? LocalUser.getLocalUser().getUsername() : "system")
                .build();

        return this.updateById(updateConfig);
    }

    @Override
    public boolean validateChainName(String chainName, Long excludeId) {
        if (StringUtils.isBlank(chainName)) {
            return false;
        }
        return !chainTraceConfigMapper.existsByChainName(chainName, excludeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChainTraceConfigDO copyChainTraceConfig(Long id, String newChainName) {
        ChainTraceConfigDO sourceConfig = this.getById(id);
        if (sourceConfig == null) {
            throw new NotFoundException(10004, "源链路配置不存在");
        }

        // 检查新链路名称是否可用
        if (!validateChainName(newChainName, null)) {
            throw new ParameterException(20001, "链路名称已存在");
        }

        // 创建新的链路配置
        ChainTraceConfigDO newConfig = ChainTraceConfigDO.builder()
                .taskId(sourceConfig.getTaskId())
                .chainName(newChainName)
                .chainDescription(sourceConfig.getChainDescription())
                .version(sourceConfig.getVersion())
                .status(1)
                .createdBy(LocalUser.getLocalUser() != null ? LocalUser.getLocalUser().getUsername() : "system")
                .build();

        this.save(newConfig);

        // 复制节点配置
        List<ChainNodeConfigDO> sourceNodes = chainNodeConfigMapper.selectByChainId(id);
        if (sourceNodes != null && !sourceNodes.isEmpty()) {
            List<CreateOrUpdateChainTraceConfigDTO.NodeConfigDTO> nodeConfigs = sourceNodes.stream()
                    .map(node -> CreateOrUpdateChainTraceConfigDTO.NodeConfigDTO.builder()
                            .nodeName(node.getNodeName())
                            .nodeType(node.getNodeType())
                            .nodeDescription(node.getNodeDescription())
                            .sequenceOrder(node.getSequenceOrder())
                            .logPath(node.getLogPath())
                            .logPattern(node.getLogPattern())
                            .timestampPattern(node.getTimestampPattern())
                            .latencyPattern(node.getLatencyPattern())
                            .requestIdPattern(node.getRequestIdPattern())
                            .dataMapping(node.getDataMapping())
                            .status(node.getStatus())
                            .build())
                    .collect(Collectors.toList());

            createNodeConfigs(newConfig.getId(), nodeConfigs);
        }

        return newConfig;
    }

    @Override
    public String exportChainTraceConfig(Long id) {
        ChainTraceConfigDO config = getChainTraceConfigDetail(id);
        List<ChainNodeConfigDO> nodes = chainNodeConfigMapper.selectByChainIdOrderBySequence(id);

        // 构建导出数据
        ExportData exportData = ExportData.builder()
                .chainConfig(config)
                .nodeConfigs(nodes)
                .exportTime(LocalDateTime.now())
                .exportUser(LocalUser.getLocalUser() != null ? LocalUser.getLocalUser().getUsername() : "system")
                .build();

        return JSON.toJSONString(exportData, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChainTraceConfigDO importChainTraceConfig(String configData) {
        try {
            ExportData exportData = JSON.parseObject(configData, ExportData.class);

            // 创建新的链路配置
            ChainTraceConfigDO sourceConfig = exportData.getChainConfig();
            String newChainName = sourceConfig.getChainName() + "_import_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            ChainTraceConfigDO newConfig = ChainTraceConfigDO.builder()
                    .taskId(sourceConfig.getTaskId())
                    .chainName(newChainName)
                    .chainDescription(sourceConfig.getChainDescription())
                    .version(sourceConfig.getVersion())
                    .status(1)
                    .createdBy(LocalUser.getLocalUser() != null ? LocalUser.getLocalUser().getUsername() : "system")
                    .build();

            this.save(newConfig);

            // 导入节点配置
            if (exportData.getNodeConfigs() != null && !exportData.getNodeConfigs().isEmpty()) {
                List<CreateOrUpdateChainTraceConfigDTO.NodeConfigDTO> nodeConfigs = exportData.getNodeConfigs()
                        .stream()
                        .map(node -> CreateOrUpdateChainTraceConfigDTO.NodeConfigDTO.builder()
                                .nodeName(node.getNodeName())
                                .nodeType(node.getNodeType())
                                .nodeDescription(node.getNodeDescription())
                                .sequenceOrder(node.getSequenceOrder())
                                .logPath(node.getLogPath())
                                .logPattern(node.getLogPattern())
                                .timestampPattern(node.getTimestampPattern())
                                .latencyPattern(node.getLatencyPattern())
                                .requestIdPattern(node.getRequestIdPattern())
                                .dataMapping(node.getDataMapping())
                                .status(node.getStatus())
                                .build())
                        .collect(Collectors.toList());

                createNodeConfigs(newConfig.getId(), nodeConfigs);
            }

            return newConfig;
        } catch (Exception e) {
            log.error("导入链路配置失败", e);
            throw new ParameterException(20003, "配置数据格式错误");
        }
    }

    /**
     * 创建节点配置
     */
    private void createNodeConfigs(Long chainId, List<CreateOrUpdateChainTraceConfigDTO.NodeConfigDTO> nodeConfigs) {
        for (CreateOrUpdateChainTraceConfigDTO.NodeConfigDTO nodeDto : nodeConfigs) {
            ChainNodeConfigDO nodeConfig = ChainNodeConfigDO.builder()
                    .chainId(chainId)
                    .nodeName(nodeDto.getNodeName())
                    .nodeType(nodeDto.getNodeType())
                    .nodeDescription(nodeDto.getNodeDescription())
                    .sequenceOrder(nodeDto.getSequenceOrder() != null ? nodeDto.getSequenceOrder() : 1)
                    .logPath(nodeDto.getLogPath())
                    .logPattern(nodeDto.getLogPattern())
                    .timestampPattern(nodeDto.getTimestampPattern())
                    .latencyPattern(nodeDto.getLatencyPattern())
                    .requestIdPattern(nodeDto.getRequestIdPattern())
                    .dataMapping(nodeDto.getDataMapping())
                    .status(nodeDto.getStatus() != null ? nodeDto.getStatus() : 1)
                    .build();

            chainNodeConfigMapper.insert(nodeConfig);
        }
    }

    /**
     * 导出数据内部类
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ExportData {
        private ChainTraceConfigDO chainConfig;
        private List<ChainNodeConfigDO> nodeConfigs;
        private LocalDateTime exportTime;
        private String exportUser;
    }
}