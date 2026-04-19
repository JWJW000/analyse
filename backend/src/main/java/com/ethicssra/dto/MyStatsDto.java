package com.ethicssra.dto;

public record MyStatsDto(
        long requirementsTotal,
        long requirementsDraft,
        long requirementsSubmitted,
        long requirementsApproved,
        long matchEvents,
        long myLiterature
) {}
