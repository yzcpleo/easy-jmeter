package io.github.guojiaxing1995.easyJmeter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainTraceConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 链路追踪配置Mapper
 *
 * @author Assistant
 * @version 1.0.0
 */
@Mapper
public interface ChainTraceConfigMapper extends BaseMapper<ChainTraceConfigDO> {

    /**
     * 根据任务ID查询链路配置
     *
     * @param taskId 任务ID
     * @return 链路配置列表
     */
    List<ChainTraceConfigDO> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 根据链路名称查询链路配置
     *
     * @param chainName 链路名称
     * @return 链路配置
     */
    ChainTraceConfigDO selectByChainName(@Param("chainName") String chainName);

    /**
     * 检查链路名称是否存在
     *
     * @param chainName 链路名称
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 是否存在
     */
    boolean existsByChainName(@Param("chainName") String chainName, @Param("excludeId") Long excludeId);
}