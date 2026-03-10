package com.ghana.commoditymonitor.dto.request;

import jakarta.validation.constraints.NotNull;

public record PriceRecordApprovalDto(
    @NotNull(message = "Price record ID is required")
    Long priceRecordId,

    @NotNull(message = "Approval decision is required")
    Boolean approved,

    String rejectionReason
) {}
