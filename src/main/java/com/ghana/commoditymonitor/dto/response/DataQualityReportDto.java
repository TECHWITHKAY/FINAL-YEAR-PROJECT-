package com.ghana.commoditymonitor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityReportDto {
    private Double overallCompleteness;
    private Map<String, Long> dataFreshnessBreakdown;
    private List<DuplicateAlertDto> duplicateAlerts;
    private List<OutlierAlertDto> outlierAlerts;
    private List<AgentSubmissionSummaryDto> submissionsByAgent;
    private OffsetDateTime reportGeneratedAt;
}
