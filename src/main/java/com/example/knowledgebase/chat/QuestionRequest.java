package com.example.knowledgebase.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class QuestionRequest {

    @NotBlank(message = "Question must not be blank")
    @Size(max = 1000, message = "Question must not exceed 1000 characters")
    @Schema(description = "The question to ask about the uploaded documents", example = "What is dependency injection in Spring?")
    private String question;

    public QuestionRequest() {
    }

    public QuestionRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
