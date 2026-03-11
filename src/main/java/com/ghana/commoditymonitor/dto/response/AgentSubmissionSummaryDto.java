package com.ghana.commoditymonitor.dto.response;

import java.math.BigDecimal;

public record AgentSubmissionSummaryDto(
    Long userId,
    String username,
    Long totalSubmitted,
    Long totalApproved,
    Long totalRejected,
    Long totalPending,
    BigDecimal approvalRate
) {}
