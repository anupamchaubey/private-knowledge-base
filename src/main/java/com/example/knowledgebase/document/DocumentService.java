package com.example.knowledgebase.document;

import com.example.knowledgebase.common.BadRequestException;
import com.example.knowledgebase.common.ForbiddenException;
import com.example.knowledgebase.common.ResourceNotFoundException;
import com.example.knowledgebase.ingestion.IngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Service managing document lifecycle, ownership authorization, and indexing orchestration.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final IngestionService ingestionService;

    public DocumentService(DocumentRepository documentRepository, IngestionService ingestionService) {
        this.documentRepository = documentRepository;
        this.ingestionService = ingestionService;
    }

    /**
     * Uploads and immediately indexes a PDF document for the authenticated user.
     * Synchronous indexing ensures immediate availability for question answering
     * while reporting clear failure reasons if parsing or embedding generation fails.
     */
    public DocumentResponse uploadDocument(MultipartFile file, UUID userId) {
        // Step 1: Strict validation of file size, PDF format, and structure
        ingestionService.validatePdfFile(file);

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded file stream: " + e.getMessage());
        }

        // Step 2: Persist initial document entity in PROCESSING state
        Document document = new Document(
                userId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
        document.markProcessing();
        Document savedDoc = documentRepository.save(document);

        // Step 3: Parse, chunk, and index vectors in pgvector
        try {
            int chunkCount = ingestionService.processAndIndexPdf(savedDoc, bytes);
            savedDoc.markCompleted(chunkCount);
            savedDoc = documentRepository.save(savedDoc);
            log.info("Document successfully indexed: id={}, chunks={}", savedDoc.getId(), chunkCount);
        } catch (Exception e) {
            log.error("Failed to index document id={}: {}", savedDoc.getId(), e.getMessage());
            savedDoc.markFailed(e.getMessage());
            documentRepository.save(savedDoc);
            throw e;
        }

        return DocumentResponse.from(savedDoc);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listUserDocuments(UUID userId) {
        return documentRepository.findAllByOwnerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getUserDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document with ID '" + documentId + "' not found."));

        if (!document.getOwnerId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to access document '" + documentId + "'.");
        }

        return DocumentResponse.from(document);
    }

    /**
     * Deletes a document and its indexed vector records.
     * To prevent leaking orphaned vector chunks, vector deletion is executed first.
     */
    @Transactional
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document with ID '" + documentId + "' not found."));

        if (!document.getOwnerId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to delete document '" + documentId + "'.");
        }

        // Step 1: Remove associated vector embeddings from pgvector
        ingestionService.deleteDocumentVectors(documentId);

        // Step 2: Remove relational metadata entity
        documentRepository.delete(document);
        log.info("Document and vectors successfully deleted: id={}, ownerId={}", documentId, userId);
    }
}
