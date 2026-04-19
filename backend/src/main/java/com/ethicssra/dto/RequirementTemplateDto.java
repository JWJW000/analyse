package com.ethicssra.dto;

import java.util.List;

public record RequirementTemplateDto(
        String id,
        String title,
        String description,
        List<String> scenarioTags,
        String starterText,
        String specBackgroundHint,
        String specGoalsHint,
        String specEthicsHint
) {}
