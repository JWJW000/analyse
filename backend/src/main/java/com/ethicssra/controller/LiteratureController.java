package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.LiteratureAnalysisDto;
import com.ethicssra.dto.LiteratureDto;
import com.ethicssra.dto.LiteratureUpsertRequest;
import com.ethicssra.service.LiteratureService;
import com.ethicssra.storage.LiteratureFileStorage;
import com.ethicssra.storage.StoredFileNames;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/literature")
public class LiteratureController {

    private final LiteratureService literatureService;
    private final LiteratureFileStorage literatureFileStorage;

    public LiteratureController(LiteratureService literatureService, LiteratureFileStorage literatureFileStorage) {
        this.literatureService = literatureService;
        this.literatureFileStorage = literatureFileStorage;
    }

    @GetMapping
    public ApiResponse<List<LiteratureDto>> list(@RequestParam(required = false) String q) {
        return ApiResponse.ok(literatureService.list(q));
    }

    @GetMapping("/{id}")
    public ApiResponse<LiteratureDto> get(@PathVariable Long id) {
        return ApiResponse.ok(literatureService.get(id));
    }

    @PostMapping
    public ApiResponse<LiteratureDto> create(@Valid @RequestBody LiteratureUpsertRequest req) {
        return ApiResponse.ok(literatureService.create(req, null));
    }

    @PostMapping("/{id}/file")
    public ApiResponse<LiteratureDto> attachFile(@PathVariable Long id, @RequestParam("file") MultipartFile file)
            throws Exception {
        String stored = literatureFileStorage.store(file);
        return ApiResponse.ok(literatureService.updateFilePath(id, stored));
    }

    @GetMapping("/{id}/file")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) throws Exception {
        LiteratureDto lit = literatureService.get(id);
        if (lit.filePath() == null || lit.filePath().isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String downloadName = StoredFileNames.downloadName(lit.filePath());
        literatureFileStorage.writeToResponse(lit.filePath(), downloadName, response);
    }

    @PutMapping("/{id}")
    public ApiResponse<LiteratureDto> update(@PathVariable Long id, @Valid @RequestBody LiteratureUpsertRequest req) {
        return ApiResponse.ok(literatureService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        literatureService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/analyze")
    public ApiResponse<LiteratureAnalysisDto> analyze(@PathVariable Long id) {
        try {
            return ApiResponse.ok(literatureService.analyzeLiterature(id));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
