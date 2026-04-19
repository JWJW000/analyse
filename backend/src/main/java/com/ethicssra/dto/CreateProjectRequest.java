package com.ethicssra.dto;

import com.ethicssra.domain.ProjectPhase;
import com.ethicssra.domain.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateProjectRequest(
    @NotBlank(message = "项目名称不能为空")
    String name,
    String description,
    Long courseId,
    List<Long> memberIds
) {}