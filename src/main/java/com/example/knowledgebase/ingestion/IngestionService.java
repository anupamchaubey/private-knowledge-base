package com.example.knowledgebase.ingestion;

import com.example.knowledgebase.common.BadRequestException;
import com.example.knowledgebase.common.IngestionException;
import com.example.knowledgebase.document.Document;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service responsible for validating uploaded PDFs, extracting textual content,
 * chunking text using token-based boundaries, and indexing vector embeddings into pgvector.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final int chunkSize;
    private final long maxFileSizeBytes;

    public IngestionService(
            VectorStore vectorStore,
            @Value("${app.rag.chunk-size-tokens:500}") int chunkSize,
            @Value("${app.upload.max-file-size-bytes:10485760}") long maxFileSizeBytes) {
        this.vectorStore = vectorStore;
        this.chunkSize = chunkSize;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /**
     * Validates that the uploaded multipart file is present, within the size limit,
     * possesses a PDF extension, starts with valid %PDF magic bytes, and parses with PDFBox.
     */
    public void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty. Please choose a valid PDF file.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File exceeds maximum allowed size (" + (maxFileSizeBytes / (1024 * 1024)) + "MB).");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are supported. Found filename: " + originalFilename);
        }

        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 5) {
                throw new BadRequestException("File is too small to be a valid PDF.");
            }

            // Verify %PDF- magic bytes header
            String header = new String(bytes, 0, Math.min(5, bytes.length), StandardCharsets.US_ASCII);
            if (!header.startsWith("%PDF")) {
                throw new BadRequestException("File signature does not match PDF format.");
            }

            // Validate that PDFBox can parse the document structure
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                if (doc.isEncrypted()) {
                    throw new BadRequestException("Password-protected or encrypted PDFs are not supported.");
                }
                if (doc.getNumberOfPages() == 0) {
                    throw new BadRequestException("The uploaded PDF has 0 pages.");
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded PDF file: " + e.getMessage());
        }
    }

    /**
     * Ingestion pipeline:
     * 1. Extracts pages using PagePdfDocumentReader
     * 2. Splits text into chunks using TokenTextSplitter
     * 3. Attaches security and provenance metadata (userId, documentId, filename, page, chunkIndex)
     * 4. Generates embeddings and writes to pgvector via VectorStore
     */
    public int processAndIndexPdf(Document document, byte[] pdfBytes) {
        log.info("Starting ingestion for document id: {}, name: {}", document.getId(), document.getOriginalFilename());

        try {
            ByteArrayResource resource = new ByteArrayResource(pdfBytes) {
                @Override
                public String getFilename() {
                    return document.getOriginalFilename();
                }
            };

            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
            List<org.springframework.ai.document.Document> pages = reader.get();

            if (pages == null || pages.isEmpty()) {
                throw new IngestionException("No readable content found in PDF: " + document.getOriginalFilename());
            }

            // TokenTextSplitter breaks text along natural token boundaries with configurable size
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(chunkSize)
                    .withMinChunkSizeChars(20)
                    .build();

            List<org.springframework.ai.document.Document> rawChunks = splitter.apply(pages);

            if (rawChunks.isEmpty()) {
                throw new IngestionException("PDF contains no extractable text chunks (scanned or image-only PDF).");
            }

            List<org.springframework.ai.document.Document> enrichedChunks = new ArrayList<>();
            for (int i = 0; i < rawChunks.size(); i++) {
                org.springframework.ai.document.Document rawChunk = rawChunks.get(i);
                Map<String, Object> metadata = new HashMap<>(rawChunk.getMetadata());

                // Enforce tenant isolation and provenance tags in vector metadata
                metadata.put("userId", document.getOwnerId().toString());
                metadata.put("documentId", document.getId().toString());
                metadata.put("originalFilename", document.getOriginalFilename());
                metadata.put("chunkIndex", i);

                // Preserve page number where available
                Object pageNumber = metadata.getOrDefault("page_number",
                        metadata.getOrDefault(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER, 1));
                metadata.put("pageNumber", pageNumber != null ? pageNumber : 1);

                org.springframework.ai.document.Document chunk =
                        new org.springframework.ai.document.Document(rawChunk.getText(), metadata);
                enrichedChunks.add(chunk);
            }

            log.info("Storing {} chunks into pgvector for document id: {}", enrichedChunks.size(), document.getId());
            vectorStore.add(enrichedChunks);

            return enrichedChunks.size();
        } catch (IngestionException e) {
            log.error("Ingestion failed for document {}: {}", document.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during PDF ingestion for document {}: {}", document.getId(), e.getMessage(), e);
            throw new IngestionException("Failed to extract and index PDF content: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes all vector records belonging to the given document from pgvector.
     * Enforces defense-in-depth by filtering on both documentId AND userId.
     */
    public void deleteDocumentVectors(UUID documentId, UUID userId) {
        log.info("Deleting vector records for document id: {} and user id: {}", documentId, userId);
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            vectorStore.delete(b.and(
                    b.eq("documentId", documentId.toString()),
                    b.eq("userId", userId.toString())
            ).build());
        } catch (Exception e) {
            log.error("Failed to delete vector records for document id {} (user {}): {}",
                    documentId, userId, e.getMessage(), e);
            throw new IngestionException("Failed to delete associated vector chunks: " + e.getMessage(), e);
        }
    }
}
