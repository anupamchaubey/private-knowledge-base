package com.example.knowledgebase.document;

import java.time.Instant;
import java.util.UUID;

public class DocumentResponse {

    private final UUID id;
    private final String originalFilename;
    private final String contentType;
    private final long fileSizeBytes;
    private final DocumentStatus status;
    private final int chunkCount;
    private final String errorMessage;
    private final Instant createdAt;
    private final Instant updatedAt;

    public DocumentResponse(UUID id, String originalFilename, String contentType, long fileSizeBytes,
                            DocumentStatus status, int chunkCount, String errorMessage,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = status;
        this.chunkCount = chunkCount;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DocumentResponse from(Document doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getOriginalFilename(),
                doc.getContentType(),
                doc.getFileSizeBytes(),
                doc.getStatus(),
                doc.getChunkCount(),
                doc.getErrorMessage(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
