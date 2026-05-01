package com.quickspeech.knowledge.controller;

import com.quickspeech.common.entity.ApiResponse;
import com.quickspeech.common.entity.PageResult;
import com.quickspeech.knowledge.entity.KnowledgeBase;
import com.quickspeech.knowledge.service.KnowledgeBaseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@ConditionalOnBean(KnowledgeBaseService.class)
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping
    public ApiResponse<KnowledgeBase> create(@Valid @RequestBody KnowledgeBase kb,
                                              HttpServletRequest request) {
        return ApiResponse.success(knowledgeBaseService.create(kb, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBase> getById(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(knowledgeBaseService.getById(id, request));
    }

    @GetMapping
    public ApiResponse<PageResult<KnowledgeBase>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.success(knowledgeBaseService.list(page, pageSize, request));
    }

    @GetMapping("/owner/{ownerId}")
    public ApiResponse<List<KnowledgeBase>> listByOwner(@PathVariable Long ownerId,
                                                         HttpServletRequest request) {
        return ApiResponse.success(knowledgeBaseService.listByOwner(ownerId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBase> update(@PathVariable Long id,
                                              @RequestBody KnowledgeBase updates,
                                              HttpServletRequest request) {
        return ApiResponse.success(knowledgeBaseService.update(id, updates, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        knowledgeBaseService.delete(id, request);
        return ApiResponse.success();
    }
}
