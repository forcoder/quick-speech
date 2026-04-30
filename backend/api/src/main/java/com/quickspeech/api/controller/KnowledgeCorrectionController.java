package com.quickspeech.api.controller;

import com.quickspeech.common.constant.ResponseCode;
import com.quickspeech.common.entity.ApiResponse;
import com.quickspeech.common.entity.PageResult;
import com.quickspeech.common.entity.UserKnowledgeCorrection;
import com.quickspeech.common.exception.BusinessException;
import com.quickspeech.common.repository.UserKnowledgeCorrectionRepository;
import com.quickspeech.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/corrections")
public class KnowledgeCorrectionController {

    private final UserKnowledgeCorrectionRepository correctionRepository;
    private final JwtUtil jwtUtil;

    public KnowledgeCorrectionController(UserKnowledgeCorrectionRepository correctionRepository,
                                         JwtUtil jwtUtil) {
        this.correctionRepository = correctionRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 提交修正内容
     */
    @PostMapping
    public ApiResponse<UserKnowledgeCorrection> submit(@RequestBody UserKnowledgeCorrection correction,
                                                        HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Long userId = getUserId(request);
        correction.setTenantId(tenantId);
        correction.setUserId(userId);
        correction.setStatus("PENDING");
        correction.setCreatedBy(userId);
        return ApiResponse.success(correctionRepository.save(correction));
    }

    /**
     * 查询我的修正记录
     */
    @GetMapping("/my")
    public ApiResponse<List<UserKnowledgeCorrection>> getMyCorrections(HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Long userId = getUserId(request);
        return ApiResponse.success(
                correctionRepository.findAllByTenantIdAndUserIdAndDeletedFalse(tenantId, userId));
    }

    /**
     * 按知识库查询修正
     */
    @GetMapping("/knowledge-base/{kbId}")
    public ApiResponse<List<UserKnowledgeCorrection>> getByKnowledgeBase(
            @PathVariable Long kbId, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        return ApiResponse.success(
                correctionRepository.findAllByTenantIdAndKnowledgeBaseIdAndDeletedFalse(tenantId, kbId));
    }

    /**
     * 按状态查询修正（管理员用）
     */
    @GetMapping("/status/{status}")
    public ApiResponse<List<UserKnowledgeCorrection>> getByStatus(
            @PathVariable String status, HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        return ApiResponse.success(
                correctionRepository.findAllByTenantIdAndStatusAndDeletedFalse(tenantId, status));
    }

    /**
     * 分页查询所有修正（管理员用）
     */
    @GetMapping
    public ApiResponse<PageResult<UserKnowledgeCorrection>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Page<UserKnowledgeCorrection> result = correctionRepository.findAllByTenantIdAndDeletedFalse(
                tenantId, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(PageResult.of(result.getContent(),
                result.getTotalElements(), page, pageSize));
    }

    /**
     * 审核修正（管理员用）
     */
    @PutMapping("/{id}/review")
    public ApiResponse<UserKnowledgeCorrection> review(
            @PathVariable Long id,
            @RequestBody ReviewRequest reviewRequest,
            HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Long userId = getUserId(request);

        UserKnowledgeCorrection correction = correctionRepository.findById(id)
                .filter(c -> c.getTenantId().equals(tenantId) && !c.getDeleted())
                .orElseThrow(() -> new BusinessException(ResponseCode.NOT_FOUND, "修正记录不存在"));

        correction.setStatus(reviewRequest.getStatus());
        correction.setReviewedBy(userId);
        correction.setReviewComment(reviewRequest.getComment());
        correction.setReviewedAt(LocalDateTime.now());
        correction.setUpdatedBy(userId);

        return ApiResponse.success(correctionRepository.save(correction));
    }

    public static class ReviewRequest {
        private String status;
        private String comment;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
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
