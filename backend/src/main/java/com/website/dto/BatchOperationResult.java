package com.website.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOperationResult {
    private int totalCount;
    private int successCount;
    private int failCount;
    private String message;
    private List<FailureDetail> failures;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureDetail {
        private Long id;
        private String companyName;
        private String domain;
        private String reason;
    }
}