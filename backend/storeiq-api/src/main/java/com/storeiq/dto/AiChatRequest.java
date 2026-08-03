package com.storeiq.dto;

/**
 * AI 对话请求
 */
public class AiChatRequest {

    private String question;
    private String context; // 可选：前端传入的额外上下文

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
}
