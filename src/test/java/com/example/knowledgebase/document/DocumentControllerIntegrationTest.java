package com.example.knowledgebase.document;

import com.example.knowledgebase.auth.AuthService;
import com.example.knowledgebase.auth.LoginRequest;
import com.example.knowledgebase.auth.RegisterRequest;
import com.example.knowledgebase.ingestion.IngestionService;
import com.example.knowledgebase.security.JwtService;
import com.example.knowledgebase.user.User;
import com.example.knowledgebase.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private IngestionService ingestionService;

    private String tokenUserA;
    private String tokenUserB;
    private UUID userAId;
    private UUID userBId;

    @BeforeEach
    void setUp() {
        // Register User A
        var userAResponse = authService.register(new RegisterRequest("user_a@example.com", "password123"));
        userAId = userAResponse.getId();
        tokenUserA = authService.login(new LoginRequest("user_a@example.com", "password123")).getAccessToken();

        // Register User B
        var userBResponse = authService.register(new RegisterRequest("user_b@example.com", "password123"));
        userBId = userBResponse.getId();
        tokenUserB = authService.login(new LoginRequest("user_b@example.com", "password123")).getAccessToken();
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint should return 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Verify tenant isolation: User B cannot view or delete User A's document")
    void testTenantIsolationBetweenTwoUsers() throws Exception {
        // Create document owned by User A
        Document docA = new Document(userAId, "java_concurrency.pdf", "application/pdf", 1048576);
        docA.markCompleted(12);
        Document savedDocA = documentRepository.save(docA);

        // 1. User A lists documents -> sees 1 document
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(savedDocA.getId().toString()))
                .andExpect(jsonPath("$[0].originalFilename").value("java_concurrency.pdf"));

        // 2. User B lists documents -> sees 0 documents (strict isolation)
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 3. User B attempts to access User A's document by ID -> 403 Forbidden
        mockMvc.perform(get("/api/documents/" + savedDocA.getId())
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access document '" + savedDocA.getId() + "'."));

        // 4. User B attempts to delete User A's document -> 403 Forbidden
        mockMvc.perform(delete("/api/documents/" + savedDocA.getId())
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));

        // 5. User A successfully deletes their own document -> 204 No Content
        doNothing().when(ingestionService).deleteDocumentVectors(savedDocA.getId());
        mockMvc.perform(delete("/api/documents/" + savedDocA.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNoContent());

        // 6. User A now has 0 documents
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
