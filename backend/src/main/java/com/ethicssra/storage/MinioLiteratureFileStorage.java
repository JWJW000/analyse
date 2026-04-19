package com.ethicssra.storage;

import com.ethicssra.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "minio")
public class MinioLiteratureFileStorage implements LiteratureFileStorage {

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public MinioLiteratureFileStorage(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        String safeName = sanitizeFileName(file.getOriginalFilename());
        String key = "literature/" + UUID.randomUUID() + "_" + safeName;
        String bucket = storageProperties.getMinio().getBucket();
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        PutObjectRequest.Builder req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(file.getSize());
        try (InputStream in = file.getInputStream()) {
            s3Client.putObject(req.build(), RequestBody.fromInputStream(in, file.getSize()));
        }
        return key;
    }

    @Override
    public void writeToResponse(String storedRef, String downloadFileName,
                                  jakarta.servlet.http.HttpServletResponse response) throws IOException {
        String bucket = storageProperties.getMinio().getBucket();
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storedRef)
                .build();
        try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(getReq);
             var out = response.getOutputStream()) {
            GetObjectResponse meta = in.response();
            String contentType = meta.contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            response.setContentType(contentType);
            String encoded = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            Long len = meta.contentLength();
            if (len != null) {
                response.setContentLengthLong(len);
            }
            in.transferTo(out);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    public void deleteIfExists(String storedRef) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(storageProperties.getMinio().getBucket())
                    .key(storedRef)
                    .build());
        } catch (Exception ignored) {
            // 忽略删除失败（如无权限），避免影响主流程
        }
    }

    private static String sanitizeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "file";
        }
        String base = original.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        return base.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}
