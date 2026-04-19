package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.EthicsModuleDto;
import com.ethicssra.dto.EthicsModuleUpsertRequest;
import com.ethicssra.domain.EthicsModuleRevision;
import com.ethicssra.service.EthicsModuleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ethics-modules")
public class EthicsModuleController {

    private final EthicsModuleService ethicsModuleService;

    public EthicsModuleController(EthicsModuleService ethicsModuleService) {
        this.ethicsModuleService = ethicsModuleService;
    }

    @GetMapping
    public ApiResponse<List<EthicsModuleDto>> list(@RequestParam(required = false) String q) {
        return ApiResponse.ok(ethicsModuleService.search(q));
    }

    @GetMapping("/{id}")
    public ApiResponse<EthicsModuleDto> get(@PathVariable Long id) {
        return ApiResponse.ok(ethicsModuleService.get(id));
    }

    @GetMapping("/{id}/revisions")
    public ApiResponse<List<EthicsModuleRevision>> revisions(@PathVariable Long id) {
        return ApiResponse.ok(ethicsModuleService.revisions(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EthicsModuleDto> create(@Valid @RequestBody EthicsModuleUpsertRequest req) {
        return ApiResponse.ok(ethicsModuleService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EthicsModuleDto> update(@PathVariable Long id, @Valid @RequestBody EthicsModuleUpsertRequest req) {
        return ApiResponse.ok(ethicsModuleService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ethicsModuleService.delete(id);
        return ApiResponse.ok(null);
    }
}
