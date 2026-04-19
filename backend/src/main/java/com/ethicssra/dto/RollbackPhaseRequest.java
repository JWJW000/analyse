package com.ethicssra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RollbackPhaseRequest(
        @NotBlank(message = "请填写回退原因")
        @Size(min = 2, max = 2000, message = "原因长度需在 2～2000 字")
        String reason
) {}
