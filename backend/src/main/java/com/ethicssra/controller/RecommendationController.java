package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.RecommendedModuleDto;
import com.ethicssra.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /** 个性化思政模块推荐（内容相似 + 全站热度冷启动）。 */
    @GetMapping("/ethics-modules")
    public ApiResponse<List<RecommendedModuleDto>> ethicsModules(
            @RequestParam(name = "limit", required = false, defaultValue = "8") int limit
    ) {
        int lim = Math.min(Math.max(limit, 1), 24);
        return ApiResponse.ok(recommendationService.recommendEthicsModules(lim));
    }
}
