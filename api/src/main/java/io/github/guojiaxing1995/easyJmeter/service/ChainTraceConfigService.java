package io.github.guojiaxing1995.easyJmeter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.github.guojiaxing1995.easyJmeter.model.ChainTraceConfigDO;
import io.github.guojiaxing1995.easyJmeter.dto.chain.CreateOrUpdateChainTraceConfigDTO;
import io.github.guojiaxing1995.easyJmeter.dto.chain.QueryChainTraceConfigDTO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 链路追踪配置服务接口
 *
 * @author Assistant
 * @version 1.0.0
 */
public interface ChainTraceConfigService extends IService<ChainTraceConfigDO> {

    /**
     * 创建链路追踪配置
     *
     * @param dto 创建DTO
     * @return 创建的配置
     */
    ChainTraceConfigDO createChainTraceConfig(CreateOrUpdateChainTraceConfigDTO dto);

    /**
     * 更新链路追踪配置
     *
     * @param dto 更新DTO
     * @return 更新后的配置
     */
    ChainTraceConfigDO updateChainTraceConfig(CreateOrUpdateChainTraceConfigDTO dto);

    /**
     * 根据ID获取链路追踪配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
    ChainTraceConfigDO getChainTraceConfigDetail(Long id);

    /**
     * 删除链路追踪配置
     *
     * @param id 配置ID
     * @return 删除结果
     */
    boolean deleteChainTraceConfig(Long id);

    /**
     * 分页查询链路追踪配置
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    IPage<ChainTraceConfigDO> getChainTraceConfigPage(QueryChainTraceConfigDTO dto);

    /**
     * 根据任务ID获取链路配置列表
     *
     * @param taskId 任务ID
     * @return 链路配置列表
     */
    List<ChainTraceConfigDO> getChainTraceConfigsByTaskId(Long taskId);

    /**
     * 启用/禁用链路配置
     *
     * @param id 配置ID
     * @param status 状态：1-启用，0-禁用
     * @return 更新结果
     */
    boolean updateChainTraceConfigStatus(Long id, Integer status);

    /**
     * 验证链路名称是否可用
     *
     * @param chainName 链路名称
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 是否可用
     */
    boolean validateChainName(String chainName, Long excludeId);

    /**
     * 复制链路配置
     *
     * @param id 源配置ID
     * @param newChainName 新链路名称
     * @return 复制后的配置
     */
    ChainTraceConfigDO copyChainTraceConfig(Long id, String newChainName);

    /**
     * 导出链路配置
     *
     * @param id 配置ID
     * @return 导出数据
     */
    String exportChainTraceConfig(Long id);

    /**
     * 导入链路配置
     *
     * @param configData 配置数据
     * @return 导入结果
     */
    ChainTraceConfigDO importChainTraceConfig(String configData);
}