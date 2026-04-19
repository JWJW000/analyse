package com.ethicssra.storage;

import com.ethicssra.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local")
public class LocalLiteratureFileStorage implements LiteratureFileStorage {

    private final Path localRoot;

    public LocalLiteratureFileStorage(StorageProperties storageProperties) {
        this.localRoot = Path.of(storageProperties.getLocalPath()).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        Files.createDirectories(localRoot);
        String safeName = sanitizeFileName(file.getOriginalFilename());
        String name = UUID.randomUUID() + "_" + safeName;
        Path dest = localRoot.resolve(name).toAbsolutePath().normalize();
        // Spring's multipart impl may treat relative targets as container temp paths.
        // Always use absolute path to make storage deterministic.
        file.transferTo(dest.toFile());
        return dest.toAbsolutePath().toString();
    }

    @Override
    public void writeToResponse(String storedRef, String downloadFileName,
                                  jakarta.servlet.http.HttpServletResponse response) throws IOException {
        Path path = Path.of(storedRef);
        if (!Files.isRegularFile(path)) {
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        response.setContentType(contentType);
        String encoded = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.setContentLengthLong(Files.size(path));
        Files.copy(path, response.getOutputStream());
    }

    @Override
    public void deleteIfExists(String storedRef) {
        try {
            Files.deleteIfExists(Path.of(storedRef));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
