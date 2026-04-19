package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.SearchResponseDto;
import com.ethicssra.service.SearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** 全局搜索：思政模块、文献、需求（按角色过滤需求可见范围）。 */
    @GetMapping
    public ApiResponse<SearchResponseDto> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "limit", required = false, defaultValue = "30") int limit
    ) {
        int lim = Math.min(Math.max(limit, 1), 60);
        return ApiResponse.ok(searchService.search(q, lim));
    }
}
