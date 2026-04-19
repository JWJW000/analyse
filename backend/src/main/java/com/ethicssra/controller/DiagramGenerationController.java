package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.DiagramGenerationDto;
import com.ethicssra.service.DiagramGenerationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class DiagramGenerationController {

    private final DiagramGenerationService diagramGenerationService;

    public DiagramGenerationController(DiagramGenerationService diagramGenerationService) {
        this.diagramGenerationService = diagramGenerationService;
    }

    @PostMapping("/generate-flowchart")
    public ApiResponse<DiagramGenerationDto> generateFlowchart(@RequestBody GenerateFlowchartRequest request) {
        try {
            DiagramGenerationDto diagram = diagramGenerationService.generateFlowchart(request.text());
            return ApiResponse.ok(diagram);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/generate-use-case")
    public ApiResponse<DiagramGenerationDto> generateUseCaseDiagram(@RequestBody GenerateFlowchartRequest request) {
        try {
            DiagramGenerationDto diagram = diagramGenerationService.generateUseCaseDiagram(request.text());
            return ApiResponse.ok(diagram);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    public record GenerateFlowchartRequest(String text) {}
}
