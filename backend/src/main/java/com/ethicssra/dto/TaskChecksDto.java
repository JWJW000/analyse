package com.ethicssra.dto;

import java.util.List;

public record TaskChecksDto(
        boolean readyForSubmission,
        List<String> blockingIssues
) {
}
