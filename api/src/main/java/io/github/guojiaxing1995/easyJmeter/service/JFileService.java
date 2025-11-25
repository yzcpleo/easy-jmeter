package io.github.guojiaxing1995.easyJmeter.service;

import io.github.guojiaxing1995.easyJmeter.model.JFileDO;
import io.github.guojiaxing1995.easyJmeter.vo.CutFileVO;
import io.github.guojiaxing1995.easyJmeter.vo.JFileVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface JFileService {

    JFileVO createFile(MultipartFile file);

    Boolean setFileCut(Integer id, Boolean cut);

    String downloadFile(Integer id, String dir);

    void downloadCutFile(List<CutFileVO> cutFileVOList, String dir);

    List<JFileDO> createCsvCutFiles(Map<Integer, List<String>> fileMp);

    JFileDO searchById(Integer id);

    Boolean needCut(String[] fileIds);

    JFileDO createFile(String filePath);

    Boolean updateById(JFileDO jFileDO);

    void downLoadJmeterLogZip(String taskId, OutputStream outputStream) throws IOException;

    List<JFileDO> searchJtlByTaskId(String taskId);

    String getStoreDir();
    
    /**
     * Get file from URL or local path. Downloads file from MinIO/URL to temp directory if needed.
     * @param jFileDO File metadata from database
     * @return File object, or null if file cannot be accessed
     */
    File getFileFromUrl(JFileDO jFileDO) throws IOException;
}
