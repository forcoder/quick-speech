package com.quickspeech.agent.controller;

import com.quickspeech.common.entity.ApiResponse;
import com.quickspeech.common.entity.PageResult;
import com.quickspeech.agent.entity.AiAgent;
import com.quickspeech.agent.entity.PromptTemplate;
import com.quickspeech.agent.service.AiAgentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
@ConditionalOnBean(AiAgentService.class)
public class AiAgentController {

    private final AiAgentService agentService;

    public AiAgentController(AiAgentService agentService) {
        this.agentService = agentService;
    }

    // === 智能体 CRUD ===

    @PostMapping
    public ApiResponse<AiAgent> create(@Valid @RequestBody AiAgent agent, HttpServletRequest request) {
        return ApiResponse.success(agentService.create(agent, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiAgent> getById(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(agentService.getById(id, request));
    }

    @GetMapping
    public ApiResponse<PageResult<AiAgent>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(agentService.list(page, pageSize, request));
    }

    @GetMapping("/scenario/{scenario}")
    public ApiResponse<List<AiAgent>> listByScenario(@PathVariable String scenario,
                                                      HttpServletRequest request) {
        return ApiResponse.success(agentService.listByScenario(scenario, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AiAgent> update(@PathVariable Long id,
                                        @RequestBody AiAgent updates,
                                        HttpServletRequest request) {
        return ApiResponse.success(agentService.update(id, updates, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        agentService.delete(id, request);
        return ApiResponse.success();
    }

    // === Prompt 模板管理 ===

    @PostMapping("/{agentId}/templates")
    public ApiResponse<PromptTemplate> createTemplate(
            @PathVariable Long agentId,
            @Valid @RequestBody PromptTemplate template,
            HttpServletRequest request) {
        return ApiResponse.success(agentService.createTemplate(agentId, template, request));
    }

    @GetMapping("/{agentId}/templates")
    public ApiResponse<List<PromptTemplate>> getTemplates(
            @PathVariable Long agentId, HttpServletRequest request) {
        return ApiResponse.success(agentService.getTemplates(agentId, request));
    }

    @GetMapping("/{agentId}/templates/default")
    public ApiResponse<PromptTemplate> getDefaultTemplate(
            @PathVariable Long agentId, HttpServletRequest request) {
        return ApiResponse.success(agentService.getDefaultTemplate(agentId, request));
    }

    @PutMapping("/templates/{templateId}")
    public ApiResponse<PromptTemplate> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody PromptTemplate updates,
            HttpServletRequest request) {
        return ApiResponse.success(agentService.updateTemplate(templateId, updates, request));
    }
}
