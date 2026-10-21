package com.example.knowledgebase.chat;

import java.util.List;

public class AnswerResponse {

    private final String question;
    private final String answer;
    private final List<SourceReference> sources;
    private final boolean grounded;

    public AnswerResponse(String question, String answer, List<SourceReference> sources, boolean grounded) {
        this.question = question;
        this.answer = answer;
        this.sources = sources;
        this.grounded = grounded;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public List<SourceReference> getSources() {
        return sources;
    }

    public boolean isGrounded() {
        return grounded;
    }
}
