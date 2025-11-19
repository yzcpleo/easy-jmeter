package io.github.guojiaxing1995.easyJmeter.service;

import io.github.guojiaxing1995.easyJmeter.dto.chain.CreateOrUpdateChainNodeDTO;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;

import java.util.List;

/**
 * 链路节点配置服务接口
 *
 * @author Assistant
 * @version 1.0.0
 */
public interface ChainNodeConfigService {

    /**
     * 创建节点配置
     *
     * @param dto 节点配置DTO
     * @return 创建的节点配置
     */
    ChainNodeConfigDO createNode(CreateOrUpdateChainNodeDTO dto);

    /**
     * 更新节点配置
     *
     * @param id  节点ID
     * @param dto 节点配置DTO
     * @return 更新后的节点配置
     */
    ChainNodeConfigDO updateNode(Long id, CreateOrUpdateChainNodeDTO dto);

    /**
     * 删除节点配置
     *
     * @param id 节点ID
     */
    void deleteNode(Long id);

    /**
     * 根据ID获取节点配置
     *
     * @param id 节点ID
     * @return 节点配置
     */
    ChainNodeConfigDO getNodeById(Long id);

    /**
     * 根据链路ID获取所有节点
     *
     * @param chainId 链路ID
     * @return 节点列表
     */
    List<ChainNodeConfigDO> getNodesByChainId(Long chainId);

    /**
     * 批量创建节点
     *
     * @param chainId 链路ID
     * @param dtos    节点配置列表
     * @return 创建的节点列表
     */
    List<ChainNodeConfigDO> batchCreateNodes(Long chainId, List<CreateOrUpdateChainNodeDTO> dtos);
}
