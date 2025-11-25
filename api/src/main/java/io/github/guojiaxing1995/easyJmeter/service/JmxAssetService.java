package io.github.guojiaxing1995.easyJmeter.service;

import io.github.guojiaxing1995.easyJmeter.dto.jmx.CopyJmxAssetDTO;
import io.github.guojiaxing1995.easyJmeter.dto.jmx.CreateOrUpdateJmxAssetDTO;
import io.github.guojiaxing1995.easyJmeter.model.JmxAssetDO;

import java.util.List;

public interface JmxAssetService {

    JmxAssetDO createAsset(CreateOrUpdateJmxAssetDTO dto);

    JmxAssetDO updateAsset(Integer id, CreateOrUpdateJmxAssetDTO dto);

    boolean deleteAsset(Integer id);

    JmxAssetDO getAsset(Integer id);

    List<JmxAssetDO> listAssets(Integer projectId);

    JmxAssetDO copyAsset(Integer id, CopyJmxAssetDTO dto);

    void refreshLatestStructureVersion(Integer assetId, Integer latestVersion);
}


