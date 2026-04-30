package com.quickspeech.agent.service;

import com.quickspeech.common.constant.ResponseCode;
import com.quickspeech.common.entity.PageResult;
import com.quickspeech.common.exception.BusinessException;
import com.quickspeech.common.util.JwtUtil;
import com.quickspeech.agent.entity.AiAgent;
import com.quickspeech.agent.entity.PromptTemplate;
import com.quickspeech.agent.repository.AiAgentRepository;
import com.quickspeech.agent.repository.PromptTemplateRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiAgentService {

    private final AiAgentRepository agentRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final JwtUtil jwtUtil;

    public AiAgentService(AiAgentRepository agentRepository,
                          PromptTemplateRepository promptTemplateRepository,
                          JwtUtil jwtUtil) {
        this.agentRepository = agentRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AiAgent create(AiAgent agent, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        agent.setTenantId(tenantId);
        agent.setCreatedBy(getUserId(request));
        agent.setStatus("ACTIVE");
        return agentRepository.save(agent);
    }

    public AiAgent getById(Long id, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        return agentRepository.findByIdAndDeletedFalse(id)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException(ResponseCode.AGENT_NOT_FOUND));
    }

    public PageResult<AiAgent> list(int page, int pageSize, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Page<AiAgent> result = agentRepository.findAllByTenantIdAndDeletedFalse(
                tenantId, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(result.getContent(), result.getTotalElements(), page, pageSize);
    }

    public List<AiAgent> listByScenario(String scenario, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        return agentRepository.findAllByTenantIdAndScenarioAndDeletedFalse(tenantId, scenario);
    }

    @Transactional
    public AiAgent update(Long id, AiAgent updates, HttpServletRequest request) {
        AiAgent existing = getById(id, request);
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getModelProvider() != null) existing.setModelProvider(updates.getModelProvider());
        if (updates.getModelName() != null) existing.setModelName(updates.getModelName());
        if (updates.getSystemPrompt() != null) existing.setSystemPrompt(updates.getSystemPrompt());
        if (updates.getTemperature() != null) existing.setTemperature(updates.getTemperature());
        if (updates.getMaxTokens() != null) existing.setMaxTokens(updates.getMaxTokens());
        if (updates.getTopP() != null) existing.setTopP(updates.getTopP());
        if (updates.getFrequencyPenalty() != null) existing.setFrequencyPenalty(updates.getFrequencyPenalty());
        if (updates.getPresencePenalty() != null) existing.setPresencePenalty(updates.getPresencePenalty());
        if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
        if (updates.getKnowledgeBaseId() != null) existing.setKnowledgeBaseId(updates.getKnowledgeBaseId());
        if (updates.getScenario() != null) existing.setScenario(updates.getScenario());
        existing.setUpdatedBy(getUserId(request));
        return agentRepository.save(existing);
    }

    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        AiAgent existing = getById(id, request);
        existing.setDeleted(true);
        existing.setUpdatedBy(getUserId(request));
        agentRepository.save(existing);
    }

    // Prompt模板管理
    @Transactional
    public PromptTemplate createTemplate(Long agentId, PromptTemplate template, HttpServletRequest request) {
        getById(agentId, request); // 验证agent存在
        Long tenantId = getTenantId(request);
        template.setTenantId(tenantId);
        template.setAgentId(agentId);
        template.setCreatedBy(getUserId(request));
        return promptTemplateRepository.save(template);
    }

    public List<PromptTemplate> getTemplates(Long agentId, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        getById(agentId, request); // 验证agent存在
        return promptTemplateRepository.findAllByTenantIdAndAgentIdAndDeletedFalse(tenantId, agentId);
    }

    public PromptTemplate getDefaultTemplate(Long agentId, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        return promptTemplateRepository.findByTenantIdAndAgentIdAndIsDefaultTrueAndDeletedFalse(tenantId, agentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PROMPT_TEMPLATE_NOT_FOUND));
    }

    @Transactional
    public PromptTemplate updateTemplate(Long templateId, PromptTemplate updates, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        PromptTemplate existing = promptTemplateRepository.findById(templateId)
                .filter(t -> t.getTenantId().equals(tenantId) && !t.getDeleted())
                .orElseThrow(() -> new BusinessException(ResponseCode.PROMPT_TEMPLATE_NOT_FOUND));
        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getTemplateContent() != null) existing.setTemplateContent(updates.getTemplateContent());
        if (updates.getVariables() != null) existing.setVariables(updates.getVariables());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getIsDefault() != null) existing.setIsDefault(updates.getIsDefault());
        existing.setUpdatedBy(getUserId(request));
        return promptTemplateRepository.save(existing);
    }

    private Long getTenantId(HttpServletRequest request) {
        return jwtUtil.getTenantId(extractToken(request));
    }

    private Long getUserId(HttpServletRequest request) {
        return jwtUtil.getUserId(extractToken(request));
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
