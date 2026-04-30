package com.quickspeech.agent.service;

import com.quickspeech.common.exception.BusinessException;
import com.quickspeech.common.constant.ResponseCode;
import com.quickspeech.agent.entity.AiAgent;
import com.quickspeech.agent.entity.PromptTemplate;
import com.quickspeech.common.entity.UserStyleProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiChatService {

    @Value("${ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${ai.openai.api-url:https://api.openai.com/v1/chat/completions}")
    private String openaiApiUrl;

    @Value("${ai.default-model:gpt-4o-mini}")
    private String defaultModel;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 知识库模式 (RAG) 生成回复
     */
    public Map<String, Object> ragChat(String query, List<Map<String, Object>> retrievedDocs,
                                       String systemPrompt, String model) {
        // 构建RAG Prompt
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < retrievedDocs.size(); i++) {
            Map<String, Object> doc = retrievedDocs.get(i);
            context.append("文档片段 ").append(i + 1).append(":\n");
            context.append(doc.getOrDefault("document", "")).append("\n\n");
        }

        String fullSystemPrompt = systemPrompt + "\n\n参考以下知识库内容回答用户问题:\n" + context;

        return callAiApi(query, fullSystemPrompt, model);
    }

    /**
     * 智能体模式生成回复
     */
    public Map<String, Object> agentChat(String query, AiAgent agent, PromptTemplate template) {
        String systemPrompt = agent.getSystemPrompt();
        if (template != null) {
            systemPrompt = template.getTemplateContent();
        }
        return callAiApi(query, systemPrompt, agent.getModelName());
    }

    /**
     * 风格化生成
     */
    public Map<String, Object> styledChat(String query, String systemPrompt,
                                           UserStyleProfile styleProfile, String model) {
        String stylePrompt = buildStylePrompt(styleProfile);
        String fullPrompt = systemPrompt + "\n\n" + stylePrompt;
        return callAiApi(query, fullPrompt, model);
    }

    /**
     * 混合模式：并行执行知识库和智能体模式，综合排序返回Top N
     */
    public Map<String, Object> hybridChat(String query, List<Map<String, Object>> retrievedDocs,
                                           AiAgent agent, PromptTemplate template,
                                           UserStyleProfile styleProfile, int topN) {
        List<Map<String, Object>> results = new ArrayList<>();

        // 知识库模式结果
        if (retrievedDocs != null && !retrievedDocs.isEmpty()) {
            try {
                Map<String, Object> ragResult = ragChat(query, retrievedDocs,
                        agent != null ? agent.getSystemPrompt() : "",
                        agent != null ? agent.getModelName() : defaultModel);
                ragResult.put("source", "knowledge");
                ragResult.put("score", computeRelevanceScore(retrievedDocs));
                results.add(ragResult);
            } catch (Exception e) {
                // 知识库模式失败不影响整体
            }
        }

        // 智能体模式结果
        if (agent != null) {
            try {
                Map<String, Object> agentResult = agentChat(query, agent, template);
                agentResult.put("source", "agent");
                agentResult.put("score", 0.8);
                results.add(agentResult);
            } catch (Exception e) {
                // 智能体模式失败不影响整体
            }
        }

        // 风格化生成结果
        if (styleProfile != null) {
            try {
                Map<String, Object> styledResult = styledChat(query,
                        agent != null ? agent.getSystemPrompt() : "",
                        styleProfile, agent != null ? agent.getModelName() : defaultModel);
                styledResult.put("source", "styled");
                styledResult.put("score", 0.7);
                results.add(styledResult);
            } catch (Exception e) {
                // 风格化失败不影响整体
            }
        }

        // 按评分排序
        results.sort((a, b) -> Double.compare(
                (double) b.getOrDefault("score", 0.0),
                (double) a.getOrDefault("score", 0.0)));

        // 返回Top N
        Map<String, Object> response = new HashMap<>();
        response.put("results", results.subList(0, Math.min(topN, results.size())));
        response.put("topResult", results.isEmpty() ? null : results.get(0));
        return response;
    }

    /**
     * 调用AI API
     */
    private Map<String, Object> callAiApi(String userMessage, String systemPrompt, String model) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
                headers.setBearerAuth(openaiApiKey);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model != null ? model : defaultModel);

            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, Object> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt);
                messages.add(systemMsg);
            }
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(openaiApiUrl, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseAiResponse(response.getBody());
            }
            throw new BusinessException(ResponseCode.INTERNAL_ERROR, "AI API调用失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCode.INTERNAL_ERROR,
                    "AI调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试对话接口
     */
    public Map<String, Object> testChat(Long agentId, String message, String model) {
        Map<String, Object> result = new HashMap<>();
        result.put("agentId", agentId);
        result.put("message", message);
        result.put("model", model != null ? model : defaultModel);
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "success");
        return result;
    }

    private String buildStylePrompt(UserStyleProfile profile) {
        if (profile == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("请按照以下风格要求回复:\n");

        if (profile.getFormalityLevel() != null) {
            if (profile.getFormalityLevel() > 0.7) {
                sb.append("- 使用正式、专业的语言\n");
            } else if (profile.getFormalityLevel() < 0.3) {
                sb.append("- 使用轻松、口语化的语言\n");
            } else {
                sb.append("- 使用适中、自然的语言\n");
            }
        }

        if (profile.getVerbosityLevel() != null) {
            if (profile.getVerbosityLevel() > 0.7) {
                sb.append("- 回复应详细、丰富\n");
            } else if (profile.getVerbosityLevel() < 0.3) {
                sb.append("- 回复应简洁、精炼\n");
            }
        }

        if (profile.getPreferredTone() != null) {
            sb.append("- 语气: ").append(profile.getPreferredTone()).append("\n");
        }

        if (profile.getEmojiFrequency() != null && profile.getEmojiFrequency() > 0.5) {
            sb.append("- 适当使用表情符号\n");
        }

        if (profile.getCommonPhrases() != null && !profile.getCommonPhrases().isEmpty()) {
            sb.append("- 常用表达: ").append(profile.getCommonPhrases()).append("\n");
        }

        return sb.toString();
    }

    private double computeRelevanceScore(List<Map<String, Object>> docs) {
        if (docs == null || docs.isEmpty()) return 0.0;
        double totalScore = 0.0;
        for (Map<String, Object> doc : docs) {
            totalScore += (double) doc.getOrDefault("score", 0.0);
        }
        return totalScore / docs.size();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAiResponse(Map<String, Object> responseBody) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            result.put("content", message.get("content"));
            result.put("model", responseBody.get("model"));
            result.put("usage", responseBody.get("usage"));
        }
        return result;
    }
}
