package com.example.knowledgebase.ingestion;

import com.example.knowledgebase.common.BadRequestException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new IngestionService(vectorStore, 500, 50, 10 * 1024 * 1024);
    }

    @Test
    @DisplayName("Should reject null or empty file")
    void testValidateEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);
        assertThrows(BadRequestException.class, () -> ingestionService.validatePdfFile(emptyFile));
        assertThrows(BadRequestException.class, () -> ingestionService.validatePdfFile(null));
    }

    @Test
    @DisplayName("Should reject non-PDF filename extension")
    void testValidateNonPdfExtension() {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "Hello world".getBytes());
        assertThrows(BadRequestException.class, () -> ingestionService.validatePdfFile(txtFile));
    }

    @Test
    @DisplayName("Should reject invalid file header magic bytes")
    void testValidateInvalidPdfHeader() {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "NOT_A_PDF_CONTENT".getBytes());
        assertThrows(BadRequestException.class, () -> ingestionService.validatePdfFile(fakePdf));
    }

    @Test
    @DisplayName("Should accept valid PDF document")
    void testValidateValidPdf() throws Exception {
        byte[] validPdfBytes;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.showText("Sample test PDF content for ingestion testing.");
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            validPdfBytes = out.toByteArray();
        }

        MockMultipartFile validFile = new MockMultipartFile(
                "file", "valid.pdf", "application/pdf", validPdfBytes);

        assertDoesNotThrow(() -> ingestionService.validatePdfFile(validFile));
    }
}
