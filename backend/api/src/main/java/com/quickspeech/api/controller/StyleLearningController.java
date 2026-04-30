package com.quickspeech.api.controller;

import com.quickspeech.common.entity.*;
import com.quickspeech.common.repository.UserBehaviorRecordRepository;
import com.quickspeech.common.repository.UserStyleProfileRepository;
import com.quickspeech.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/style")
public class StyleLearningController {

    private final UserBehaviorRecordRepository behaviorRecordRepository;
    private final UserStyleProfileRepository styleProfileRepository;
    private final JwtUtil jwtUtil;

    public StyleLearningController(UserBehaviorRecordRepository behaviorRecordRepository,
                                  UserStyleProfileRepository styleProfileRepository,
                                  JwtUtil jwtUtil) {
        this.behaviorRecordRepository = behaviorRecordRepository;
        this.styleProfileRepository = styleProfileRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 记录用户行为
     */
    @PostMapping("/behavior")
    public ApiResponse<UserBehaviorRecord> recordBehavior(@RequestBody UserBehaviorRecord record,
                                                           HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Long userId = getUserId(request);
        record.setTenantId(tenantId);
        record.setUserId(userId);
        record.setCreatedBy(userId);
        return ApiResponse.success(behaviorRecordRepository.save(record));
    }

    /**
     * 批量记录用户行为
     */
    @PostMapping("/behaviors")
    public ApiResponse<List<UserBehaviorRecord>> recordBehaviors(
            @RequestBody List<UserBehaviorRecord> records,
            HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Long userId = getUserId(request);
        records.forEach(r -> {
            r.setTenantId(tenantId);
            r.setUserId(userId);
            r.setCreatedBy(userId);
        });
        return ApiResponse.success(behaviorRecordRepository.saveAll(records));
    }

    /**
     * 获取用户行为记录
     */
    @GetMapping("/behaviors")
    public ApiResponse<PageResult<UserBehaviorRecord>> getBehaviors(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Long userId = getUserId(request);
        var result = behaviorRecordRepository.findAllByTenantIdAndUserIdAndDeletedFalse(
                tenantId, userId,
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ApiResponse.success(PageResult.of(result.getContent(),
                result.getTotalElements(), page, pageSize));
    }

    /**
     * 获取用户风格画像
     */
    @GetMapping("/profile")
    public ApiResponse<UserStyleProfile> getProfile(HttpServletRequest request) {
        Long userId = getUserId(request);
        return styleProfileRepository.findByUserIdAndDeletedFalse(userId)
                .map(ApiResponse::success)
                .orElseGet(() -> {
                    UserStyleProfile newProfile = new UserStyleProfile();
                    newProfile.setUserId(userId);
                    newProfile.setTenantId(getTenantId(request));
                    newProfile.setFormalityLevel(0.5);
                    newProfile.setVerbosityLevel(0.5);
                    newProfile.setEmojiFrequency(0.3);
                    newProfile.setPreferredTone("neutral");
                    newProfile.setVocabularyLevel("standard");
                    newProfile.setAvgSentenceLength(20.0);
                    newProfile.setPreferredLanguageMix("zh");
                    newProfile.setSampleCount(0);
                    return ApiResponse.success(styleProfileRepository.save(newProfile));
                });
    }

    /**
     * 更新用户风格画像
     */
    @PutMapping("/profile")
    public ApiResponse<UserStyleProfile> updateProfile(@RequestBody UserStyleProfile updates,
                                                        HttpServletRequest request) {
        Long userId = getUserId(request);
        UserStyleProfile profile = styleProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElse(new UserStyleProfile());

        profile.setUserId(userId);
        profile.setTenantId(getTenantId(request));
        if (updates.getFormalityLevel() != null) profile.setFormalityLevel(updates.getFormalityLevel());
        if (updates.getVerbosityLevel() != null) profile.setVerbosityLevel(updates.getVerbosityLevel());
        if (updates.getEmojiFrequency() != null) profile.setEmojiFrequency(updates.getEmojiFrequency());
        if (updates.getPreferredTone() != null) profile.setPreferredTone(updates.getPreferredTone());
        if (updates.getCommonPhrases() != null) profile.setCommonPhrases(updates.getCommonPhrases());
        if (updates.getVocabularyLevel() != null) profile.setVocabularyLevel(updates.getVocabularyLevel());
        if (updates.getAvgSentenceLength() != null) profile.setAvgSentenceLength(updates.getAvgSentenceLength());
        if (updates.getPreferredLanguageMix() != null) profile.setPreferredLanguageMix(updates.getPreferredLanguageMix());
        if (updates.getStyleKeywords() != null) profile.setStyleKeywords(updates.getStyleKeywords());
        profile.setLastAnalyzedAt(java.time.LocalDateTime.now());

        return ApiResponse.success(styleProfileRepository.save(profile));
    }

    /**
     * 生成/重新分析风格画像（基于行为记录）
     */
    @PostMapping("/profile/analyze")
    public ApiResponse<UserStyleProfile> analyzeProfile(HttpServletRequest request) {
        Long tenantId = getTenantId(request);
        Long userId = getUserId(request);

        // 获取最近的用户行为记录
        List<UserBehaviorRecord> records = behaviorRecordRepository
                .findAllByTenantIdAndUserIdAndDeletedFalse(tenantId, userId);

        if (records.isEmpty()) {
            return ApiResponse.error(com.quickspeech.common.constant.ResponseCode.BAD_REQUEST.getCode(),
                    "没有足够的行为数据用于分析");
        }

        // 简易风格分析
        UserStyleProfile profile = styleProfileRepository.findByUserIdAndDeletedFalse(userId)
                .orElse(new UserStyleProfile());
        profile.setUserId(userId);
        profile.setTenantId(tenantId);
        profile.setSampleCount(records.size());

        // 分析平均句子长度
        double avgLength = records.stream()
                .filter(r -> r.getModifiedText() != null && !r.getModifiedText().isEmpty())
                .mapToInt(r -> r.getModifiedText().length())
                .average().orElse(20.0);
        profile.setAvgSentenceLength(avgLength);

        // 分析emoji使用频率
        long emojiCount = records.stream()
                .filter(r -> r.getModifiedText() != null && containsEmoji(r.getModifiedText()))
                .count();
        profile.setEmojiFrequency((double) emojiCount / records.size());

        // 分析简洁度
        if (avgLength < 15) {
            profile.setVerbosityLevel(0.3);
        } else if (avgLength > 40) {
            profile.setVerbosityLevel(0.8);
        } else {
            profile.setVerbosityLevel(0.5);
        }

        profile.setLastAnalyzedAt(java.time.LocalDateTime.now());
        return ApiResponse.success(styleProfileRepository.save(profile));
    }

    private boolean containsEmoji(String text) {
        return text.codePoints().anyMatch(cp ->
                (cp >= 0x1F600 && cp <= 0x1F64F) || // Emoticons
                (cp >= 0x1F300 && cp <= 0x1F5FF) || // Misc Symbols
                (cp >= 0x1F680 && cp <= 0x1F6FF) || // Transport
                (cp >= 0x2600 && cp <= 0x26FF) ||   // Misc symbols
                (cp >= 0x2700 && cp <= 0x27BF));     // Dingbats
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
