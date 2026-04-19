package com.ethicssra.dto;

public record SubmissionRowDto(
        Long submissionId,
        Long studentId,
        String studentName,
        Long requirementId,
        String requirementTitle,
        String status,
        String submittedAt,
        String teacherComment
) {}
