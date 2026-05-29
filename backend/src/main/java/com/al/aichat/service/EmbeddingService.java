package com.al.aichat.service;

import com.al.aichat.common.BusinessException;
import com.al.aichat.config.DeepSeekConfig;
import com.al.aichat.dto.EmbeddingRequest;
import com.al.aichat.dto.EmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * DeepSeek Embedding 服务
 * 调用 Embedding API 将文本转为向量，供知识库语义检索使用
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private final DeepSeekConfig config;
    private final RestTemplate restTemplate;

    public EmbeddingService(DeepSeekConfig config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    /**
     * 对单条文本生成 embedding 向量
     * @return 1536 维 float 数组
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文本不能为空");
        }

        int maxRetries = 2;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                EmbeddingRequest req = new EmbeddingRequest(config.getEmbeddingModel(), text);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(config.getApiKey());

                HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(req, headers);

                ResponseEntity<EmbeddingResponse> resp = restTemplate.postForEntity(
                        config.getEmbeddingApiUrl(), entity, EmbeddingResponse.class);

                EmbeddingResponse body = resp.getBody();
                if (body == null || body.getData() == null || body.getData().isEmpty()) {
                    throw new BusinessException("Embedding API 返回为空");
                }

                List<Float> embeddingList = body.getData().get(0).getEmbedding();
                float[] arr = new float[embeddingList.size()];
                for (int i = 0; i < embeddingList.size(); i++) {
                    arr[i] = embeddingList.get(i);
                }
                return arr;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    log.error("Embedding 失败（已重试{}次）: {}", maxRetries, e.getMessage());
                    throw new BusinessException("向量生成失败，请稍后重试");
                }
                log.warn("Embedding 重试 {}/{}", attempt + 1, maxRetries);
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        throw new BusinessException("向量生成失败");
    }
}
