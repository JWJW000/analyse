package com.ethicssra.dto;

public record IntegrityCheckRequest(
        String title,
        String textContent,
        String specWizardJson,
        String diagramJson,
        String embeddedModules
) {}
