package com.quickspeech.common.constant;

import lombok.Getter;

@Getter
public enum ResponseCode {

    SUCCESS(200, "操作成功"),
    CREATED(201, "创建成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务错误码
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    INVALID_CREDENTIALS(1003, "用户名或密码错误"),
    TOKEN_EXPIRED(1004, "Token已过期"),
    TOKEN_INVALID(1005, "Token无效"),
    KNOWLEDGE_BASE_NOT_FOUND(2001, "知识库不存在"),
    DOCUMENT_NOT_FOUND(2002, "文档不存在"),
    AGENT_NOT_FOUND(3001, "智能体不存在"),
    PROMPT_TEMPLATE_NOT_FOUND(3002, "Prompt模板不存在"),
    MODEL_NOT_SUPPORTED(3003, "不支持的模型"),
    VECTOR_DB_ERROR(4001, "向量数据库操作失败"),
    EMBEDDING_ERROR(4002, "向量化失败"),
    FILE_PARSE_ERROR(4003, "文件解析失败"),
    TENANT_ACCESS_DENIED(5001, "租户访问受限");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
