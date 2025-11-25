package io.github.guojiaxing1995.easyJmeter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.guojiaxing1995.easyJmeter.mapper.JFileMapper;
import io.github.guojiaxing1995.easyJmeter.model.JFileDO;
import io.github.guojiaxing1995.easyJmeter.module.file.FileProperties;
import io.github.guojiaxing1995.easyJmeter.service.JFileService;
import io.github.guojiaxing1995.easyJmeter.vo.CutFileVO;
import io.github.guojiaxing1995.easyJmeter.vo.JFileVO;
import io.github.talelin.autoconfigure.exception.FailedException;
import io.github.talelin.autoconfigure.exception.NotFoundException;
import io.github.talelin.autoconfigure.exception.ParameterException;
import io.minio.DownloadObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class JFileServiceImpl implements JFileService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Autowired
    public JFileServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Autowired
    private FileProperties fileProperties;

    @Autowired
    private JFileMapper jFileMapper;

    private static final int BUFFER_SIZE = 1024 * 1024 * 2;

    @Override
    public JFileVO createFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        String fileExtension = StringUtils.getFilenameExtension(name);
        if (!Arrays.asList("csv", "jar", "jmx").contains(fileExtension)) {
            throw new ParameterException(12102);
        }
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String objectName = timestamp + "." + fileExtension;
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(file.getInputStream(), file.getSize(), -1).build());
        } catch (Exception e) {
            log.info(e.toString());
            throw new FailedException(12101);
        }
        JFileDO jFileDO = new JFileDO();
        jFileDO.setName(name);
        jFileDO.setPath("/" + bucketName + "/" + objectName);
        jFileDO.setUrl(endpoint + "/" + bucketName + "/" + objectName);
        jFileDO.setSize(file.getSize());
        jFileDO.setType(file.getContentType());
        jFileMapper.insert(jFileDO);

        JFileDO fileDO = jFileMapper.selectById(jFileDO.getId());
        if (fileDO.getSize()/(1024 * 1024) >= 1) {
            return new JFileVO(fileDO, String.format("%.2f", fileDO.getSize()/(float)(1024 * 1024)) + "MB");
        } else if (fileDO.getSize()/1024 >= 1) {
            return new JFileVO(fileDO, String.format("%.2f", fileDO.getSize()/(float)1024) + "KB");
        } else {
            return new JFileVO(fileDO, fileDO.getSize() + "B");
        }
    }

    @Override
    public Boolean setFileCut(Integer id, Boolean cut) {
        JFileDO jFileDO = jFileMapper.selectById(id);
        if (jFileDO == null){
            throw new NotFoundException(12103);
        }
        jFileDO.setCut(cut);
        return jFileMapper.updateById(jFileDO) > 0;
    }

    @Override
    public String downloadFile(Integer id, String dir){
        JFileDO jFileDO = jFileMapper.selectById(id);
        String filePath;
        if (dir == null) {
            String storeDir = this.getStoreDir();
            filePath = Paths.get(storeDir, String.valueOf(Instant.now().toEpochMilli()) + jFileDO.getName()).toString();
        } else {
            java.io.File file = new File(dir);
            if (!file.exists()) {
                file.mkdirs();
            }
            filePath = Paths.get(dir, jFileDO.getName()).toString();

        }

        String bucket = jFileDO.getPath().split("/")[1];
        String name = jFileDO.getPath().split("/")[2];
        try {
            minioClient.downloadObject(DownloadObjectArgs.builder().bucket(bucket).object(name).filename(filePath).build());
        } catch (Exception e) {
            log.error("文件下载异常:" + e);
            throw new RuntimeException(e);
        }

        return filePath;
    }

    @Override
    public void downloadCutFile(List<CutFileVO> cutFileVOList, String dir) {
        java.io.File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
        for (CutFileVO cutFileVO : cutFileVOList) {
            JFileDO jFileDO = jFileMapper.selectById(cutFileVO.getId());
            String bucket = jFileDO.getPath().split("/")[1];
            String name = jFileDO.getPath().split("/")[2];
            String filePath = Paths.get(dir, cutFileVO.getOriginName()).toString();
            try {
                minioClient.downloadObject(DownloadObjectArgs.builder().bucket(bucket).object(name).filename(filePath).build());
            } catch (Exception e) {
                log.error("文件下载异常:" + e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<JFileDO> createCsvCutFiles(Map<Integer, List<String>> fileMap) {
        Map.Entry<Integer, List<String>> entry = fileMap.entrySet().iterator().next();
        // 切分文件id
        Integer fid = entry.getKey();
        List<String> filePath = entry.getValue();
        List<JFileDO> jFileDOS = new ArrayList<>();
        for (String path: filePath) {
            File file = new File(path);
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            String fileName = timestamp + "_" + file.getName();
            try {
                minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(new FileInputStream(file), file.length(), -1).build());
            } catch (Exception e) {
                log.error("上传切分文件异常:" + e);
            }
            JFileDO cutfile = new JFileDO();
            cutfile.setName(file.getName());
            cutfile.setPath("/" + bucketName + "/" + fileName);
            cutfile.setUrl(endpoint + "/" + bucketName + "/" + fileName);
            cutfile.setSize(file.length());
            cutfile.setType(file.getName().substring(file.getName().lastIndexOf(".")+1));
            cutfile.setOriginId(fid);
            jFileMapper.insert(cutfile);
            JFileDO fileDO = jFileMapper.selectById(cutfile.getId());
            jFileDOS.add(fileDO);
        }
        return jFileDOS;
    }

    @Override
    public JFileDO searchById(Integer id) {
        return jFileMapper.selectById(id);
    }

    @Override
    public Boolean needCut(String[] fileIds) {
        for (String id : fileIds) {
            JFileDO jFileDO = jFileMapper.selectById(Integer.valueOf(id));
            if (jFileDO.getCut()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JFileDO createFile(String filePath) {
        File file = new File(filePath);
        String fileName = Instant.now().toEpochMilli() + "_" + file.getName();
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(new FileInputStream(file), file.length(), -1).build());
        } catch (Exception e) {
            log.error("文件上传异常:" + e);
            throw new RuntimeException(e);
        }
        JFileDO jFileDO = new JFileDO();
        jFileDO.setPath("/" + bucketName + "/" + fileName);
        jFileDO.setName(file.getName());
        jFileDO.setUrl(endpoint + "/" + bucketName + "/" + fileName);
        jFileDO.setSize(file.length());
        jFileDO.setType(file.getName().substring(file.getName().lastIndexOf(".")+1));
        jFileMapper.insert(jFileDO);
        return jFileMapper.selectById(jFileDO.getId());
    }

    @Override
    public Boolean updateById(JFileDO jFileDO) {
        return jFileMapper.updateById(jFileDO) > 0;
    }

    @Override
    public void downLoadJmeterLogZip(String taskId, OutputStream outputStream) throws IOException {
        QueryWrapper<JFileDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("type", "log");
        List<JFileDO> jFileDOS = jFileMapper.selectList(queryWrapper);
        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(outputStream)) {
            for (JFileDO fileDO : jFileDOS){
                String fileName = new File(fileDO.getPath()).getName();
                log.info(fileName);
                try {
                    InputStream stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(fileName).build());
                    ZipArchiveEntry entry = new ZipArchiveEntry(fileName);
                    zipOut.putArchiveEntry(entry);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = stream.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, bytesRead);
                    }
                    zipOut.closeArchiveEntry();
                } catch (Exception e) {
                    log.error("文件上传异常:" + e);
                    throw new RuntimeException(e);
                }
            }
            zipOut.finish();
        }
    }

    @Override
    public List<JFileDO> searchJtlByTaskId(String taskId) {
        QueryWrapper<JFileDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("task_id", taskId).eq("type", "jtl");

        return jFileMapper.selectList(queryWrapper);
    }

    @Override
    public String getStoreDir() {
        String format = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        Path path = Paths.get(fileProperties.getStoreDir(), format).toAbsolutePath();
        java.io.File file = new File(path.toString());
        if (!file.exists()) {
            file.mkdirs();
        }
        return path.toString();
    }

    @Override
    public File getFileFromUrl(JFileDO jFileDO) throws IOException {
        if (jFileDO == null) {
            return null;
        }
        
        String path = jFileDO.getPath();
        if (path == null || path.isEmpty()) {
            log.warn("File path is null or empty for file ID: {}", jFileDO.getId());
            return null;
        }
        
        // Try local file first
        File localFile = new File(path);
        if (localFile.exists() && localFile.isFile()) {
            log.info("Found local file: {}", path);
            return localFile;
        }
        
        // Try with store directory prefix
        String storeDir = fileProperties.getStoreDir();
        File prefixedFile = new File(storeDir, path);
        if (prefixedFile.exists() && prefixedFile.isFile()) {
            log.info("Found file with store dir prefix: {}", prefixedFile.getAbsolutePath());
            return prefixedFile;
        }
        
        // Download from MinIO to temp directory
        try {
            // Parse path to extract bucket and object name
            // Path format: /bucketName/objectName (e.g., /dev/1764042285427.jmx)
            String[] pathParts = path.split("/");
            String bucket = pathParts.length > 1 ? pathParts[1] : bucketName;
            String objectName = pathParts.length > 2 ? pathParts[2] : path;
            
            // Create temp file
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = objectName; // Use objectName as filename
            File tempFile = new File(tempDir, "jmx_temp_" + System.currentTimeMillis() + "_" + fileName);
            
            log.info("Downloading file from MinIO: bucket={}, object={}, tempFile={}", 
                     bucket, objectName, tempFile.getAbsolutePath());
            
            // Download object from MinIO
            minioClient.downloadObject(
                DownloadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(tempFile.getAbsolutePath())
                    .build()
            );
            
            if (tempFile.exists() && tempFile.length() > 0) {
                log.info("Successfully downloaded file from MinIO: {} bytes", tempFile.length());
                return tempFile;
            } else {
                log.error("Downloaded file is empty or doesn't exist");
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: path={}, error={}", path, e.getMessage(), e);
            throw new IOException("Failed to download file from storage: " + e.getMessage(), e);
        }
    }

}
