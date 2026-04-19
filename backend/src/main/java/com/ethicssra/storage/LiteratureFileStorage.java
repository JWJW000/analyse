package com.ethicssra.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文献附件存储：本地磁盘或 MinIO（S3 兼容）。
 */
public interface LiteratureFileStorage {

    /**
     * 上传文件并返回持久化标识（本地为绝对路径，MinIO 为 object key）。
     */
    String store(MultipartFile file) throws IOException;

    /**
     * 将已存储文件以附件形式写入 HTTP 响应。
     */
    void writeToResponse(String storedRef, String downloadFileName, jakarta.servlet.http.HttpServletResponse response)
            throws IOException;

    /**
     * 删除存储对象（忽略不存在）。
     */
    void deleteIfExists(String storedRef);
}
