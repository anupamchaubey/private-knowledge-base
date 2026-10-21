package com.example.knowledgebase.document;

import com.example.knowledgebase.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for authenticated PDF document management and ingestion.
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Endpoints for uploading, inspecting, and deleting private documents")
@SecurityRequirement(name = "BearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and index a PDF document",
            description = "Validates the PDF, extracts its text, splits it into chunks, generates vector embeddings, and stores them in pgvector.")
    @ApiResponse(responseCode = "201", description = "Document uploaded and indexed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file or unreadable PDF")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        DocumentResponse response = documentService.uploadDocument(file, userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List documents", description = "Retrieves all indexed documents belonging to the authenticated user.")
    @ApiResponse(responseCode = "200", description = "List of user documents")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<DocumentResponse> documents = documentService.listUserDocuments(userPrincipal.getId());
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID", description = "Retrieves metadata and status of a specific document owned by the user.")
    @ApiResponse(responseCode = "200", description = "Document details")
    @ApiResponse(responseCode = "403", description = "Forbidden - document owned by another user")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        DocumentResponse document = documentService.getUserDocument(id, userPrincipal.getId());
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Permanently removes a document and its associated vector chunks from the vector store.")
    @ApiResponse(responseCode = "204", description = "Document deleted successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - document owned by another user")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        documentService.deleteDocument(id, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }
}
