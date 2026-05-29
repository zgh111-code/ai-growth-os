package com.al.aichat.dto;

import lombok.Data;
import java.util.List;

/**
 * DeepSeek Embedding API 响应体
 */
@Data
public class EmbeddingResponse {
    private String object;
    private List<EmbeddingData> data;
    private String model;
    private Usage usage;

    @Data
    public static class EmbeddingData {
        private String object;
        private List<Float> embedding;
        private Integer index;
    }

    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer totalTokens;
    }
}
