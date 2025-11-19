package io.github.guojiaxing1995.easyJmeter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainNodeConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 链路节点配置Mapper
 *
 * @author Assistant
 * @version 1.0.0
 */
@Mapper
public interface ChainNodeConfigMapper extends BaseMapper<ChainNodeConfigDO> {

    /**
     * 根据链路ID查询节点配置列表
     *
     * @param chainId 链路ID
     * @return 节点配置列表（按执行顺序排序）
     */
    List<ChainNodeConfigDO> selectByChainIdOrderBySequence(@Param("chainId") Long chainId);

    /**
     * 根据链路ID查询节点配置列表（不分页）
     *
     * @param chainId 链路ID
     * @return 节点配置列表
     */
    List<ChainNodeConfigDO> selectByChainId(@Param("chainId") Long chainId);

    /**
     * 根据节点名称查询节点配置
     *
     * @param chainId 链路ID
     * @param nodeName 节点名称
     * @return 节点配置
     */
    ChainNodeConfigDO selectByChainIdAndNodeName(@Param("chainId") Long chainId, @Param("nodeName") String nodeName);

    /**
     * 获取链路中节点的最大执行顺序
     *
     * @param chainId 链路ID
     * @return 最大执行顺序
     */
    Integer getMaxSequenceOrder(@Param("chainId") Long chainId);

    /**
     * 更新节点执行顺序
     *
     * @param nodeId 节点ID
     * @param sequenceOrder 执行顺序
     * @return 更新结果
     */
    int updateSequenceOrder(@Param("nodeId") Long nodeId, @Param("sequenceOrder") Integer sequenceOrder);

    /**
     * 根据链路ID删除节点配置
     *
     * @param chainId 链路ID
     * @return 删除结果
     */
    int deleteByChainId(@Param("chainId") Long chainId);

    /**
     * 逻辑删除节点（设置 delete_time）
     *
     * @param id 节点ID
     * @param deleteTime 删除时间
     * @return 更新结果
     */
    int updateDeleteTime(@Param("id") Long id, @Param("deleteTime") java.time.LocalDateTime deleteTime);
}