package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.ContinueTextDto;
import com.ethicssra.dto.ContinueTextRequest;
import com.ethicssra.dto.DraftSpecDto;
import com.ethicssra.dto.DraftSpecRequest;
import com.ethicssra.service.RequirementAssistService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requirements/assist")
public class RequirementAssistController {

    private final RequirementAssistService requirementAssistService;

    public RequirementAssistController(RequirementAssistService requirementAssistService) {
        this.requirementAssistService = requirementAssistService;
    }

    /** 根据标题与正文生成规格说明书向导初稿（规则生成，可再人工修改）。 */
    @PostMapping("/draft-spec")
    public ApiResponse<DraftSpecDto> draftSpec(@RequestBody DraftSpecRequest req) {
        return ApiResponse.ok(requirementAssistService.draftSpec(req.title(), req.textContent()));
    }

    /** 根据当前需求正文生成一段可追加的续写内容。 */
    @PostMapping("/continue-text")
    public ApiResponse<ContinueTextDto> continueText(@RequestBody ContinueTextRequest req) {
        return ApiResponse.ok(requirementAssistService.continueText(
                req.title(), req.textContent(), req.specWizardJson()));
    }
}
