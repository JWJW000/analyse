package com.ethicssra.dto;

import java.util.List;

public record AiAnswerDto(
    String answer,
    List<DiscussionPostDto> similarQuestions
) {}