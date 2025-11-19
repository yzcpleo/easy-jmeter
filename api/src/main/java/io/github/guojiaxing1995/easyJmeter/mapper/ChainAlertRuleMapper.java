package io.github.guojiaxing1995.easyJmeter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.guojiaxing1995.easyJmeter.model.ChainAlertRuleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 链路告警规则Mapper
 *
 * @author Assistant
 * @version 1.0.0
 */
@Mapper
public interface ChainAlertRuleMapper extends BaseMapper<ChainAlertRuleDO> {

    /**
     * 根据链路ID查询告警规则
     *
     * @param chainId 链路ID
     * @return 告警规则列表
     */
    List<ChainAlertRuleDO> selectByChainId(@Param("chainId") Long chainId);

    /**
     * 根据节点ID查询告警规则
     *
     * @param nodeId 节点ID
     * @return 告警规则列表
     */
    List<ChainAlertRuleDO> selectByNodeId(@Param("nodeId") Long nodeId);

    /**
     * 根据链路ID和规则类型查询告警规则
     *
     * @param chainId 链路ID
     * @param ruleType 规则类型
     * @return 告警规则列表
     */
    List<ChainAlertRuleDO> selectByChainIdAndRuleType(@Param("chainId") Long chainId,
                                                      @Param("ruleType") String ruleType);

    /**
     * 根据节点ID和规则类型查询告警规则
     *
     * @param nodeId 节点ID
     * @param ruleType 规则类型
     * @return 告警规则列表
     */
    List<ChainAlertRuleDO> selectByNodeIdAndRuleType(@Param("nodeId") Long nodeId,
                                                      @Param("ruleType") String ruleType);

    /**
     * 根据规则名称查询告警规则
     *
     * @param chainId 链路ID
     * @param ruleName 规则名称
     * @return 告警规则
     */
    ChainAlertRuleDO selectByChainIdAndRuleName(@Param("chainId") Long chainId,
                                                @Param("ruleName") String ruleName);

    /**
     * 检查规则名称是否存在
     *
     * @param chainId 链路ID
     * @param ruleName 规则名称
     * @param excludeId 排除的ID（用于更新时检查）
     * @return 是否存在
     */
    boolean existsByRuleName(@Param("chainId") Long chainId,
                             @Param("ruleName") String ruleName,
                             @Param("excludeId") Long excludeId);

    /**
     * 查询启用的告警规则
     *
     * @param chainId 链路ID（可选）
     * @param nodeId 节点ID（可选）
     * @return 启用的告警规则列表
     */
    List<ChainAlertRuleDO> selectEnabledRules(@Param("chainId") Long chainId,
                                              @Param("nodeId") Long nodeId);
}