package com.example.knowledgebase.chat;

import java.util.UUID;

/**
 * Metadata citation extracted from an actual retrieved chunk.
 * Built strictly on the backend to avoid model hallucinations of sources or page numbers.
 */
public class SourceReference {

    private final UUID documentId;
    private final String originalFilename;
    private final Integer pageNumber;
    private final String excerpt;
    private final Double score;

    public SourceReference(UUID documentId, String originalFilename, Integer pageNumber, String excerpt, Double score) {
        this.documentId = documentId;
        this.originalFilename = originalFilename;
        this.pageNumber = pageNumber;
        this.excerpt = excerpt;
        this.score = score;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public Double getScore() {
        return score;
    }
}
