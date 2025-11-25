package io.github.guojiaxing1995.easyJmeter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.guojiaxing1995.easyJmeter.model.JmxAssetDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JmxAssetMapper extends BaseMapper<JmxAssetDO> {
    /**
     * 逻辑删除资产（设置 delete_time 为当前时间）
     *
     * @param id 资产ID
     * @return 更新结果
     */
    int updateDeleteTime(@Param("id") Integer id);
}


