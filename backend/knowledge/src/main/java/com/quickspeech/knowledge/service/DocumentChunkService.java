package com.quickspeech.knowledge.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkService {

    /**
     * 按段落分块
     */
    public List<String> chunkByParagraph(String content, int chunkSize, int chunkOverlap) {
        String[] paragraphs = content.split("\\n\\n+|\\r\\n\\r\\n+");
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                // 保留overlap
                String overlapText = getOverlapText(currentChunk.toString(), chunkOverlap);
                currentChunk = new StringBuilder(overlapText);
            }

            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 按固定长度分块
     */
    public List<String> chunkByFixedLength(String content, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());
            // 尝试在句子边界截断
            if (end < content.length()) {
                int lastPeriod = Math.max(
                        content.lastIndexOf('。', end),
                        content.lastIndexOf('.', end));
                int lastNewline = content.lastIndexOf('\n', end);
                int bestBreak = Math.max(lastPeriod, lastNewline);
                if (bestBreak > start && bestBreak < end) {
                    end = bestBreak + 1;
                }
            }
            chunks.add(content.substring(start, end).trim());
            start = end - chunkOverlap;
            if (start < 0) start = 0;
        }

        return chunks;
    }

    /**
     * 按语义分块（基于句子边界和主题切换）
     */
    public List<String> chunkBySemantic(String content, int chunkSize, int chunkOverlap) {
        // 先按自然段落分组
        String[] paragraphs = content.split("\\n+");
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // 标题行单独成块
            if (isHeading(paragraph)) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                chunks.add(paragraph);
                continue;
            }

            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                String overlapText = getOverlapText(currentChunk.toString(), chunkOverlap);
                currentChunk = new StringBuilder(overlapText);
            }

            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 智能分块 - 自动选择策略
     */
    public List<String> smartChunk(String content, String strategy, int chunkSize, int chunkOverlap) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        if (strategy == null) {
            strategy = "PARAGRAPH";
        }

        switch (strategy.toUpperCase()) {
            case "FIXED":
                return chunkByFixedLength(content, chunkSize, chunkOverlap);
            case "SEMANTIC":
                return chunkBySemantic(content, chunkSize, chunkOverlap);
            case "PARAGRAPH":
            default:
                return chunkByParagraph(content, chunkSize, chunkOverlap);
        }
    }

    private boolean isHeading(String text) {
        // Markdown标题
        if (text.startsWith("#")) return true;
        // 短行且以数字或中文数字开头
        if (text.length() < 50 && text.matches("^[一二三四五六七八九十]+[、.].*")) return true;
        if (text.length() < 50 && text.matches("^\\d+[、.].*")) return true;
        // 全大写短行
        if (text.length() < 30 && text.equals(text.toUpperCase()) && text.matches("[A-Z\\s]+")) return true;
        return false;
    }

    private String getOverlapText(String text, int overlapSize) {
        if (text.length() <= overlapSize) return text;
        return text.substring(text.length() - overlapSize);
    }
}
