package com.al.aichat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DeepSeek Embedding API 请求体
 * POST https://api.deepseek.com/v1/embeddings
 */
@Data
@AllArgsConstructor
public class EmbeddingRequest {
    private String model;
    private String input;
}
