package com.ethicssra.dto;

import java.util.List;
import java.util.Map;

public record DiagramGenerationDto(
    String diagramType,
    List<DiagramNode> nodes,
    List<DiagramEdge> edges,
    String explanation,
    List<String> recommendations
) {
    public record DiagramNode(
        String id,
        String label,
        String type,
        String description
    ) {}

    public record DiagramEdge(
        String id,
        String source,
        String target,
        String label
    ) {}
}
