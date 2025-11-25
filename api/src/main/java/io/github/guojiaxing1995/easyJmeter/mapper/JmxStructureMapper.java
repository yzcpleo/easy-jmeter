package io.github.guojiaxing1995.easyJmeter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.guojiaxing1995.easyJmeter.model.JmxStructureDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Mapper for JMX structure data access
 */
@Repository
public interface JmxStructureMapper extends BaseMapper<JmxStructureDO> {
    
    /**
     * Find latest version by case ID
     * 
     * @param caseId Case ID
     * @return JMX structure
     */
    JmxStructureDO findLatestByCaseId(@Param("caseId") Integer caseId);
    
    /**
     * Find by case ID and version
     * 
     * @param caseId Case ID
     * @param version Version number
     * @return JMX structure
     */
    JmxStructureDO findByCaseIdAndVersion(@Param("caseId") Integer caseId, @Param("version") Integer version);
    
    /**
     * Get all versions for a case
     * 
     * @param caseId Case ID
     * @return List of structures ordered by version desc
     */
    List<JmxStructureDO> findAllVersionsByCaseId(@Param("caseId") Integer caseId);
}

