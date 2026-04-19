package com.ethicssra.dto;

public record ContinueTextRequest(
        String title,
        String textContent,
        String specWizardJson
) {
}
