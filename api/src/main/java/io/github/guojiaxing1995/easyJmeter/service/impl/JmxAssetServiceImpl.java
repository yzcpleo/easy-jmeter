package io.github.guojiaxing1995.easyJmeter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.guojiaxing1995.easyJmeter.common.LocalUser;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.CopyJmxAssetDTO;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.CreateOrUpdateJmxAssetDTO;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.JmxTreeNodeDTO;
import io.github.guojiaxing1995.easyJmeter.mapper.JmxAssetMapper;
import io.github.guojiaxing1995.easyJmeter.model.JmxAssetDO;
import io.github.guojiaxing1995.easyJmeter.model.JmxStructureDO;
import io.github.guojiaxing1995.easyJmeter.service.JmxAssetService;
import io.github.guojiaxing1995.easyJmeter.service.JmxStructureService;
import io.github.talelin.autoconfigure.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class JmxAssetServiceImpl implements JmxAssetService {

    @Autowired
    private JmxAssetMapper jmxAssetMapper;

    @Autowired
    private JmxStructureService jmxStructureService;

    @Override
    public JmxAssetDO createAsset(CreateOrUpdateJmxAssetDTO dto) {
        JmxAssetDO asset = new JmxAssetDO();
        asset.setName(dto.getName());
        asset.setProjectId(dto.getProjectId());
        asset.setDescription(dto.getDescription());
        asset.setJmxFileId(dto.getJmxFileId());
        asset.setCreator(LocalUser.getLocalUser().getId());
        asset.setCreationMode(resolveCreationMode(dto.getCreationMode()));
        asset.setLatestStructureVersion(0);
        jmxAssetMapper.insert(asset);
        return asset;
    }

    @Override
    public JmxAssetDO updateAsset(Integer id, CreateOrUpdateJmxAssetDTO dto) {
        JmxAssetDO asset = jmxAssetMapper.selectById(id);
        if (asset == null) {
            return null;
        }
        asset.setName(dto.getName());
        asset.setProjectId(dto.getProjectId());
        asset.setDescription(dto.getDescription());
        asset.setJmxFileId(dto.getJmxFileId());
        if (StringUtils.hasText(dto.getCreationMode())) {
            asset.setCreationMode(resolveCreationMode(dto.getCreationMode()));
        }
        jmxAssetMapper.updateById(asset);
        return asset;
    }

    @Override
    public boolean deleteAsset(Integer id) {
        // 注意：由于 deleteTime 字段带有 @TableLogic 注解，MyBatis-Plus 的 deleteById 方法
        // 对于 Date 类型会使用默认值 1 而不是当前时间，需要使用自定义的 SQL 方法来更新 delete_time
        int affectedRows = jmxAssetMapper.updateDeleteTime(id);
        if (affectedRows == 0) {
            throw new NotFoundException(12401); // JMX资产不存在
        }
        return affectedRows > 0;
    }

    @Override
    public JmxAssetDO getAsset(Integer id) {
        return jmxAssetMapper.selectById(id);
    }

    @Override
    public List<JmxAssetDO> listAssets(Integer projectId) {
        QueryWrapper<JmxAssetDO> wrapper = new QueryWrapper<>();
        wrapper.isNull("delete_time");
        if (projectId != null) {
            wrapper.eq("project_id", projectId);
        }
        wrapper.orderByDesc("update_time");
        return jmxAssetMapper.selectList(wrapper);
    }

    @Override
    public JmxAssetDO copyAsset(Integer id, CopyJmxAssetDTO dto) {
        JmxAssetDO asset = jmxAssetMapper.selectById(id);
        if (asset == null) {
            return null;
        }
        JmxAssetDO copy = new JmxAssetDO();
        BeanUtils.copyProperties(asset, copy, "id", "createTime", "updateTime", "deleteTime", "name", "latestStructureVersion");
        copy.setName(dto.getName());
        copy.setLatestStructureVersion(asset.getLatestStructureVersion());
        jmxAssetMapper.insert(copy);

        // clone structure if exists
        try {
            JmxTreeNodeDTO structure = jmxStructureService.getLatestStructureForAsset(id);
            if (structure != null) {
                JmxStructureDO saved = jmxStructureService.saveAssetStructure(copy.getId(), structure);
                if (saved != null) {
                    refreshLatestStructureVersion(copy.getId(), saved.getVersion());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clone structure for asset copy {} -> {}", id, copy.getId(), e);
        }
        return copy;
    }

    @Override
    public void refreshLatestStructureVersion(Integer assetId, Integer latestVersion) {
        JmxAssetDO asset = jmxAssetMapper.selectById(assetId);
        if (asset == null) {
            return;
        }
        asset.setLatestStructureVersion(latestVersion);
        jmxAssetMapper.updateById(asset);
    }

    private String resolveCreationMode(String incoming) {
        if (!StringUtils.hasText(incoming)) {
            return "UPLOAD";
        }
        return incoming.toUpperCase();
    }
}


