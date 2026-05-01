package com.quickspeech.agent.controller;

import com.quickspeech.common.entity.ApiResponse;
import com.quickspeech.common.entity.UserStyleProfile;
import com.quickspeech.agent.service.AiAgentService;
import com.quickspeech.agent.service.AiChatService;
import com.quickspeech.common.repository.UserStyleProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@ConditionalOnBean(AiChatService.class)
public class AiChatController {

    private final AiChatService chatService;
    private final AiAgentService agentService;
    private final UserStyleProfileRepository styleProfileRepository;

    public AiChatController(AiChatService chatService,
                            AiAgentService agentService,
                            UserStyleProfileRepository styleProfileRepository) {
        this.chatService = chatService;
        this.agentService = agentService;
        this.styleProfileRepository = styleProfileRepository;
    }

    /**
     * 知识库模式 (RAG) 回复
     */
    @PostMapping("/rag")
    public ApiResponse<Map<String, Object>> ragChat(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        List<Map<String, Object>> docs = (List<Map<String, Object>>) request.get("documents");
        String systemPrompt = (String) request.getOrDefault("systemPrompt", "");
        String model = (String) request.get("model");
        return ApiResponse.success(chatService.ragChat(query, docs, systemPrompt, model));
    }

    /**
     * 智能体模式回复
     */
    @PostMapping("/agent")
    public ApiResponse<Map<String, Object>> agentChat(@RequestBody Map<String, Object> request,
                                                       HttpServletRequest httpRequest) {
        String query = (String) request.get("query");
        Long agentId = Long.valueOf(request.get("agentId").toString());
        Long templateId = request.get("templateId") != null ?
                Long.valueOf(request.get("templateId").toString()) : null;

        var agent = agentService.getById(agentId, httpRequest);
        var template = templateId != null ?
                agentService.getTemplates(agentId, httpRequest).stream()
                        .filter(t -> t.getId().equals(templateId))
                        .findFirst().orElse(null) : null;

        return ApiResponse.success(chatService.agentChat(query, agent, template));
    }

    /**
     * 混合模式回复
     */
    @PostMapping("/hybrid")
    public ApiResponse<Map<String, Object>> hybridChat(@RequestBody Map<String, Object> request,
                                                        HttpServletRequest httpRequest) {
        String query = (String) request.get("query");
        Long agentId = request.get("agentId") != null ?
                Long.valueOf(request.get("agentId").toString()) : null;
        List<Map<String, Object>> docs = (List<Map<String, Object>>) request.get("documents");
        int topN = request.get("topN") != null ? (int) request.get("topN") : 3;

        var agent = agentId != null ? agentService.getById(agentId, httpRequest) : null;
        var template = agent != null ? agentService.getDefaultTemplate(agentId, httpRequest) : null;

        // 加载用户风格画像
        UserStyleProfile styleProfile = null;
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId != null) {
            styleProfile = styleProfileRepository.findByUserIdAndDeletedFalse(Long.valueOf(userId))
                    .orElse(null);
        }

        return ApiResponse.success(
                chatService.hybridChat(query, docs, agent, template, styleProfile, topN));
    }

    /**
     * 风格化回复
     */
    @PostMapping("/styled")
    public ApiResponse<Map<String, Object>> styledChat(@RequestBody Map<String, Object> request,
                                                        HttpServletRequest httpRequest) {
        String query = (String) request.get("query");
        String systemPrompt = (String) request.getOrDefault("systemPrompt", "");
        String model = (String) request.get("model");

        UserStyleProfile styleProfile = null;
        String userId = httpRequest.getHeader("X-User-Id");
        if (userId != null) {
            styleProfile = styleProfileRepository.findByUserIdAndDeletedFalse(Long.valueOf(userId))
                    .orElse(null);
        }

        return ApiResponse.success(chatService.styledChat(query, systemPrompt, styleProfile, model));
    }

    /**
     * 测试对话接口
     */
    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> testChat(@RequestBody Map<String, Object> request) {
        Long agentId = Long.valueOf(request.get("agentId").toString());
        String message = (String) request.get("message");
        String model = (String) request.get("model");
        return ApiResponse.success(chatService.testChat(agentId, message, model));
    }
}
