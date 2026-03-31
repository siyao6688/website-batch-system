package com.website.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchOperationRequest {
    private List<Long> ids;
}