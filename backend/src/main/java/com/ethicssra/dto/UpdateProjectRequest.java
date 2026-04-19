package com.ethicssra.dto;

import com.ethicssra.domain.ProjectPhase;
import com.ethicssra.domain.ProjectStatus;

public record UpdateProjectRequest(
    String name,
    String description,
    ProjectPhase currentPhase,
    ProjectStatus status
) {}