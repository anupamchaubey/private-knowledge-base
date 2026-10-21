package com.example.knowledgebase.chat;

import com.example.knowledgebase.common.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagChatServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private RagChatService ragChatService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ragChatService = new RagChatService(vectorStore, chatClientBuilder, 4, 0.60);
    }

    @Test
    @DisplayName("Should abstain cleanly when no matching chunks are retrieved above threshold")
    void testAskQuestionEmptyRetrievalAbstention() {
        UUID userId = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        AnswerResponse response = ragChatService.askQuestion("What is Spring Boot?", userId);

        assertNotNull(response);
        assertFalse(response.isGrounded());
        assertTrue(response.getSources().isEmpty());
        assertTrue(response.getAnswer().contains("could not find any sufficiently relevant information"));

        // Critical safety verification: ChatClient should NEVER be called when retrieval is empty
        verify(chatClient, never()).prompt();
    }

    @Test
    @DisplayName("Should retrieve user-scoped context and generate grounded answer with citations")
    void testAskQuestionWithRetrievedContext() {
        UUID userId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Document chunk = new Document(
                "Spring Boot makes it easy to create stand-alone Spring-based applications.",
                Map.of(
                        "userId", userId.toString(),
                        "documentId", docId.toString(),
                        "originalFilename", "spring_notes.pdf",
                        "pageNumber", 3
                )
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(chunk));

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Spring Boot simplifies creating stand-alone Spring applications.");

        AnswerResponse response = ragChatService.askQuestion("What does Spring Boot do?", userId);

        assertNotNull(response);
        assertTrue(response.isGrounded());
        assertEquals("Spring Boot simplifies creating stand-alone Spring applications.", response.getAnswer());
        assertEquals(1, response.getSources().size());

        SourceReference source = response.getSources().get(0);
        assertEquals(docId, source.getDocumentId());
        assertEquals("spring_notes.pdf", source.getOriginalFilename());
        assertEquals(3, source.getPageNumber());
        assertTrue(source.getExcerpt().contains("stand-alone"));
    }

    @Test
    @DisplayName("Similarity search must include user-scoped filter expression")
    void testUserScopedSearchRequest() {
        UUID userId = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        ragChatService.askQuestion("Explain vector embeddings", userId);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest captured = captor.getValue();
        assertEquals("Explain vector embeddings", captured.getQuery());
        assertEquals(4, captured.getTopK());
        assertEquals(0.60, captured.getSimilarityThreshold());
        assertNotNull(captured.getFilterExpression());
        // Verify user filter is present in expression string
        assertTrue(captured.getFilterExpression().toString().contains(userId.toString()));
    }

    @Test
    @DisplayName("Should throw BadRequestException if question is null or blank")
    void testBlankQuestionThrowsBadRequest() {
        UUID userId = UUID.randomUUID();
        assertThrows(BadRequestException.class, () -> ragChatService.askQuestion(null, userId));
        assertThrows(BadRequestException.class, () -> ragChatService.askQuestion("   ", userId));
    }
}
