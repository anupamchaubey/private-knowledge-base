package com.example.knowledgebase.chat;

import com.example.knowledgebase.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing the RAG question-answering endpoint.
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Endpoints for asking questions against user's private documents via RAG")
@SecurityRequirement(name = "BearerAuth")
public class ChatController {

    private final RagChatService ragChatService;

    public ChatController(RagChatService ragChatService) {
        this.ragChatService = ragChatService;
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask a question",
            description = "Retrieves semantically similar document chunks for the authenticated user and synthesizes an answer using OpenAI LLM.")
    @ApiResponse(responseCode = "200", description = "Question answered successfully with citations")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<AnswerResponse> askQuestion(
            @Valid @RequestBody QuestionRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        AnswerResponse response = ragChatService.askQuestion(request.getQuestion(), userPrincipal.getId());
        return ResponseEntity.ok(response);
    }
}
