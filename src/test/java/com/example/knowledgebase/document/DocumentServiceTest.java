package com.example.knowledgebase.document;

import com.example.knowledgebase.common.ForbiddenException;
import com.example.knowledgebase.common.ResourceNotFoundException;
import com.example.knowledgebase.ingestion.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private IngestionService ingestionService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, ingestionService);
    }

    @Test
    @DisplayName("Upload document should validate, persist PROCESSING, index vectors, and mark COMPLETED")
    void testUploadDocumentSuccess() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "%PDF-1.4 test content".getBytes());

        UUID docId = UUID.randomUUID();
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            if (doc.getId() == null) {
                doc = spy(doc);
                when(doc.getId()).thenReturn(docId);
            }
            return doc;
        });
        when(ingestionService.processAndIndexPdf(any(Document.class), any(byte[].class))).thenReturn(5);

        DocumentResponse response = documentService.uploadDocument(file, userId);

        assertNotNull(response);
        verify(ingestionService).validatePdfFile(file);
        verify(ingestionService).processAndIndexPdf(any(Document.class), any(byte[].class));
        verify(documentRepository, atLeast(2)).save(any(Document.class));
    }

    @Test
    @DisplayName("Get document should forbid access if requested by a different user")
    void testGetDocumentForbiddenForOtherUser() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Document doc = new Document(ownerId, "private.pdf", "application/pdf", 1024);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThrows(ForbiddenException.class, () -> documentService.getUserDocument(docId, otherUserId));
    }

    @Test
    @DisplayName("Delete document should remove vectors before deleting relational record")
    void testDeleteDocumentRemovesVectorsFirst() {
        UUID ownerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        Document doc = new Document(ownerId, "notes.pdf", "application/pdf", 2048);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        documentService.deleteDocument(docId, ownerId);

        InOrder inOrder = inOrder(ingestionService, documentRepository);
        inOrder.verify(ingestionService).deleteDocumentVectors(docId);
        inOrder.verify(documentRepository).delete(doc);
    }

    @Test
    @DisplayName("Delete document should throw ResourceNotFoundException if document does not exist")
    void testDeleteDocumentNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> documentService.deleteDocument(docId, ownerId));
        verify(ingestionService, never()).deleteDocumentVectors(any());
    }
}
