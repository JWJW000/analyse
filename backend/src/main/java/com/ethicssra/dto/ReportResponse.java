package com.ethicssra.dto;

public record ReportResponse(
    String fileName,
    String downloadUrl,
    long fileSize
) {}